package com.commander4j.email;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import com.commander4j.gui.widgets.JCheckBox4j;
import com.commander4j.gui.widgets.JLabel4j_std;
import com.commander4j.gui.widgets.JSpinner4j;
import com.commander4j.gui.widgets.JTextField4j;

public class DistributionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;
	public JTextField4j fld_ListID;
	public JTextField4j fld_Address;
	public JSpinner4j fld_MaxFrequency;
	public JCheckBox4j fld_Enabled;

	public static int rowheight = 23;
	public static int idwidth = 70;
	public static int addresswidth = 355;
	public static int maxfrequencywidth = 80;
	public static int enabledwidth = 27;
	private JLabel seperator = new JLabel("  ");
	
	public int totalwidth = idwidth+addresswidth+maxfrequencywidth+enabledwidth+(seperator.getWidth()*4)+15;

	/**
	 * Create the panel.
	 */
	public DistributionPanel()
	{
		setBackground(new Color(255, 255, 255));
		setBorder(new LineBorder(new Color(0, 0, 0)));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setPreferredSize(new Dimension(totalwidth,rowheight));
		
		fld_ListID = new JTextField4j();
		fld_ListID.setHorizontalAlignment(SwingConstants.LEADING);
		fld_ListID.setEnabled(true);
		fld_ListID.setEditable(true);
		fld_ListID.setPreferredSize(new Dimension(idwidth,rowheight));
		fld_ListID.setMaximumSize(new Dimension(idwidth,rowheight));
		
		add(fld_ListID);
		add(new JLabel4j_std("  "));

		fld_Address = new JTextField4j();
		fld_Address.setPreferredSize(new Dimension(addresswidth,rowheight));
		fld_Address.setMaximumSize(new Dimension(addresswidth,rowheight));
		
		add(fld_Address);
		add(new JLabel4j_std("   "));
		
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 120, 1);
		
		fld_MaxFrequency = new JSpinner4j();
		fld_MaxFrequency.setModel(model);
		fld_MaxFrequency.setPreferredSize(new Dimension(maxfrequencywidth,rowheight));
		fld_MaxFrequency.setMaximumSize(new Dimension(maxfrequencywidth,rowheight));
		
		add(fld_MaxFrequency);

		add(new JLabel4j_std("   "));
 
		fld_Enabled = new JCheckBox4j();
		fld_Enabled.setHorizontalAlignment(SwingConstants.CENTER);
		fld_Enabled.setPreferredSize(new Dimension(enabledwidth,rowheight));
		fld_Enabled.setMaximumSize(new Dimension(enabledwidth,rowheight));
		
		add(fld_Enabled);
		
		add(new JLabel4j_std(" "));
	}
}
