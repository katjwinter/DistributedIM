import java.net.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;

/**
 * Class to represent a single chat conversation
 * @author Kat Winter
 */
public class Conversation implements Runnable {
	
	private PrintStream output;
	private BufferedReader input;
	private Socket chatPartner;
	private JFrame frame;
	protected Thread listenerThread;
	private JTextArea incoming;
	private JTextPane outgoing;
	private JButton sendButton;
	
	/**
	 * Cosntructor for the conversation. Sets up the GUI interface and listeners.
	 * Creates a new thread for the conversation.
	 * @param chatPartner Socket for the other participant in the conversation
	 * @param inputStream Inputstream for the other participant
	 * @param outputStream Outputstream for the other participant
	 */
	public Conversation(Socket chatPartner, InputStream inputStream, OutputStream outputStream) {
		
		this.chatPartner = chatPartner;	
	    
		// Create buffered reader and dataoutputstream from the inputstream and outputstream that were passed in
	    input = new BufferedReader(new InputStreamReader(inputStream));
	    output = new PrintStream(outputStream);
	    
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 368);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setBounds(5, 5, 448, 360);
		frame.getContentPane().add(panel);
		panel.setLayout(null);
		
		incoming = new JTextArea();
		incoming.setText("");
		incoming.setBounds(12, 0, 424, 160);
		incoming.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		incoming.setEditable(false);
		panel.add(incoming);
		
		outgoing = new JTextPane();
		outgoing.setBounds(12, 169, 424, 89);
		outgoing.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		panel.add(outgoing);
		
		sendButton = new JButton("Send");
		sendButton.setBounds(144, 270, 150, 30);
		sendButton.addActionListener(new SendListener());
		panel.add(sendButton);
		
		frame.setVisible(true);
		
		listenerThread = new Thread(this);
		listenerThread.start();
	}
	
	private boolean connected = true;
	
	/**
	 * Reads input from the other participant and tests to make sure
	 * the connective is still active. If the connection is active, incoming text
	 * is displayed in the upper display window. If the connection is not still active,
	 * attempts to close gracefully and notify the user that the partner disconnected.
	 */
	public void run() {
			
		String line = "";
		
		while (connected) {
			
			try {
				
				line = input.readLine();
				
				if (line != null) { // ignore if partner already disconnected
				
					if (!(line.equals("TEST"))) { // ignore tests from chat partner
						incoming.append("Them: " + line + "\n");
					}
				}
				
			} catch (IOException e) {
				return; // stop the thread if you can't even get input
			}
			
			output.print("TEST\n"); // test if chat partner is still connected
			if (output.checkError()) {
				connected = false;
			}
		}
		
		// close streams and socket gracefully
	     try {
	    	 input.close();
	    	 output.close();
	    	 chatPartner.close();
		     } catch (IOException ex) {
		    	 ex.printStackTrace ();  
		     }
	     
	     if (line != null) { // if socket closed there's no more incoming data to print anyway
	    	 if (!(line.equals("TEST"))) {
	    		 incoming.append("Them: " + line + "\n"); // print any final incoming (non-test) data
	    	 }
	     }
	     
	     incoming.append("CHAT PARTNER HAS DISCONNECTED");
	}
	
	/**
	 * Listener for the send button. When clicked, the text in the send box is 
	 * transmitted to the other participant as well as displayed in the display window
	 * of this conversation.
	 * @author Kat Winter
	 */
	class SendListener implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			
			String sendText = outgoing.getText();
			
			incoming.append("You: "+ sendText + "\n");

			output.println(sendText);
			outgoing.setText("");
		}	
	}
}