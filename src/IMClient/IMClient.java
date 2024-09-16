package IMClient;
/*
IMClient.java - Instant Message client using UDP and TCP communication.

Text-based communication of commands.
*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class IMClient {
	// Protocol and system constants
	public static String serverAddress = "localhost";
	public static int TCPServerPort = 1234;					// connection to server
	
	/* 	
	 * This value will need to be unique for each client you are running
	 */
	public static int TCPMessagePort = (int)Math.floor(Math.random() *(1233 - 1111 + 1) + 1111);				// port for connection between 2 clients
	
	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";

	private BufferedReader reader;							// Used for reading from standard input

	// Client state variables
	private String userId;
	private String status;
	private ArrayList<BuddyStatusRecord> buddies;
	private Socket connection;

	public static void main(String []argv) throws Exception
	{
		IMClient client = new IMClient();
		client.execute();
	}

	public IMClient()
	{
		// Initialize variables
		userId = null;
		status = null;
	}


	public void execute() throws IOException
	{
		initializeThreads();

		String choice;
		reader = new BufferedReader(new InputStreamReader(System.in));

		printMenu();
		choice = getLine().toUpperCase();

		while (!choice.equals("X"))
		{
			if (choice.equals("Y"))
			{	// Must have accepted an incoming connection
				acceptConnection();
			}
			else if (choice.equals("N"))
			{	// Must have rejected an incoming connection
				rejectConnection();
			}
			else if (choice.equals("R"))				// Register
			{	registerUser();
			}
			else if (choice.equals("L"))		// Login as user id
			{	loginUser();
			}
			else if (choice.equals("A"))		// Add buddy
			{	addBuddy();
			}
			else if (choice.equals("D"))		// Delete buddy
			{	deleteBuddy();
			}
			else if (choice.equals("S"))		// Buddy list status
			{	buddyStatus();
			}
			else if (choice.equals("M"))		// Start messaging with a buddy
			{	buddyMessage();
			}
			else
				System.out.println("Invalid input!");

			printMenu();
			choice = getLine().toUpperCase();
		}
		shutdown();
	}

	private void initializeThreads() throws IOException
	{
		TCPMessenger tcpMessenger = new TCPMessenger(this);
    Thread tcpThread = new Thread(tcpMessenger);
    tcpThread.start();

    // Start UDP Send thread
    InetAddress serverAddress = InetAddress.getByName(IMClient.serverAddress);
    UDPSendThread udpSendThread = new UDPSendThread(this, serverAddress, 1235);
    Thread udpSend = new Thread(udpSendThread);
    udpSend.start();

    // Start UDP Receive thread
    UDPReceiveThread udpReceiveThread = new UDPReceiveThread(this);
    Thread udpReceive = new Thread(udpReceiveThread);
    udpReceive.start();

		buddies = new ArrayList<BuddyStatusRecord>();
	}

	private void registerUser() throws IOException
	{	// Register user id
		
		System.out.print("Enter user id: ");
		userId = getLine();
		String request = "REG " + userId;


		
		Socket clientSocket = new Socket(serverAddress, TCPServerPort);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		outToServer.writeBytes(request+'\n');
		String response = inFromServer.readLine();
		System.out.println("FROM SERVER: "+ response);

		if (response.equals("200 OK")) {
			status = onlineStatus;
			System.out.println("User registered successfully: " + userId);
	} else {
			System.out.println("Error registering user: " + response);
	}
		
		clientSocket.close();
		
	}

	private void loginUser() throws IOException
	{	// Login an existing user (no verification required - just set userId to input)
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("User id set to: "+userId);
		status = onlineStatus;

		String statusUpdate = "SET " + userId + " " + status + " " + TCPMessagePort;

		DatagramSocket clientSocket = new DatagramSocket();
		
		InetAddress IPAddress = InetAddress.getByName(serverAddress);

		byte[] sendData = statusUpdate.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
		clientSocket.send(sendPacket);

		clientSocket.close();
  	}

	private void addBuddy() throws UnknownHostException, IOException
	{	// Add buddy if have current user id
		System.out.print("Enter buddy id: ");
    String buddyId = getLine();
    String request = "ADD " + userId + " " + buddyId;
    
    Socket clientSocket = new Socket(serverAddress, TCPServerPort);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    outToServer.writeBytes(request + '\n');
    String response = inFromServer.readLine();
    System.out.println("FROM SERVER: " + response);


    
    clientSocket.close();
	}

	private void deleteBuddy() throws UnknownHostException, IOException
	{	// Delete buddy if have current user id
		System.out.print("Enter buddy id: ");
    String buddyId = getLine();
    String request = "DEL " + userId + " " + buddyId;
    
    Socket clientSocket = new Socket(serverAddress, TCPServerPort);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    outToServer.writeBytes(request + '\n');
    String response = inFromServer.readLine();
    System.out.println("FROM SERVER: " + response);
    
    clientSocket.close();
	}

	private void buddyStatus() throws IOException
	{	// Print out buddy status (need to store state in instance variable that received from previous UDP message)
		DatagramSocket clientSocket = new DatagramSocket();
		
		InetAddress IPAddress = InetAddress.getByName(serverAddress);

		String request = "GET " + userId;

		byte[] sendData = request.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
		clientSocket.send(sendPacket);

		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);

		String response = new String(receivePacket.getData());

		System.out.println("Buddy List:\n" + response);
		clientSocket.close();

		String[] lines = response.split("\n");
		for (String line : lines) {
				String[] parts = line.trim().split(" ");
				if (parts.length == 5) {
						String buddyId = parts[0];
						String buddyStatus = parts[1] + " " +  parts[2];
						String buddyIP = parts[3];
						String buddyPort = parts[4];

						updateBuddyStatus(buddyId, buddyStatus, buddyIP, buddyPort);
				}
		}
		
	}

	private void buddyMessage() throws NumberFormatException, UnknownHostException, IOException
	{	// Make connection to a buddy that is online
		System.out.print("Enter buddy id: ");
    String bId = getLine();
		// Must verify that they are online and should prompt to see if they accept the connection
		BuddyStatusRecord buddy = findBuddyById(bId);

		if (buddy == null) {
			System.out.println("Buddy not found.");
			return;
	}

	if (!buddy.isOnline()) {
			System.out.println("Buddy is not online.");
			return;
	}

	System.out.println("Attempting to connect...");
  Socket buddySocket = new Socket(buddy.IPaddress, Integer.parseInt(buddy.buddyPort));

	BufferedReader inFromBuddy = new BufferedReader(new InputStreamReader(buddySocket.getInputStream()));
  DataOutputStream outToBuddy = new DataOutputStream(buddySocket.getOutputStream());

    String response = inFromBuddy.readLine();
    if ("ACCEPTED".equals(response)) {
        System.out.println("Buddy accepted connection.");
        System.out.println("Enter your text to send to buddy. Enter q to quit.");

        String message;
        while (true) {
            System.out.print("> ");
            message = getLine();
            if (message.equalsIgnoreCase("q")) break;
            outToBuddy.writeBytes(message + "\n");

            String reply = inFromBuddy.readLine();
            if (reply == null || reply.equalsIgnoreCase("q")) {
                System.out.println("Buddy has closed the connection.");
                break;
            }
            System.out.println("B: " + reply);
        }
    } else {
        System.out.println("Buddy rejected connection.");
    }

    buddySocket.close();
	}

	private BuddyStatusRecord findBuddyById(String buddyId) {
    for (BuddyStatusRecord record : buddies) {
        if (record.buddyId.equals(buddyId)) {
            return record;
        }
    }
    return null;
}

	private void shutdown() throws IOException
	{	// Close down client and all threads
		status = offlineStatus;
		String statusUpdate = "SET " + userId + " " + status + " " + TCPMessagePort;
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(serverAddress);
        byte[] sendData = statusUpdate.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1235);
        clientSocket.send(sendPacket);
        clientSocket.close();

				if (connection != null && !connection.isClosed()) {
					try {
							connection.close();
					} catch (IOException e) {
							System.out.println("Error closing TCP connection: " + e.getMessage());
					}
			}

			reader.close();
			System.exit(0);
	}

	private void acceptConnection() throws IOException
	{	// User pressed 'Y' on this side to accept connection from another user
		// Send confirmation to buddy over TCP socket

		Socket connection = getConnection();
        if (connection != null) {
            DataOutputStream outToBuddy = new DataOutputStream(connection.getOutputStream());
            outToBuddy.writeBytes("ACCEPTED\n");
            System.out.println("Buddy accepted connection.");
				}
		// Enter messaging mode

		System.out.println("Enter your text to send to buddy. Enter q to quit.");

        String message;
				BufferedReader inFromBuddy = new BufferedReader(new InputStreamReader(connection.getInputStream()));
  			DataOutputStream outToBuddy = new DataOutputStream(connection.getOutputStream());

        while (true) {
					String reply = inFromBuddy.readLine();
					if (reply == null || reply.equalsIgnoreCase("q")) {
							System.out.println("Buddy has closed the connection.");
							break;
					}
					System.out.println("B: " + reply);

					System.out.print("> ");
					message = getLine();
					if (message.equalsIgnoreCase("q")) break;
					outToBuddy.writeBytes(message + "\n");

            
        }
		

	}

	private void rejectConnection() throws IOException
	{	// User pressed 'N' on this side to decline connection from another user
		// Send no message over TCP socket then close socket
		Socket connection = getConnection();
        if (connection != null) {
            DataOutputStream outToBuddy = new DataOutputStream(connection.getOutputStream());
            outToBuddy.writeBytes("");
            connection.close();
            System.out.println("Buddy connection rejected.");
				}
	}

	public void setConnection(Socket connection) throws IOException{
		this.connection = connection;
	}

	private Socket getConnection() {
		return connection;
}

	private String getLine()
	{	// Read a line from standard input
		String inputLine = null;
		  try{
			  inputLine = reader.readLine();
		  }catch(IOException e){
			 System.out.println(e);
		  }
	 	 return inputLine;
	}

	private void printMenu()
	{	System.out.println("\n\nSelect one of these options: ");
		System.out.println("  R - Register user id");
		System.out.println("  L - Login as user id");
		System.out.println("  A - Add buddy");
		System.out.println("  D - Delete buddy");
		System.out.println("  M - Message buddy");
		System.out.println("  S - Buddy status");
		System.out.println("  X - Exit application");
		System.out.print("Your choice: ");
	}

	public String getUserId() {
		// TODO Auto-generated method stub
		return userId;
	}

	public int getTCPMessagePort() {
		// TODO Auto-generated method stub
		return TCPMessagePort;
	}

	public String getStatus() {
		// TODO Auto-generated method stub
		return status;
	}

	public void updateBuddyStatus(String buddyId, String buddyStatus, String buddyIP, String buddyPort) {
    
    for (BuddyStatusRecord record : buddies) {
        if (record.buddyId.equals(buddyId)) {
            record.status = buddyStatus;
            record.IPaddress = buddyIP;
            record.buddyPort = buddyPort;
            return;
        }
    }
    
    BuddyStatusRecord newRecord = new BuddyStatusRecord(buddyIP, buddyStatus, buddyId, buddyPort);
    buddies.add(newRecord);
}

}

// A record structure to keep track of each individual buddy's status
class BuddyStatusRecord
{	public String IPaddress;
	public String status;
	public String buddyId;
	public String buddyPort;

	public BuddyStatusRecord(String IPaddress, String status, String buddyId, String buddyPort){
		this.IPaddress = IPaddress;
		this.status = status;
		this.buddyId = buddyId;
		this.buddyPort = buddyPort;
	}

	public String toString()
	{	return buddyId+"\t"+status+"\t"+IPaddress+"\t"+buddyPort; }

	public boolean isOnline()
	{	return status.indexOf("100") >= 0; }
}

// This class implements the TCP welcome socket for other buddies to connect to.
// I have left it here as an example to show where the prompt to ask for incoming connections could come from.

class TCPMessenger implements Runnable
{
	private IMClient client;
	private ServerSocket welcomeSocket;
	private static Socket connection;

	public TCPMessenger(IMClient c) throws IOException
	{	client = c;
		welcomeSocket = new ServerSocket(client.TCPMessagePort);
	}

    public void run()
	{
		// This thread starts an infinite loop looking for TCP requests.
		try
		{
			while (true)
			{
		    	// Listen for a TCP connection request.
		    	Socket connection = welcomeSocket.accept();
		    	System.out.print("\nDo you want to accept an incoming connection (y/n)? ");
		    	// Read actually occurs with menu readline
					client.setConnection(connection);
			}
	    }
		catch (Exception e)
		{	System.out.println(e); }
	}
}

class UDPSendThread implements Runnable {
	private IMClient client;
	private DatagramSocket socket;
	private InetAddress serverAddress;
	private int serverPort;

	public UDPSendThread(IMClient client, InetAddress serverAddress, int serverPort) throws SocketException {
			this.client = client;
			this.serverAddress = serverAddress;
			this.serverPort = serverPort;
			this.socket = new DatagramSocket();
	}

	public void run() {
			try {
					while (true) {
							String statusUpdate = "SET " + client.getUserId() + " " + client.getStatus() + " " + client.getTCPMessagePort();
							byte[] sendData = statusUpdate.getBytes();
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 1235);
							socket.send(sendPacket);

							String request = "GET " + client.getUserId();

							byte[] sendData2 = request.getBytes();
							DatagramPacket sendPacket2 = new DatagramPacket(sendData2, sendData2.length, serverAddress, 1235);
							socket.send(sendPacket2);

							Thread.sleep(10000);
					}
			} catch (Exception e) {
					System.out.println(e);
			}
	}
}

class UDPReceiveThread implements Runnable {
	private IMClient client;
	private DatagramSocket socket;
	private String response;

	public UDPReceiveThread(IMClient client) throws SocketException {
		this.client = client;
		this.socket = new DatagramSocket();
	}

	public void run() {
		try {
			while (true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);

				response = new String(receivePacket.getData());

				processResponse(response);
				
				Thread.sleep(10000);
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			socket.close();
		}
	}

	private void processResponse(String response) {
		String[] lines = response.split("\n");
		for (String line : lines) {
				String[] parts = line.trim().split(" ");
				if (parts.length == 5) {
						String buddyId = parts[0];
						String buddyStatus = parts[1] + " " +  parts[2];
						String buddyIP = parts[3];
						String buddyPort = parts[4];

						client.updateBuddyStatus(buddyId, buddyStatus, buddyIP, buddyPort);
				}
		}
}
}
