package com.commander4j.gui.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.commander4j.crypto.KeyGenUtil;
import com.commander4j.email.DistributionPanel;
import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailPanel;
import com.commander4j.email.EmailRecord;
import com.commander4j.gui.jdialog.JDialogAssignPassword;
import com.commander4j.gui.jdialog.JDialogPassword;
import com.commander4j.gui.widgets.JButton4j;
import com.commander4j.gui.widgets.JCheckBox4j;
import com.commander4j.gui.widgets.JComboBox4j;
import com.commander4j.gui.widgets.JLabel4j_std;
import com.commander4j.gui.widgets.JPasswordField4j;
import com.commander4j.gui.widgets.JSpinner4j;
import com.commander4j.gui.widgets.JTextField4j;
import com.commander4j.gui.widgets.JToggleButton4j;
import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschPanel;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.Common;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsGet;
import com.commander4j.settings.SettingsPut;
import com.commander4j.sftp.Start;
import com.commander4j.thread.TransferGET;
import com.commander4j.thread.TransferPUT;
import com.commander4j.util.JUtility;

public class JFrameSFTPTransfer extends JFrame
{

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JToolBar statusPanel;

	private static final int messageType_INFO = 0;
	private static final int messageType_WARN = 1;
	private static final int messageType_ERROR = 2;

	private JTextField4j fld_Title_Common;
	private JTextField4j fld_HostAddress_Common;
	private JTextField4j fld_Port_Common;
	private JTextField4j fld_KnownHostsFile_Common;
	private JTextField4j fld_Username_Common;
	private JCheckBox4j chckbx_checkPrivateKey_Common;
	private JButton4j btn_PrivateKeySelect_Common;
	private JButton4j btn_PublicKeySelect_Common;
	private JButton4j btn_PrivateKeyCreate_Common;
	private JButton4j btn_KnownHostsSelect_Common;
	private JButton4j btn_PrivateKeyPassword;
	private JPasswordField4j fld_applicationPassword;
	private JButton4j btnClearPrivateKeyPassword;

	private JTextField4j fld_PrivateKeyFile_Common;
	private JTextField4j fld_PublicKeyFile_Common;
	private JTextField4j fld_PrivateKeyComment_Common;
	private JPasswordField4j fld_Password_Common;
	private JPasswordField4j fld_PrivateKeyPassword_Common;
	private JCheckBox4j chkbox_KnownHosts_Common;
	private JCheckBox4j chkbox_AddKnownHosts_Common;
	private JCheckBox4j checkBx_EmailEnabled_Common;
	private JComboBox4j<String> comboBox_Authentication_Type_Common;
	private JLabel4j_std messageLabel;

	private JTextField4j fld_Title_Put;
	private JTextField4j fld_LocalFolder_Put;
	private JTextField4j fld_LocalMask_Put;
	private JTextField4j fld_BackupFolder_Put;
	private JTextField4j fld_RemoteFolder_Put;
	private JTextField4j fld_TempFileExtension_Put;
	private JSpinner4j spinner_PollFrequency_Put;
	private JSpinner4j spinner_BackupRetention_Put;
	private JCheckBox4j checkBx_Enabled_Put;
	private JCheckBox4j checkBx_BackupEnable_Put;
	private JButton4j btn_BackupFolderSelect_Put;

	private JTextField4j fld_Title_Get;
	private JTextField4j fld_RemoteFolder_Get;
	private JTextField4j fld_Remote_Mask_Get;
	private JTextField4j fld_LocalFolder_Get;
	private JTextField4j fld_TempFileExtension_Get;
	private JSpinner4j spinner_PollFrequency_Get;
	private JCheckBox4j checkBx_Enabled_Get;

	// private JCheckBox chckbx_PrivateKeyPassword = new JCheckBox("");
	private JCheckBox4j chckbx_PrivateKeyPassword_Common;

	private JTabbedPane tabbedPane;

	private JPanel sftp_common_Tab_Panel;
	private JPanel sftp_put_Tab_Panel;
	private JPanel sftp_get_Tab_Panel;
	private JPanel propertes_Tab_Panel;
	private JPanel email_Tab_Panel;


	private JPanel panel_Email_Scrollable;
	private JPanel panel_Distribution_Scrollable;
	private JPanel panel_Properties_Scrollable;
	private JLogPanel panel_Log_Put_Scrollable;
	private JLogPanel panel_Log_Get_Scrollable;
	private JLogPanel panel_Log_System_Scrollable;

	private JScrollPane scrollPane_System_Log;
	private JScrollPane scrollPane_Put_Log;
	private JScrollPane scrollPane_Get_Log;
	private JScrollPane scrollPane_Properties;
	private JScrollPane scrollPane_Email;
	private JScrollPane scrollPanel_Distribution;

	private int maxLogRows = 100;
	private int actualPutLogRows = 0;
	private int actualGetLogRows = 0;
	private int actualSystemLogRows = 0;

	private SettingsCommon settingsCommon = new SettingsCommon();
	private SettingsPut settingsPut = new SettingsPut();
	private SettingsGet settingsGet = new SettingsGet();
	private HashMap<String, EmailRecord> emailConfig = new HashMap<String, EmailRecord>();
	private HashMap<String, DistributionRecord> distConfig = new HashMap<String, DistributionRecord>();
	private HashMap<String, JschRecord> jschConfig = new HashMap<String, JschRecord>();

	private SettingUtil settingsUtil = new SettingUtil();
	private JUtility util = new JUtility();
	private Dimension btn = new Dimension(32, 32);
	private JSeparator seperator = new JSeparator();
	private JschCommands jcmd = new JschCommands(JschCommands.LogDestination_NoGUI);

	public JFrameSFTPTransfer()
	{
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowListener());

		setTitle("SFTP Transfer (" + Start.version + ")");

		System.setProperty("apple.laf.useScreenMenuBar", "true");
		util.setLookAndFeel("Nimbus");

		settingsCommon = settingsUtil.readSFTPCommonFromXml();
		settingsPut = settingsUtil.readSFTPPutFromXml();
		settingsGet = settingsUtil.readSFTPGetFromXml();
		emailConfig = settingsUtil.readEmailPropertiesFromXml();
		distConfig = settingsUtil.readDistributionListFromXml();
		jschConfig = settingsUtil.readJschPropertiesFromXml();

		if (settingsCommon.applicationPassword.data.equals("") == false)
		{
			boolean success = false;

			int attempt = 1;

			while (attempt <= 3)
			{
				attempt++;

				JDialogPassword password = new JDialogPassword(settingsCommon.applicationPassword.data);
				password.setVisible(true);

				if (password.action.equals("OK"))
				{

					if (password.enteredPassword.equals(settingsCommon.applicationPassword.data))
					{
						success = true;
						break;
					}
				}
				else
				{
					attempt = 4;
					success = false;
				}
			}

			if (success == false)
			{
				System.exit(attempt);
			}

		}

		tabbedPane = new JTabbedPane();
		tabbedPane.setFocusable(false);

		sftp_common_Tab_Panel = new JPanel();
		sftp_common_Tab_Panel.setLayout(null);
		sftp_common_Tab_Panel.setBorder(null);

		sftp_put_Tab_Panel = new JPanel();
		sftp_put_Tab_Panel.setLayout(null);
		sftp_put_Tab_Panel.setBorder(null);

		sftp_get_Tab_Panel = new JPanel();
		sftp_get_Tab_Panel.setLayout(null);
		sftp_get_Tab_Panel.setBorder(null);

		propertes_Tab_Panel = new JPanel();
		propertes_Tab_Panel.setLayout(null);
		propertes_Tab_Panel.setBorder(null);

		email_Tab_Panel = new JPanel();
		email_Tab_Panel.setLayout(null);
		email_Tab_Panel.setBorder(null);
		

		panel_Log_Put_Scrollable = new JLogPanel();
		panel_Log_Put_Scrollable.setVisible(true);
		panel_Log_Put_Scrollable.setLayout(new BoxLayout(panel_Log_Put_Scrollable, BoxLayout.PAGE_AXIS));

		scrollPane_Put_Log = new JScrollPane(panel_Log_Put_Scrollable);
		scrollPane_Put_Log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		panel_Log_Get_Scrollable = new JLogPanel();
		panel_Log_Get_Scrollable.setVisible(true);
		panel_Log_Get_Scrollable.setLayout(new BoxLayout(panel_Log_Get_Scrollable, BoxLayout.PAGE_AXIS));

		scrollPane_Get_Log = new JScrollPane(panel_Log_Get_Scrollable);
		scrollPane_Get_Log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		panel_Log_System_Scrollable = new JLogPanel();
		panel_Log_System_Scrollable.setVisible(true);
		panel_Log_System_Scrollable.setLayout(new BoxLayout(panel_Log_System_Scrollable,BoxLayout.PAGE_AXIS));
		
		scrollPane_System_Log = new JScrollPane(panel_Log_System_Scrollable);
		scrollPane_System_Log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		setSize(new Dimension(740, 570));

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JToolBar toolBarRight = new JToolBar();
		toolBarRight.setFloatable(false);
		toolBarRight.setOrientation(SwingConstants.VERTICAL);
		toolBarRight.setAlignmentX(SwingConstants.VERTICAL);
		toolBarRight.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.add(toolBarRight, BorderLayout.EAST);

		statusPanel = new JToolBar();
		statusPanel.setFloatable(false);
		statusPanel.setOrientation(SwingConstants.HORIZONTAL);
		statusPanel.setAlignmentX(SwingConstants.HORIZONTAL);
		statusPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		messageLabel = new JLabel4j_std();
		messageLabel.setFont(Common.font_bold);
		messageLabel.setForeground(Color.red);
		messageLabel.setHorizontalAlignment(SwingConstants.LEFT);
		messageLabel.setSize(new Dimension(740, 35));
		messageLabel.setMinimumSize(new Dimension(740, 35));
		statusPanel.add(messageLabel);

		contentPane.add(statusPanel, BorderLayout.SOUTH);

		contentPane.add(statusPanel, BorderLayout.SOUTH);

		toolBarRight.add(seperator);
		toolBarRight.add(seperator);

		JToggleButton4j btnStartStop = new JToggleButton4j(Common.icon_disconnected);
		btnStartStop.setPreferredSize(btn);
		toolBarRight.add(btnStartStop);
		btnStartStop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (btnStartStop.isSelected())
				{
					btnStartStop.setIcon(Common.icon_connected);
					Start.transferPut.requestMode(TransferPUT.Mode_RUN);
					Start.transferGet.requestMode(TransferPUT.Mode_RUN);
				}
				else
				{
					btnStartStop.setIcon(Common.icon_disconnected);
					Start.transferPut.requestMode(TransferPUT.Mode_PAUSE);
					Start.transferGet.requestMode(TransferPUT.Mode_PAUSE);
				}
			}
		});

		tabbedPane.addTab("SFTP", null, (Component) sftp_common_Tab_Panel, "SFTP Settings");
		tabbedPane.addTab("Put Properties", null, (Component) sftp_put_Tab_Panel, "SFTP Put Settings");
		tabbedPane.addTab("Get Properties", null, (Component) sftp_get_Tab_Panel, "SFTP Get Settings");
		tabbedPane.addTab("jsch Properties", null, (Component) propertes_Tab_Panel, "SFTP Put");
		tabbedPane.addTab("Email Properties", null, (Component) email_Tab_Panel, "Email Settings");
		tabbedPane.addTab("Put Log", null, scrollPane_Put_Log, "Put Activity Log");
		tabbedPane.addTab("Get Log", null, scrollPane_Get_Log, "Get Activity Log");
		tabbedPane.addTab("System Log",null,scrollPane_System_Log,"System Log");

		contentPane.add(tabbedPane, BorderLayout.CENTER);

		// JSCH PROPERTIES

		scrollPane_Properties = new JScrollPane();
		scrollPane_Properties.setBounds(50, 20, 585, 450);
		propertes_Tab_Panel.add(scrollPane_Properties);

		panel_Properties_Scrollable = new JPanel();
		panel_Properties_Scrollable.setLayout(new BoxLayout(panel_Properties_Scrollable, BoxLayout.Y_AXIS));
		scrollPane_Properties.setViewportView(panel_Properties_Scrollable);

		for (HashMap.Entry<String, JschRecord> entry : jschConfig.entrySet())
		{
			JschPanel ped = new JschPanel();

			ped.fld_Id.setText(entry.getKey());

			ped.fld_Value.setText(entry.getValue().value);
			ped.fld_Encrypted.setSelected(Boolean.valueOf(entry.getValue().encrypted));
			ped.fld_Enabled.setSelected(Boolean.valueOf(entry.getValue().enabled));

			panel_Properties_Scrollable.add(ped);
		}

		JLabel4j_std lbl_jschProperty_Title = new JLabel4j_std("Property                                                   Value                                                                                   Encrypt  Enable");
		lbl_jschProperty_Title.setFont(Common.font_bold);
		lbl_jschProperty_Title.setBounds(scrollPane_Properties.getX() + 5, scrollPane_Properties.getY() - 20, 570, 30);
		propertes_Tab_Panel.add(lbl_jschProperty_Title);

		// EMAIL

		scrollPane_Email = new JScrollPane();
		scrollPane_Email.setBounds(50, 20, 585, 250);
		email_Tab_Panel.add(scrollPane_Email);

		panel_Email_Scrollable = new JPanel();
		panel_Email_Scrollable.setLayout(new BoxLayout(panel_Email_Scrollable, BoxLayout.Y_AXIS));
		scrollPane_Email.setViewportView(panel_Email_Scrollable);

		for (HashMap.Entry<String, EmailRecord> entry : emailConfig.entrySet())
		{
			EmailPanel ped = new EmailPanel();

			ped.fld_Property.setText(entry.getKey());

			ped.fld_Value.setText(entry.getValue().value);
			ped.fld_Encrypted.setSelected(Boolean.valueOf(entry.getValue().encrypted));
			ped.fld_Enabled.setSelected(Boolean.valueOf(entry.getValue().enabled));

			panel_Email_Scrollable.add(ped);
		}

		JLabel4j_std lbl_EmailProperty_Title = new JLabel4j_std("Property                                                   Value                                                                                   Encrypt  Enable");
		lbl_EmailProperty_Title.setFont(Common.font_bold);
		lbl_EmailProperty_Title.setBounds(scrollPane_Email.getX() + 5, scrollPane_Email.getY() - 20, 570, 30);
		email_Tab_Panel.add(lbl_EmailProperty_Title);

		// DISTRIBUTION

		scrollPanel_Distribution = new JScrollPane();
		scrollPanel_Distribution.setBounds(50, 290, 585, 170);
		email_Tab_Panel.add(scrollPanel_Distribution);

		panel_Distribution_Scrollable = new JPanel();
		panel_Distribution_Scrollable.setLayout(new BoxLayout(panel_Distribution_Scrollable, BoxLayout.Y_AXIS));
		scrollPanel_Distribution.setViewportView(panel_Distribution_Scrollable);

		for (HashMap.Entry<String, DistributionRecord> entry : distConfig.entrySet())
		{
			DistributionPanel dl = new DistributionPanel();

			dl.fld_ListID.setText(entry.getKey());

			dl.fld_Address.setText(entry.getValue().addressList);
			dl.fld_MaxFrequency.setValue(entry.getValue().maxFrequencyMins);
			dl.fld_Enabled.setSelected(Boolean.valueOf(entry.getValue().enabled));

			panel_Distribution_Scrollable.add(dl);
		}

		JLabel4j_std lbl_Distribution_Title = new JLabel4j_std("ID                     Address List                                                                                                    Frequency        Enable");
		lbl_Distribution_Title.setFont(Common.font_bold);
		lbl_Distribution_Title.setBounds(scrollPanel_Distribution.getX() + 5, scrollPanel_Distribution.getY() - 20, 570, 30);
		email_Tab_Panel.add(lbl_Distribution_Title);

		// COMMON

		JLabel4j_std lbl_Title_Common = new JLabel4j_std("Title");
		lbl_Title_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Title_Common.setBounds(6, 10, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Title_Common);

		checkBx_EmailEnabled_Common = new JCheckBox4j("");
		checkBx_EmailEnabled_Common.setSelected(Boolean.valueOf(settingsCommon.emailEnabled.data));
		checkBx_EmailEnabled_Common.setBounds(161, 370, 23, 23);
		sftp_common_Tab_Panel.add(checkBx_EmailEnabled_Common);

		JLabel4j_std lbl_HostAddress_Common = new JLabel4j_std("Host Address");
		lbl_HostAddress_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_HostAddress_Common.setBounds(6, 40, 143, 27);
		sftp_common_Tab_Panel.add(lbl_HostAddress_Common);

		JLabel4j_std lbl_Port_Common = new JLabel4j_std("Port");
		lbl_Port_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Port_Common.setBounds(6, 70, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Port_Common);

		JLabel4j_std lbl_KnownHosts_Common = new JLabel4j_std("Known Hosts File");
		lbl_KnownHosts_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_KnownHosts_Common.setBounds(6, 100, 143, 27);
		sftp_common_Tab_Panel.add(lbl_KnownHosts_Common);

		JLabel4j_std lbl_AddKnownHosts_Common = new JLabel4j_std("Add to Known Hosts File");
		lbl_AddKnownHosts_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_AddKnownHosts_Common.setBounds(6, 130, 143, 27);
		sftp_common_Tab_Panel.add(lbl_AddKnownHosts_Common);

		comboBox_Authentication_Type_Common = new JComboBox4j<String>();
		comboBox_Authentication_Type_Common.setModel(new DefaultComboBoxModel<String>(new String[]
		{ "user password", "user public key" }));
		comboBox_Authentication_Type_Common.setSelectedItem(settingsCommon.authType);
		comboBox_Authentication_Type_Common.setBounds(161, 160, 184, 23);
		sftp_common_Tab_Panel.add(comboBox_Authentication_Type_Common);

		fld_Title_Common = new JTextField4j();
		fld_Title_Common.setBounds(161, 10, 489, 23);
		fld_Title_Common.setText(settingsCommon.title.data);
		sftp_common_Tab_Panel.add(fld_Title_Common);

		fld_HostAddress_Common = new JTextField4j();
		fld_HostAddress_Common.setBounds(161, 40, 122, 23);
		fld_HostAddress_Common.setText(settingsCommon.remoteHost.data);
		sftp_common_Tab_Panel.add(fld_HostAddress_Common);

		fld_Port_Common = new JTextField4j();
		fld_Port_Common.setBounds(161, 70, 122, 23);
		fld_Port_Common.setText(settingsCommon.remotePort.data);
		sftp_common_Tab_Panel.add(fld_Port_Common);

		fld_KnownHostsFile_Common = new JTextField4j();
		fld_KnownHostsFile_Common.setText(settingsCommon.knownHostsFile.data);
		fld_KnownHostsFile_Common.setBounds(184, 100, 466, 23);
		sftp_common_Tab_Panel.add(fld_KnownHostsFile_Common);

		chkbox_AddKnownHosts_Common = new JCheckBox4j("");
		chkbox_AddKnownHosts_Common.setBounds(161, 130, 23, 23);
		chkbox_AddKnownHosts_Common.setSelected(Boolean.valueOf(settingsCommon.autoAddtoKnownHostsFile.data));
		sftp_common_Tab_Panel.add(chkbox_AddKnownHosts_Common);

		chkbox_KnownHosts_Common = new JCheckBox4j("");
		chkbox_KnownHosts_Common.setBounds(161, 100, 23, 23);
		chkbox_KnownHosts_Common.setSelected(Boolean.valueOf(settingsCommon.checkKnownHosts.data));
		fld_KnownHostsFile_Common.setEnabled(Boolean.valueOf(settingsCommon.checkKnownHosts.data));
		sftp_common_Tab_Panel.add(chkbox_KnownHosts_Common);
		chkbox_KnownHosts_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setKnownHostsState(chkbox_KnownHosts_Common.isSelected());
			}
		});

		JLabel4j_std lbl_AuthenticationType_Common = new JLabel4j_std("Authentication Type");
		lbl_AuthenticationType_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_AuthenticationType_Common.setBounds(6, 160, 143, 27);
		sftp_common_Tab_Panel.add(lbl_AuthenticationType_Common);

		fld_Username_Common = new JTextField4j();
		fld_Username_Common.setBounds(161, 190, 202, 23);
		fld_Username_Common.setText(settingsCommon.username.data);
		sftp_common_Tab_Panel.add(fld_Username_Common);

		JLabel4j_std lbl_Username_Common = new JLabel4j_std("Username");
		lbl_Username_Common.setText("SFTP Username");
		lbl_Username_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Username_Common.setBounds(6, 190, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Username_Common);

		fld_Password_Common = new JPasswordField4j();
		fld_Password_Common.setEnabled(false);
		fld_Password_Common.setBounds(161, 220, 202, 23);
		fld_Password_Common.setText(settingsCommon.password.data);
		sftp_common_Tab_Panel.add(fld_Password_Common);

		JLabel4j_std lbl_Password_Common = new JLabel4j_std("Password");
		lbl_Password_Common.setText("SFTP Password");
		lbl_Password_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Password_Common.setBounds(6, 220, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Password_Common);

		///////////////
		fld_PrivateKeyFile_Common = new JTextField4j();
		fld_PrivateKeyFile_Common.setBounds(184, 250, 466, 23);
		fld_PrivateKeyFile_Common.setText(settingsCommon.privateKeyFile.data);
		sftp_common_Tab_Panel.add(fld_PrivateKeyFile_Common);

		///////////////
		fld_PublicKeyFile_Common = new JTextField4j();
		fld_PublicKeyFile_Common.setBounds(184, 310, 466, 23);
		fld_PublicKeyFile_Common.setText(settingsCommon.publicKeyFile.data);
		sftp_common_Tab_Panel.add(fld_PublicKeyFile_Common);

		///////////////
		fld_PrivateKeyComment_Common = new JTextField4j();
		fld_PrivateKeyComment_Common.setBounds(184, 340, 227, 23);
		fld_PrivateKeyComment_Common.setText(settingsCommon.privateKeyComment.data);
		sftp_common_Tab_Panel.add(fld_PrivateKeyComment_Common);

		///////////////
		fld_PrivateKeyPassword_Common = new JPasswordField4j();
		fld_PrivateKeyPassword_Common.setBounds(184, 280, 180, 23);
		fld_PrivateKeyPassword_Common.setText(settingsCommon.privateKeyPassword.data);
		sftp_common_Tab_Panel.add(fld_PrivateKeyPassword_Common);

		///////////////
		chckbx_PrivateKeyPassword_Common = new JCheckBox4j("");
		chckbx_PrivateKeyPassword_Common.setBounds(161, 280, 23, 23);
		chckbx_PrivateKeyPassword_Common.setSelected(Boolean.valueOf(settingsCommon.privateKeyPasswordProtected.data));
		chckbx_PrivateKeyPassword_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setPrivateKeyPasswordState(chckbx_PrivateKeyPassword_Common.isSelected());
			}
		});
		sftp_common_Tab_Panel.add(chckbx_PrivateKeyPassword_Common);
		fld_PrivateKeyPassword_Common.setEnabled(chckbx_PrivateKeyPassword_Common.isSelected());

		///////////////
		chckbx_checkPrivateKey_Common = new JCheckBox4j("");
		chckbx_checkPrivateKey_Common.setBounds(161, 250, 23, 23);
		chckbx_checkPrivateKey_Common.setSelected(Boolean.valueOf(settingsCommon.checkPrivateKeyFile.data));
		chckbx_checkPrivateKey_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setPrivateKeyState(chckbx_checkPrivateKey_Common.isSelected());
			}
		});
		sftp_common_Tab_Panel.add(chckbx_checkPrivateKey_Common);

		///////////////
		///
		JLabel4j_std lbl_PrivateKeyPassword_Common = new JLabel4j_std("Private Key Password");
		lbl_PrivateKeyPassword_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PrivateKeyPassword_Common.setBounds(6, 280, 143, 27);
		sftp_common_Tab_Panel.add(lbl_PrivateKeyPassword_Common);

		JLabel4j_std lbl_PrivateKeyFile_Common = new JLabel4j_std("Private Key File");
		lbl_PrivateKeyFile_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PrivateKeyFile_Common.setBounds(6, 250, 143, 27);
		sftp_common_Tab_Panel.add(lbl_PrivateKeyFile_Common);

		JLabel4j_std lbl_PublicKeyFile_Common = new JLabel4j_std("Public Key File");
		lbl_PublicKeyFile_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PublicKeyFile_Common.setBounds(6, 310, 143, 27);
		sftp_common_Tab_Panel.add(lbl_PublicKeyFile_Common);

		JLabel4j_std lbl_PrivateKeyComment_Common = new JLabel4j_std("Key Comment");
		lbl_PrivateKeyComment_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PrivateKeyComment_Common.setBounds(6, 340, 143, 27);
		sftp_common_Tab_Panel.add(lbl_PrivateKeyComment_Common);

		btn_KnownHostsSelect_Common = new JButton4j(Common.icon_select_folder);
		btn_KnownHostsSelect_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_KnownHostsFile_Common.setText(selectFile(fld_KnownHostsFile_Common.getText(), "Known Hosts", ""));
			}
		});
		btn_KnownHostsSelect_Common.setBounds(648, 95, 30, 30);
		sftp_common_Tab_Panel.add(btn_KnownHostsSelect_Common);

		btn_PrivateKeySelect_Common = new JButton4j(Common.icon_select_folder);
		btn_PrivateKeySelect_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_PrivateKeyFile_Common.setText(selectFile(fld_PrivateKeyFile_Common.getText(), "Private Key (.pem)", "pem"));
			}
		});
		btn_PrivateKeySelect_Common.setBounds(648, 245, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeySelect_Common);

		btn_PublicKeySelect_Common = new JButton4j(Common.icon_select_folder);
		btn_PublicKeySelect_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_PublicKeyFile_Common.setText(selectFile(fld_PublicKeyFile_Common.getText(), "Public Key (.pub)", "pub"));
			}
		});
		btn_PublicKeySelect_Common.setBounds(648, 305, 30, 30);
		sftp_common_Tab_Panel.add(btn_PublicKeySelect_Common);

		btn_PrivateKeyCreate_Common = new JButton4j(Common.icon_create_key);
		btn_PrivateKeyCreate_Common.setToolTipText("Create Private Key and Public Key Files");
		btn_PrivateKeyCreate_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				boolean proceed = true;

				if (chckbx_PrivateKeyPassword_Common.isSelected())
				{
					if (String.valueOf(fld_PrivateKeyPassword_Common.getPassword()).equals(""))
					{
						proceed = false;
						displayMessage("Please provide a password to protect your private key.", messageType_ERROR);
						JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, "Password option enabled but password is blank.", "Error", JOptionPane.ERROR_MESSAGE);

					}
				}

				if (proceed)
				{
					File testPrivateKeyFile = new File(fld_PrivateKeyFile_Common.getText());
					if (testPrivateKeyFile.exists())
					{

						if (testPrivateKeyFile.isFile())
						{
							fld_PrivateKeyFile_Common.requestFocus();
							
							displayMessage("Danger - do you want to overwrite the existing private key ?", messageType_WARN);

							int confirm = JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, "Overwrite " + fld_PrivateKeyFile_Common.getText() + " ?", "Confirm", JOptionPane.YES_NO_OPTION);

							if (confirm == 1)
							{
								proceed = false;
							}
						}

						if (testPrivateKeyFile.isDirectory())
						{
							proceed = false;
							
							fld_PrivateKeyFile_Common.requestFocus();
							
							displayMessage("You need to specify a filename for the private key file .pem", messageType_ERROR);

							JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, fld_PrivateKeyFile_Common.getText() + " is a directory !", "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
				
				if (proceed)
				{
					File testPublicKeyFile = new File(fld_PublicKeyFile_Common.getText());
					if (testPublicKeyFile.exists())
					{

						if (testPublicKeyFile.isFile())
						{
							fld_PublicKeyFile_Common.requestFocus();
							
							displayMessage("Danger - do you want to overwrite the existing private key ?", messageType_WARN);

							int confirm = JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, "Overwrite " + fld_PublicKeyFile_Common.getText() + " ?", "Confirm", JOptionPane.YES_NO_OPTION);

							if (confirm == 1)
							{
								proceed = false;
							}
						}

						if (testPublicKeyFile.isDirectory())
						{
							proceed = false;
							
							fld_PublicKeyFile_Common.requestFocus();
							
							displayMessage("You need to specify a filename for the public key file .pub", messageType_ERROR);

							JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, fld_PublicKeyFile_Common.getText() + " is a directory !", "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}

				clearMessage();

				if (proceed)
				{
					char[] pass = fld_PrivateKeyPassword_Common.getPassword();
					Path priv = Path.of(fld_PrivateKeyFile_Common.getText());
					Path pub = Path.of(fld_PublicKeyFile_Common.getText());

					if (pass.length > 0)
					{
						try
						{
							KeyGenUtil.generateEd25519AndWriteEncryptedPKCS8(priv, pub, pass, fld_PrivateKeyComment_Common.getText());
							writeToSystemLog("Created Private Key",JLogPanel.INFO);
						}
						catch (GeneralSecurityException e1)
						{
							writeToSystemLog("Error creating Private Key :"+e1.getMessage(),JLogPanel.ERROR);
						}
						catch (IOException e1)
						{
							writeToSystemLog("Error creating Private Key :"+e1.getMessage(),JLogPanel.ERROR);
						}
					}
				}
			}
		});
		btn_PrivateKeyCreate_Common.setBounds(425, 275, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeyCreate_Common);

		JLabel4j_std lbl_EmailEnable_Common = new JLabel4j_std("Email Notifications");
		lbl_EmailEnable_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_EmailEnable_Common.setBounds(0, 370, 149, 27);
		sftp_common_Tab_Panel.add(lbl_EmailEnable_Common);

		// PUT TAB

		JLabel4j_std lbl_Enabled_Put = new JLabel4j_std("Enabled");
		lbl_Enabled_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Enabled_Put.setBounds(6, 10, 143, 27);
		sftp_put_Tab_Panel.add(lbl_Enabled_Put);

		JLabel4j_std lbl_Title_Put = new JLabel4j_std("Title");
		lbl_Title_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Title_Put.setBounds(6, 40, 143, 27);
		sftp_put_Tab_Panel.add(lbl_Title_Put);

		checkBx_Enabled_Put = new JCheckBox4j("");
		checkBx_Enabled_Put.setSelected(Boolean.valueOf(settingsPut.enabled.data));
		checkBx_Enabled_Put.setBounds(161, 10, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_Enabled_Put);

		checkBx_BackupEnable_Put = new JCheckBox4j("");
		checkBx_BackupEnable_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setPutBackupState(checkBx_BackupEnable_Put.isSelected());
			}
		});
		checkBx_BackupEnable_Put.setSelected(Boolean.valueOf(settingsPut.backupEnabled.data));
		checkBx_BackupEnable_Put.setBounds(161, 130, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_BackupEnable_Put);

		fld_Title_Put = new JTextField4j();
		fld_Title_Put.setBounds(161, 40, 489, 23);
		fld_Title_Put.setText(settingsPut.title.data);
		sftp_put_Tab_Panel.add(fld_Title_Put);

		fld_LocalFolder_Put = new JTextField4j();
		fld_LocalFolder_Put.setBounds(161, 70, 489, 23);
		fld_LocalFolder_Put.setText(settingsPut.localDir.data);
		sftp_put_Tab_Panel.add(fld_LocalFolder_Put);

		JLabel4j_std lbl_LocalFolder_Put = new JLabel4j_std("Local Folder");
		lbl_LocalFolder_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalFolder_Put.setBounds(6, 70, 143, 27);
		sftp_put_Tab_Panel.add(lbl_LocalFolder_Put);

		fld_LocalMask_Put = new JTextField4j();
		fld_LocalMask_Put.setBounds(161, 100, 184, 23);
		fld_LocalMask_Put.setText(settingsPut.localFileMask.data);
		sftp_put_Tab_Panel.add(fld_LocalMask_Put);

		JLabel4j_std lbl_LocalMask_Put = new JLabel4j_std("Local File Mask");
		lbl_LocalMask_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalMask_Put.setBounds(6, 100, 143, 27);
		sftp_put_Tab_Panel.add(lbl_LocalMask_Put);

		fld_BackupFolder_Put = new JTextField4j();
		fld_BackupFolder_Put.setBounds(196, 130, 454, 23);
		fld_BackupFolder_Put.setText(settingsPut.backupDir.data);
		sftp_put_Tab_Panel.add(fld_BackupFolder_Put);

		JLabel4j_std lbl_BackupFolder_Put = new JLabel4j_std("Backup Folder");
		lbl_BackupFolder_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_BackupFolder_Put.setBounds(6, 130, 143, 27);
		sftp_put_Tab_Panel.add(lbl_BackupFolder_Put);

		JLabel4j_std lbl_BackupRetention_Put = new JLabel4j_std("Backup Retention");
		lbl_BackupRetention_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_BackupRetention_Put.setBounds(6, 160, 143, 27);
		sftp_put_Tab_Panel.add(lbl_BackupRetention_Put);

		spinner_BackupRetention_Put = new JSpinner4j();
		spinner_BackupRetention_Put.setBounds(161, 160, 67, 23);
		spinner_BackupRetention_Put.setValue(Integer.valueOf(settingsPut.backupRetention.data));
		sftp_put_Tab_Panel.add(spinner_BackupRetention_Put);

		spinner_PollFrequency_Put = new JSpinner4j();
		spinner_PollFrequency_Put.setBounds(161, 190, 67, 23);
		spinner_PollFrequency_Put.setValue(Integer.valueOf(settingsPut.pollFrequencySeconds.data));
		sftp_put_Tab_Panel.add(spinner_PollFrequency_Put);

		JLabel4j_std lbl_PollFrequency_Put = new JLabel4j_std("Poll Frequency");
		lbl_PollFrequency_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PollFrequency_Put.setBounds(6, 190, 143, 27);
		sftp_put_Tab_Panel.add(lbl_PollFrequency_Put);

		fld_RemoteFolder_Put = new JTextField4j();
		fld_RemoteFolder_Put.setBounds(161, 220, 489, 23);
		fld_RemoteFolder_Put.setText(settingsPut.remoteDir.data);
		sftp_put_Tab_Panel.add(fld_RemoteFolder_Put);

		JLabel4j_std lbl_RemoteFolder_Put = new JLabel4j_std("Remote Folder");
		lbl_RemoteFolder_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_RemoteFolder_Put.setBounds(6, 220, 143, 27);
		sftp_put_Tab_Panel.add(lbl_RemoteFolder_Put);

		JLabel4j_std lbl_TempFileExtension_Put = new JLabel4j_std("Temporary File Extension");
		lbl_TempFileExtension_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_TempFileExtension_Put.setBounds(0, 250, 149, 27);
		sftp_put_Tab_Panel.add(lbl_TempFileExtension_Put);

		fld_TempFileExtension_Put = new JTextField4j();
		fld_TempFileExtension_Put.setBounds(161, 250, 184, 23);
		fld_TempFileExtension_Put.setText(settingsPut.tempFileExtension.data);
		sftp_put_Tab_Panel.add(fld_TempFileExtension_Put);

		JButton4j btn_LocalFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_LocalFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_LocalFolder_Put.setText(selectFolder(fld_LocalFolder_Put.getText()));
			}
		});
		btn_LocalFolderSelect_Put.setBounds(648, 65, 30, 30);
		sftp_put_Tab_Panel.add(btn_LocalFolderSelect_Put);

		btn_BackupFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_BackupFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_BackupFolder_Put.setText(selectFolder(fld_BackupFolder_Put.getText()));
			}
		});
		btn_BackupFolderSelect_Put.setBounds(648, 125, 30, 30);
		sftp_put_Tab_Panel.add(btn_BackupFolderSelect_Put);

		JButton4j btn_RemoteFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_RemoteFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_RemoteFolder_Put.setText(jcmd.viewTree(settingsCommon, JFrameSFTPTransfer.this, "/", fld_RemoteFolder_Put.getText()));
			}
		});
		btn_RemoteFolderSelect_Put.setBounds(648, 215, 30, 30);
		sftp_put_Tab_Panel.add(btn_RemoteFolderSelect_Put);

		// GET TAB

		JLabel4j_std lbl_Enabled_Get = new JLabel4j_std("Enabled");
		lbl_Enabled_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Enabled_Get.setBounds(6, 10, 143, 27);
		sftp_get_Tab_Panel.add(lbl_Enabled_Get);

		checkBx_Enabled_Get = new JCheckBox4j("");
		checkBx_Enabled_Get.setSelected(Boolean.valueOf(settingsGet.enabled.data));
		checkBx_Enabled_Get.setBounds(161, 10, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_Enabled_Get);

		fld_LocalFolder_Get = new JTextField4j();
		fld_LocalFolder_Get.setBounds(161, 160, 489, 23);
		fld_LocalFolder_Get.setText(settingsGet.localDir.data);
		sftp_get_Tab_Panel.add(fld_LocalFolder_Get);

		JLabel4j_std lbl_LocalFolder_Get = new JLabel4j_std("Local Folder");
		lbl_LocalFolder_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalFolder_Get.setBounds(6, 160, 143, 27);
		sftp_get_Tab_Panel.add(lbl_LocalFolder_Get);

		fld_Remote_Mask_Get = new JTextField4j();
		fld_Remote_Mask_Get.setBounds(161, 100, 184, 23);
		fld_Remote_Mask_Get.setText(settingsGet.remoteFileMask.data);
		sftp_get_Tab_Panel.add(fld_Remote_Mask_Get);

		JLabel4j_std lbl_RemoteMask_Get = new JLabel4j_std("Local File Mask");
		lbl_RemoteMask_Get.setText("Remote File Mask");
		lbl_RemoteMask_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_RemoteMask_Get.setBounds(6, 100, 143, 27);
		sftp_get_Tab_Panel.add(lbl_RemoteMask_Get);

		spinner_PollFrequency_Get = new JSpinner4j();
		spinner_PollFrequency_Get.setBounds(161, 130, 67, 23);
		spinner_PollFrequency_Get.setValue(Integer.valueOf(settingsGet.pollFrequencySeconds.data));
		sftp_get_Tab_Panel.add(spinner_PollFrequency_Get);

		JLabel4j_std lbl_PollFrequency_Get = new JLabel4j_std("Poll Frequency");
		lbl_PollFrequency_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PollFrequency_Get.setBounds(6, 130, 143, 27);
		sftp_get_Tab_Panel.add(lbl_PollFrequency_Get);

		fld_Title_Get = new JTextField4j();
		fld_Title_Get.setBounds(161, 40, 489, 23);
		fld_Title_Get.setText(settingsGet.title.data);
		sftp_get_Tab_Panel.add(fld_Title_Get);

		fld_RemoteFolder_Get = new JTextField4j();
		fld_RemoteFolder_Get.setBounds(161, 70, 489, 23);
		fld_RemoteFolder_Get.setText(settingsGet.remoteDir.data);
		sftp_get_Tab_Panel.add(fld_RemoteFolder_Get);

		JLabel4j_std lbl_Title_Get = new JLabel4j_std("Title");
		lbl_Title_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Title_Get.setBounds(6, 40, 143, 27);
		sftp_get_Tab_Panel.add(lbl_Title_Get);

		JLabel4j_std lbl_RemoteFolder_Get = new JLabel4j_std("Remote Folder");
		lbl_RemoteFolder_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_RemoteFolder_Get.setBounds(6, 70, 143, 27);
		sftp_get_Tab_Panel.add(lbl_RemoteFolder_Get);

		JLabel4j_std lbl_TempFileExtension_Get = new JLabel4j_std("Temporary File Extension");
		lbl_TempFileExtension_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_TempFileExtension_Get.setBounds(0, 190, 149, 27);
		sftp_get_Tab_Panel.add(lbl_TempFileExtension_Get);

		fld_TempFileExtension_Get = new JTextField4j();
		fld_TempFileExtension_Get.setBounds(161, 190, 184, 23);
		fld_TempFileExtension_Get.setText(settingsGet.tempFileExtension.data);
		sftp_get_Tab_Panel.add(fld_TempFileExtension_Get);

		JButton4j btn_LocalFolderSelect_Get = new JButton4j(Common.icon_select_folder);
		btn_LocalFolderSelect_Get.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_LocalFolder_Get.setText(selectFolder(fld_LocalFolder_Get.getText()));
			}
		});
		btn_LocalFolderSelect_Get.setBounds(648, 155, 30, 30);
		sftp_get_Tab_Panel.add(btn_LocalFolderSelect_Get);

		JButton4j btn_RemoteFolderSelect_Get = new JButton4j(Common.icon_select_folder);
		btn_RemoteFolderSelect_Get.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_RemoteFolder_Get.setText(jcmd.viewTree(settingsCommon, JFrameSFTPTransfer.this, "/", fld_RemoteFolder_Get.getText()));
			}
		});
		btn_RemoteFolderSelect_Get.setBounds(648, 65, 30, 30);
		sftp_get_Tab_Panel.add(btn_RemoteFolderSelect_Get);

		// *******

		JButton4j btn_Save = new JButton4j(Common.icon_save);
		btn_Save.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				settingsCommon.authType.data = comboBox_Authentication_Type_Common.getSelectedItem().toString();

				settingsCommon.checkKnownHosts.data = String.valueOf(chkbox_KnownHosts_Common.isSelected());
				settingsCommon.emailEnabled.data = String.valueOf(checkBx_EmailEnabled_Common.isSelected());
				settingsCommon.knownHostsFile.data = fld_KnownHostsFile_Common.getText();
				settingsCommon.autoAddtoKnownHostsFile.data = String.valueOf(chkbox_AddKnownHosts_Common.isSelected());

				char[] passwordChars = fld_Password_Common.getPassword();
				String passwordText = new String(passwordChars);
				settingsCommon.password.data = passwordText;
				settingsCommon.password.encrypted = "true";

				settingsCommon.checkPrivateKeyFile.data = String.valueOf(chckbx_checkPrivateKey_Common.isSelected());
				settingsCommon.privateKeyFile.data = fld_PrivateKeyFile_Common.getText();
				settingsCommon.privateKeyComment.data = fld_PrivateKeyComment_Common.getText();
				settingsCommon.publicKeyFile.data = fld_PublicKeyFile_Common.getText();
				passwordChars = fld_PrivateKeyPassword_Common.getPassword();
				passwordText = new String(passwordChars);
				settingsCommon.privateKeyPassword.data = passwordText;
				settingsCommon.privateKeyPassword.encrypted = "true";
				settingsCommon.privateKeyPasswordProtected.data = String.valueOf(chckbx_PrivateKeyPassword_Common.isSelected());

				settingsCommon.remoteHost.data = fld_HostAddress_Common.getText();
				settingsCommon.remotePort.data = fld_Port_Common.getText();

				settingsCommon.title.data = fld_Title_Common.getText();
				settingsCommon.username.data = fld_Username_Common.getText();
				settingsCommon.applicationPassword.data = new String(fld_applicationPassword.getPassword());

				writeToSystemLog("Saving connection settings to sftp_common.xml",JLogPanel.INFO);
				settingsUtil.saveSFTPCommonToXml(settingsCommon);

				settingsPut.enabled.data = String.valueOf(checkBx_Enabled_Put.isSelected());
				settingsPut.remoteDir.data = fld_RemoteFolder_Put.getText();
				settingsPut.pollFrequencySeconds.data = spinner_PollFrequency_Put.getValue().toString();
				settingsPut.localDir.data = fld_LocalFolder_Put.getText();
				settingsPut.localFileMask.data = fld_LocalMask_Put.getText();
				settingsPut.backupEnabled.data = String.valueOf(checkBx_BackupEnable_Put.isSelected());
				settingsPut.backupRetention.data = String.valueOf(spinner_BackupRetention_Put.getValue());
				settingsPut.backupDir.data = fld_BackupFolder_Put.getText();
				settingsPut.tempFileExtension.data = fld_TempFileExtension_Put.getText();
				settingsPut.title.data = fld_Title_Put.getText();

				writeToSystemLog("Saving PUT settings to sftp_put.xml",JLogPanel.INFO);
				settingsUtil.saveSFTPPutToXml(settingsPut);

				settingsGet.enabled.data = String.valueOf(checkBx_Enabled_Get.isSelected());
				settingsGet.remoteDir.data = fld_RemoteFolder_Get.getText();
				settingsGet.pollFrequencySeconds.data = spinner_PollFrequency_Get.getValue().toString();
				settingsGet.localDir.data = fld_LocalFolder_Get.getText();
				settingsGet.remoteFileMask.data = fld_Remote_Mask_Get.getText();
				settingsGet.tempFileExtension.data = fld_TempFileExtension_Get.getText();
				settingsGet.title.data = fld_Title_Get.getText();

				writeToSystemLog("Saving GET settings to sftp_put.xml",JLogPanel.INFO);
				settingsUtil.saveSFTPGetToXml(settingsGet);

				// update email config and distribution lists from panel data
				// here.

				emailConfig.clear();
				for (Component comp : panel_Email_Scrollable.getComponents())
				{

					if (comp instanceof EmailPanel)
					{
						EmailPanel x = (EmailPanel) comp;

						emailConfig.put(x.fld_Property.getText(), new EmailRecord(x.fld_Property.getText(), x.fld_Value.getText(), x.fld_Encrypted.isSelected(), x.fld_Enabled.isSelected()));

					}
				}

				distConfig.clear();
				for (Component comp : panel_Distribution_Scrollable.getComponents())
				{

					if (comp instanceof DistributionPanel)
					{
						DistributionPanel x = (DistributionPanel) comp;

						distConfig.put(x.fld_ListID.getText(), new DistributionRecord(x.fld_ListID.getText(), x.fld_Address.getText(), Long.valueOf(x.fld_MaxFrequency.getValue().toString()), x.fld_Enabled.isSelected()));

					}
				}

				jschConfig.clear();
				for (Component comp : panel_Properties_Scrollable.getComponents())
				{

					if (comp instanceof JschPanel)
					{
						JschPanel x = (JschPanel) comp;

						jschConfig.put(x.fld_Id.getText(), new JschRecord(x.fld_Id.getText(), x.fld_Value.getText(), x.fld_Encrypted.isSelected(), x.fld_Enabled.isSelected()));

					}
				}

				writeToSystemLog("Saving email properties to email_properties.xml",JLogPanel.INFO);
				settingsUtil.saveEmailPropertiesToXml(emailConfig);
				
				writeToSystemLog("Saving email distribution lists to email_distribution_properties.xml",JLogPanel.INFO);
				settingsUtil.saveEmailDistributionListToXml(distConfig);
				
				writeToSystemLog("Saving Java jsch library properties to jsch_properties.xml",JLogPanel.INFO);
				settingsUtil.saveJschPropertiesToXml(jschConfig);
				
				writeToSystemLog("Notifying threads of new configuration.",JLogPanel.INFO);
				Start.emailthread.loadSmtpPropertie();
				Start.transferPut.requestMode(TransferPUT.Mode_CONFIG_UPDATE);
				Start.transferGet.requestMode(TransferGET.Mode_CONFIG_UPDATE);

			}
		});
		btn_Save.setPreferredSize(btn);
		toolBarRight.add(btn_Save);

		JButton4j btn_Help = new JButton4j(Common.icon_help);
		btn_Help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

			}
		});
		btn_Help.setPreferredSize(btn);
		toolBarRight.add(btn_Help);

		JButton4j btn_Close = new JButton4j(Common.icon_exit);
		btn_Close.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				if (confirmExit())
				{
					Start.requestServiceShutdown();

					System.exit(0);
				}
			}
		});
		btn_Close.setPreferredSize(btn);
		toolBarRight.add(btn_Close);

		fld_Title_Common.requestFocus();
		fld_Title_Common.setCaretPosition(fld_Title_Common.getText().length());

		JLabel4j_std lbl_AppPassword = new JLabel4j_std();
		lbl_AppPassword.setBounds(0, 400, 149, 22);
		sftp_common_Tab_Panel.add(lbl_AppPassword);
		lbl_AppPassword.setText("SettingsPassword");
		lbl_AppPassword.setHorizontalAlignment(SwingConstants.RIGHT);

		fld_applicationPassword = new JPasswordField4j();
		fld_applicationPassword.setBounds(161, 400, 202, 23);
		sftp_common_Tab_Panel.add(fld_applicationPassword);
		fld_applicationPassword.setText((String) null);
		fld_applicationPassword.setEnabled(false);
		fld_applicationPassword.setText(settingsCommon.applicationPassword.data);

		JButton4j btnPassword = new JButton4j(Common.icon_password);
		btnPassword.setBounds(365, 395, 30, 30);
		sftp_common_Tab_Panel.add(btnPassword);
		btnPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JDialogAssignPassword changePassword = new JDialogAssignPassword();
				changePassword.setVisible(true);
				fld_applicationPassword.setText(changePassword.enteredPassword);
				settingsCommon.applicationPassword.data = changePassword.enteredPassword;
			}
		});
		btnPassword.setToolTipText("Assign Application Password");

		JButton4j btnClearPassword = new JButton4j();
		btnClearPassword.setBounds(395, 395, 30, 30);
		sftp_common_Tab_Panel.add(btnClearPassword);
		btnClearPassword.setIcon(Common.icon_erase);
		btnClearPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_applicationPassword.setText("");
				settingsCommon.applicationPassword.data = "";
			}
		});
		btnClearPassword.setToolTipText("Clear Applicatiion Password");

		btnClearPrivateKeyPassword = new JButton4j();
		btnClearPrivateKeyPassword.setBounds(395, 275, 30, 30);
		sftp_common_Tab_Panel.add(btnClearPrivateKeyPassword);
		btnClearPrivateKeyPassword.setIcon(Common.icon_erase);
		btnClearPrivateKeyPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_PrivateKeyPassword_Common.setText("");
				settingsCommon.privateKeyPassword.data = "";
			}
		});
		btnClearPrivateKeyPassword.setToolTipText("Clear Private Key Password");

		JButton4j btnClearFTPassword = new JButton4j();
		btnClearFTPassword.setToolTipText("Clear SFTP user password");
		btnClearFTPassword.setBounds(395, 215, 30, 30);
		sftp_common_Tab_Panel.add(btnClearFTPassword);
		btnClearFTPassword.setIcon(Common.icon_erase);
		btnClearFTPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_Password_Common.setText("");
				settingsCommon.password.data = "";
			}
		});

		JButton4j btnSFTPPassword = new JButton4j(Common.icon_password);
		btnSFTPPassword.setToolTipText("Assign SFTP user password");
		btnSFTPPassword.setBounds(365, 215, 30, 30);
		sftp_common_Tab_Panel.add(btnSFTPPassword);
		btnSFTPPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JDialogAssignPassword changePassword = new JDialogAssignPassword();
				changePassword.setVisible(true);
				fld_PrivateKeyPassword_Common.setText(changePassword.enteredPassword);
				settingsCommon.privateKeyPassword.data = changePassword.enteredPassword;
			}
		});

		btn_PrivateKeyPassword = new JButton4j(Common.icon_password);
		btn_PrivateKeyPassword.setToolTipText("Assign Password for Private Key");
		btn_PrivateKeyPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				JDialogAssignPassword changePassword = new JDialogAssignPassword();
				changePassword.setVisible(true);

				fld_PrivateKeyPassword_Common.setText(changePassword.enteredPassword);

				settingsCommon.privateKeyPassword.data = changePassword.enteredPassword;
			}
		});
		btn_PrivateKeyPassword.setBounds(365, 275, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeyPassword);

		setPrivateKeyState(Boolean.valueOf(settingsCommon.checkPrivateKeyFile.data));
		setPrivateKeyPasswordState(chckbx_PrivateKeyPassword_Common.isSelected());
		setKnownHostsState(chkbox_KnownHosts_Common.isSelected());
		setPutBackupState(checkBx_BackupEnable_Put.isSelected());

		setLocationRelativeTo(null);

		int widthadjustment = util.getOSWidthAdjustment();
		int heightadjustment = util.getOSHeightAdjustment();

		GraphicsDevice gd = util.getGraphicsDevice();

		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		Rectangle screenBounds = gc.getBounds();

		setBounds(screenBounds.x + ((screenBounds.width - JFrameSFTPTransfer.this.getWidth()) / 2), screenBounds.y + ((screenBounds.height - JFrameSFTPTransfer.this.getHeight()) / 2), JFrameSFTPTransfer.this.getWidth() + widthadjustment,
				JFrameSFTPTransfer.this.getHeight() + heightadjustment);
		setVisible(true);

	}

	public boolean confirmExit()
	{
		boolean result = false;

		int question = JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, "Exit application ?", "Confirm", JOptionPane.YES_NO_OPTION, 0, Common.icon_confirm);

		if (question == 0)
		{
			result = true;
		}
		return result;
	}

	class WindowListener extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			if (confirmExit())
			{
				Start.requestServiceShutdown();

				System.exit(0);
			}
		}

	}
	
	public void writeToLog(int logDestination,String logdata, int level)
	{
		switch (logDestination)
		{
		case JschCommands.LogDestination_NoGUI:
			break;
		case JschCommands.LogDestination_PUT:
			writeToPutLog(logdata, level);
			break;
		case JschCommands.LogDestination_GET:
			writeToGetLog(logdata, level);
			break;
		case JschCommands.LogDestination_SYS:
			writeToSystemLog(logdata,level);
			break;
		}
	}

	private void writeToPutLog(String logdata, int level)
	{
		actualPutLogRows++;

		panel_Log_Put_Scrollable.addLog(logdata,level);

		SwingUtilities.invokeLater(() -> {

			if (actualPutLogRows > maxLogRows)
			{
				panel_Log_Put_Scrollable.remove(0);
				actualPutLogRows = maxLogRows;
			}

			JScrollBar vertical = scrollPane_Put_Log.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());

			panel_Log_Put_Scrollable.revalidate();
			panel_Log_Put_Scrollable.repaint();
		});
	}

	private void writeToGetLog(String logdata, int level)
	{
		actualGetLogRows++;

		SwingUtilities.invokeLater(() -> {

			panel_Log_Get_Scrollable.addLog(logdata,level);

			if (actualGetLogRows > maxLogRows)
			{
				panel_Log_Get_Scrollable.remove(0);
				actualGetLogRows = maxLogRows;
			}

			JScrollBar vertical = scrollPane_Get_Log.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());

			panel_Log_Get_Scrollable.revalidate();
			panel_Log_Get_Scrollable.repaint();
		});
	}
	
	private void writeToSystemLog(String logdata, int level)
	{
		actualSystemLogRows++;
		

		SwingUtilities.invokeLater(() -> {
			
			panel_Log_System_Scrollable.addLog(logdata,level);

			if (actualSystemLogRows > maxLogRows)
			{
				panel_Log_System_Scrollable.remove(0);
				actualSystemLogRows = maxLogRows;
			}

			JScrollBar vertical = scrollPane_System_Log.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());

			panel_Log_System_Scrollable.revalidate();
			panel_Log_System_Scrollable.repaint();
		});
	}

	private void setPrivateKeyState(boolean state)
	{
		fld_PrivateKeyFile_Common.setEnabled(state);
		fld_PublicKeyFile_Common.setEnabled(state);
		fld_PrivateKeyComment_Common.setEnabled(state);
		btn_PrivateKeySelect_Common.setEnabled(state);
		btnClearPrivateKeyPassword.setEnabled(state);
		btn_PublicKeySelect_Common.setEnabled(state);
		btn_PrivateKeyCreate_Common.setEnabled(state);
		btn_PrivateKeyPassword.setEnabled(state);
		fld_PrivateKeyPassword_Common.setEnabled(state);
		chckbx_PrivateKeyPassword_Common.setEnabled(state);
	}

	private void setPrivateKeyPasswordState(boolean state)
	{
		if (chckbx_checkPrivateKey_Common.isSelected())
		{
			fld_PrivateKeyPassword_Common.setEnabled(state);
			btn_PrivateKeyPassword.setEnabled(state);
		}
	}

	private void setKnownHostsState(boolean state)
	{
		fld_KnownHostsFile_Common.setEnabled(state);
		btn_KnownHostsSelect_Common.setEnabled(state);
		chkbox_AddKnownHosts_Common.setEnabled(state);
	}

	private void setPutBackupState(boolean state)
	{
		fld_BackupFolder_Put.setEnabled(state);
		spinner_BackupRetention_Put.setEnabled(state);
		btn_BackupFolderSelect_Put.setEnabled(state);
	}

	private String selectFile(String currentSelection, String filetype, String fileext)
	{
		Path basePath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path selectedPath = Paths.get(currentSelection).toAbsolutePath().normalize();

		JFileChooser fc = new JFileChooser(selectedPath.toFile());
		fc.setApproveButtonText("Select");
		fc.setMultiSelectionEnabled(false);

		if (fileext.equals("") == false)
		{
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter(filetype, fileext));
		}

		int returnVal = fc.showOpenDialog(JFrameSFTPTransfer.this);

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			selectedPath = fc.getSelectedFile().toPath().toAbsolutePath().normalize();
		}
		else
		{
			// User cancelled → return the original selection unchanged
			return currentSelection;
		}

		// Check if selectedPath is inside basePath
		if (selectedPath.startsWith(basePath))
		{
			Path relativePath = basePath.relativize(selectedPath);
			return "./" + relativePath.toString().replace("\\", "/");
		}
		else
		{
			// Outside → return absolute path
			return selectedPath.toString();
		}
	}

	private String toProjectAwarePath(Path basePath, Path selectedPath)
	{
		if (selectedPath.startsWith(basePath))
		{
			Path rel = basePath.relativize(selectedPath);
			String relStr = rel.toString().replace("\\", "/");
			return relStr.isEmpty() ? "./" : "./" + relStr;
		}
		else
		{
			return selectedPath.toString();
		}
	}

	private String selectFolder(String currentSelection)
	{
		Path basePath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path initialPath = Paths.get(currentSelection).toAbsolutePath().normalize();

		// Start the chooser at a sensible place (if a file path is passed, use
		// its parent)
		Path chooserStart = Files.isDirectory(initialPath) ? initialPath : (initialPath.getParent() != null ? initialPath.getParent() : basePath);

		JFileChooser fc = new JFileChooser(chooserStart.toFile());
		fc.setDialogTitle("Select Folder");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setApproveButtonText("Select");
		fc.setMultiSelectionEnabled(false);

		int returnVal = fc.showOpenDialog(JFrameSFTPTransfer.this);

		if (returnVal != JFileChooser.APPROVE_OPTION)
		{
			// User cancelled → return the original selection unchanged
			return currentSelection;
		}

		Path selectedPath = fc.getSelectedFile().toPath().toAbsolutePath().normalize();
		return toProjectAwarePath(basePath, selectedPath);
	}

	public String selectFile(String defaultFolder, String defaultFilename, String filetype, String fileext)
	{
		JFileChooser fileChooser = new JFileChooser();

		// Set default folder if valid
		File folder = new File(defaultFolder);
		if (folder.exists() && folder.isDirectory())
		{
			fileChooser.setCurrentDirectory(folder);
		}

		// Preselect default filename
		fileChooser.setSelectedFile(new File(folder, defaultFilename));

		fileChooser.setDialogTitle("Save As");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(filetype, fileext));

		int userSelection = fileChooser.showSaveDialog(null);

		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File fileToSave = fileChooser.getSelectedFile();

			// Ensure .txt extension if missing
			if (!fileToSave.getName().toLowerCase().endsWith(".pem"))
			{
				fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".pem");
			}

			return fileToSave.getAbsolutePath();
		}

		// Cancel pressed → return default
		return new File(folder, defaultFilename).getAbsolutePath();
	}

	private void clearMessage()
	{
		displayMessage("", messageType_INFO);
	}

	private void displayMessage(String msg, int msgType)
	{
		messageLabel.setText(msg);

		switch (msgType)
		{
		case messageType_INFO:
			messageLabel.setForeground(Color.BLACK);
			break;
		case messageType_WARN:
			messageLabel.setForeground(Color.BLUE);
			break;
		case messageType_ERROR:
			messageLabel.setForeground(Color.RED);
			util.errorBeep();
			break;
		}
	}

}
