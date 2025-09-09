package com.commander4j.log;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A vertical stack of LogRowPanel entries which lives inside a JScrollPane.
 * - Tracks viewport width (no horizontal scrollbar).
 * - Keeps only the last maxRows entries.
 * - Scrolls to bottom as new rows are added.
 */
public class JLogPanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
	private int maxRows;
	
	public static final int NORMAL = 0;
	public static final int WARN = 1;
	public static final int ERROR = 2;
	public static final int INFO = 3;
	public static final int DIRECTORY = 4;
	
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
        add(new JLogRow(message,logType));

        // remove oldest entries if over limit
        while (getComponentCount() > maxRows) {
            remove(0);
        }
        revalidate();
        repaint();

        // scroll to newest row
        SwingUtilities.invokeLater(() -> {
            int last = getComponentCount() - 1;
            if (last >= 0) {
                JComponent lastComp = (JComponent) getComponent(last);
                lastComp.scrollRectToVisible(lastComp.getBounds());
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
