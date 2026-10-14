package com.commander4j.log;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A vertical stack of LogRowPanel entries which lives inside a JScrollPane.
 * - Tracks viewport width (no horizontal scrollbar).
 * - Keeps only the last maxRows entries.
 * - Scrolls to bottom as new rows are added.
 * - Rows carry a tag (the log they came from); a tag can be hidden and shown
 *   again, which hides the rows already on screen as well as the ones still
 *   to come. Hidden rows stay in the stack and count towards maxRows.
 */
public class JLogPanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
	private int maxRows;

	// Tags currently hidden - everything is shown unless it is in here.
	private final java.util.Set<Integer> hiddenTags = new java.util.HashSet<Integer>();
	
	// Row types - each has its own colour in JLogRow.
	public static final int NORMAL = 0;
	public static final int WARN = 1;
	public static final int ERROR = 2;
	public static final int INFO = 3;
	public static final int DIRECTORY = 4;
	// Polling detail (the ls of each remote folder on every poll) - only shown at the Debug display level.
	public static final int DEBUG = 5;

	// Display levels, from the least to the most detail. A window shows a row when
	// the row's type is at or above the level - see shows().
	public static final int LEVEL_ERRORS = 0;
	public static final int LEVEL_WARNINGS = 1;
	public static final int LEVEL_NORMAL = 2;
	public static final int LEVEL_DEBUG = 3;

	public static final String[] LEVEL_NAMES = new String[] { "Errors only", "Warnings and errors", "Normal", "Debug" };

	/** The display level with this name; Normal for blank or unknown. */
	public static int levelOf(String name)
	{
		for (int i = 0; i < LEVEL_NAMES.length; i++)
		{
			if (LEVEL_NAMES[i].equals(name))
			{
				return i;
			}
		}

		return LEVEL_NORMAL;
	}

	/**
	 * Whether a row of the given type is shown at a display level. Normal
	 * covers everything the transfer does (commands, listings, information);
	 * Debug adds the polling detail; the two levels below Normal keep only the
	 * problems.
	 */
	public static boolean shows(int level, int logType)
	{
		switch (logType)
		{
		case ERROR:
			return true;
		case WARN:
			return level >= LEVEL_WARNINGS;
		case DEBUG:
			return level >= LEVEL_DEBUG;
		default:
			return level >= LEVEL_NORMAL;
		}
	}
	
    public JLogPanel() {
        this(100);
    }

    public JLogPanel(int maxRows) {
        this.maxRows = maxRows;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setBackground(UIManager.getColor("Panel.background"));
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public void addLog(String message,int logType) {
        addLog(message, logType, 0);
    }

    /**
     * Adds a row tagged with the log it came from. A row for a hidden tag is
     * added invisible, so BoxLayout leaves it out until the tag is shown again.
     */
    public void addLog(String message,int logType,int tag) {
        JLogRow row = new JLogRow(message, logType, tag);
        row.setVisible(hiddenTags.contains(tag) == false);
        add(row);

        // remove oldest entries if over limit
        while (getComponentCount() > maxRows) {
            remove(0);
        }
        revalidate();
        repaint();

        scrollToLastVisible();
    }

    /** Empties the window. The log file and the web viewer keep their lines. */
    public void clear() {
        removeAll();
        revalidate();
        repaint();
    }

    /**
     * The rows as shown - the ones whose log is ticked, oldest first - one
     * line each: the time, then the text with its [PUT]/[GET]/[SYS] prefix.
     */
    public java.util.List<String> visibleLines() {
        java.util.List<String> lines = new java.util.ArrayList<String>();

        for (Component c : getComponents()) {
            if ((c instanceof JLogRow) && c.isVisible()) {
                JLogRow row = (JLogRow) c;
                lines.add(row.getTime() + " " + row.getMessage());
            }
        }

        return lines;
    }

    /** Whether rows with this tag are currently shown. */
    public boolean isTagVisible(int tag) {
        return hiddenTags.contains(tag) == false;
    }

    /**
     * Hides or shows every row with the tag, now and from now on. BoxLayout
     * skips invisible rows, so the stack closes up around them.
     */
    public void setTagVisible(int tag, boolean visible) {
        if (visible) {
            hiddenTags.remove(tag);
        } else {
            hiddenTags.add(tag);
        }

        for (Component c : getComponents()) {
            if ((c instanceof JLogRow) && (((JLogRow) c).getTag() == tag)) {
                c.setVisible(visible);
            }
        }

        revalidate();
        repaint();

        scrollToLastVisible();
    }

    /**
     * Scrolls to the newest row that is shown. A hidden row has no bounds, so
     * scrolling to it would jump the view to the top.
     */
    private void scrollToLastVisible() {
        SwingUtilities.invokeLater(() -> {
            for (int i = getComponentCount() - 1; i >= 0; i--) {
                JComponent c = (JComponent) getComponent(i);
                if (c.isVisible()) {
                    // The panel's bottom inset comes along, so the view really ends at the bottom.
                    c.scrollRectToVisible(new Rectangle(0, 0, c.getWidth(), c.getHeight() + getInsets().bottom));
                    break;
                }
            }
        });
    }

    // --- Scrollable interface ---
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 30;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
