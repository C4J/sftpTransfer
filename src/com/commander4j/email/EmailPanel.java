package com.commander4j.email;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import com.commander4j.gui.widgets.JCheckBox4j;
import com.commander4j.gui.widgets.JLabel4j_std;
import com.commander4j.gui.widgets.JTextField4j;

public class EmailPanel extends JPanel
{

	private static final long serialVersionUID = 1L;
	public JTextField4j fld_Property;
	public JTextField4j fld_Value;
	public JCheckBox4j fld_Encrypted;
	public JCheckBox4j fld_Enabled;

	public static int rowheight = 23;
	public static int propertywidth = 200;
	public static int valuewidth = 280;
	public static int encryptedwidth = rowheight;
	public static int enabledwidth = rowheight;
	private JLabel seperator = new JLabel("---");
	public int totalwidth = propertywidth+valuewidth+encryptedwidth+enabledwidth+(seperator.getWidth()*4)+15;

	/**
	 * Create the panel.
	 */
	public EmailPanel()
	{
		setBackground(new Color(255, 255, 255));
		setBorder(new LineBorder(new Color(0, 0, 0)));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setPreferredSize(new Dimension(totalwidth,rowheight));
		
		fld_Property = new JTextField4j();
		fld_Property.setHorizontalAlignment(SwingConstants.LEADING);
		fld_Property.setEnabled(false);
		fld_Property.setEditable(false);
		fld_Property.setPreferredSize(new Dimension(propertywidth,rowheight));
		fld_Property.setMaximumSize(new Dimension(propertywidth,rowheight));
		
		add(fld_Property);
		add(new JLabel4j_std("  "));

		fld_Value = new JTextField4j();
		fld_Value.setPreferredSize(new Dimension(valuewidth,rowheight));
		fld_Value.setMaximumSize(new Dimension(valuewidth,rowheight));
		
		add(fld_Value);
		add(new JLabel4j_std("   "));
		
		fld_Encrypted = new JCheckBox4j();
		fld_Encrypted.setHorizontalAlignment(SwingConstants.CENTER);
		fld_Encrypted.setPreferredSize(new Dimension(encryptedwidth,rowheight));
		fld_Encrypted.setMaximumSize(new Dimension(encryptedwidth,rowheight));
		
		add(fld_Encrypted);

		add(new JLabel4j_std("   "));
 
		fld_Enabled = new JCheckBox4j();
		fld_Enabled.setHorizontalAlignment(SwingConstants.CENTER);
		fld_Enabled.setPreferredSize(new Dimension(enabledwidth,rowheight));
		fld_Enabled.setMaximumSize(new Dimension(enabledwidth,rowheight));
		
		add(fld_Enabled);
		
		add(new JLabel4j_std(" "));
	}
}
