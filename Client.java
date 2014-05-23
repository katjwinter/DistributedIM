import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

/**
 * Chat application that uses a DHT for rendezvous
 * @author Kat Winter
 */
public class Client {

	private JFrame frame;
	private JTextField recipientName;
	private JTextField usernameField;
	private JTextField bootstrapField;
	private JCheckBox beBootstrapCheckBox;
	private JCheckBox createNetworkCheckBox;
	private JButton loginButton;
	private JButton chatButton;
	private JLabel statusLabel;
	private ChatButtonListener chatListener;
	private LogInListener loginListener;
	private DisconnectListener disconnectListener;
	private Node DHT;
	private String myIP;
	private String username;
	private boolean newNetwork = false;
	private boolean beBoot = false;
	private boolean joined = false;
	private boolean loggedIn = false;
	private ArrayList<Socket> activeConnections;

	/**
	 * Constructor for the client
	 * Initializes the contents of the frame.
	 * Creates a new Node in the DHT.
	 */
	public Client() {

		activeConnections = new ArrayList<Socket>();
		
		createNode();
		
		frame = new JFrame();
		frame.setBounds(100, 100, 300, 360);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new ApplicationCloseListener());
		frame.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setAlignmentX(0.0f);
		panel.setBounds(0, 0, 298, 330);
		frame.getContentPane().add(panel);
		panel.setLayout(null);
		
		chatButton = new JButton("Initiate Chat");
		chatButton.setBounds(70, 268, 155, 25);
		panel.add(chatButton);
		
		recipientName = new JTextField();
		recipientName.setBounds(159, 235, 114, 19);
		recipientName.setEditable(false);
		panel.add(recipientName);
		recipientName.setColumns(10);
		
		loginListener = new LogInListener();
		disconnectListener = new DisconnectListener();
		loginButton = new JButton("Log In");
		loginButton.setBounds(70, 132, 155, 25);
		loginButton.addActionListener(loginListener);
		panel.add(loginButton);
		
		usernameField = new JTextField();
		usernameField.setColumns(10);
		usernameField.setBounds(148, 15, 138, 19);
		panel.add(usernameField);
		
		JLabel usernameLabel = new JLabel("Your username:");
		usernameLabel.setBounds(12, 12, 130, 25);
		panel.add(usernameLabel);
		
		JLabel friendnameLabel = new JLabel("Friend's Username:");
		friendnameLabel.setBounds(12, 232, 155, 25);
		panel.add(friendnameLabel);
		
		JLabel bootstrapLabel = new JLabel("Bootstrap Server:");
		bootstrapLabel.setBounds(12, 46, 155, 15);
		panel.add(bootstrapLabel);
		
		bootstrapField = new JTextField();
		bootstrapField.setBounds(158, 46, 128, 19);
		panel.add(bootstrapField);
		bootstrapField.setColumns(10);
		
		statusLabel = new JLabel("Your Status: Disconnected");
		statusLabel.setBounds(12, 176, 261, 15);
		panel.add(statusLabel);
		
		createNetworkCheckBox = new JCheckBox("Create New Network?");
		createNetworkCheckBox.setIconTextGap(20);
		createNetworkCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
		createNetworkCheckBox.setBounds(8, 69, 202, 23);
		createNetworkCheckBox.addActionListener(new NewNetworkListener());
		panel.add(createNetworkCheckBox);
		
		beBootstrapCheckBox = new JCheckBox("Be a bootstrap?");
		beBootstrapCheckBox.setIconTextGap(20);
		beBootstrapCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
		beBootstrapCheckBox.setBounds(8, 96, 202, 23);
		beBootstrapCheckBox.addActionListener(new BeBootStrapListener());
		panel.add(beBootstrapCheckBox);
		
		frame.setTitle("KAT CHAT");
		frame.setVisible(true);
	}
	
	/**
	 * Gets the IP address for the client and creates a new Node object to represent
	 * this client in the DHT.
	 */
	private void createNode() {
		
		// Open a brief socket to google.com to allow retrieval of local IP address even on Debian-based systems
		// which otherwise return 127.0.0.1 when InetAddress.getLocalHost().getHostAddress() is called.
		try {
			Socket testSocket = new Socket("www.google.com", 80);
			myIP = testSocket.getLocalAddress().getHostAddress();
			testSocket.close();
		} catch (UnknownHostException e1) {
			System.out.println("Unable to reach google.com for local IP testing");
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Unable to open socket to google.com on port 80 for local IP testing");
			e1.printStackTrace();
		}
		
		DHT = new Node(myIP);
	}

	/**
	 * Check if the username is already taken and if not,
	 * register with the DHT by "putting" the username and IP pair into the DHT
	 * @param username Username the client wants to use
	 * @throws RemoteException
	 */
	private void registerWithDHT(String username) throws RemoteException {

		boolean nameTaken;

		nameTaken = DHT.checkKeyExists(username);
		
		if (nameTaken) {
			
			JOptionPane.showMessageDialog(frame, "Username already taken.");
			clearLogin();
		}

		else {
			
			DHT.put(username, myIP);
			loggedIn = true;
		}
	}
	
	/**
	 * Method to listen for incoming connections on a hardcoded port
	 * When a connection comes in on that port, a new conversation is started.
	 */
	private void startReceiving() {
		
		ServerSocket listener;
		
		try {
			
			listener = new ServerSocket(4444);
			Socket sender;
			
			int i = 0;
			int maxConnections = 0;	
			
			while((i++ < maxConnections) || (maxConnections == 0)){

		        sender = listener.accept();
		        activeConnections.add(sender);
		        
		        Conversation connection = new Conversation(sender, sender.getInputStream(), sender.getOutputStream());
		    }
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Something failed in the receiving socket");
		}
	}
	
	/**
	 * Clears all information in the chat interface - resets everything.
	 */
	private void clearLogin() {
		
		loginButton.setText("Log In");
		loginButton.removeActionListener(disconnectListener);
		loginButton.addActionListener(loginListener);
		
		statusLabel.setText("Your Status: Disconnected");
		
		usernameField.setText("");
		username = "";
		usernameField.setEditable(true);
		bootstrapField.setText("");
		bootstrapField.setEditable(true);
		
		createNetworkCheckBox.setEnabled(true);
		createNetworkCheckBox.setSelected(false);
		newNetwork = false;
		beBootstrapCheckBox.setEnabled(true);
		beBootstrapCheckBox.setSelected(false);
		beBoot = false;
		
		recipientName.setEditable(false);
		chatButton.removeActionListener(chatListener);
	}
	
	/**
	 * Checks that the username field is not blank and that
	 * the bootstrap field is not blank (unless the box is checked
	 * to create a new network)
	 * @return True if there are no problems with the input fields
	 * False if the user has not input everything correctly.
	 */
	private boolean validateFields() {
		
		if (usernameField.getText().equalsIgnoreCase("")) {
			
			return false;
		}
		
		if (!newNetwork) {
			
			if (bootstrapField.getText().equalsIgnoreCase("")) {
				
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Listener for the Log In button. 
	 * Attempts to create or join a DHT and then log in with the
	 * username chosen by the user.
	 * Updates the connection status and
	 * the relevant checkboxes/fields so the user can now chat with someone
	 * or receive incoming chats.
	 * @author Kat Winter
	 */
	class LogInListener implements ActionListener {
		
		public void actionPerformed(ActionEvent evt) {
			
			loginButton.setText("Disconnect");
			loginButton.removeActionListener(loginListener);
			loginButton.addActionListener(disconnectListener);
			statusLabel.setText("Your Status: Connected");

			usernameField.setEditable(false);
			bootstrapField.setEditable(false);
			createNetworkCheckBox.setEnabled(false);
			beBootstrapCheckBox.setEnabled(false);

			recipientName.setEditable(true);
			chatListener = new ChatButtonListener();
			chatButton.addActionListener(chatListener);
			
			if (validateFields()) {

				if (newNetwork) {

					try {
						
						DHT.create();
						
					} catch (RemoteException e) {
						JOptionPane.showMessageDialog(frame, "Unable to create registry on port 1099. Cannot create network. Please provide a bootstrap server.");
						clearLogin();
					}
				}

				else {

					String bootstrapIP = bootstrapField.getText();

					if (beBoot) {

						try {
							
							DHT.beBootStrap();
							
						} catch (RemoteException e) {
							JOptionPane.showMessageDialog(frame, "Unable to create registry on port 1099. Joining network as a regular node instead.");
							beBootstrapCheckBox.setSelected(false);
						}
					}
					
					if (!joined) {

						try {

							DHT.join(bootstrapIP);
							joined = true;

						} catch (RemoteException e) {
							JOptionPane.showMessageDialog(frame, "Unable to join using this bootstrap server");
							clearLogin();
						} catch (NotBoundException e) {
							JOptionPane.showMessageDialog(frame, "The selected bootstrap server is not bound");
							clearLogin();
						}
					}
				}

				try {
					
					username = usernameField.getText();
					registerWithDHT(username);
					
					Thread test = new Thread() {
						public void run() {
							startReceiving();
						}
					};
					test.start();
					
				} catch (RemoteException e) {
					JOptionPane.showMessageDialog(frame, "DHT failure. Unable to log in at this time.");
					clearLogin();
				}
			}
			
			else {
				
				JOptionPane.showMessageDialog(frame, "Invalid login parameters. Make sure you are entering a username and bootstrap IP, or username and creating a new network");
				clearLogin();
			}
		}
	}

	/**
	 * Listener for the Disconnect button which will close out all connections
	 * and open sockets.
	 * @author Kat Winter
	 */
	class DisconnectListener implements ActionListener {
		
		public void actionPerformed(ActionEvent evt) {
			
			for (Socket connection : activeConnections) {
				try {
					connection.close();
				} catch (IOException e) {
					System.out.println("Unable to close connection");
				}
			}
			
			DHT.remove(username);
			loggedIn = false;
			DHT.leave();
			statusLabel.setText("Your Status: Disconnected");
			clearLogin();
		}
	}
	
	/**
	 * Listener for the Chat button - checks the DHT for the provided username
	 * in order to get the IP address for them. Then creates a socket with that IP
	 * information to rendezvous and initiate a conversation.
	 * @author Kat Winter
	 */
	class ChatButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent evt) {

			String recipientIP = null;

			try {

				recipientIP = (String) DHT.get(recipientName.getText());

			} catch (RemoteException e1) {
				JOptionPane.showMessageDialog(frame, "DHT failure. Unable to search for recipient at this time");
			}

			if (recipientIP != null) {
				
				try {
					Socket recipientSocket = new Socket(recipientIP, 4444);
					activeConnections.add(recipientSocket);
					Conversation initiateChat = new Conversation(recipientSocket, recipientSocket.getInputStream(), recipientSocket.getOutputStream());
					
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Error creating socket");
				}
			}

			else {
				JOptionPane.showMessageDialog(frame, "That recipient was not found");
				recipientName.setText("");
			}
		}
	}
	
	/**
	 * Listener for the new network checkbox which indicates
	 * if the user wants to create a new Ring instead of joining
	 * an existing DHT.
	 * @author Kat Winter
	 */
	class NewNetworkListener implements ActionListener {
		
		public void actionPerformed(ActionEvent evt) {
			
			if (createNetworkCheckBox.isSelected()) {
				
				newNetwork = true;
				
				bootstrapField.setText("");
				bootstrapField.setEditable(false);
				
				beBootstrapCheckBox.setEnabled(false);
			}
			
			else {
				
				newNetwork = false;
				
				bootstrapField.setEditable(true);
				beBootstrapCheckBox.setEnabled(true);
			}
		}
	}
	
	/**
	 * Listener for the Be a Bootstrap checkbox. If selected, sets up the
	 * client as a bootstrap in the DHT ring.
	 * Note that once you have logged in successfully once as a bootstrap or created a new network, you will
	 * technically remain a bootstrap until you have closed out the application.
	 */
	class BeBootStrapListener implements ActionListener {
		
		public void actionPerformed(ActionEvent evt) {
			
			if (beBootstrapCheckBox.isSelected()) {
				
				beBoot = true;
				createNetworkCheckBox.setEnabled(false);
			}
			
			else {
				
				beBoot = false;
				createNetworkCheckBox.setEnabled(true);
			}
		}
	}
	
	/**
	 * Attempts to shut down gracefully when the application is closed
	 * by properly removing the client information from the DHT and
	 * having all keys/data transferred to other nodes.
	 * Note if this is the final node in the ring, then currently this will bring up 
	 * exceptions.
	 * @author Kat Winter
	 */
	class ApplicationCloseListener extends WindowAdapter {
		
		public void windowClosing(WindowEvent evt) {
			
			if (loggedIn) {
				
				DHT.remove(username);
				DHT.leave();
			}
			
			frame.dispose();
			System.exit(0);
		}
	}
}
