package com.commander4j.log;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.commander4j.gui.widgets.JLabel4j_std;
import com.commander4j.settings.Common;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one log row: [time] [message].
 * Fills the width of the scroll pane; the message wraps, so the row is as
 * tall as the wrapped text needs (one line = 20px, as before).
 * A row carries the tag of the log it came from (Put, Get or System - see
 * JschCommands.LogDestination_*) so the composite log panel can hide or show
 * each source.
 */
public class JLogRow extends JPanel {
    private static final long serialVersionUID = 1L;
	private static final int ROW_HEIGHT = 20;
    private static final int TIME_COL_WIDTH = 50;
    private static final int COLUMN_GAP = 15;
    
	
	private Color Color_NORMAL = Color.BLUE;
	private Color Color_WARN = new Color(220,88,42);
	private Color Color_ERROR = Color.RED;
	private Color Color_INFO = Color.BLACK;
	private Color Color_DIRECTORY = new Color(0,153,0);
	private Color Color_DEBUG = Color.GRAY;
    
	LocalTime now = LocalTime.now();
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	String formattedTime = now.format(formatter);

	private final JTextArea msgArea;
	private final int tag;

    public JLogRow(String message,int logType) {
    	this(message, logType, 0);
    }

    public JLogRow(String message,int logType,int tag) {
    	
        super(new BorderLayout(COLUMN_GAP, 0));
        this.tag = tag;
        setOpaque(true);
        setBackground(UIManager.getColor("List.background"));
        setBorder(new EmptyBorder(2, 2, 2, 2));

        // Time column (fixed width, beside the first line of the message)
        JLabel4j_std timeLbl = new JLabel4j_std(formattedTime);
        timeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        timeLbl.setVerticalAlignment(SwingConstants.TOP);
        Dimension timeSize = new Dimension(TIME_COL_WIDTH, ROW_HEIGHT - 4);
        timeLbl.setPreferredSize(timeSize);
        timeLbl.setMinimumSize(timeSize);
        timeLbl.setMaximumSize(timeSize);

        // Message column (grows, wraps at the row width so long paths are not cut off; text can be selected and copied)
        msgArea = new JTextArea(message);
        msgArea.setFont(Common.font_std);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setEditable(false);
        msgArea.setOpaque(false);
        msgArea.setBorder(null);
        msgArea.setMargin(new Insets(0, 0, 0, 0));
        
		switch (logType)
		{
		case JLogPanel.NORMAL:
			msgArea.setForeground(Color_NORMAL);
			break;
		case JLogPanel.WARN:
			msgArea.setForeground(Color_WARN);
			break;
		case JLogPanel.ERROR:
			msgArea.setForeground(Color_ERROR);
			break;
		case JLogPanel.INFO:
			msgArea.setForeground(Color_INFO);
			break;
		case JLogPanel.DIRECTORY:
			msgArea.setForeground(Color_DIRECTORY);
			break;	
		case JLogPanel.DEBUG:
			msgArea.setForeground(Color_DEBUG);
			break;
		}

        add(timeLbl, BorderLayout.WEST);
        add(msgArea, BorderLayout.CENTER);

        // Stretch to width; the height follows the wrapped text (see getPreferredSize).
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** The log this row came from (JschCommands.LogDestination_*; 0 = untagged). */
    public int getTag()
    {
        return tag;
    }

    /** The time the row was added, as shown in its first column. */
    public String getTime()
    {
        return formattedTime;
    }

    /** The row's text, prefix included. */
    public String getMessage()
    {
        return msgArea.getText();
    }

    /**
     * The width the message column will be given by the BorderLayout once the
     * row is laid out at the width of the log panel (which tracks the viewport).
     */
    private int messageWidth()
    {
        Container parent = getParent();

        int width = (parent == null) ? getWidth() : parent.getWidth();

        if ((parent instanceof JComponent) && (width > 0))
        {
            Insets parentInsets = ((JComponent) parent).getInsets();
            width = width - parentInsets.left - parentInsets.right;
        }

        Insets insets = getInsets();

        return width - insets.left - insets.right - TIME_COL_WIDTH - COLUMN_GAP;
    }

    /**
     * BoxLayout asks for this before the row has a width, so the height is
     * worked out from the panel's width rather than the row's own. The
     * message area is sized to that width first so its preferred height is the
     * wrapped one.
     */
    @Override
    public Dimension getPreferredSize()
    {
        Insets insets = getInsets();
        int height = ROW_HEIGHT - insets.top - insets.bottom;

        int width = messageWidth();

        if (width > 0)
        {
            msgArea.setSize(width, Short.MAX_VALUE);
            height = Math.max(height, msgArea.getPreferredSize().height);
        }

        return new Dimension(0, height + insets.top + insets.bottom);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
