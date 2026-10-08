package com.commander4j.web;

/**
 * Minimal JSON support — just enough for sftpTransfer's small, fixed payloads, so the
 * project keeps its "zero new jars" promise (no Jackson/Gson on the classpath).
 *
 * <p>{@link Obj} and {@link Arr} are tiny append-only builders. Ported from
 * {@code util_modbusBridge}, with the request-body parsers dropped: the web
 * front end is read-only, so nothing here ever reads JSON.
 */
final class Json
{
	private Json()
	{
	}

	static Obj obj()
	{
		return new Obj();
	}

	static Arr arr()
	{
		return new Arr();
	}

	/** Append-only JSON object builder. {@code build()} is non-mutating and re-callable. */
	static final class Obj
	{
		private final StringBuilder sb = new StringBuilder("{");
		private boolean first = true;

		private void key(String k)
		{
			if (!first)
			{
				sb.append(',');
			}
			first = false;
			sb.append('"').append(escape(k)).append("\":");
		}

		Obj put(String k, String v)
		{
			key(k);
			sb.append(v == null ? "null" : '"' + escape(v) + '"');
			return this;
		}

		Obj put(String k, long v)
		{
			key(k);
			sb.append(v);
			return this;
		}

		Obj put(String k, boolean v)
		{
			key(k);
			sb.append(v);
			return this;
		}

		/** Embeds an already-serialised JSON fragment (object or array) as the value. */
		Obj putRaw(String k, String json)
		{
			key(k);
			sb.append(json);
			return this;
		}

		String build()
		{
			return sb + "}";
		}

		@Override
		public String toString()
		{
			return build();
		}
	}

	/** Append-only JSON array builder. */
	static final class Arr
	{
		private final StringBuilder sb = new StringBuilder("[");
		private boolean first = true;

		private void sep()
		{
			if (!first)
			{
				sb.append(',');
			}
			first = false;
		}

		Arr add(Obj o)
		{
			sep();
			sb.append(o.build());
			return this;
		}

		Arr add(String s)
		{
			sep();
			sb.append('"').append(escape(s)).append('"');
			return this;
		}

		String build()
		{
			return sb + "]";
		}

		@Override
		public String toString()
		{
			return build();
		}
	}

	static String escape(String s)
	{
		StringBuilder out = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default ->
				{
					if (c < 0x20)
					{
						out.append(String.format("\\u%04x", (int) c));
					}
					else
					{
						out.append(c);
					}
				}
			}
		}
		return out.toString();
	}
}
