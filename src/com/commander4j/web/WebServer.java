package com.commander4j.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.commander4j.sftp.Start;
import com.commander4j.thread.TransferGET;
import com.commander4j.thread.TransferPUT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded HTTP front end on the JDK's {@code com.sun.net.httpserver} (no extra
 * jars), carried over from c4j_labelserver4j. It exists so the three log
 * windows can be watched from a browser when sftpTransfer runs as a service
 * and has no window of its own.
 *
 * <p><b>Read-only.</b> Nothing here can start, pause, apply or transfer
 * anything - the endpoints observe and no more. There is no login, so the port
 * should only be reachable from the LAN the server sits on.
 *
 * <p><b>Never fatal.</b> Transfers are the product; this is diagnostics.
 * {@link Start} treats a failure to bind as a logged warning and carries on.
 *
 * <p>The page itself is a resource inside the jar (index.html next to this
 * class), so nothing new has to be shipped or mounted by the installer.
 *
 * <p>The executor is a daemon-threaded cached pool: the default executor pins
 * one handler thread per open connection, which deadlocks once SSE holds a
 * connection open. Daemon threads also keep an exit from hanging on the pool.
 */
public final class WebServer
{
	/** Used when the port setting is missing or unreadable. */
	public static final int DEFAULT_PORT = 8080;

	private static final String[] DEST_NAMES = new String[] { "", "put", "get", "sys" };

	private final int port;
	private final HttpServer http;
	private final SseHub logHub;

	public WebServer(int port) throws IOException
	{
		this.port = port;
		this.logHub = new SseHub("log", null);

		http = HttpServer.create(new InetSocketAddress(port), 0);
		http.setExecutor(Executors.newCachedThreadPool(daemonFactory()));
		http.createContext("/api/status", routeSafely(this::handleStatus));
		http.createContext("/api/log", routeSafely(this::handleLog));
		// SSE handlers bypass routeSafely (which would close the exchange) and hold the
		// handler thread for the connection's lifetime.
		http.createContext("/events", sseHandler(logHub));
		http.createContext("/", routeSafely(this::handleStatic));
	}

	public int getPort()
	{
		return port;
	}

	public void start()
	{
		LogHub.setListener(entry -> logHub.broadcast(entryJson(entry).build()));
		http.start();
	}

	public void stop()
	{
		LogHub.setListener(null);
		logHub.stop();
		http.stop(0);
	}

	/** The port setting as a number, or the default when it is blank or not a number. */
	public static int parsePort(String value, int fallback)
	{
		try
		{
			int result = Integer.parseInt(value.trim());

			if ((result >= 1) && (result <= 65535))
			{
				return result;
			}
		}
		catch (Exception e)
		{
			// blank in a config file written before the setting existed, or not a number
		}

		return fallback;
	}

	// --- routing ------------------------------------------------------------------------

	@FunctionalInterface
	private interface Route
	{
		void handle(HttpExchange ex) throws Exception;
	}

	/** Wraps a route so any uncaught exception becomes a 500 instead of a dropped socket. */
	private com.sun.net.httpserver.HttpHandler routeSafely(Route route)
	{
		return ex ->
		{
			try (ex)
			{
				route.handle(ex);
			}
			catch (Exception e)
			{
				trySendError(ex, 500, "internal error");
			}
		};
	}

	/**
	 * Handler for the SSE stream. Deliberately does <em>not</em> use
	 * {@link #routeSafely}: its try-with-resources would close the exchange
	 * immediately, ending the stream. {@link SseHub#register} keeps the
	 * connection open and closes the exchange itself.
	 */
	private com.sun.net.httpserver.HttpHandler sseHandler(SseHub hub)
	{
		return ex ->
		{
			if (!ex.getRequestMethod().equals("GET"))
			{
				try (ex)
				{
					sendError(ex, 405, "method not allowed");
				}
				return;
			}
			try
			{
				hub.register(ex);
			}
			catch (Exception e)
			{
				ex.close();
			}
		};
	}

	// --- /api/status --------------------------------------------------------------------

	private void handleStatus(HttpExchange ex) throws Exception
	{
		if (!ex.getRequestMethod().equals("GET"))
		{
			sendError(ex, 405, "method not allowed");
			return;
		}

		TransferPUT put = Start.transferPut;
		TransferGET get = Start.transferGet;

		sendJson(ex, 200, Json.obj()
			.put("appName", "SFTP Transfer")
			.put("appVersion", Start.version)
			.put("title", Start.webTitle == null ? "" : Start.webTitle)
			.put("serverTime", System.currentTimeMillis())
			.put("putState", threadState(put == null ? -1 : put.getRunMode(), put != null && put.isAlive()))
			.put("getState", threadState(get == null ? -1 : get.getRunMode(), get != null && get.isAlive()))
			.build());
	}

	private static String threadState(int runMode, boolean alive)
	{
		if (!alive)
		{
			return "stopped";
		}

		switch (runMode)
		{
		case TransferPUT.Mode_RUN:
			return "running";
		case TransferPUT.Mode_PAUSE:
			return "paused";
		default:
			return "stopping";
		}
	}

	// --- /api/log -----------------------------------------------------------------------

	/**
	 * {@code /api/log?dest=put|get|sys[&after=seq]} - the backlog of one window,
	 * oldest first; the page then follows via {@code /events}.
	 */
	private void handleLog(HttpExchange ex) throws Exception
	{
		if (!ex.getRequestMethod().equals("GET"))
		{
			sendError(ex, 405, "method not allowed");
			return;
		}

		int destination = -1;
		long after = 0;

		String query = ex.getRequestURI().getQuery();

		if (query != null)
		{
			for (String pair : query.split("&"))
			{
				int eq = pair.indexOf('=');
				String key = eq < 0 ? pair : pair.substring(0, eq);
				String value = eq < 0 ? "" : pair.substring(eq + 1);

				if (key.equals("dest"))
				{
					destination = destinationFor(value);
				}
				else if (key.equals("after"))
				{
					try
					{
						after = Long.parseLong(value);
					}
					catch (NumberFormatException ignored)
					{
						// keep 0
					}
				}
			}
		}

		if (destination < 0)
		{
			sendError(ex, 400, "dest must be put, get or sys");
			return;
		}

		Json.Arr arr = Json.arr();
		List<LogHub.Entry> entries = LogHub.backlog(destination, after);

		for (LogHub.Entry entry : entries)
		{
			arr.add(entryJson(entry));
		}

		sendJson(ex, 200, Json.obj().put("dest", DEST_NAMES[destination]).putRaw("entries", arr.build()).build());
	}

	private static int destinationFor(String name)
	{
		for (int i = 1; i < DEST_NAMES.length; i++)
		{
			if (DEST_NAMES[i].equals(name))
			{
				return i;
			}
		}

		return -1;
	}

	private static Json.Obj entryJson(LogHub.Entry entry)
	{
		return Json.obj()
			.put("dest", DEST_NAMES[entry.destination])
			.put("seq", entry.seq)
			.put("time", entry.time)
			.put("level", entry.level)
			.put("msg", entry.message);
	}

	// --- the page -----------------------------------------------------------------------

	private void handleStatic(HttpExchange ex) throws Exception
	{
		if (!ex.getRequestMethod().equals("GET"))
		{
			sendError(ex, 405, "method not allowed");
			return;
		}

		String path = ex.getRequestURI().getPath();

		if (!(path.equals("/") || path.equals("/index.html")))
		{
			sendError(ex, 404, "not found");
			return;
		}

		byte[] body;

		try (InputStream in = WebServer.class.getResourceAsStream("index.html"))
		{
			if (in == null)
			{
				sendError(ex, 404, "index.html is missing from the jar");
				return;
			}

			body = in.readAllBytes();
		}

		ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
		// Revalidate on every use so a browser never serves a stale page after an upgrade.
		ex.getResponseHeaders().set("Cache-Control", "no-cache");
		ex.sendResponseHeaders(200, body.length);

		try (OutputStream os = ex.getResponseBody())
		{
			os.write(body);
		}
	}

	// --- helpers ------------------------------------------------------------------------

	private void sendJson(HttpExchange ex, int code, String json) throws IOException
	{
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.getResponseHeaders().set("Cache-Control", "no-store");
		ex.sendResponseHeaders(code, body.length);

		try (OutputStream os = ex.getResponseBody())
		{
			os.write(body);
		}
	}

	private void sendError(HttpExchange ex, int code, String message) throws IOException
	{
		sendJson(ex, code, Json.obj().put("error", message).build());
	}

	private void trySendError(HttpExchange ex, int code, String message)
	{
		try
		{
			sendError(ex, code, message);
		}
		catch (IOException ignored)
		{
			// response already started or socket gone; nothing to do
		}
	}

	private static ThreadFactory daemonFactory()
	{
		AtomicInteger n = new AtomicInteger(1);
		return r ->
		{
			Thread t = new Thread(r, "webserver-http-" + n.getAndIncrement());
			t.setDaemon(true);
			return t;
		};
	}
}
