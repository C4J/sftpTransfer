package com.commander4j.gui.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Painter;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import com.commander4j.crypto.KeyGenUtil;
import com.commander4j.email.DistributionPanel;
import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailPanel;
import com.commander4j.email.EmailRecord;
import com.commander4j.email.Notify;
import com.commander4j.gui.jdialog.JDialogAbout;
import com.commander4j.gui.jdialog.JDialogAssignPassword;
import com.commander4j.gui.jdialog.JDialogLicenses;
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
import com.commander4j.settings.ReloadRequest;
import com.commander4j.settings.ServiceLock;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.thread.StatsThread;
import com.commander4j.settings.SettingsGet;
import com.commander4j.settings.SettingsPut;
import com.commander4j.sftp.Start;
import com.commander4j.thread.TransferPUT;
import com.commander4j.util.JSafeFile;
import com.commander4j.util.JUtility;
import com.commander4j.web.WebServer;

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
	private JCheckBox4j checkBx_WebEnabled_Common;
	private JSpinner4j spinner_WebPort_Common;
	private JCheckBox4j checkBx_StatsEnabled_Common;
	private JTextField4j fld_StatsTime_Common;
	private JButton4j btn_TestEmail;
	private JComboBox4j<String> comboBox_Authentication_Type_Common;
	private JLabel4j_std messageLabel;

	// The strip above the status line, shown only while a service is running on this machine.
	private JLabel4j_std serviceBanner;
	private JToggleButton4j btnStartStop;
	private boolean servicePresent = false;
	private boolean serviceProbeFailing = false;

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
	private JCheckBox4j checkBx_SubFolders_Put;
	private JCheckBox4j checkBx_Sync_Put;
	// Hint beside Sync Mode - says what the box does ticked AND what the default (move) does unticked.
	private JLabel4j_std lbl_SyncHint_Put;
	private static final String SYNC_HINT_PUT = "Copy new or changed files and leave the local files in place";
	private static final String MOVE_HINT_PUT = "Move every file, deleting the local copy once it has been sent";
	private JCheckBox4j checkBx_SyncDelete_Put;
	private JCheckBox4j checkBx_SyncDeleteFolders_Put;
	private JCheckBox4j checkBx_Stability_Put;
	private JCheckBox4j checkBx_OverwriteNewer_Put;
	private JSpinner4j spinner_StabilitySeconds_Put;
	private JButton4j btn_BackupFolderSelect_Put;

	private JTextField4j fld_Title_Get;
	private JTextField4j fld_RemoteFolder_Get;
	private JTextField4j fld_Remote_Mask_Get;
	private JTextField4j fld_LocalFolder_Get;
	private JTextField4j fld_TempFileExtension_Get;
	private JSpinner4j spinner_PollFrequency_Get;
	private JCheckBox4j checkBx_Enabled_Get;
	private JCheckBox4j checkBx_SubFolders_Get;
	private JCheckBox4j checkBx_Sync_Get;
	private JLabel4j_std lbl_SyncHint_Get;
	private static final String SYNC_HINT_GET = "Copy new or changed files and leave the remote files in place";
	private static final String MOVE_HINT_GET = "Move every file, deleting the remote copy once it has been received";
	private JCheckBox4j checkBx_SyncDelete_Get;
	private JCheckBox4j checkBx_SyncDeleteFolders_Get;
	private JCheckBox4j checkBx_Stability_Get;
	private JCheckBox4j checkBx_OverwriteNewer_Get;
	private JSpinner4j spinner_StabilitySeconds_Get;

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
	// One log panel for Put, Get and System, to the right of the settings tabs.
	private JLogPanel panel_Log_Scrollable;
	private JScrollPane scrollPane_Log;
	private JSplitPane splitPane;
	private JCheckBox4j checkBx_ShowSystem_Log;
	private JCheckBox4j checkBx_ShowPut_Log;
	private JCheckBox4j checkBx_ShowGet_Log;
	// Two bars under the log strips, one per direction, for the file in flight.
	// Put (upload) in orange, Get (download) in blue, both on the same flat
	// painter. Swing thread only.
	private JProgressBar progressBar_Put;
	private JProgressBar progressBar_Get;
	private static final Color PROGRESS_PUT_COLOUR = new Color(191, 98, 4);
	private static final Color PROGRESS_GET_COLOUR = new Color(51, 122, 183);
	public static final int PROGRESS_START = 0;
	public static final int PROGRESS_UPDATE = 1;
	public static final int PROGRESS_END = 2;
	public static final int PROGRESS_FAILED = 3;
	private JScrollPane scrollPane_Properties;
	private JScrollPane scrollPane_Email;
	private JScrollPane scrollPanel_Distribution;

	// Rows kept in the log panel - the three logs used to keep 100 each.
	private static final int maxLogRows = 300;

	// The settings tabs are laid out by hand (null layout) and so report no size of
	// their own; this is the width their widest row needs (the button columns on the
	// Put and Get tabs and the property tables end at 547), and the split pane divider
	// starts here. The log panel has the rest of the window.
	private static final int TAB_PANEL_WIDTH = 560;
	private static final int LOG_PANEL_MIN_WIDTH = 460; // the one-row System/Put/Get + buttons + Log Level strip needs ~446; the log side has 473

	// Display level of the log panel (JLogPanel.LEVEL_*). Written on the Swing thread
	// from the Log Level combo, read by the Put and Get threads. Remembered in sftp_common.xml
	// (logLevel) the moment it changes - it is a view setting, outside Apply/Save/Reload.
	private volatile int logLevel = JLogPanel.LEVEL_NORMAL;
	private DefaultComboBoxModel<String> logLevelModel;

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
	// Its lines (browse and safe-file connections) show on the System tab, which is where the status line sends the user.
	private JschCommands jcmd = new JschCommands(JschCommands.LogDestination_SYS);

	private JButton4j btn_Apply;
	private JButton4j btn_Reload;

	// Fingerprints of the settings form as last saved to disk and as last handed to the threads.
	// Apply, Save and Reload are only enabled while the form differs from the matching one.
	private String savedFingerprint = null;
	// Weak so that row panels replaced by a reload can be collected.
	private Set<Component> changeWiredComponents = Collections.newSetFromMap(new WeakHashMap<Component, Boolean>());

	// How long Apply waits for a service on this machine to collect its reload request before concluding there is none.
	private static final long RELOAD_REQUEST_WAIT_MS = 5000;

	private Instant reloadRequestedAt = null;

	// Reports on the status line once this program's threads have taken up the settings handed to them,
	// then once more when a service on this machine has collected the reload request - or when none has.
	private javax.swing.Timer applyTimer = new javax.swing.Timer(200, new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			if ((Start.transferPut.isSettingsPending() == false) && (Start.transferGet.isSettingsPending() == false))
			{
				if (reloadRequestedAt == null)
				{
					applyTimer.stop();

					displayMessage("Settings saved and applied.", messageType_INFO);
				}
				else if (ReloadRequest.isPending() == false)
				{
					applyTimer.stop();
					reloadRequestedAt = null;

					writeToSystemLog("Reload request collected by a service on this machine.", JLogPanel.INFO);

					displayMessage("Settings saved and applied - a service on this machine has collected the reload request.", messageType_INFO);
				}
				else if (Duration.between(reloadRequestedAt, Instant.now()).toMillis() > RELOAD_REQUEST_WAIT_MS)
				{
					applyTimer.stop();
					reloadRequestedAt = null;

					if (ReloadRequest.clear())
					{
						writeToSystemLog("Reload request not collected - no service running on this machine, request removed.", JLogPanel.INFO);
					}
					else
					{
						writeToSystemLog("Reload request not collected and could not be removed : " + ReloadRequest.getFile().getPath(), JLogPanel.WARN);
					}

					displayMessage("Settings saved and applied - no service is running on this machine.", messageType_INFO);
				}
			}
		}
	});

	// Looks for a service on this machine every 2 s and switches the window between "alone" and "beside a service".
	private javax.swing.Timer serviceTimer = new javax.swing.Timer(2000, new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			boolean present = ServiceLock.isHeldElsewhere();

			// A probe which fails says nothing either way - reported once, so "alone" is not silently assumed.
			String probeError = ServiceLock.getLastProbeError();

			if ((probeError != null) && (serviceProbeFailing == false))
			{
				serviceProbeFailing = true;
				writeToSystemLog("Service lock probe failed - cannot tell whether a service is running on this machine : " + probeError, JLogPanel.WARN);
			}
			else if (probeError == null)
			{
				serviceProbeFailing = false;
			}

			if (present != servicePresent)
			{
				servicePresent = present;
				servicePresenceChanged();
			}
		}
	});

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


		panel_Log_Scrollable = new JLogPanel(maxLogRows);
		panel_Log_Scrollable.setVisible(true);
		panel_Log_Scrollable.setLayout(new BoxLayout(panel_Log_Scrollable, BoxLayout.PAGE_AXIS));

		scrollPane_Log = new JScrollPane(panel_Log_Scrollable);
		scrollPane_Log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		setSize(new Dimension(1100, 570));

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
		messageLabel.setSize(new Dimension(1100, 35));
		messageLabel.setMinimumSize(new Dimension(1100, 35));
		statusPanel.add(messageLabel);

		serviceBanner = new JLabel4j_std("A service is running on this machine - transfers and the web log viewer are its job. Apply hands it the new settings.");
		serviceBanner.setFont(Common.font_bold);
		serviceBanner.setForeground(new Color(0, 100, 0));
		serviceBanner.setBorder(new EmptyBorder(4, 10, 0, 10));
		serviceBanner.setVisible(false);

		JPanel southPanel = new JPanel(new BorderLayout(0, 0));
		southPanel.add(serviceBanner, BorderLayout.NORTH);
		southPanel.add(statusPanel, BorderLayout.SOUTH);

		contentPane.add(southPanel, BorderLayout.SOUTH);

		toolBarRight.add(seperator);
		toolBarRight.add(seperator);

		btnStartStop = new JToggleButton4j(Common.icon_disconnected);
		btnStartStop.setToolTipText("Run or pause the transfers from this window");
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
					Start.processingStateChanged(true, "Run button pressed");
				}
				else
				{
					btnStartStop.setIcon(Common.icon_disconnected);
					Start.transferPut.requestMode(TransferPUT.Mode_PAUSE);
					Start.transferGet.requestMode(TransferPUT.Mode_PAUSE);
					Start.processingStateChanged(false, "Run button released");
				}
			}
		});

		tabbedPane.addTab("SFTP", null, sftp_common_Tab_Panel, "SFTP Settings");
		tabbedPane.addTab("Put Properties", null, sftp_put_Tab_Panel, "SFTP Put Settings");
		tabbedPane.addTab("Get Properties", null, sftp_get_Tab_Panel, "SFTP Get Settings");
		tabbedPane.addTab("jsch Properties", null, propertes_Tab_Panel, "SFTP Put");
		tabbedPane.addTab("Email Properties", null,email_Tab_Panel, "Email Settings");
		// The model behind the Log Level combo above the log panel.
		// The saved level is selected before the listener is added, so starting up never writes the file.
		logLevelModel = new DefaultComboBoxModel<String>(JLogPanel.LEVEL_NAMES);
		logLevel = JLogPanel.levelOf(settingsCommon.logLevel.data);
		logLevelModel.setSelectedItem(JLogPanel.LEVEL_NAMES[logLevel]);
		logLevelModel.addListDataListener(new ListDataListener()
		{
			public void contentsChanged(ListDataEvent e)
			{
				int level = logLevelModel.getIndexOf(logLevelModel.getSelectedItem());

				if ((level >= 0) && (level != logLevel))
				{
					logLevel = level;

					// Kept in the form's copy too, or the next Save would write the old level back.
					settingsCommon.logLevel.data = JLogPanel.LEVEL_NAMES[level];
					settingsUtil.saveSFTPCommonLogLevel(settingsCommon.logLevel.data);
				}
			}

			public void intervalAdded(ListDataEvent e)
			{
			}

			public void intervalRemoved(ListDataEvent e)
			{
			}
		});

		// Settings tabs on the left at the width their rows need, the log panel on the
		// right with the rest; the divider can be dragged to give the log more room.
		tabbedPane.setPreferredSize(new Dimension(TAB_PANEL_WIDTH, 10));
		tabbedPane.setMinimumSize(new Dimension(TAB_PANEL_WIDTH, 10));

		JPanel logSide = logSide(scrollPane_Log);
		logSide.setMinimumSize(new Dimension(LOG_PANEL_MIN_WIDTH, 10));

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, logSide);
		splitPane.setResizeWeight(0);
		splitPane.setContinuousLayout(true);
		splitPane.setBorder(null);

		contentPane.add(splitPane, BorderLayout.CENTER);

		// JSCH PROPERTIES

		scrollPane_Properties = new JScrollPane();
		scrollPane_Properties.setBounds(6, 20, 541, 450);
		propertes_Tab_Panel.add(scrollPane_Properties);

		panel_Properties_Scrollable = new JPanel();
		panel_Properties_Scrollable.setLayout(new BoxLayout(panel_Properties_Scrollable, BoxLayout.Y_AXIS));
		scrollPane_Properties.setViewportView(panel_Properties_Scrollable);


		JLabel4j_std lbl_jschProperty_Title = new JLabel4j_std("Property                                                   Value                                                                          Encrypt  Enable");
		lbl_jschProperty_Title.setFont(Common.font_bold);
		lbl_jschProperty_Title.setBounds(scrollPane_Properties.getX() + 5, scrollPane_Properties.getY() - 20, 536, 30);
		propertes_Tab_Panel.add(lbl_jschProperty_Title);

		// EMAIL

		scrollPane_Email = new JScrollPane();
		scrollPane_Email.setBounds(6, 20, 541, 250);
		email_Tab_Panel.add(scrollPane_Email);

		panel_Email_Scrollable = new JPanel();
		panel_Email_Scrollable.setLayout(new BoxLayout(panel_Email_Scrollable, BoxLayout.Y_AXIS));
		scrollPane_Email.setViewportView(panel_Email_Scrollable);


		JLabel4j_std lbl_EmailProperty_Title = new JLabel4j_std("Property                                                   Value                                                                          Encrypt  Enable");
		lbl_EmailProperty_Title.setFont(Common.font_bold);
		lbl_EmailProperty_Title.setBounds(scrollPane_Email.getX() + 5, scrollPane_Email.getY() - 20, 536, 30);
		email_Tab_Panel.add(lbl_EmailProperty_Title);

		// DISTRIBUTION

		scrollPanel_Distribution = new JScrollPane();
		// Three rows (Error, Monitor, Transfer) - sized for them so the button below stays inside the tab on Windows.
		scrollPanel_Distribution.setBounds(6, 290, 541, 85);
		email_Tab_Panel.add(scrollPanel_Distribution);

		panel_Distribution_Scrollable = new JPanel();
		panel_Distribution_Scrollable.setLayout(new BoxLayout(panel_Distribution_Scrollable, BoxLayout.Y_AXIS));
		scrollPanel_Distribution.setViewportView(panel_Distribution_Scrollable);


		JLabel4j_std lbl_Distribution_Title = new JLabel4j_std("ID                     Address List                                                                                        Frequency        Enable");
		lbl_Distribution_Title.setFont(Common.font_bold);
		lbl_Distribution_Title.setBounds(scrollPanel_Distribution.getX() + 5, scrollPanel_Distribution.getY() - 20, 536, 30);
		email_Tab_Panel.add(lbl_Distribution_Title);

		// Sends one message with the values on screen, whatever the Email Notifications box says.
		btn_TestEmail = new JButton4j("Send Test Email");
		btn_TestEmail.setToolTipText("Send a test email to one of the lists above using the settings on screen");
		btn_TestEmail.setBounds(6, 381, 150, 25);
		btn_TestEmail.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				sendTestEmail();
			}
		});
		email_Tab_Panel.add(btn_TestEmail);

		// COMMON

		JLabel4j_std lbl_Title_Common = new JLabel4j_std("Title");
		lbl_Title_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Title_Common.setBounds(6, 10, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Title_Common);

		checkBx_EmailEnabled_Common = new JCheckBox4j("");
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
		comboBox_Authentication_Type_Common.setBounds(161, 160, 184, 23);
		sftp_common_Tab_Panel.add(comboBox_Authentication_Type_Common);

		fld_Title_Common = new JTextField4j();
		fld_Title_Common.setBounds(161, 10, 386, 23);
		sftp_common_Tab_Panel.add(fld_Title_Common);

		fld_HostAddress_Common = new JTextField4j();
		fld_HostAddress_Common.setBounds(161, 40, 122, 23);
		sftp_common_Tab_Panel.add(fld_HostAddress_Common);

		fld_Port_Common = new JTextField4j();
		fld_Port_Common.setBounds(161, 70, 122, 23);
		sftp_common_Tab_Panel.add(fld_Port_Common);

		fld_KnownHostsFile_Common = new JTextField4j();
		fld_KnownHostsFile_Common.setBounds(184, 100, 211, 23);
		sftp_common_Tab_Panel.add(fld_KnownHostsFile_Common);

		chkbox_AddKnownHosts_Common = new JCheckBox4j("");
		chkbox_AddKnownHosts_Common.setBounds(161, 130, 23, 23);
		sftp_common_Tab_Panel.add(chkbox_AddKnownHosts_Common);

		chkbox_KnownHosts_Common = new JCheckBox4j("");
		chkbox_KnownHosts_Common.setBounds(161, 100, 23, 23);
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
		sftp_common_Tab_Panel.add(fld_Username_Common);

		JLabel4j_std lbl_Username_Common = new JLabel4j_std("Username");
		lbl_Username_Common.setText("SFTP Username");
		lbl_Username_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Username_Common.setBounds(6, 190, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Username_Common);

		fld_Password_Common = new JPasswordField4j();
		fld_Password_Common.setEnabled(false);
		fld_Password_Common.setBounds(161, 220, 202, 23);
		sftp_common_Tab_Panel.add(fld_Password_Common);

		JLabel4j_std lbl_Password_Common = new JLabel4j_std("Password");
		lbl_Password_Common.setText("SFTP Password");
		lbl_Password_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Password_Common.setBounds(6, 220, 143, 27);
		sftp_common_Tab_Panel.add(lbl_Password_Common);

		///////////////
		fld_PrivateKeyFile_Common = new JTextField4j();
		fld_PrivateKeyFile_Common.setBounds(184, 250, 211, 23);
		sftp_common_Tab_Panel.add(fld_PrivateKeyFile_Common);

		///////////////
		fld_PublicKeyFile_Common = new JTextField4j();
		fld_PublicKeyFile_Common.setBounds(184, 310, 211, 23);
		sftp_common_Tab_Panel.add(fld_PublicKeyFile_Common);

		///////////////
		fld_PrivateKeyComment_Common = new JTextField4j();
		fld_PrivateKeyComment_Common.setBounds(184, 340, 227, 23);
		sftp_common_Tab_Panel.add(fld_PrivateKeyComment_Common);

		///////////////
		fld_PrivateKeyPassword_Common = new JPasswordField4j();
		fld_PrivateKeyPassword_Common.setBounds(184, 280, 150, 23);
		sftp_common_Tab_Panel.add(fld_PrivateKeyPassword_Common);

		///////////////
		chckbx_PrivateKeyPassword_Common = new JCheckBox4j("");
		chckbx_PrivateKeyPassword_Common.setBounds(161, 280, 23, 23);
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
		btn_KnownHostsSelect_Common.setBounds(395, 95, 30, 30);
		sftp_common_Tab_Panel.add(btn_KnownHostsSelect_Common);

		btn_PrivateKeySelect_Common = new JButton4j(Common.icon_select_folder);
		btn_PrivateKeySelect_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_PrivateKeyFile_Common.setText(selectFile(fld_PrivateKeyFile_Common.getText(), "Private Key (.pem)", "pem"));
			}
		});
		btn_PrivateKeySelect_Common.setBounds(395, 245, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeySelect_Common);

		btn_PublicKeySelect_Common = new JButton4j(Common.icon_select_folder);
		btn_PublicKeySelect_Common.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_PublicKeyFile_Common.setText(selectFile(fld_PublicKeyFile_Common.getText(), "Public Key (.pub)", "pub"));
			}
		});
		btn_PublicKeySelect_Common.setBounds(395, 305, 30, 30);
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
		btn_PrivateKeyCreate_Common.setBounds(395, 275, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeyCreate_Common);

		JLabel4j_std lbl_EmailEnable_Common = new JLabel4j_std("Email Notifications");
		lbl_EmailEnable_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_EmailEnable_Common.setBounds(0, 370, 149, 27);
		sftp_common_Tab_Panel.add(lbl_EmailEnable_Common);

		// DAILY STATISTICS EMAIL - to the Monitor list, once a day at HH:mm on this machine's clock.

		JLabel4j_std lbl_StatsEnable_Common = new JLabel4j_std("Daily Statistics Email");
		lbl_StatsEnable_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_StatsEnable_Common.setBounds(190, 370, 150, 27);
		sftp_common_Tab_Panel.add(lbl_StatsEnable_Common);

		checkBx_StatsEnabled_Common = new JCheckBox4j("");
		checkBx_StatsEnabled_Common.setBounds(345, 370, 23, 23);
		checkBx_StatsEnabled_Common.setToolTipText("Email the day's transfer counts to the Monitor list");
		sftp_common_Tab_Panel.add(checkBx_StatsEnabled_Common);

		JLabel4j_std lbl_StatsTime_Common = new JLabel4j_std("at");
		lbl_StatsTime_Common.setBounds(372, 370, 20, 27);
		sftp_common_Tab_Panel.add(lbl_StatsTime_Common);

		fld_StatsTime_Common = new JTextField4j(5);
		fld_StatsTime_Common.setBounds(395, 370, 50, 23);
		fld_StatsTime_Common.setToolTipText("HH:mm, 24 hour, this machine's clock");
		sftp_common_Tab_Panel.add(fld_StatsTime_Common);

		checkBx_StatsEnabled_Common.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				fld_StatsTime_Common.setEnabled(checkBx_StatsEnabled_Common.isSelected());
			}
		});

		// WEB LOG VIEWER - a read-only page showing the three logs, for service installs.

		JLabel4j_std lbl_WebEnable_Common = new JLabel4j_std("Web Log Viewer");
		lbl_WebEnable_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_WebEnable_Common.setBounds(0, 430, 149, 27);
		sftp_common_Tab_Panel.add(lbl_WebEnable_Common);

		checkBx_WebEnabled_Common = new JCheckBox4j("");
		checkBx_WebEnabled_Common.setBounds(161, 430, 23, 23);
		checkBx_WebEnabled_Common.setToolTipText("Serve the Put, Get and System logs as a read-only web page");
		sftp_common_Tab_Panel.add(checkBx_WebEnabled_Common);

		JLabel4j_std lbl_WebPort_Common = new JLabel4j_std("Port");
		lbl_WebPort_Common.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_WebPort_Common.setBounds(190, 430, 40, 27);
		sftp_common_Tab_Panel.add(lbl_WebPort_Common);

		SpinnerNumberModel webPortModel_Common = new SpinnerNumberModel(WebServer.parsePort(settingsCommon.webPort.data, WebServer.DEFAULT_PORT), 1, 65535, 1);

		spinner_WebPort_Common = new JSpinner4j();
		spinner_WebPort_Common.setBounds(240, 430, 80, 23);
		spinner_WebPort_Common.setModel(webPortModel_Common);
		// Plain digits - the default editor would show 8080 as 8,080.
		spinner_WebPort_Common.setEditor(new JSpinner.NumberEditor(spinner_WebPort_Common, "#"));
		spinner_WebPort_Common.setToolTipText("TCP port the web page listens on");
		sftp_common_Tab_Panel.add(spinner_WebPort_Common);

		JLabel4j_std lbl_WebHint_Common = new JLabel4j_std("http://<this host>:<port>/  (read only, no login)");
		lbl_WebHint_Common.setFont(Common.font_italic);
		lbl_WebHint_Common.setBounds(190, 455, 300, 27);
		sftp_common_Tab_Panel.add(lbl_WebHint_Common);

		checkBx_WebEnabled_Common.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				spinner_WebPort_Common.setEnabled(checkBx_WebEnabled_Common.isSelected());
			}
		});

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
		checkBx_Enabled_Put.setBounds(161, 10, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_Enabled_Put);

		checkBx_BackupEnable_Put = new JCheckBox4j("");
		checkBx_BackupEnable_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setPutBackupRowState();
			}
		});
		checkBx_BackupEnable_Put.setBounds(161, 130, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_BackupEnable_Put);

		fld_Title_Put = new JTextField4j();
		fld_Title_Put.setBounds(161, 40, 386, 23);
		sftp_put_Tab_Panel.add(fld_Title_Put);

		fld_LocalFolder_Put = new JTextField4j();
		fld_LocalFolder_Put.setBounds(161, 70, 296, 23);
		sftp_put_Tab_Panel.add(fld_LocalFolder_Put);

		JLabel4j_std lbl_LocalFolder_Put = new JLabel4j_std("Local Folder");
		lbl_LocalFolder_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalFolder_Put.setBounds(6, 70, 143, 27);
		sftp_put_Tab_Panel.add(lbl_LocalFolder_Put);

		fld_LocalMask_Put = new JTextField4j();
		fld_LocalMask_Put.setBounds(161, 100, 184, 23);
		fld_LocalMask_Put.setToolTipText("Wildcards * and ? - separate several masks with ; for example *.xml;*.csv");
		sftp_put_Tab_Panel.add(fld_LocalMask_Put);

		JLabel4j_std lbl_LocalMask_Put = new JLabel4j_std("Local File Mask");
		lbl_LocalMask_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalMask_Put.setBounds(6, 100, 143, 27);
		sftp_put_Tab_Panel.add(lbl_LocalMask_Put);

		fld_BackupFolder_Put = new JTextField4j();
		fld_BackupFolder_Put.setBounds(196, 130, 321, 23);
		sftp_put_Tab_Panel.add(fld_BackupFolder_Put);

		JLabel4j_std lbl_BackupFolder_Put = new JLabel4j_std("Backup Folder");
		lbl_BackupFolder_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_BackupFolder_Put.setBounds(6, 130, 143, 27);
		sftp_put_Tab_Panel.add(lbl_BackupFolder_Put);

		JLabel4j_std lbl_BackupRetention_Put = new JLabel4j_std("Backup Retention");
		lbl_BackupRetention_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_BackupRetention_Put.setBounds(6, 160, 143, 27);
		sftp_put_Tab_Panel.add(lbl_BackupRetention_Put);

		JLabel4j_std lbl_BackupRetentionUnit_Put = new JLabel4j_std("Days");
		lbl_BackupRetentionUnit_Put.setBounds(240, 160, 63, 27);
		sftp_put_Tab_Panel.add(lbl_BackupRetentionUnit_Put);


		int backupRetention_Put = Integer.parseInt(settingsPut.backupRetention.data);
		SpinnerNumberModel backupRetentionModel_Put =  new SpinnerNumberModel(backupRetention_Put,0,365,1);

		spinner_BackupRetention_Put = new JSpinner4j();
		spinner_BackupRetention_Put.setBounds(161, 160, 67, 23);
		sftp_put_Tab_Panel.add(spinner_BackupRetention_Put);
		spinner_BackupRetention_Put.setModel(backupRetentionModel_Put);

		int pollingFrequency_Put = Integer.parseInt(settingsPut.pollFrequencySeconds.data);
		SpinnerNumberModel pollingFrequencyModel_Put =  new SpinnerNumberModel(pollingFrequency_Put,0,365,1);

		spinner_PollFrequency_Put = new JSpinner4j();
		spinner_PollFrequency_Put.setBounds(161, 190, 67, 23);
		sftp_put_Tab_Panel.add(spinner_PollFrequency_Put);
		spinner_PollFrequency_Put.setModel(pollingFrequencyModel_Put);

		JLabel4j_std lbl_PollFrequency_Put = new JLabel4j_std("Poll Frequency");
		lbl_PollFrequency_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PollFrequency_Put.setBounds(6, 190, 143, 27);
		sftp_put_Tab_Panel.add(lbl_PollFrequency_Put);

		JLabel4j_std lbl_PollFrequencyUnit_Put = new JLabel4j_std("Seconds");
		lbl_PollFrequencyUnit_Put.setBounds(240, 190, 63, 27);
		sftp_put_Tab_Panel.add(lbl_PollFrequencyUnit_Put);

		fld_RemoteFolder_Put = new JTextField4j();
		fld_RemoteFolder_Put.setBounds(161, 220, 296, 23);
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
		sftp_put_Tab_Panel.add(fld_TempFileExtension_Put);

		JLabel4j_std lbl_SubFolders_Put = new JLabel4j_std("Include Sub Folders");
		lbl_SubFolders_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SubFolders_Put.setBounds(6, 280, 143, 27);
		sftp_put_Tab_Panel.add(lbl_SubFolders_Put);

		checkBx_SubFolders_Put = new JCheckBox4j("");
		checkBx_SubFolders_Put.setBounds(161, 280, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_SubFolders_Put);

		JLabel4j_std lbl_SubFoldersHint_Put = new JLabel4j_std("Also send files from folders beneath the local folder");
		lbl_SubFoldersHint_Put.setBounds(196, 280, 351, 27);
		sftp_put_Tab_Panel.add(lbl_SubFoldersHint_Put);

		JLabel4j_std lbl_Sync_Put = new JLabel4j_std("Sync Mode");
		lbl_Sync_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Sync_Put.setBounds(6, 310, 143, 27);
		sftp_put_Tab_Panel.add(lbl_Sync_Put);

		checkBx_Sync_Put = new JCheckBox4j("");
		checkBx_Sync_Put.setBounds(161, 310, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_Sync_Put);

		lbl_SyncHint_Put = new JLabel4j_std(SYNC_HINT_PUT);
		lbl_SyncHint_Put.setBounds(196, 310, 351, 27);
		sftp_put_Tab_Panel.add(lbl_SyncHint_Put);

		JLabel4j_std lbl_OverwriteNewer_Put = new JLabel4j_std("Overwrite Newer");
		lbl_OverwriteNewer_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_OverwriteNewer_Put.setBounds(6, 340, 143, 27);
		sftp_put_Tab_Panel.add(lbl_OverwriteNewer_Put);

		checkBx_OverwriteNewer_Put = new JCheckBox4j("");
		checkBx_OverwriteNewer_Put.setBounds(161, 340, 23, 23);
		checkBx_OverwriteNewer_Put.setToolTipText("Sync only - untick to leave a remote file alone when it is newer than the local one");
		sftp_put_Tab_Panel.add(checkBx_OverwriteNewer_Put);

		JLabel4j_std lbl_OverwriteNewerHint_Put = new JLabel4j_std("Also replace a newer remote file with an older local one");
		lbl_OverwriteNewerHint_Put.setBounds(196, 340, 351, 27);
		sftp_put_Tab_Panel.add(lbl_OverwriteNewerHint_Put);

		JLabel4j_std lbl_SyncDelete_Put = new JLabel4j_std("Sync Deletions");
		lbl_SyncDelete_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SyncDelete_Put.setBounds(6, 370, 143, 27);
		sftp_put_Tab_Panel.add(lbl_SyncDelete_Put);

		checkBx_SyncDelete_Put = new JCheckBox4j("");
		checkBx_SyncDelete_Put.setEnabled(checkBx_Sync_Put.isSelected());
		checkBx_SyncDelete_Put.setBounds(161, 370, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_SyncDelete_Put);

		JLabel4j_std lbl_SyncDeleteHint_Put = new JLabel4j_std("Also delete remote files which are no longer in the local folder");
		lbl_SyncDeleteHint_Put.setBounds(196, 370, 351, 27);
		sftp_put_Tab_Panel.add(lbl_SyncDeleteHint_Put);

		JLabel4j_std lbl_SyncDeleteFolders_Put = new JLabel4j_std("Sync Folder Deletions");
		lbl_SyncDeleteFolders_Put.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SyncDeleteFolders_Put.setBounds(6, 400, 143, 27);
		sftp_put_Tab_Panel.add(lbl_SyncDeleteFolders_Put);

		checkBx_SyncDeleteFolders_Put = new JCheckBox4j("");
		checkBx_SyncDeleteFolders_Put.setBounds(161, 400, 23, 23);
		sftp_put_Tab_Panel.add(checkBx_SyncDeleteFolders_Put);

		JLabel4j_std lbl_SyncDeleteFoldersHint_Put = new JLabel4j_std("Also remove remote folders which are no longer in the local folder");
		lbl_SyncDeleteFoldersHint_Put.setBounds(196, 400, 351, 27);
		sftp_put_Tab_Panel.add(lbl_SyncDeleteFoldersHint_Put);

		setSyncDeleteState(checkBx_SubFolders_Put, checkBx_Sync_Put, checkBx_SyncDelete_Put, checkBx_SyncDeleteFolders_Put, checkBx_OverwriteNewer_Put, lbl_SyncHint_Put, SYNC_HINT_PUT, MOVE_HINT_PUT);

		ActionListener syncDeleteStateListener_Put = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setSyncDeleteState(checkBx_SubFolders_Put, checkBx_Sync_Put, checkBx_SyncDelete_Put, checkBx_SyncDeleteFolders_Put, checkBx_OverwriteNewer_Put, lbl_SyncHint_Put, SYNC_HINT_PUT, MOVE_HINT_PUT);
				setPutBackupRowState();
			}
		};

		checkBx_SubFolders_Put.addActionListener(syncDeleteStateListener_Put);
		checkBx_Sync_Put.addActionListener(syncDeleteStateListener_Put);
		checkBx_SyncDelete_Put.addActionListener(syncDeleteStateListener_Put);

		checkBx_Stability_Put = new JCheckBox4j("");
		spinner_StabilitySeconds_Put = new JSpinner4j();
		addStabilityRow(sftp_put_Tab_Panel, 430, checkBx_Stability_Put, spinner_StabilitySeconds_Put, settingsPut.stabilitySeconds.data, "Wait, then skip a file whose size or time has changed");

		JButton4j btn_LocalFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_LocalFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_LocalFolder_Put.setText(selectFolder(fld_LocalFolder_Put.getText()));
			}
		});
		btn_LocalFolderSelect_Put.setBounds(457, 65, 30, 30);
		sftp_put_Tab_Panel.add(btn_LocalFolderSelect_Put);

		addSafeFileButtons(sftp_put_Tab_Panel, fld_LocalFolder_Put, 65, false);

		btn_BackupFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_BackupFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_BackupFolder_Put.setText(selectFolder(fld_BackupFolder_Put.getText()));
			}
		});
		btn_BackupFolderSelect_Put.setBounds(517, 125, 30, 30);
		sftp_put_Tab_Panel.add(btn_BackupFolderSelect_Put);

		JButton4j btn_RemoteFolderSelect_Put = new JButton4j(Common.icon_select_folder);
		btn_RemoteFolderSelect_Put.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				browseRemoteFolder(fld_RemoteFolder_Put);
			}
		});
		btn_RemoteFolderSelect_Put.setBounds(457, 215, 30, 30);
		sftp_put_Tab_Panel.add(btn_RemoteFolderSelect_Put);

		addSafeFileButtons(sftp_put_Tab_Panel, fld_RemoteFolder_Put, 215, true);

		// GET TAB

		JLabel4j_std lbl_Enabled_Get = new JLabel4j_std("Enabled");
		lbl_Enabled_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Enabled_Get.setBounds(6, 10, 143, 27);
		sftp_get_Tab_Panel.add(lbl_Enabled_Get);

		checkBx_Enabled_Get = new JCheckBox4j("");
		checkBx_Enabled_Get.setBounds(161, 10, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_Enabled_Get);

		fld_LocalFolder_Get = new JTextField4j();
		fld_LocalFolder_Get.setBounds(161, 160, 296, 23);
		sftp_get_Tab_Panel.add(fld_LocalFolder_Get);

		JLabel4j_std lbl_LocalFolder_Get = new JLabel4j_std("Local Folder");
		lbl_LocalFolder_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_LocalFolder_Get.setBounds(6, 160, 143, 27);
		sftp_get_Tab_Panel.add(lbl_LocalFolder_Get);

		fld_Remote_Mask_Get = new JTextField4j();
		fld_Remote_Mask_Get.setBounds(161, 100, 184, 23);
		fld_Remote_Mask_Get.setToolTipText("Wildcards * and ? - separate several masks with ; for example *.xml;*.csv");
		sftp_get_Tab_Panel.add(fld_Remote_Mask_Get);

		JLabel4j_std lbl_RemoteMask_Get = new JLabel4j_std("Local File Mask");
		lbl_RemoteMask_Get.setText("Remote File Mask");
		lbl_RemoteMask_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_RemoteMask_Get.setBounds(6, 100, 143, 27);
		sftp_get_Tab_Panel.add(lbl_RemoteMask_Get);

		int pollingFrequency_Get = Integer.parseInt(settingsGet.pollFrequencySeconds.data);
		SpinnerNumberModel pollingFrequencyModel_Get =  new SpinnerNumberModel(pollingFrequency_Get,0,365,1);

		spinner_PollFrequency_Get = new JSpinner4j();
		spinner_PollFrequency_Get.setBounds(161, 130, 67, 23);
		sftp_get_Tab_Panel.add(spinner_PollFrequency_Get);
		spinner_PollFrequency_Get.setModel(pollingFrequencyModel_Get);

		JLabel4j_std lbl_PollFrequency_Get = new JLabel4j_std("Poll Frequency");
		lbl_PollFrequency_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_PollFrequency_Get.setBounds(6, 130, 143, 27);
		sftp_get_Tab_Panel.add(lbl_PollFrequency_Get);

		JLabel4j_std lbl_PollFrequencyUnit_Get = new JLabel4j_std("Seconds");
		lbl_PollFrequencyUnit_Get.setHorizontalAlignment(SwingConstants.LEADING);
		lbl_PollFrequencyUnit_Get.setBounds(246, 130, 57, 27);
		sftp_get_Tab_Panel.add(lbl_PollFrequencyUnit_Get);

		fld_Title_Get = new JTextField4j();
		fld_Title_Get.setBounds(161, 40, 386, 23);
		sftp_get_Tab_Panel.add(fld_Title_Get);

		fld_RemoteFolder_Get = new JTextField4j();
		fld_RemoteFolder_Get.setBounds(161, 70, 296, 23);
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
		sftp_get_Tab_Panel.add(fld_TempFileExtension_Get);

		JLabel4j_std lbl_SubFolders_Get = new JLabel4j_std("Include Sub Folders");
		lbl_SubFolders_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SubFolders_Get.setBounds(6, 220, 143, 27);
		sftp_get_Tab_Panel.add(lbl_SubFolders_Get);

		checkBx_SubFolders_Get = new JCheckBox4j("");
		checkBx_SubFolders_Get.setBounds(161, 220, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_SubFolders_Get);

		JLabel4j_std lbl_SubFoldersHint_Get = new JLabel4j_std("Also fetch files from folders beneath the remote folder");
		lbl_SubFoldersHint_Get.setBounds(196, 220, 351, 27);
		sftp_get_Tab_Panel.add(lbl_SubFoldersHint_Get);

		JLabel4j_std lbl_Sync_Get = new JLabel4j_std("Sync Mode");
		lbl_Sync_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Sync_Get.setBounds(6, 250, 143, 27);
		sftp_get_Tab_Panel.add(lbl_Sync_Get);

		checkBx_Sync_Get = new JCheckBox4j("");
		checkBx_Sync_Get.setBounds(161, 250, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_Sync_Get);

		lbl_SyncHint_Get = new JLabel4j_std(SYNC_HINT_GET);
		lbl_SyncHint_Get.setBounds(196, 250, 351, 27);
		sftp_get_Tab_Panel.add(lbl_SyncHint_Get);

		JLabel4j_std lbl_OverwriteNewer_Get = new JLabel4j_std("Overwrite Newer");
		lbl_OverwriteNewer_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_OverwriteNewer_Get.setBounds(6, 280, 143, 27);
		sftp_get_Tab_Panel.add(lbl_OverwriteNewer_Get);

		checkBx_OverwriteNewer_Get = new JCheckBox4j("");
		checkBx_OverwriteNewer_Get.setBounds(161, 280, 23, 23);
		checkBx_OverwriteNewer_Get.setToolTipText("Sync only - untick to leave a local file alone when it is newer than the remote one");
		sftp_get_Tab_Panel.add(checkBx_OverwriteNewer_Get);

		JLabel4j_std lbl_OverwriteNewerHint_Get = new JLabel4j_std("Also replace a newer local file with an older remote one");
		lbl_OverwriteNewerHint_Get.setBounds(196, 280, 351, 27);
		sftp_get_Tab_Panel.add(lbl_OverwriteNewerHint_Get);

		JLabel4j_std lbl_SyncDelete_Get = new JLabel4j_std("Sync Deletions");
		lbl_SyncDelete_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SyncDelete_Get.setBounds(6, 310, 143, 27);
		sftp_get_Tab_Panel.add(lbl_SyncDelete_Get);

		checkBx_SyncDelete_Get = new JCheckBox4j("");
		checkBx_SyncDelete_Get.setEnabled(checkBx_Sync_Get.isSelected());
		checkBx_SyncDelete_Get.setBounds(161, 310, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_SyncDelete_Get);

		JLabel4j_std lbl_SyncDeleteHint_Get = new JLabel4j_std("Also delete local files which are no longer in the remote folder");
		lbl_SyncDeleteHint_Get.setBounds(196, 310, 351, 27);
		sftp_get_Tab_Panel.add(lbl_SyncDeleteHint_Get);

		JLabel4j_std lbl_SyncDeleteFolders_Get = new JLabel4j_std("Sync Folder Deletions");
		lbl_SyncDeleteFolders_Get.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_SyncDeleteFolders_Get.setBounds(6, 340, 143, 27);
		sftp_get_Tab_Panel.add(lbl_SyncDeleteFolders_Get);

		checkBx_SyncDeleteFolders_Get = new JCheckBox4j("");
		checkBx_SyncDeleteFolders_Get.setBounds(161, 340, 23, 23);
		sftp_get_Tab_Panel.add(checkBx_SyncDeleteFolders_Get);

		JLabel4j_std lbl_SyncDeleteFoldersHint_Get = new JLabel4j_std("Also remove local folders which are no longer in the remote folder");
		lbl_SyncDeleteFoldersHint_Get.setBounds(196, 340, 351, 27);
		sftp_get_Tab_Panel.add(lbl_SyncDeleteFoldersHint_Get);

		setSyncDeleteState(checkBx_SubFolders_Get, checkBx_Sync_Get, checkBx_SyncDelete_Get, checkBx_SyncDeleteFolders_Get, checkBx_OverwriteNewer_Get, lbl_SyncHint_Get, SYNC_HINT_GET, MOVE_HINT_GET);

		ActionListener syncDeleteStateListener_Get = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setSyncDeleteState(checkBx_SubFolders_Get, checkBx_Sync_Get, checkBx_SyncDelete_Get, checkBx_SyncDeleteFolders_Get, checkBx_OverwriteNewer_Get, lbl_SyncHint_Get, SYNC_HINT_GET, MOVE_HINT_GET);
			}
		};

		checkBx_SubFolders_Get.addActionListener(syncDeleteStateListener_Get);
		checkBx_Sync_Get.addActionListener(syncDeleteStateListener_Get);
		checkBx_SyncDelete_Get.addActionListener(syncDeleteStateListener_Get);

		checkBx_Stability_Get = new JCheckBox4j("");
		spinner_StabilitySeconds_Get = new JSpinner4j();
		addStabilityRow(sftp_get_Tab_Panel, 370, checkBx_Stability_Get, spinner_StabilitySeconds_Get, settingsGet.stabilitySeconds.data, "Wait, then skip a file whose size or time has changed");

		JButton4j btn_LocalFolderSelect_Get = new JButton4j(Common.icon_select_folder);
		btn_LocalFolderSelect_Get.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fld_LocalFolder_Get.setText(selectFolder(fld_LocalFolder_Get.getText()));
			}
		});
		btn_LocalFolderSelect_Get.setBounds(457, 155, 30, 30);
		sftp_get_Tab_Panel.add(btn_LocalFolderSelect_Get);

		addSafeFileButtons(sftp_get_Tab_Panel, fld_LocalFolder_Get, 155, false);

		JButton4j btn_RemoteFolderSelect_Get = new JButton4j(Common.icon_select_folder);
		btn_RemoteFolderSelect_Get.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				browseRemoteFolder(fld_RemoteFolder_Get);
			}
		});
		btn_RemoteFolderSelect_Get.setBounds(457, 65, 30, 30);
		sftp_get_Tab_Panel.add(btn_RemoteFolderSelect_Get);

		addSafeFileButtons(sftp_get_Tab_Panel, fld_RemoteFolder_Get, 65, true);

		// *******

		btn_Apply = new JButton4j(Common.icon_ok);
		btn_Apply.setToolTipText("Save and apply settings - a service running on this machine reloads them too");
		btn_Apply.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				applyChanges();
			}
		});
		btn_Apply.setPreferredSize(btn);
		toolBarRight.add(btn_Apply);

		btn_Reload = new JButton4j(Common.icon_reload);
		btn_Reload.setToolTipText("Reload saved settings (discard changes not yet applied)");
		btn_Reload.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int question = JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, "Discard the changes not yet applied and reload the saved settings ?", "Confirm", JOptionPane.YES_NO_OPTION, 0, Common.icon_confirm);

				if (question == 0)
				{
					reloadSettings();
				}
			}
		});
		btn_Reload.setPreferredSize(btn);
		toolBarRight.add(btn_Reload);

		JButton4j btn_Help = new JButton4j(Common.icon_help);
		btn_Help.setToolTipText("Help");
		btn_Help.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				openHelp();
			}
		});
		btn_Help.setPreferredSize(btn);
		toolBarRight.add(btn_Help);

		JButton4j btn_About = new JButton4j(Common.icon_about);
		btn_About.setToolTipText("About");
		btn_About.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JDialogAbout about = new JDialogAbout();
				about.setVisible(true);
			}
		});
		btn_About.setPreferredSize(btn);
		toolBarRight.add(btn_About);

		JButton4j btn_License = new JButton4j(Common.icon_license);
		btn_License.setToolTipText("Licences");
		btn_License.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JDialogLicenses dl = new JDialogLicenses(JFrameSFTPTransfer.this);
				dl.setVisible(true);
			}
		});
		btn_License.setPreferredSize(btn);
		toolBarRight.add(btn_License);

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
		btnClearPrivateKeyPassword.setBounds(365, 275, 30, 30);
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
				fld_Password_Common.setText(changePassword.enteredPassword);
				settingsCommon.password.data = changePassword.enteredPassword;
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
		btn_PrivateKeyPassword.setBounds(335, 275, 30, 30);
		sftp_common_Tab_Panel.add(btn_PrivateKeyPassword);

		populateFromSettings();

		// Wired after the form is populated so that loading does not count as a change.
		wireChangeListeners();

		// The threads read the same files at start-up, so saved and shown agree here.
		savedFingerprint = currentFingerprint();
		refreshSettingsButtons();

		setLocationRelativeTo(null);

		int widthadjustment = util.getOSWidthAdjustment();
		int heightadjustment = util.getOSHeightAdjustment();

		GraphicsDevice gd = util.getGraphicsDevice();

		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		Rectangle screenBounds = gc.getBounds();

		setBounds(screenBounds.x + ((screenBounds.width - JFrameSFTPTransfer.this.getWidth()) / 2), screenBounds.y + ((screenBounds.height - JFrameSFTPTransfer.this.getHeight()) / 2), JFrameSFTPTransfer.this.getWidth() + widthadjustment,
				JFrameSFTPTransfer.this.getHeight() + heightadjustment);
		setVisible(true);

		// The first look comes soon after the threads exist (Start creates them right after this constructor).
		serviceTimer.setInitialDelay(500);
		serviceTimer.start();
	}

	/**
	 * A service has appeared on, or gone from, this machine. While it is
	 * there transfers are its job: the Run button is disabled, transfers
	 * running from this window are paused (the current pass completes first,
	 * as on any pause - a pause is only taken up between passes), and the web
	 * viewer is left to the service. When it goes the window is on its own
	 * again and gets everything back.
	 */
	private void servicePresenceChanged()
	{
		if (servicePresent)
		{
			writeToSystemLog("Service detected on this machine (" + ServiceLock.FILENAME + " is held) - transfers and the web viewer are its job.", JLogPanel.INFO);

			serviceBanner.setVisible(true);

			if (btnStartStop.isSelected())
			{
				btnStartStop.setSelected(false);
				btnStartStop.setIcon(Common.icon_disconnected);

				if (Start.transferPut != null)
				{
					Start.transferPut.requestMode(TransferPUT.Mode_PAUSE);
					Start.transferGet.requestMode(TransferPUT.Mode_PAUSE);
				}

				writeToSystemLog("Transfers from this window paused - the service has taken over.", JLogPanel.WARN);
				displayMessage("Transfers from this window paused - the service has taken over.", messageType_WARN);

				// setSelected(false) above does not fire the button's listener.
				Start.processingStateChanged(false, "service on this machine has taken over");
			}

			btnStartStop.setEnabled(false);
			btnStartStop.setToolTipText("Transfers are being run by the service on this machine");
		}
		else
		{
			writeToSystemLog("No service running on this machine - transfers may be run from this window.", JLogPanel.INFO);

			serviceBanner.setVisible(false);

			btnStartStop.setEnabled(true);
			btnStartStop.setToolTipText("Run or pause the transfers from this window");
		}

		// Stops this window's viewer while a service is there, starts it again (if wanted) when the service has gone.
		if (Start.transferPut != null)
		{
			Start.applyWebSettings(Boolean.valueOf(settingsCommon.webEnabled.data), WebServer.parsePort(settingsCommon.webPort.data, WebServer.DEFAULT_PORT), settingsCommon.title.data);
		}

		contentPane.revalidate();
	}

	public boolean confirmExit()
	{
		boolean result = false;

		String message = "Exit application ?";

		if (hasUnsavedChanges())
		{
			message = "Settings have been changed but not applied.\nThe changes will be lost on exit.\n\nExit application ?";
		}

		int question = JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, message, "Confirm", JOptionPane.YES_NO_OPTION, 0, Common.icon_confirm);

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

	/**
	 * The log side of the window: the scrolling rows beneath two strips - the Log
	 * Level combo, then the System / Put / Get check boxes (no caption, Dave's
	 * choice 2026-10-12) which hide or show that
	 * log's rows (the ones already on screen as well as the ones to come). The
	 * boxes start ticked at every start-up; only the level is remembered.
	 */
	private JPanel logSide(JScrollPane rows)
	{
		JLabel4j_std lbl_Show = new JLabel4j_std("Log Level");

		JComboBox4j<String> comboBox_Level = new JComboBox4j<String>();
		comboBox_Level.setModel(logLevelModel);
		// Arial rather than the monospaced combo font: "Warnings and errors" needs 170 in
		// Monospaced and the one-row strip would then not fit the log side (473 at the fixed
		// 1100 window) - see LOG_PANEL_MIN_WIDTH.
		comboBox_Level.setFont(Common.font_std);
		comboBox_Level.setPreferredSize(new Dimension(140, 23));
		comboBox_Level.setToolTipText("Detail shown in the log from now on - Debug adds the polling of the remote folders");

		checkBx_ShowSystem_Log = logCheckBox("System", JschCommands.LogDestination_SYS, "Show the System log lines");
		checkBx_ShowPut_Log = logCheckBox("Put", JschCommands.LogDestination_PUT, "Show the Put log lines");
		checkBx_ShowGet_Log = logCheckBox("Get", JschCommands.LogDestination_GET, "Show the Get log lines");

		// Clear empties the window only; Save writes the rows as shown. The file under logs/ is the complete record either way.
		Dimension logBtn = new Dimension(32, 27);

		JButton4j btn_ClearLog = new JButton4j(Common.icon_erase);
		btn_ClearLog.setToolTipText("Clear the log window (the log file is untouched)");
		btn_ClearLog.setPreferredSize(logBtn);
		btn_ClearLog.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				panel_Log_Scrollable.clear();
			}
		});

		JButton4j btn_SaveLog = new JButton4j(Common.icon_save);
		btn_SaveLog.setToolTipText("Save the log window as shown to a text file");
		btn_SaveLog.setPreferredSize(logBtn);
		btn_SaveLog.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveLogWindow();
			}
		});

		// One row: the three logs, the two buttons, then the level.
		JPanel showStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
		showStrip.add(checkBx_ShowSystem_Log);
		showStrip.add(checkBx_ShowPut_Log);
		showStrip.add(checkBx_ShowGet_Log);
		showStrip.add(btn_ClearLog);
		showStrip.add(btn_SaveLog);
		showStrip.add(lbl_Show);
		showStrip.add(comboBox_Level);

		progressBar_Put = progressBar("Put", "Progress of the file being sent (upload)", PROGRESS_PUT_COLOUR);
		progressBar_Get = progressBar("Get", "Progress of the file being received (download)", PROGRESS_GET_COLOUR);

		// Edges in line with the strip above (FlowLayout hgap 5).
		JPanel progressStrip = new JPanel(new java.awt.GridLayout(2, 1, 0, 3));
		progressStrip.setBorder(new EmptyBorder(2, 5, 4, 5));
		progressStrip.add(progressBar_Put);
		progressStrip.add(progressBar_Get);
		progressStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

		JPanel strips = new JPanel();
		strips.setLayout(new BoxLayout(strips, BoxLayout.PAGE_AXIS));
		showStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
		progressStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
		strips.add(showStrip);
		strips.add(progressStrip);

		JPanel side = new JPanel(new BorderLayout(0, 0));
		side.add(strips, BorderLayout.NORTH);
		side.add(rows, BorderLayout.CENTER);

		return side;
	}

	/**
	 * Writes the log window as shown (ticked logs only, oldest first) to a
	 * text file of the user's choosing, suggested under logs/ with the time
	 * in its name. The window holds at most the last maxLogRows rows - the
	 * file under logs/ is the complete record.
	 */
	private void saveLogWindow()
	{
		File logFolder = new File(System.getProperty("user.dir"), "logs");
		String suggested = "sftpTransfer_window_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

		JFileChooser fc = new JFileChooser(logFolder.isDirectory() ? logFolder : new File(System.getProperty("user.dir")));
		fc.setDialogTitle("Save log window");
		fc.setSelectedFile(new File(fc.getCurrentDirectory(), suggested));

		if (fc.showSaveDialog(JFrameSFTPTransfer.this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File target = fc.getSelectedFile();

		if (target.exists())
		{
			if (JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, target.getName() + " already exists - replace it ?", "Confirm", JOptionPane.YES_NO_OPTION, 0, Common.icon_confirm) != JOptionPane.YES_OPTION)
			{
				return;
			}
		}

		java.util.List<String> lines = panel_Log_Scrollable.visibleLines();

		try
		{
			Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
			displayMessage("Log window saved - " + lines.size() + " lines to " + target.getPath(), messageType_INFO);
		}
		catch (IOException e)
		{
			displayMessage("Could not save the log window - " + e.getMessage(), messageType_ERROR);
		}
	}

	private JCheckBox4j logCheckBox(String caption, int tag, String tip)
	{
		JCheckBox4j box = new JCheckBox4j();
		box.init();
		box.setText(caption);
		box.setToolTipText(tip);
		box.setSelected(true);
		box.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				panel_Log_Scrollable.setTagVisible(tag, box.isSelected());
				scrollLogToEnd();
			}
		});

		return box;
	}

	/**
	 * Rows below the display level are not added at all - the maxLogRows trimming
	 * only sees what passed the level. A row whose log is unticked is added hidden
	 * and comes back when the box is ticked again. The file and the web viewer
	 * have already had the line by the time it gets here.
	 */
	public void writeToLog(int logDestination,String logdata, int level)
	{
		if (JLogPanel.shows(logLevel, level) == false)
		{
			return;
		}

		String prefix;

		switch (logDestination)
		{
		case JschCommands.LogDestination_PUT:
			prefix = "[PUT] ";
			break;
		case JschCommands.LogDestination_GET:
			prefix = "[GET] ";
			break;
		case JschCommands.LogDestination_SYS:
			prefix = "[SYS] ";
			break;
		default:
			return;
		}

		SwingUtilities.invokeLater(() -> {

			panel_Log_Scrollable.addLog(prefix + logdata, level, logDestination);
			scrollLogToEnd();
		});
	}

	/**
	 * One bar per direction, idle text is just the direction. The colour is a
	 * per-component Nimbus painter override (setForeground only colours the
	 * text under Nimbus), a flat full-height round rect so both bars match;
	 * null would fall back to the look and feel's inset default painter.
	 */
	private JProgressBar progressBar(String idleText, String tip, Color colour)
	{
		JProgressBar bar = new JProgressBar(0, 100);
		bar.setStringPainted(true);
		bar.setString(idleText);
		bar.setFont(Common.font_std);
		bar.setToolTipText(tip);
		bar.setPreferredSize(new Dimension(100, 23));

		if (colour != null)
		{
			// Nimbus paints the filled part with these painters, sized to the filled width.
			Painter<JProgressBar> fill = (g, b, w, h) -> {
				g.setPaint(new GradientPaint(0, 0, colour.brighter(), 0, h, colour));
				g.fillRoundRect(0, 0, w, h, 5, 5);
			};

			UIDefaults overrides = new UIDefaults();
			overrides.put("ProgressBar[Enabled].foregroundPainter", fill);
			overrides.put("ProgressBar[Enabled+Finished].foregroundPainter", fill);
			overrides.put("ProgressBar[Enabled+Indeterminate].foregroundPainter", fill);
			overrides.put("ProgressBar[Enabled+Finished+Indeterminate].foregroundPainter", fill);
			bar.putClientProperty("Nimbus.Overrides", overrides);
			bar.putClientProperty("Nimbus.Overrides.InheritDefaults", Boolean.TRUE);
		}

		return bar;
	}

	/**
	 * Called from the Put and Get threads through JschCommands for the file in
	 * flight (already throttled to whole percentages there). Get has its own
	 * bar; everything else (Put, and the System connection's safe-file writes)
	 * shows on the Put bar. A finished file stays on show until the next one
	 * starts.
	 */
	public void transferProgress(int logDestination, String fileName, long done, long total, int state)
	{
		SwingUtilities.invokeLater(() -> {

			boolean isGet = (logDestination == JschCommands.LogDestination_GET);
			JProgressBar bar = isGet ? progressBar_Get : progressBar_Put;
			String direction = isGet ? "Get " : "Put ";
			boolean known = total > 0;
			int pct = known ? (int) Math.min(100, (done * 100) / total) : 0;

			switch (state)
			{
			case PROGRESS_END:
				bar.setIndeterminate(false);
				bar.setValue(100);
				bar.setString(direction + fileName + " - " + sizeText(done) + " complete");
				break;
			case PROGRESS_FAILED:
				bar.setIndeterminate(false);
				bar.setValue(pct);
				bar.setString(direction + fileName + " - failed after " + sizeText(done));
				break;
			default:
				bar.setIndeterminate(!known);
				bar.setValue(pct);
				if (known)
				{
					bar.setString(direction + fileName + "  " + pct + "%  (" + sizeText(done) + " of " + sizeText(total) + ")");
				}
				else
				{
					bar.setString(direction + fileName + "  " + sizeText(done));
				}
				break;
			}
		});
	}

	private static String sizeText(long bytes)
	{
		if (bytes < 1024)
		{
			return bytes + " B";
		}
		if (bytes < 1024 * 1024)
		{
			return String.format("%.1f KB", bytes / 1024.0);
		}
		if (bytes < 1024L * 1024 * 1024)
		{
			return String.format("%.1f MB", bytes / (1024.0 * 1024));
		}
		return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
	}

	private void scrollLogToEnd()
	{
		panel_Log_Scrollable.revalidate();
		panel_Log_Scrollable.repaint();

		JScrollBar vertical = scrollPane_Log.getVerticalScrollBar();
		vertical.setValue(vertical.getMaximum());
	}

	/**
	 * The frame's own System log lines (saving, applying, reloading ...) go
	 * through JschCommands so they also reach the log file and the web viewer,
	 * which then hands them back to writeToLog for the panel.
	 */
	private void writeToSystemLog(String logdata, int level)
	{
		jcmd.writeToSystemLog(logdata, level);

		if (Start.gui == null)
		{
			// Still inside the constructor - nothing hands the line back yet.
			writeToLog(JschCommands.LogDestination_SYS, logdata, level);
		}
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

	/**
	 * The Stability Check row of a Put or Get tab: tick box, seconds spinner
	 * (only live when ticked) and a hint. The thread waits that many seconds,
	 * then leaves alone any file whose size or modified time has changed since
	 * it was found.
	 */
	private void addStabilityRow(JPanel panel, int y, JCheckBox4j check, JSpinner4j spinner, String seconds, String hint)
	{
		JLabel4j_std lbl_Stability = new JLabel4j_std("Stability Check");
		lbl_Stability.setHorizontalAlignment(SwingConstants.TRAILING);
		lbl_Stability.setBounds(6, y, 143, 27);
		panel.add(lbl_Stability);

		check.setBounds(161, y, 23, 23);
		check.setToolTipText("Only transfer a file once it has stopped changing");
		panel.add(check);

		spinner.setBounds(196, y, 67, 23);
		spinner.setModel(new SpinnerNumberModel(stabilitySeconds(seconds), 1, 3600, 1));
		spinner.setToolTipText("How long a file must stay the same size and time before it is transferred");
		spinner.setEnabled(check.isSelected());
		panel.add(spinner);

		JLabel4j_std lbl_Unit = new JLabel4j_std("Seconds");
		lbl_Unit.setBounds(270, y, 63, 27);
		panel.add(lbl_Unit);

		// The hint sits on the row beneath, so the tab stays narrow.
		JLabel4j_std lbl_Hint = new JLabel4j_std(hint);
		lbl_Hint.setFont(Common.font_italic);
		lbl_Hint.setBounds(161, y + 30, 386, 27);
		panel.add(lbl_Hint);

		check.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				spinner.setEnabled(check.isSelected());
			}
		});
	}

	/** The saved seconds value within the spinner's range; 5 if it is not a number. */
	private static int stabilitySeconds(String value)
	{
		int result = 5;

		try
		{
			result = Integer.parseInt(value.trim());
		}
		catch (Exception e)
		{
			result = 5;
		}

		return Math.max(1, Math.min(3600, result));
	}

	private void setSyncDeleteState(JCheckBox4j subFolders, JCheckBox4j sync, JCheckBox4j syncDelete, JCheckBox4j syncDeleteFolders, JCheckBox4j overwriteNewer, JLabel4j_std syncHint, String syncText, String moveText)
	{
		// The hint describes the mode in force: unticked is the original move behaviour.
		syncHint.setText(sync.isSelected() ? syncText : moveText);

		// Overwrite Newer only applies to a sync. Its value is kept while sync is off - it is inert then, and comes back as it was.
		overwriteNewer.setEnabled(sync.isSelected());

		// Deleting only applies to a sync, and is never left armed while sync is switched off.
		if (sync.isSelected() == false)
		{
			syncDelete.setSelected(false);
		}

		syncDelete.setEnabled(sync.isSelected());

		// Removing folders only applies when deletions and sub folders are both switched on.
		boolean folderDeletions = syncDelete.isSelected() && subFolders.isSelected();

		if (folderDeletions == false)
		{
			syncDeleteFolders.setSelected(false);
		}

		syncDeleteFolders.setEnabled(folderDeletions);
	}

	/**
	 * A backup only applies to a move (the copy of a file about to be deleted
	 * locally), so the whole row is greyed out while Sync Mode is ticked. The
	 * tick is kept, inert, and comes back when sync is switched off; the Put
	 * thread also skips the copy in sync mode.
	 */
	private void setPutBackupRowState()
	{
		boolean move = (checkBx_Sync_Put.isSelected() == false);
		checkBx_BackupEnable_Put.setEnabled(move);
		setPutBackupState(move && checkBx_BackupEnable_Put.isSelected());
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

	/**
	 * Copies everything on screen into the settings objects held by this frame.
	 */
	private void collectSettings()
	{
		settingsCommon.authType.data = comboBox_Authentication_Type_Common.getSelectedItem().toString();

		settingsCommon.checkKnownHosts.data = String.valueOf(chkbox_KnownHosts_Common.isSelected());
		settingsCommon.emailEnabled.data = String.valueOf(checkBx_EmailEnabled_Common.isSelected());
		settingsCommon.webEnabled.data = String.valueOf(checkBx_WebEnabled_Common.isSelected());
		settingsCommon.webPort.data = String.valueOf(spinner_WebPort_Common.getValue());
		settingsCommon.statsReportEnabled.data = String.valueOf(checkBx_StatsEnabled_Common.isSelected());
		settingsCommon.statsReportTime.data = fld_StatsTime_Common.getText().trim();
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
		settingsPut.includeSubFolders.data = String.valueOf(checkBx_SubFolders_Put.isSelected());
		settingsPut.syncEnabled.data = String.valueOf(checkBx_Sync_Put.isSelected());
		settingsPut.syncDeleteEnabled.data = String.valueOf(checkBx_Sync_Put.isSelected() && checkBx_SyncDelete_Put.isSelected());
		settingsPut.syncDeleteFoldersEnabled.data = String.valueOf(checkBx_SubFolders_Put.isSelected() && checkBx_Sync_Put.isSelected() && checkBx_SyncDelete_Put.isSelected() && checkBx_SyncDeleteFolders_Put.isSelected());
		settingsPut.overwriteNewer.data = String.valueOf(checkBx_OverwriteNewer_Put.isSelected());
		settingsPut.stabilityCheckEnabled.data = String.valueOf(checkBx_Stability_Put.isSelected());
		settingsPut.stabilitySeconds.data = spinner_StabilitySeconds_Put.getValue().toString();

		settingsGet.enabled.data = String.valueOf(checkBx_Enabled_Get.isSelected());
		settingsGet.remoteDir.data = fld_RemoteFolder_Get.getText();
		settingsGet.pollFrequencySeconds.data = spinner_PollFrequency_Get.getValue().toString();
		settingsGet.localDir.data = fld_LocalFolder_Get.getText();
		settingsGet.remoteFileMask.data = fld_Remote_Mask_Get.getText();
		settingsGet.tempFileExtension.data = fld_TempFileExtension_Get.getText();
		settingsGet.title.data = fld_Title_Get.getText();
		settingsGet.includeSubFolders.data = String.valueOf(checkBx_SubFolders_Get.isSelected());
		settingsGet.syncEnabled.data = String.valueOf(checkBx_Sync_Get.isSelected());
		settingsGet.syncDeleteEnabled.data = String.valueOf(checkBx_Sync_Get.isSelected() && checkBx_SyncDelete_Get.isSelected());
		settingsGet.syncDeleteFoldersEnabled.data = String.valueOf(checkBx_SubFolders_Get.isSelected() && checkBx_Sync_Get.isSelected() && checkBx_SyncDelete_Get.isSelected() && checkBx_SyncDeleteFolders_Get.isSelected());
		settingsGet.overwriteNewer.data = String.valueOf(checkBx_OverwriteNewer_Get.isSelected());
		settingsGet.stabilityCheckEnabled.data = String.valueOf(checkBx_Stability_Get.isSelected());
		settingsGet.stabilitySeconds.data = spinner_StabilitySeconds_Get.getValue().toString();

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
	}

	// Saves the six settings files. Returns false, having logged which one, as soon as a save fails.
	private boolean saveSettings()
	{
		writeToSystemLog("Saving connection settings to sftp_common.xml",JLogPanel.INFO);
		if (settingsUtil.saveSFTPCommonToXml(settingsCommon) == false)
		{
			return saveFailed("sftp_common.xml");
		}

		writeToSystemLog("Saving PUT settings to sftp_put.xml",JLogPanel.INFO);
		if (settingsUtil.saveSFTPPutToXml(settingsPut) == false)
		{
			return saveFailed("sftp_put.xml");
		}

		writeToSystemLog("Saving GET settings to sftp_get.xml",JLogPanel.INFO);
		if (settingsUtil.saveSFTPGetToXml(settingsGet) == false)
		{
			return saveFailed("sftp_get.xml");
		}

		writeToSystemLog("Saving email properties to email_properties.xml",JLogPanel.INFO);
		if (settingsUtil.saveEmailPropertiesToXml(emailConfig) == false)
		{
			return saveFailed("email_properties.xml");
		}

		writeToSystemLog("Saving email distribution lists to email_distribution_properties.xml",JLogPanel.INFO);
		if (settingsUtil.saveEmailDistributionListToXml(distConfig) == false)
		{
			return saveFailed("email_distribution_properties.xml");
		}

		writeToSystemLog("Saving Java jsch library properties to jsch_properties.xml",JLogPanel.INFO);
		if (settingsUtil.saveJschPropertiesToXml(jschConfig) == false)
		{
			return saveFailed("jsch_properties.xml");
		}

		return true;
	}

	private boolean saveFailed(String filename)
	{
		writeToSystemLog("Could not save " + filename + " - settings not applied.", JLogPanel.ERROR);
		return false;
	}

	/**
	 * Hands a copy of the settings held by this frame to the running threads.
	 * Each thread takes them up itself, so a transfer pass which is in progress
	 * completes with the old settings first.
	 */
	private void applySettings()
	{
		writeToSystemLog("Notifying threads of new configuration.",JLogPanel.INFO);

		// Copies, so that later edits on screen do not change the settings under the threads. The viewer changes straight away.
		Start.handOverSettings(settingsUtil.copySettings(settingsCommon), settingsUtil.copySettings(settingsPut), settingsUtil.copySettings(settingsGet), new HashMap<String, EmailRecord>(emailConfig), new HashMap<String, DistributionRecord>(distConfig), new HashMap<String, JschRecord>(jschConfig));

		displayMessage("Waiting for the current transfer to complete before applying the new settings.", messageType_WARN);

		applyTimer.restart();
	}

	/**
	 * The remote folder browse button: connects with the SFTP tab's settings
	 * as they are on screen and opens the chooser. A connection which fails
	 * used to leave the field as it was and say nothing but a log line.
	 */
	private void browseRemoteFolder(JTextField4j folderField)
	{
		collectSettings();

		displayMessage("Connecting to " + settingsCommon.remoteHost.data + " ...", messageType_INFO);

		String chosen = jcmd.viewTree(settingsCommon, jschConfig, JFrameSFTPTransfer.this, "/", folderField.getText());

		if (chosen == null)
		{
			displayMessage("Could not connect to " + settingsCommon.remoteHost.data + " to browse its folders - see the System log for the reason.", messageType_ERROR);
		}
		else
		{
			folderField.setText(chosen);
			displayMessage("", messageType_INFO);
		}
	}

	/**
	 * The two buttons after a folder field - create the safe file in that
	 * folder, and remove it again.
	 */
	private void addSafeFileButtons(JPanel panel, final JTextField4j folderField, int y, final boolean remote)
	{
		String safeFile = remote ? JSafeFile.remote : JSafeFile.local;

		JButton4j btn_SafeFileCreate = new JButton4j(Common.icon_ok);
		btn_SafeFileCreate.setToolTipText("Create the safe file " + safeFile + " in this folder");
		btn_SafeFileCreate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				createSafeFile(folderField.getText().trim(), remote);
			}
		});
		btn_SafeFileCreate.setBounds(487, y, 30, 30);
		panel.add(btn_SafeFileCreate);

		JButton4j btn_SafeFileRemove = new JButton4j(Common.icon_cancel);
		btn_SafeFileRemove.setToolTipText("Remove the safe file " + safeFile + " from this folder");
		btn_SafeFileRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				removeSafeFile(folderField.getText().trim(), remote);
			}
		});
		btn_SafeFileRemove.setBounds(517, y, 30, 30);
		panel.add(btn_SafeFileRemove);
	}

	private void createSafeFile(String folder, boolean remote)
	{
		File template = new File(JSafeFile.templateFolder, remote ? JSafeFile.remote : JSafeFile.local);

		if (folder.equals(""))
		{
			safeFileError("No folder has been entered.");
			return;
		}

		if (template.isFile() == false)
		{
			safeFileError("The safe file to copy was not found :\n\n" + template.getPath());
			return;
		}

		String safeFile = safeFilePath(folder, remote);

		String message = "This will create the safe file\n\n" + safeFileDescription(safeFile, remote) + "\n\n";
		message = message + "The safe file marks this as a folder SFTP Transfer may work in.\n";
		message = message + "Without it nothing is sent, received or deleted there.\n\n";
		message = message + "Only continue if this is the correct folder" + (remote ? " on the correct server." : "\nand, for a network share, the share is connected.") + "\n\n";
		message = message + "Create the safe file ?";

		if (safeFileConfirm(message))
		{
			if (remote)
			{
				if (openSafeFileSession())
				{
					if (jcmd.isRemoteFolderPresent(folder) == false)
					{
						safeFileError("Remote folder not found :\n\n" + folder);
					}
					else if (jcmd.isRemoteFileListed(folder, JSafeFile.remote))
					{
						displayMessage("Safe file already present : " + safeFile, messageType_WARN);
					}
					else if (jcmd.put(template.getPath(), safeFile))
					{
						safeFileDone("Safe file created : " + safeFile);
					}
					else
					{
						safeFileError("Unable to create the safe file :\n\n" + safeFile);
					}

					closeSafeFileSession();
				}
			}
			else
			{
				File localFolder = new File(folder);
				File localFile = new File(safeFile);

				if (localFolder.isDirectory() == false)
				{
					safeFileError("Local folder not found :\n\n" + localFolder.getAbsolutePath());
				}
				else if (localFile.isFile())
				{
					displayMessage("Safe file already present : " + safeFile, messageType_WARN);
				}
				else
				{
					try
					{
						Files.copy(template.toPath(), localFile.toPath());

						safeFileDone("Safe file created : " + safeFile);
					}
					catch (IOException e)
					{
						safeFileError("Unable to create the safe file :\n\n" + safeFile + "\n\n" + e.getMessage());
					}
				}
			}
		}
	}

	private void removeSafeFile(String folder, boolean remote)
	{
		if (folder.equals(""))
		{
			safeFileError("No folder has been entered.");
			return;
		}

		String safeFile = safeFilePath(folder, remote);

		String message = "This will remove the safe file\n\n" + safeFileDescription(safeFile, remote) + "\n\n";
		message = message + "Nothing will be sent, received or deleted in this folder\n";
		message = message + "until the safe file is created again.\n\n";
		message = message + "Remove the safe file ?";

		if (safeFileConfirm(message))
		{
			if (remote)
			{
				if (openSafeFileSession())
				{
					if (jcmd.isRemoteFileListed(folder, JSafeFile.remote) == false)
					{
						displayMessage("Safe file not present : " + safeFile, messageType_WARN);
					}
					else if (jcmd.rm(safeFile))
					{
						safeFileDone("Safe file removed : " + safeFile);
					}
					else
					{
						safeFileError("Unable to remove the safe file :\n\n" + safeFile);
					}

					closeSafeFileSession();
				}
			}
			else
			{
				File localFile = new File(safeFile);

				if (localFile.isFile() == false)
				{
					displayMessage("Safe file not present : " + safeFile, messageType_WARN);
				}
				else
				{
					try
					{
						Files.delete(localFile.toPath());

						safeFileDone("Safe file removed : " + safeFile);
					}
					catch (IOException e)
					{
						safeFileError("Unable to remove the safe file :\n\n" + safeFile + "\n\n" + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * The full name of the safe file. A local folder is shown in full so that
	 * a relative path such as ./send cannot be mistaken for somewhere else.
	 */
	private String safeFilePath(String folder, boolean remote)
	{
		if (remote)
		{
			return folder.endsWith("/") ? folder + JSafeFile.remote : folder + "/" + JSafeFile.remote;
		}

		return new File(new File(folder), JSafeFile.local).getAbsoluteFile().toPath().normalize().toString();
	}

	private String safeFileDescription(String safeFile, boolean remote)
	{
		if (remote)
		{
			return safeFile + "\n\non the server " + settingsCommon.remoteHost.data + ":" + settingsCommon.remotePort.data;
		}

		return safeFile;
	}

	private boolean safeFileConfirm(String message)
	{
		return JOptionPane.showConfirmDialog(JFrameSFTPTransfer.this, message, "Confirm", JOptionPane.YES_NO_OPTION, 0, Common.icon_confirm) == JOptionPane.YES_OPTION;
	}

	private void safeFileError(String message)
	{
		JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void safeFileDone(String message)
	{
		writeToSystemLog(message, JLogPanel.INFO);

		displayMessage(message, messageType_INFO);
	}

	/**
	 * As with the remote folder look-up, the connection is made with the SFTP
	 * settings as last applied or saved.
	 */
	private boolean openSafeFileSession()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		boolean connected = jcmd.connect(settingsCommon, jschConfig);

		if (connected == false)
		{
			setCursor(Cursor.getDefaultCursor());

			safeFileError("Unable to connect to " + settingsCommon.remoteHost.data + ":" + settingsCommon.remotePort.data);
		}

		return connected;
	}

	private void closeSafeFileSession()
	{
		jcmd.resetConnection();

		setCursor(Cursor.getDefaultCursor());
	}

	private void clearMessage()
	{
		displayMessage("", messageType_INFO);
	}

	// Sets every widget on the settings tabs from the settings objects, rebuilding the property rows.
	private void populateFromSettings()
	{
		comboBox_Authentication_Type_Common.setSelectedItem(settingsCommon.authType.data);
		checkBx_EmailEnabled_Common.setSelected(Boolean.valueOf(settingsCommon.emailEnabled.data));
		checkBx_WebEnabled_Common.setSelected(Boolean.valueOf(settingsCommon.webEnabled.data));
		spinner_WebPort_Common.setValue(Integer.valueOf(WebServer.parsePort(settingsCommon.webPort.data, WebServer.DEFAULT_PORT)));
		spinner_WebPort_Common.setEnabled(checkBx_WebEnabled_Common.isSelected());
		checkBx_StatsEnabled_Common.setSelected(Boolean.valueOf(settingsCommon.statsReportEnabled.data));
		fld_StatsTime_Common.setText(StatsThread.parseTime(settingsCommon.statsReportTime.data, StatsThread.DEFAULT_TIME).format(StatsThread.TIME_FORMAT));
		fld_StatsTime_Common.setEnabled(checkBx_StatsEnabled_Common.isSelected());
		if (logLevelModel != null)
		{
			logLevelModel.setSelectedItem(JLogPanel.LEVEL_NAMES[JLogPanel.levelOf(settingsCommon.logLevel.data)]);
		}
		fld_Title_Common.setText(settingsCommon.title.data);
		fld_HostAddress_Common.setText(settingsCommon.remoteHost.data);
		fld_Port_Common.setText(settingsCommon.remotePort.data);
		fld_KnownHostsFile_Common.setText(settingsCommon.knownHostsFile.data);
		chkbox_AddKnownHosts_Common.setSelected(Boolean.valueOf(settingsCommon.autoAddtoKnownHostsFile.data));
		chkbox_KnownHosts_Common.setSelected(Boolean.valueOf(settingsCommon.checkKnownHosts.data));
		fld_Username_Common.setText(settingsCommon.username.data);
		fld_Password_Common.setText(settingsCommon.password.data);
		fld_PrivateKeyFile_Common.setText(settingsCommon.privateKeyFile.data);
		fld_PublicKeyFile_Common.setText(settingsCommon.publicKeyFile.data);
		fld_PrivateKeyComment_Common.setText(settingsCommon.privateKeyComment.data);
		fld_PrivateKeyPassword_Common.setText(settingsCommon.privateKeyPassword.data);
		chckbx_PrivateKeyPassword_Common.setSelected(Boolean.valueOf(settingsCommon.privateKeyPasswordProtected.data));
		chckbx_checkPrivateKey_Common.setSelected(Boolean.valueOf(settingsCommon.checkPrivateKeyFile.data));
		fld_applicationPassword.setText(settingsCommon.applicationPassword.data);

		checkBx_Enabled_Put.setSelected(Boolean.valueOf(settingsPut.enabled.data));
		checkBx_BackupEnable_Put.setSelected(Boolean.valueOf(settingsPut.backupEnabled.data));
		fld_Title_Put.setText(settingsPut.title.data);
		fld_LocalFolder_Put.setText(settingsPut.localDir.data);
		fld_LocalMask_Put.setText(settingsPut.localFileMask.data);
		fld_BackupFolder_Put.setText(settingsPut.backupDir.data);
		spinner_BackupRetention_Put.setValue(Integer.parseInt(settingsPut.backupRetention.data));
		spinner_PollFrequency_Put.setValue(Integer.parseInt(settingsPut.pollFrequencySeconds.data));
		fld_RemoteFolder_Put.setText(settingsPut.remoteDir.data);
		fld_TempFileExtension_Put.setText(settingsPut.tempFileExtension.data);
		checkBx_SubFolders_Put.setSelected(Boolean.valueOf(settingsPut.includeSubFolders.data));
		checkBx_Sync_Put.setSelected(Boolean.valueOf(settingsPut.syncEnabled.data));
		checkBx_SyncDelete_Put.setSelected(checkBx_Sync_Put.isSelected() && Boolean.valueOf(settingsPut.syncDeleteEnabled.data));
		checkBx_SyncDeleteFolders_Put.setSelected(Boolean.valueOf(settingsPut.syncDeleteFoldersEnabled.data));
		checkBx_OverwriteNewer_Put.setSelected(Boolean.valueOf(settingsPut.overwriteNewer.data));
		checkBx_Stability_Put.setSelected(Boolean.valueOf(settingsPut.stabilityCheckEnabled.data));
		spinner_StabilitySeconds_Put.setValue(Integer.valueOf(stabilitySeconds(settingsPut.stabilitySeconds.data)));
		spinner_StabilitySeconds_Put.setEnabled(checkBx_Stability_Put.isSelected());

		checkBx_Enabled_Get.setSelected(Boolean.valueOf(settingsGet.enabled.data));
		fld_LocalFolder_Get.setText(settingsGet.localDir.data);
		fld_Remote_Mask_Get.setText(settingsGet.remoteFileMask.data);
		spinner_PollFrequency_Get.setValue(Integer.parseInt(settingsGet.pollFrequencySeconds.data));
		fld_Title_Get.setText(settingsGet.title.data);
		fld_RemoteFolder_Get.setText(settingsGet.remoteDir.data);
		fld_TempFileExtension_Get.setText(settingsGet.tempFileExtension.data);
		checkBx_SubFolders_Get.setSelected(Boolean.valueOf(settingsGet.includeSubFolders.data));
		checkBx_Sync_Get.setSelected(Boolean.valueOf(settingsGet.syncEnabled.data));
		checkBx_SyncDelete_Get.setSelected(checkBx_Sync_Get.isSelected() && Boolean.valueOf(settingsGet.syncDeleteEnabled.data));
		checkBx_SyncDeleteFolders_Get.setSelected(Boolean.valueOf(settingsGet.syncDeleteFoldersEnabled.data));
		checkBx_OverwriteNewer_Get.setSelected(Boolean.valueOf(settingsGet.overwriteNewer.data));
		checkBx_Stability_Get.setSelected(Boolean.valueOf(settingsGet.stabilityCheckEnabled.data));
		spinner_StabilitySeconds_Get.setValue(Integer.valueOf(stabilitySeconds(settingsGet.stabilitySeconds.data)));
		spinner_StabilitySeconds_Get.setEnabled(checkBx_Stability_Get.isSelected());

		panel_Properties_Scrollable.removeAll();

		for (HashMap.Entry<String, JschRecord> entry : jschConfig.entrySet())
		{
			JschPanel ped = new JschPanel();

			ped.fld_Id.setText(entry.getKey());

			ped.fld_Value.setText(entry.getValue().value);
			ped.fld_Encrypted.setSelected(Boolean.valueOf(entry.getValue().encrypted));
			ped.fld_Enabled.setSelected(Boolean.valueOf(entry.getValue().enabled));

			panel_Properties_Scrollable.add(ped);
		}

		panel_Email_Scrollable.removeAll();

		for (HashMap.Entry<String, EmailRecord> entry : emailConfig.entrySet())
		{
			EmailPanel ped = new EmailPanel();

			ped.fld_Property.setText(entry.getKey());

			ped.fld_Value.setText(entry.getValue().value);
			ped.fld_Encrypted.setSelected(Boolean.valueOf(entry.getValue().encrypted));
			ped.fld_Enabled.setSelected(Boolean.valueOf(entry.getValue().enabled));

			panel_Email_Scrollable.add(ped);
		}

		panel_Distribution_Scrollable.removeAll();

		// By id, as the test-email chooser lists them - the map's own order changes from run to run.
		ArrayList<String> distIds = new ArrayList<String>(distConfig.keySet());
		java.util.Collections.sort(distIds);

		for (String id : distIds)
		{
			DistributionRecord record = distConfig.get(id);
			DistributionPanel dl = new DistributionPanel();

			dl.fld_ListID.setText(id);

			dl.fld_Address.setText(record.addressList);
			dl.fld_MaxFrequency.setValue(record.maxFrequencyMins);
			dl.fld_Enabled.setSelected(Boolean.valueOf(record.enabled));

			panel_Distribution_Scrollable.add(dl);
		}

		panel_Properties_Scrollable.revalidate();
		panel_Properties_Scrollable.repaint();
		panel_Email_Scrollable.revalidate();
		panel_Email_Scrollable.repaint();
		panel_Distribution_Scrollable.revalidate();
		panel_Distribution_Scrollable.repaint();

		// setSelected does not fire the checkbox listeners, so the dependent widgets are set here.
		setPrivateKeyState(chckbx_checkPrivateKey_Common.isSelected());
		setPrivateKeyPasswordState(chckbx_PrivateKeyPassword_Common.isSelected());
		setKnownHostsState(chkbox_KnownHosts_Common.isSelected());
		setSyncDeleteState(checkBx_SubFolders_Put, checkBx_Sync_Put, checkBx_SyncDelete_Put, checkBx_SyncDeleteFolders_Put, checkBx_OverwriteNewer_Put, lbl_SyncHint_Put, SYNC_HINT_PUT, MOVE_HINT_PUT);
		setSyncDeleteState(checkBx_SubFolders_Get, checkBx_Sync_Get, checkBx_SyncDelete_Get, checkBx_SyncDeleteFolders_Get, checkBx_OverwriteNewer_Get, lbl_SyncHint_Get, SYNC_HINT_GET, MOVE_HINT_GET);
		setPutBackupRowState();
	}

	private JPanel[] settingsPanels()
	{
		return new JPanel[]
		{ sftp_common_Tab_Panel, sftp_put_Tab_Panel, sftp_get_Tab_Panel, propertes_Tab_Panel, email_Tab_Panel };
	}

	// One string holding the value of every editable widget on the settings tabs, in component order.
	// Spinners contribute their editor text, so a value typed without Enter counts as a change.
	private String currentFingerprint()
	{
		StringBuilder sb = new StringBuilder();

		for (JPanel panel : settingsPanels())
		{
			collectFingerprint(panel, sb);
		}

		return sb.toString();
	}

	private void collectFingerprint(Container container, StringBuilder sb)
	{
		for (Component comp : container.getComponents())
		{
			if (comp instanceof JTextComponent)
			{
				sb.append(((JTextComponent) comp).getText()).append('\u0001');
			}
			else if (comp instanceof JToggleButton)
			{
				sb.append(((JToggleButton) comp).isSelected() ? '1' : '0').append('\u0001');
			}
			else if (comp instanceof JComboBox)
			{
				sb.append(String.valueOf(((JComboBox<?>) comp).getSelectedItem())).append('\u0001');
			}

			if (comp instanceof Container)
			{
				collectFingerprint((Container) comp, sb);
			}
		}
	}

	// Attaches a change listener to every editable widget on the settings tabs (once per widget).
	private void wireChangeListeners()
	{
		for (JPanel panel : settingsPanels())
		{
			wireChangeListeners(panel);
		}
	}

	private void wireChangeListeners(Container container)
	{
		for (Component comp : container.getComponents())
		{
			if (changeWiredComponents.add(comp))
			{
				if (comp instanceof JTextComponent)
				{
					((JTextComponent) comp).getDocument().addDocumentListener(new DocumentListener()
					{
						public void insertUpdate(DocumentEvent e)
						{
							refreshSettingsButtons();
						}

						public void removeUpdate(DocumentEvent e)
						{
							refreshSettingsButtons();
						}

						public void changedUpdate(DocumentEvent e)
						{
							refreshSettingsButtons();
						}
					});
				}
				else if ((comp instanceof JToggleButton) || (comp instanceof JComboBox))
				{
					ItemListener listener = new ItemListener()
					{
						public void itemStateChanged(ItemEvent e)
						{
							refreshSettingsButtons();
						}
					};

					if (comp instanceof JToggleButton)
					{
						((JToggleButton) comp).addItemListener(listener);
					}
					else
					{
						((JComboBox<?>) comp).addItemListener(listener);
					}
				}
				else if (comp instanceof JSpinner)
				{
					((JSpinner) comp).addChangeListener(new ChangeListener()
					{
						public void stateChanged(ChangeEvent e)
						{
							refreshSettingsButtons();
						}
					});
				}
			}

			if (comp instanceof Container)
			{
				wireChangeListeners((Container) comp);
			}
		}
	}

	// Pushes a value typed into a spinner, but not confirmed with Enter, into the spinner's model.
	// The toolbar buttons do not take the focus, so nothing else commits it before collectSettings.
	private void commitSpinnerEdits()
	{
		for (JPanel panel : settingsPanels())
		{
			commitSpinnerEdits(panel);
		}
	}

	private void commitSpinnerEdits(Container container)
	{
		for (Component comp : container.getComponents())
		{
			if (comp instanceof JSpinner)
			{
				try
				{
					((JSpinner) comp).commitEdit();
				}
				catch (Exception ex)
				{
					// Unparseable text - the spinner keeps its last good value.
				}
			}

			if (comp instanceof Container)
			{
				commitSpinnerEdits((Container) comp);
			}
		}
	}

	private boolean hasUnsavedChanges()
	{
		return (savedFingerprint != null) && (savedFingerprint.equals(currentFingerprint()) == false);
	}

	private void refreshSettingsButtons()
	{
		if ((btn_Apply == null) || (btn_Reload == null) || (savedFingerprint == null))
		{
			return;
		}

		boolean changed = hasUnsavedChanges();

		btn_Apply.setEnabled(changed);
		btn_Reload.setEnabled(changed);
	}

	/**
	 * Apply: saves the six settings files, hands the settings to this program's
	 * threads, then leaves a reload request for a service running on this
	 * machine. The request is written last, and only when every save
	 * succeeded, so a request always means whole files. A failed save stops
	 * everything - the files on disk are not trusted and the threads keep what
	 * they have.
	 */
	private void applyChanges()
	{
		commitSpinnerEdits();
		collectSettings();

		// Checked here, not in collectSettings(), which the remote folder browse also calls.
		if (checkBx_StatsEnabled_Common.isSelected() && (StatsThread.parseTime(settingsCommon.statsReportTime.data, null) == null))
		{
			displayMessage("Settings NOT applied - Daily Statistics time must be HH:mm (e.g. 06:00).", messageType_ERROR);
			return;
		}

		if (saveSettings() == false)
		{
			displayMessage("Settings NOT applied - a settings file could not be saved, see the System log.", messageType_ERROR);
			return;
		}

		savedFingerprint = currentFingerprint();

		refreshSettingsButtons();

		applySettings();

		if (ReloadRequest.request())
		{
			reloadRequestedAt = Instant.now();

			writeToSystemLog("Reload request written for any service running on this machine : " + ReloadRequest.FILENAME, JLogPanel.INFO);
		}
		else
		{
			reloadRequestedAt = null;

			writeToSystemLog("Reload request could not be written : " + ReloadRequest.getFile().getPath() + " - a service on this machine will not pick up the new settings until restarted.", JLogPanel.WARN);
		}
	}

	// Re-reads the saved files into the form, discarding edits not yet applied. What the threads are running is untouched.
	private void reloadSettings()
	{
		writeToSystemLog("Reloading saved settings - changes not yet applied discarded.", JLogPanel.INFO);

		settingsCommon = settingsUtil.readSFTPCommonFromXml();
		settingsPut = settingsUtil.readSFTPPutFromXml();
		settingsGet = settingsUtil.readSFTPGetFromXml();
		emailConfig = settingsUtil.readEmailPropertiesFromXml();
		distConfig = settingsUtil.readDistributionListFromXml();
		jschConfig = settingsUtil.readJschPropertiesFromXml();

		populateFromSettings();
		wireChangeListeners();

		savedFingerprint = currentFingerprint();

		refreshSettingsButtons();

		displayMessage("Saved settings reloaded.", messageType_INFO);
	}

	private void openHelp()
	{
		try
		{
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			{
				Desktop.getDesktop().browse(new URI(Common.helpURL));
			}
			else
			{
				JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, "Unable to open a browser on this system.\n\nHelp is at " + Common.helpURL, "Help", JOptionPane.INFORMATION_MESSAGE, Common.icon_confirm);
			}
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(JFrameSFTPTransfer.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, Common.icon_confirm);
		}
	}

	/**
	 * The Send Test Email button: one message to a list chosen from those on
	 * screen, with the SMTP rows on screen, sent at once on a worker thread.
	 * Ignores the Email Notifications box, the queue and the frequency limit -
	 * it is there to prove the credentials.
	 */
	private void sendTestEmail()
	{
		commitSpinnerEdits();
		collectSettings();

		ArrayList<String> ids = new ArrayList<String>(distConfig.keySet());
		java.util.Collections.sort(ids);

		if (ids.isEmpty())
		{
			displayMessage("No distribution list on screen to send a test email to.", messageType_ERROR);
			return;
		}

		Object choice = JOptionPane.showInputDialog(JFrameSFTPTransfer.this, "Send the test email to which distribution list ?", "Test Email", JOptionPane.QUESTION_MESSAGE, Common.icon_confirm, ids.toArray(), ids.get(0));

		if (choice == null)
		{
			return;
		}

		final String id = choice.toString();
		final DistributionRecord list = distConfig.get(id);
		final HashMap<String, EmailRecord> props = new HashMap<String, EmailRecord>(emailConfig);
		final boolean notificationsOff = (checkBx_EmailEnabled_Common.isSelected() == false);
		final String host = String.valueOf(props.containsKey("mail.smtp.host") ? props.get("mail.smtp.host").value : "");

		Notify.configure(settingsCommon);

		displayMessage("Sending test email to [" + id + "] ...", messageType_INFO);
		btn_TestEmail.setEnabled(false);
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

		new SwingWorker<String, Void>()
		{
			protected String doInBackground()
			{
				return Start.emailthread.sendNow(props, list, Notify.subject("Test Email"), Notify.body("Test Email", "List : " + id, "Sent from the Email Properties tab with the settings on screen."));
			}

			protected void done()
			{
				setCursor(java.awt.Cursor.getDefaultCursor());
				btn_TestEmail.setEnabled(true);

				String reason;

				try
				{
					reason = get();
				}
				catch (Exception e)
				{
					reason = e.getMessage();
				}

				String suffix = notificationsOff ? "  (Email Notifications is unticked - the threads will not send)" : "";

				if (reason == null)
				{
					writeToSystemLog("Test email sent to [" + id + "] " + list.addressList + " via " + host, JLogPanel.INFO);
					displayMessage("Test email sent to " + list.addressList + " via " + host + suffix, messageType_INFO);
				}
				else
				{
					writeToSystemLog("Test email to [" + id + "] failed - " + reason, JLogPanel.ERROR);
					displayMessage("Test email failed - " + reason + suffix, messageType_ERROR);
				}
			}
		}.execute();
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
