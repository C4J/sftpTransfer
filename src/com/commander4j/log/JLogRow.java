package com.commander4j.log;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.commander4j.gui.widgets.JLabel4j_std;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one log row: [time] [message].
 * Fixed height (30px), fills width of scrollpane.
 */
public class JLogRow extends JPanel {
    private static final long serialVersionUID = 1L;
	private static final int ROW_HEIGHT = 20;
    private static final int TIME_COL_WIDTH = 50;
    
	
	private Color Color_NORMAL = Color.BLUE;
	private Color Color_WARN = new Color(220,88,42);
	private Color Color_ERROR = Color.RED;
	private Color Color_INFO = Color.BLACK;
	private Color Color_DIRECTORY = new Color(0,153,0);
    
	LocalTime now = LocalTime.now();
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	String formattedTime = now.format(formatter);

    public JLogRow(String message,int logType) {
    	
        super(new BorderLayout(15, 0));
        setOpaque(true);
        setBackground(UIManager.getColor("List.background"));
        setBorder(new EmptyBorder(2, 2, 2, 2));

        // Time column (fixed width)
        JLabel4j_std timeLbl = new JLabel4j_std(formattedTime);
        timeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        Dimension timeSize = new Dimension(TIME_COL_WIDTH, ROW_HEIGHT);
        timeLbl.setPreferredSize(timeSize);
        timeLbl.setMinimumSize(timeSize);
        timeLbl.setMaximumSize(timeSize);

        // Message column (grows)
        JLabel4j_std msgLbl = new JLabel4j_std(message);
        msgLbl.setVerticalAlignment(SwingConstants.CENTER);
        
		switch (logType)
		{
		case JLogPanel.NORMAL:
			msgLbl.setForeground(Color_NORMAL);
			break;
		case JLogPanel.WARN:
			msgLbl.setForeground(Color_WARN);
			break;
		case JLogPanel.ERROR:
			msgLbl.setForeground(Color_ERROR);
			break;
		case JLogPanel.INFO:
			msgLbl.setForeground(Color_INFO);
			break;
		case JLogPanel.DIRECTORY:
			msgLbl.setForeground(Color_DIRECTORY);
			break;	
		}

        add(timeLbl, BorderLayout.WEST);
        add(msgLbl, BorderLayout.CENTER);

        // Stretch to width, lock height
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(0, ROW_HEIGHT));
        setMinimumSize(new Dimension(0, ROW_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
    }
}
