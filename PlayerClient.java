import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

public class PlayerClient {

	private static final int PORT = 9231;
	private String name = null;
	private CardCollection hand = new CardCollection();
	private CardCollection pile = null;
	private Suit selectedSuit = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Socket socket;
	
	
	//  ______________PUBLIC______________
	
	public PlayerClient(String name) {
		this.name = name;
	}
	
	/*
	 * Creates a socket and a couple of Object streams to communicate with the server 
	 */
	public void openConnection() throws IOException {
		socket = new Socket("localhost", PORT);
		out = new ObjectOutputStream(socket.getOutputStream());
    	in = new ObjectInputStream(socket.getInputStream());   
	}
	
	/*
	 * Listens to the server's command and responds to them appropriately.
	 */
	public void listen() throws IOException, ClassNotFoundException {
		// The client never initiates communication with the server. It only
		// responds to the server's requests. Hence why such a loop is possible.
		
		GameProtocol serverMsg;
		while ((serverMsg = (GameProtocol) in.readObject()) != GameProtocol.GAME_OVER) {
				respond((serverMsg));
		}
		receiveGameResult();
	}
	
	
	
	// ______________PRIVATE______________
	
	/*
	 * Responds to the specified server command. 
	 * Returns some information to server for commands prefixed with "R" (R-> resource)
	 * Performs some action for commands prefixed with "C"  (C -> command)
	 */
	private void respond(GameProtocol cmd) throws IOException, ClassNotFoundException {
		
		switch (cmd) {
			case SEND_NAME:
				cmdSendName();
				break;
			case RECEIVE_CARD:
				cmdReceiveCard();
				break;
			case RECEIVE_CARD_COLLECTION:
				cmdReceiveCardCollection();
				break;
			case RECEIVE_SUIT:
				cmdReceiveSuit();
				break;
			case SEND_CARDS_RANDOMLY_HAND:
				cmdSendRandomlyFromHand();
				break;
			case SEND_CARDS_STRATEGICALLY_HAND:
				cmdSendStrategicallyFromHand();
				break;
			case SEND_CARDS_STRATEGICALLY_PILE:
				cmdSendStrategicallyFromPile();
				break;		
			case SEND_HAND:
				cmdSendHand();
				break;
			case SEND_HAND_SIZE:
				cmdSendHandSize();
				break;
			case SEND_SUIT:
				cmdSendSuit();
				break;
			case SORT_HAND:
				cmdSortHand();
				break;
			case YOU_WIN:
				System.err.println("Game is not over yet!");
				break;
			case YOU_LOSE:
				System.err.println("Game is not over yet!");
				break;
			default:
				System.err.println("Invalid protocol command!");
		}
	}
	
	/*
	 * Selects a random card suit.
	 */
	private Suit selectSuit() {
		return Suit.values()[new Random().nextInt(Suit.values().length)];
	}
	
	/*
	 * Retrieve whether client won or lost the game from the server.
	 */
	private void receiveGameResult() throws ClassNotFoundException, IOException {
		System.out.println("\n" + ((GameProtocol) in.readObject()).toString());
	}

	
	// __Response methods for server commands__
	
	private void cmdSendName() throws IOException {
		out.writeObject(name);
		out.reset();
	}
	
	private void cmdSendSuit() throws IOException {
		Suit randomSuit = selectSuit();
		out.writeObject(randomSuit);
		out.reset();
	}

	private void cmdReceiveSuit() throws ClassNotFoundException, IOException {
		selectedSuit = (Suit) in.readObject();
		
		if (selectedSuit == null)
			throw new IllegalStateException("Selected suit sent to " + name + " is null");
	}
	
	private void cmdReceiveCard() throws ClassNotFoundException, IOException {
		hand.addCard((Card) in.readObject());
	}
	
	private void cmdReceiveCardCollection() throws ClassNotFoundException, IOException {
		CardCollection pileFromServer = (CardCollection) in.readObject();
		
		if (pileFromServer == null) 
			throw new SocketException("Collection received by " + name + "'s client is null");
		else if (pileFromServer.isEmpty())
			System.err.println("Collection received by " + name +"s client is empty");
		
		this.pile = new CardCollection(pileFromServer);		
	}
	
	private void cmdSendRandomlyFromHand() throws IOException, ClassNotFoundException {
		if (hand.isEmpty()) 
			throw new IllegalStateException(name + "'s client has not received its hand yet!");
			
		int numOfCards = (Integer) in.readObject();
		CardCollection toSend = new CardCollection();
		
		for(int i = 0; i < numOfCards; ++i) {			
			toSend.addCard(hand.drawRandomCard());
		}
		out.writeObject(toSend);
		out.reset();
	}
	
	/*
	 * Draw the maximum scoring cards from the pile and send them to the server.
	 */
	private void cmdSendStrategicallyFromPile() throws IOException, ClassNotFoundException {
		if (pile == null)
			throw new IllegalStateException(name + "'s client has not received the game pile yet!");
		if (selectedSuit == null)
			throw new IllegalStateException("Suit has not been set in " + name + "'s client!");
		
		int numOfCards = (Integer) in.readObject();
		CardCollection toSend = new CardCollection();
			
		for(int i = 0; i < numOfCards; ++i) {	
			toSend.addCard(pile.drawMaxScoreCard(selectedSuit));
		}
		hand.addCardCollection(toSend);   // Add drawn cards from the pile to the hand
		out.writeObject(toSend);
		out.reset();
	}
	
	private void cmdSendStrategicallyFromHand() throws IOException, ClassNotFoundException {
		if (hand.isEmpty()) 
			throw new IllegalStateException(name + "'s client has not received its hand yet!");
		if (selectedSuit == null)
			throw new IllegalStateException("Suit has not been set in " + name + "'s client!");
		
		int numOfCards = (Integer) in.readObject();
		CardCollection toSend = new CardCollection();
			
		for(int i = 0; i < numOfCards; ++i) {	
			toSend.addCard(hand.drawMinScoreCard(selectedSuit));
		}
		out.writeObject(toSend);
		out.reset();
	}
	
	private void cmdSendHand() throws IOException {
		out.writeObject(hand);
		out.reset();
	}
	
	private void cmdSendHandSize() throws IOException {
		out.writeObject(hand.size());
		out.reset();
	}
	
	private void cmdSortHand() {
		hand.sort();
	}
	
	
	// ______________MAIN______________
	
	public static void main(String[] args) throws Exception {
	
		System.out.print("Enter player name: ");
		Scanner cin = new Scanner(System.in);
		String playerName = cin.nextLine();
		
		PlayerClient player = new PlayerClient(playerName);
		player.openConnection();
		player.listen();

		cin.close();
	}

}
