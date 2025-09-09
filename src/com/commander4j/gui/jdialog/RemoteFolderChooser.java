package com.commander4j.gui.jdialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.commander4j.gui.widgets.JButton4j;
import com.commander4j.util.JUtility;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public final class RemoteFolderChooser extends JDialog
{

	private static final long serialVersionUID = 1L;
	private final ChannelSftp sftp;
	private final String startFolder;
	private final String initialSelectedFolder;
	private JUtility util = new JUtility();

	private JTree tree;
	private DefaultTreeModel model;
	private JButton4j okBtn;
	private JButton4j cancelBtn;

	private String result; // final selected path or initialSelectedFolder on
							// cancel

	private RemoteFolderChooser(Window owner, ChannelSftp sftp, String startFolder, String initialSelectedFolder)
	{
		super(owner, "Select Remote Folder", ModalityType.APPLICATION_MODAL);
		this.sftp = sftp;
		this.startFolder = normalizeRemoteFolder(startFolder);
		this.initialSelectedFolder = normalizeRemoteFolder((initialSelectedFolder == null || initialSelectedFolder.isBlank()) ? this.startFolder : initialSelectedFolder);
		this.result = this.initialSelectedFolder;

		buildUI();
		loadRootAndMaybePreselect();
		pack();
		setLocationRelativeTo(owner);
		setMinimumSize(new Dimension(520, 420));
		

		int widthadjustment = util.getOSWidthAdjustment();
		int heightadjustment = util.getOSHeightAdjustment();

		GraphicsDevice gd = util.getGraphicsDevice();

		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		Rectangle screenBounds = gc.getBounds();

		setBounds(screenBounds.x + ((screenBounds.width - RemoteFolderChooser.this.getWidth()) / 2), screenBounds.y + ((screenBounds.height - RemoteFolderChooser.this.getHeight()) / 2), RemoteFolderChooser.this.getWidth() + widthadjustment,
				RemoteFolderChooser.this.getHeight() + heightadjustment);

	}

	/** Public entry point */
	public static String chooseRemoteFolder(Window owner, ChannelSftp sftp, String startFolder, String initialSelectedFolder)
	{
		RemoteFolderChooser dlg = new RemoteFolderChooser(owner, sftp, startFolder, initialSelectedFolder);
		dlg.setVisible(true); // blocks
		return dlg.result;
	}

	// ---------- UI ----------

	private void buildUI()
	{
		setLayout(new BorderLayout());

		DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(new FolderNode(startFolder, extractName(startFolder), false));
		// add a placeholder child so it shows an expand handle
		rootTreeNode.add(new DefaultMutableTreeNode(new LoadingMarker()));
		model = new DefaultTreeModel(rootTreeNode);

		tree = new JTree(model);

		tree = new JTree(model);
		tree.setRootVisible(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);

		// nicer renderer (folder icons optional; uses defaults otherwise)
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(renderer.getOpenIcon()); // folders only; treat as
														// folder
		renderer.setClosedIcon(UIManager.getIcon("FileView.directoryIcon"));
		renderer.setOpenIcon(UIManager.getIcon("FileView.directoryIcon"));
		tree.setCellRenderer(renderer);

		// Lazy load folders when expanding
		tree.addTreeWillExpandListener(new TreeWillExpandListener()
		{
			@Override
			public void treeWillExpand(TreeExpansionEvent event)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
				if (node.getUserObject() instanceof FolderNode)
				{
					ensureChildrenLoaded((FolderNode) node.getUserObject(), node);
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event)
			{
				/* no-op */ }
		});

		// Double-click to accept
		tree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					onOk();
				}
			}
		});

		// Keyboard: ENTER -> OK, ESC -> Cancel
		tree.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					onOk();
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					onCancel();
			}
		});

		add(new JScrollPane(tree), BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		okBtn = new JButton4j("OK");
		cancelBtn = new JButton4j("Cancel");
		okBtn.addActionListener(e -> onOk());
		cancelBtn.addActionListener(e -> onCancel());
		buttons.add(okBtn);
		buttons.add(cancelBtn);
		add(buttons, BorderLayout.SOUTH);

		// close -> cancel
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				onCancel();
			}
		});
	}

	private void loadRootAndMaybePreselect()
	{
		SwingUtilities.invokeLater(() -> {
			// Expand root to show immediate children
			DefaultMutableTreeNode rootTreeNode = (DefaultMutableTreeNode) model.getRoot();
			ensureChildrenLoaded((FolderNode) rootTreeNode.getUserObject(), rootTreeNode);
			tree.expandPath(new TreePath(model.getPathToRoot(rootTreeNode)));

			// Try to expand to the initial selected folder if it's under the
			// start folder
			if (isUnder(initialSelectedFolder, startFolder))
			{
				expandToPath(initialSelectedFolder);
			}
			else if (!startFolder.equals(initialSelectedFolder))
			{
				// If initial selection is outside start folder, just keep
				// result as-is (cancel will return it)
				// Optionally, you could jump root to that location, but spec
				// says start at specified location.
			}
		});
	}

	// ---------- Actions ----------

	private void onOk()
	{
		TreePath sel = tree.getSelectionPath();
		if (sel != null)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) sel.getLastPathComponent();
			Object uo = node.getUserObject();
			if (uo instanceof FolderNode)
			{
				result = ((FolderNode) uo).fullPath;
				dispose();
				return;
			}
		}
		// If nothing selected, default to startFolder
		result = startFolder;
		dispose();
	}

	private void onCancel()
	{
		// Return initialSelectedFolder on cancel
		result = initialSelectedFolder;
		dispose();
	}

	// ---------- Tree loading helpers ----------

	private void ensureChildrenLoaded(FolderNode folderNode, DefaultMutableTreeNode treeNode)
	{
		if (folderNode.loaded)
			return;

		// Clear "Loading..." placeholder(s)
		removeLoadingMarkers(treeNode);

		List<FolderNode> children;
		try
		{
			children = listSubdirectories(folderNode.fullPath);
		}
		catch (SftpException ex)
		{
			// Show error and leave node as-is (no children)
			JOptionPane.showMessageDialog(this, "Failed to list: " + folderNode.fullPath + "\n" + ex.getMessage(), "SFTP Error", JOptionPane.ERROR_MESSAGE);
			folderNode.loaded = true;
			model.nodeStructureChanged(treeNode);
			return;
		}

		// Add children sorted case-insensitively
		children.sort(Comparator.comparing(fn -> fn.name.toLowerCase(Locale.ROOT)));
		for (FolderNode child : children)
		{
			DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child);
			// add a loading marker so it can be expanded later (if it has
			// children)
			childTreeNode.add(new DefaultMutableTreeNode(new LoadingMarker()));
			treeNode.add(childTreeNode);
		}

		folderNode.loaded = true;
		model.nodeStructureChanged(treeNode);
	}

	private void removeLoadingMarkers(DefaultMutableTreeNode node)
	{
		List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
		Enumeration<?> e = node.children();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) e.nextElement();
			if (child.getUserObject() instanceof LoadingMarker)
			{
				toRemove.add(child);
			}
		}
		for (DefaultMutableTreeNode rm : toRemove)
			node.remove(rm);
	}

	private void expandToPath(String targetFullPath)
	{
		// Iteratively walk from startFolder to targetFullPath, loading each
		// level.
		String base = startFolder.equals("/") ? "" : startFolder;
		String remainder = targetFullPath;
		if (remainder.startsWith("/"))
			remainder = remainder.substring(1);

		List<String> parts = new ArrayList<>();
		if (!base.isEmpty())
		{
			String b = base.startsWith("/") ? base.substring(1) : base;
			if (!b.isEmpty())
				parts.addAll(Arrays.asList(b.split("/")));
		}
		if (!remainder.isEmpty())
			parts.addAll(Arrays.asList(remainder.split("/")));

		DefaultMutableTreeNode cur = (DefaultMutableTreeNode) model.getRoot();
		//StringBuilder curPath = new StringBuilder(startFolder);

		// Special case: root "/"
		if ("/".equals(startFolder))
		{
			parts.removeIf(String::isEmpty);
		}

		for (int i = ("/".equals(startFolder) ? 0 : 1); i < parts.size(); i++)
		{
			String part = parts.get(i);
			if (part.isEmpty())
				continue;

			// Ensure children loaded
			ensureChildrenLoaded((FolderNode) cur.getUserObject(), cur);

			// Find child with matching name
			DefaultMutableTreeNode next = null;
			Enumeration<?> children = cur.children();
			while (children.hasMoreElements())
			{
				DefaultMutableTreeNode c = (DefaultMutableTreeNode) children.nextElement();
				Object uo = c.getUserObject();
				if (uo instanceof FolderNode && ((FolderNode) uo).name.equals(part))
				{
					next = c;
					break;
				}
			}
			if (next == null)
			{
				break; // path component not found; stop here
			}
			cur = next;
			tree.expandPath(new TreePath(model.getPathToRoot(cur)));
		}
		// Select the deepest node we reached
		tree.setSelectionPath(new TreePath(model.getPathToRoot(cur)));
		tree.scrollPathToVisible(new TreePath(model.getPathToRoot(cur)));
	}

	// ---------- SFTP helpers ----------

	private List<FolderNode> listSubdirectories(String parent) throws SftpException
	{
	//	@SuppressWarnings("unchecked")
		Vector<ChannelSftp.LsEntry> entries = sftp.ls(parent);
		List<FolderNode> result = new ArrayList<>();
		for (ChannelSftp.LsEntry e : entries)
		{
			String name = e.getFilename();
			if (".".equals(name) || "..".equals(name))
				continue;

			SftpATTRS attrs = e.getAttrs();
			boolean isDir = attrs != null && attrs.isDir();

			// If it's a symlink, attempt to stat the link target to see if it's
			// a dir
			if (!isDir && attrs != null && attrs.isLink())
			{
				try
				{
					SftpATTRS linkTarget = sftp.stat(parentPath(parent, name));
					isDir = (linkTarget != null && linkTarget.isDir());
				}
				catch (SftpException ignore)
				{
					// Broken link or no permission; treat as non-dir
				}
			}

			if (isDir)
			{
				String full = parentPath(parent, name);
				result.add(new FolderNode(full, name, false));
			}
		}
		return result;
	}

	private static String parentPath(String parent, String name)
	{
		if ("/".equals(parent))
			return "/" + name;
		if (parent.endsWith("/"))
			return parent + name;
		return parent + "/" + name;
	}

	private static boolean isUnder(String path, String base)
	{
		if (base.equals("/"))
			return true; // everything under root
		String p = normalizeRemoteFolder(path);
		String b = normalizeRemoteFolder(base);
		return p.equals(b) || p.startsWith(b.endsWith("/") ? b : (b + "/"));
	}

	private static String normalizeRemoteFolder(String p)
	{
		if (p == null || p.isBlank())
			return "/";
		String s = p.replace('\\', '/').trim();
		// collapse duplicate slashes
		s = s.replaceAll("/{2,}", "/");
		// ensure leading slash
		if (!s.startsWith("/"))
			s = "/" + s;
		// remove trailing slash (except root)
		if (s.length() > 1 && s.endsWith("/"))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	private static String extractName(String fullPath)
	{
		if ("/".equals(fullPath))
			return "/";
		int idx = fullPath.lastIndexOf('/');
		return (idx >= 0 && idx < fullPath.length() - 1) ? fullPath.substring(idx + 1) : fullPath;
	}

	// ---------- Model objects ----------

	private static final class FolderNode
	{
		final String fullPath;
		final String name;
		boolean loaded;

		FolderNode(String fullPath, String name, boolean loaded)
		{
			this.fullPath = fullPath;
			this.name = name;
			this.loaded = loaded;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	private static final class LoadingMarker
	{
		@Override
		public String toString()
		{
			return "Loading...";
		}
	}
}
