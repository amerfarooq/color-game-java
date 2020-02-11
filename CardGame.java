import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

public class CardGame {
	
	private int numOfDecks;
	private int numOfPlayers;
	private int numOfDeals;
	private CardCollection pile;
	private CyclicBarrier barrier;
	private GameLogger logger;
	private ArrayList<PlayerThread> players;
	private PlayerThread firstPlayer;
	private PlayerThread winner = null;
	private Suit selectedSuit;
	private Thread[] playerThreads; 
	private Boolean isTied;
	
	private final int DECK_LIMIT = 4;
	
	
	// ______________PUBLIC______________
	
	/*
	 * Constructor that initializes game state and sets the game parameters by reading them from
	 * the start file passed as the argument.
	 */
	public CardGame(String startFile) throws FileNotFoundException {
		setGameParameters(startFile);
	}
	
	/*
	 * Runs the whole game.
	 */
	public void run() throws InterruptedException {		
		logger.logHeading("GAME PARAMETERS");
		logger.addNewLine();
		logger.log("--Number of decks: " + numOfDecks);
		logger.log("--Number of players: " + numOfPlayers);
			
		// Request Player objects to obtain their player's name over their corresponding 
		// sockets and set it.
		try {
			setPlayerNames();
			logger.log("\n--PLAYER NAMES:");
			for(PlayerThread player : players) {
				System.out.println("Connected with client of " + player.getPlayerName());
				logger.log("----" + player.getPlayerName());
			}
		} 
		catch (ClassNotFoundException | IOException e) {
			System.err.println("Error setting player names!");
			e.printStackTrace();
		} 
		
		// Determine the first player. Also log the card distribution that leads to the winner.
		determineFirstPlayer();
		
		// Request the first player to select a suit.
		try {
			selectWinningSuit();
			logger.log("--SELECTED SUIT: " + selectedSuit + "\n");
		} 
		catch (ClassNotFoundException | IOException e) {
			System.err.println("Error during suit selection from first player!");
			e.printStackTrace();
		}
		
		setPlayerTurns();
		logger.logHeading("ORDER OF TURNS");
		logger.addNewLine();
		for(PlayerThread player : players) {
			logger.log("----" + player.getTurn() + " -> " + player.getPlayerName());
		}
		logger.addNewLine();
	
		setPlayerParameters();
		
		try {
			// Send selected suit to clients
			sendInfoToClients();
		}
		catch(IOException e) {
			System.err.println("Error sending information to clients!");
		}
		
		try {
			dealCards();
		} 
		catch (IOException e) {
			System.err.println("Error dealing cards!");
			e.printStackTrace();
		}
		
		logger.logHeading("INITIAL HANDS");
		logger.addNewLine();
		try {
			logPlayerHands();
		} 
		catch (ClassNotFoundException | IOException e) {
			System.err.println("Error logging initial player hands");
			e.printStackTrace();
		}
		
		// Player threads coordinate their turns between themselves. The game only spawns
		// the player threads, passes them some information and waits for them to complete.
		// This is a requirement of the assignment which states that player threads must work
		// independently.
		spawnPlayerThreads();
		
		// Wait for player threads to complete both their turns
		for (int i = 0; i < players.size(); ++i) {
			playerThreads[i].join();
		}
		
		logger.logHeading("FINAL HANDS");
		logger.addNewLine();
		try {
			logPlayerHands();
		} 
		catch (ClassNotFoundException | IOException e) {
			System.err.println("Error logging final player hands!");
			e.printStackTrace();
		}
		
		try {
			determineWinner();
		} 
		catch (ClassNotFoundException | IOException e) {
			System.err.println("Error determining winner of the game!");		
			e.printStackTrace();
		}
		closeClientConnections();
		logger.close();
	}
		
	/*
	 * Returns the name of the winner.
	 */
	public String getWinnerName() {
		if (winner == null) 
			return "Game has not finished yet!";
		else 
			return winner.getPlayerName();
	}
	
	/*
	 * Returns the number of decks being used for the game.
	 */
	public int getNumberOfDecks() {
		return numOfDecks;
	}
	
	/*
	 * Returns the number of players playing the game.
	 */
	public int getNumberOfPlayers() {
		return numOfPlayers;
	}

	/*
	 * Add a new player and its store its socket which is passed by the server.
	 * Create Object streams using the socket and pass them to the Player object
	 * so that player threads can use these streams to communication with their
	 * corresponding client. The player object is not sent the socket because
	 * it is not possible to open and close streams multiple times.
	 */
	public void addPlayer(Socket playerSocket) {
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		
    	try {
			out = new ObjectOutputStream(playerSocket.getOutputStream());
			in = new ObjectInputStream(playerSocket.getInputStream());
		}
    	catch (IOException e) {
			System.err.println("Failed to open object stream with client!");
			e.printStackTrace();
		}		
		players.add(new PlayerThread(out, in));
	}
	
	
	
	// ______________PRIVATE______________
	
	/*
	 * Informs player threads to terminate their connections with their clients.
	 */
	private void closeClientConnections() {
		for(PlayerThread player : players) {
			try {
				if (isTied && winner == null) {
					player.closeConnection(GameProtocol.GAME_TIED);
				}
				else if (player == winner) {
					player.closeConnection(GameProtocol.YOU_WIN);
				}
				else {
					player.closeConnection(GameProtocol.YOU_LOSE);
				}
			} 
			catch (IOException e) {
				System.err.println("Failed to close client connection of " + player.getPlayerName());
				e.printStackTrace();
			}
		}
	}
		
	/*
	 * Gets the player's name using the socket associated with that player.
	 */
	private void setPlayerNames() throws ClassNotFoundException, IOException {
		for(PlayerThread player : players) {
			player.setPlayerName();
		}
	}
	
	/*
	 * Parses and sets the game parameters from the String returned by getGameParameters.
	 */
	private void setGameParameters(String filename) throws FileNotFoundException {
		String gameInputs = getGameParameters(filename);
		
		// Tokenise the string containing game parameters.
		String[] tokens = gameInputs.split(",");
		
		// Trimming white space from all tokens
		Arrays.parallelSetAll(tokens, i -> tokens[i].trim());
		
		this.numOfDecks = Integer.parseInt(tokens[0]);
		this.numOfPlayers = Integer.parseInt(tokens[1]);
		this.numOfDeals = (numOfDecks * Deck.DECK_SIZE) / numOfPlayers; 
			
		if (tokens.length < 2) 
			throw new InvalidGameParameterException("Invalid number of game parameters in the start file!");
		
		if (numOfDecks < 1 || numOfDecks > 4) 
			throw new InvalidGameParameterException("Invalid number of decks in the start file!");
		
		if (numOfPlayers < 2 || numOfPlayers > 4) 
			throw new InvalidGameParameterException("Invalid number of players in the start file!");
		
		players = new ArrayList<>(numOfPlayers);
		
		// DECK_LIMIT indicates the maximum number of decks that can be in the pile
		pile = new CardCollection(numOfDecks, DECK_LIMIT);	
		pile.shuffle();
			
		barrier = new CyclicBarrier(numOfPlayers);
		logger = new GameLogger("logfile.txt");
	}
		
	/*
	 * 	Reads the game parameters from the given file and returns them as a String. These game
	 *  parameters include the number of players and the number of decks being used for the game.
	 */	
	private String getGameParameters(String filename) throws FileNotFoundException {
		File startFile = new File(filename);
		Scanner scanner = new Scanner(startFile);
	  		 
	    // Ignore first line of file which only contains headings
	    scanner.nextLine();
	    
	    String gameInputs = scanner.nextLine();
	    scanner.close();
	    return gameInputs;
	}
	
	/*
	 * Determines who the first player will be by randomly dealing cards to each player until a Jack is drawn. 
	 * The player that draws the Jack is set as the first player
	 */
	private void determineFirstPlayer() {
		logger.log("\n--DETERMINIG FIRST PLAYER:");

		for (int i = 0; i < numOfDeals; ++i) {
			for (int j = 0; j < numOfPlayers; ++j) {
				
				Card drawnCard = pile.drawRandomCard();
				logger.log("----" + drawnCard.getName() + " dealt to " + players.get(j).getPlayerName());
				
				if (drawnCard.getCardType() == CardType.JACK) { 
					firstPlayer = players.get(j);	
					logger.log("\n--FIRST PLAYER: " + firstPlayer.getPlayerName());
					return;
				}
			}	
		}
	}

	/*
	 * First player selects and sets the a winning suit
	 */
	private void selectWinningSuit() throws ClassNotFoundException, IOException {
		selectedSuit = firstPlayer.getSelectedSuitFromClient();
	}
	
	/*
	 * Places first player at the start of the players ArrayList. Also assigns each player a turn.
	 */
	private void setPlayerTurns() {
		// Setting firstPlayer at the start of the players ArrayList. The order of the
		// ArrayList defines the sequence of turns of each player in the game.
		int index = players.indexOf(firstPlayer);
		players.remove(index);
		players.add(0, firstPlayer);
		
		// Set the turns of each player
		for (int i = 0; i < numOfPlayers; ++i) {
			players.get(i).setTurn(i + 1);
		}
	}
	
	/*
	 * Pass Player class some important parameters so that the player threads can play the game independently.
	 */
	private void setPlayerParameters() {
		PlayerThread.setLogger(logger);
		PlayerThread.setPile(pile);
		PlayerThread.setBarrier(barrier);
	}
	
	/*
	 * Passes some necessary information to the clients such as the selected suit of this game.
	 */
	private void sendInfoToClients() throws IOException {
		if (selectedSuit == null)
			throw new IllegalStateException("Suit has not been selected for the game yet");
		
		for(PlayerThread player : players)
			player.sendSelectedSuit(selectedSuit);
	}
		
	private void dealCards() throws IOException {	
		// Reset pile before dealing
		pile.clear();
		pile.addDecks(numOfDecks);
		pile.shuffle();
		
		// Deal cards one by one to all players
		for (int i = 0; i < numOfDeals; ++i) {
			for (int j = 0; j < numOfPlayers; ++j) {
				
				Card drawnCard = pile.drawRandomCard();
				players.get(j).pickupCard(drawnCard);
			}	
		}	
	}
	
	/*
	 * Log all the cards in the current hand of all players.
	 */
	private void logPlayerHands() throws ClassNotFoundException, IOException {
		for (PlayerThread player : players) {
			logger.log("--" + player.getPlayerName() + "'s hand " + "(" + player.getHandSize() + ")"+ ":");
			logger.logCards(player.getHand());
			logger.addNewLine();
		}
	}

	/*
	 * Create player threads that will play the game independently.
	 */
	private void spawnPlayerThreads() {
		playerThreads = new Thread[numOfPlayers];	
		
		for (int i = 0; i < players.size(); ++i) {
			Thread playerThread = new Thread(players.get(i));
			playerThread.start();
			playerThreads[i] = playerThread;
		}
	}
	
	/*
	 * Determine the winner of the game by calculating the scores of each player. Also checks whether
	 * the game has been tied in case no winner is found.
	 */
	private void determineWinner() throws ClassNotFoundException, IOException {
		int maxScore = 0;
		isTied = false;
			
		logger.logHeading("PLAYER SCORES");
		logger.addNewLine();
		for(PlayerThread player : players) {
			int score = calculateHandScore(player.getHand());
			
			if (score > maxScore) {
				maxScore = score;
				winner = player;
			}
			else if (score == maxScore) {
				isTied = true;
			}
			logger.log("----" + player.getPlayerName() + "'s score is " + score);
		}
		if (!isTied) {
			logger.log("\n--WINNER: " + winner.getPlayerName());
			System.out.println("\nWinner of the game is " + winner.getPlayerName());
		}
		else {
			System.out.println("\nGame is tied");
			logger.log("\n--GAME IS TIED");
		}
	}
	
	/*
	 * Returns the score of a card.
	 */
	private int getCardScore(Card card) {
		if (card.getSuit() != selectedSuit) 
			return card.getCardType().getCardValue();	
		else 
			return (2 * card.getCardType().getCardValue());
	}

	/*
	 * Returns the total score of a players hand.
	 */
	private int calculateHandScore(CardCollection hand) {
		int totalScore = 0;
		Iterator<Card> iter = hand.iterator();
		
		while(iter.hasNext()) {
			totalScore +=  getCardScore(iter.next());
		}
		return totalScore;
	}
	
}
