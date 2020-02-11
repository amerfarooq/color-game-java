import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class PlayerThread implements Runnable {
	
	private String name = null;
	private int turn = -1;
	
	private static CyclicBarrier barrier;
	private static Suit selectedSuit = null;
	private static CardCollection pile;
	private static GameLogger logger;
	private static int numOfPlayers = 0;
	private static int currentTurn = 1;
	private static Object roundOneLock = new Object();
	private static Object roundTwoLock = new Object();
	
	private static final int CARDS_IN_FIRST_TURN = 5;
	private static final int CARDS_IN_SECOND_TURN = 2;
	
	private ClientInterface clientInterface;
	
	private class ClientInterface {
		private ObjectOutputStream out;
		private ObjectInputStream in;
		
		public ClientInterface(ObjectOutputStream out, ObjectInputStream in) {
			this.out = out;
			this.in = in;
		}
		
		// __Methods for communicating with the client__
		
		/**
		 * Requests a resource from the client over the socket.
		 * @param resType -> Specifies the type of resource required as defined by GameProtocol.
		 * @return The requested resource is returned as an Object. Caller must apply appropriate cast to convert the object into the
		 * 		   required type.
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private Object retrieveClientResource(GameProtocol resType) throws IOException, ClassNotFoundException {
			out.writeObject(resType);
			out.reset();
			return in.readObject();
		}
		
		/**
		 * Requests some number of resources from the client over the socket.
		 * @param resType -> Specifies the type of resource required as defined by GameProtocol.
		 * @param count -> Specifies the how many resources are required.
		 * @return The requested resource is returned as an Object. Caller must apply appropriate cast to convert the object into the
		 * 		   required type.
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private Object retrieveClientResource(GameProtocol resType, int count) throws IOException, ClassNotFoundException {
			out.writeObject(resType);
			out.writeObject(count);
			out.reset();
			return in.readObject();
		}
		
		/**
		 * Sends some resource to the client.
		 * @param resType -> Specifies which resource is being sent to the client.
		 * @param res -> Represents the actual resource.
		 * @throws IOException
		 */
		private void sendClientResource(GameProtocol resType, Object res) throws IOException {
			out.writeObject(resType);
			out.writeObject(res);
			out.reset();
		}
		
		/**
		 * Send some number of resources to the client.
		 * @param resType -> Specifies which resource is being requested from the client.
		 * @param res -> Represents the actual resource.
		 * @param count -> Specifies how many resource of resType are needed.
		 * @throws IOException
		 */
		private void sendClientResource(GameProtocol resType, Object res, int count) throws IOException {
			out.writeObject(resType);
			out.writeObject(count);
			out.writeObject(res);
			out.reset();
		}

		/**
		 * Tells client to perform some operation on its end.
		 * @param cmd -> Specifies the operation which the client should perform as specified by GameProtocol.
		 * @throws IOException
		 */
		private void issueClientCommand(GameProtocol cmd) throws IOException {
			out.writeObject(cmd);
			out.reset();
		}
		
	}
	
	
	// ______________PUBLIC______________
		
	public PlayerThread(ObjectOutputStream out, ObjectInputStream in) {
		clientInterface = new ClientInterface(out, in);
		numOfPlayers++;
	}
	
	
	// __Setters__
	
	public static void setBarrier(CyclicBarrier barrier) {
		PlayerThread.barrier = barrier;
	} 
	
	public static void setLogger(GameLogger logger) {
		PlayerThread.logger = logger;
	}
	
	public static void setPile(CardCollection pile) {
		PlayerThread.pile = pile;
	}

	public void setTurn(int turn) {
		if (this.turn == -1) {
			this.turn = turn;
		}
		else {
			System.err.println("Player turn has already been set and cannot be changed");
		}
	}
	
	
	// __Getters__
	
	public static Suit getSelectedSuit() {
		return selectedSuit;
	}
	
	/*
	 * Returns the CardCollection representing the players current hand.
	 */
	public CardCollection getHand() throws ClassNotFoundException, IOException {
		return (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_HAND);
	}
	
	public int getHandSize() throws ClassNotFoundException, IOException {
		return (Integer) clientInterface.retrieveClientResource(GameProtocol.SEND_HAND_SIZE);
	}
			
	public int getTurn() {
		return turn;
	}

	public String getPlayerName() {
		return name;
	}
	
	
	// __Methods__
	
	/*
	 * Runs the player thread which completes it first turn, waits for all other players to complete their
	 * first turns and then proceeds to complete its second turn.
	 */
	@Override
	public void run() {	
				
		try {
			firstTurn();
		} 
		catch (ClassNotFoundException | IOException e1) {
			System.err.println("Error exchanging cards b/w client and player during first turn of " + name);
			e1.printStackTrace();
		}
		
		waitForFirstRoundToComplete();
		
		try {
			secondTurn();
		} 
		catch (IOException | ClassNotFoundException e) {
			System.err.println("Error exchanging cards b/w client and player during second turn of " + name);
			e.printStackTrace();
		}
	}
	
	
	// __Client interaction methods__
	
	/*
	 * Send the specified card to the player client so that he/she can 
	 * add it to their hand.
	 */
	public void pickupCard(Card card) throws IOException {
		clientInterface.sendClientResource(GameProtocol.RECEIVE_CARD, card);
	}
	
	/*
	 * Removes the specified number of cards from the players hand. The removed cards
	 * are selected randomly.
	 */
	public CardCollection dumpRandomCards(int numOfCards) throws IOException, ClassNotFoundException {
		// Request PlayerClient to return the specified number of Card's in a random fashion (i.e. Card's are in a random order)
		return (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_CARDS_RANDOMLY_HAND, numOfCards);
	}
	
	/*
	 * Removes the specified number of cards from the players hand. The removed cards
	 * are lowest score card that the player currently has.
	 */
	public CardCollection dumpCardsStrategically(int numOfCards) throws ClassNotFoundException, IOException {
		return (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_CARDS_STRATEGICALLY_HAND, numOfCards);		
	}
	
	/*
	 * Prints the names of all the cards currently in the players hand.
	 */
	public void printHand() throws ClassNotFoundException, IOException {
		CardCollection collec = (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_HAND);
		collec.printCollection();
	}
		
	/*
	 * Sorts the hand according to suit and card value. Sorting criteria specified in 
	 * the assignment file.
	 */
	public void sortHand() throws IOException {
		clientInterface.issueClientCommand(GameProtocol.SORT_HAND);
	}
	
	public CardCollection retrievePlayerHand() throws ClassNotFoundException, IOException {
		return (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_HAND);
	}
	
	public int retrievePlayerHandSize() throws ClassNotFoundException, IOException {
		return (Integer) clientInterface.retrieveClientResource(GameProtocol.SEND_HAND_SIZE);
	}
	
	/*
	 * Asks client to send player name. If name has already been sent previously, 
	 * returns that instead.
	 */
	public void setPlayerName() throws IOException, ClassNotFoundException {
		name = (String) clientInterface.retrieveClientResource(GameProtocol.SEND_NAME);
	}
	
	public void closeConnection(GameProtocol status) throws IOException {
		clientInterface.issueClientCommand(GameProtocol.GAME_OVER);
		clientInterface.issueClientCommand(status);
	}
	
	/*
	 * Ask client to select a suit and send it back to the Player thread over the socket.
	 */
	public Suit getSelectedSuitFromClient() throws IOException, ClassNotFoundException {
		if (this.selectedSuit == null) {
			return (Suit) clientInterface.retrieveClientResource(GameProtocol.SEND_SUIT);
		}
		else {
			return this.selectedSuit;
		}
	}

	/*
	 * Sends the specified suit to the client.
	 */
	public void sendSelectedSuit(Suit suit) throws IOException  {
		clientInterface.sendClientResource(GameProtocol.RECEIVE_SUIT, suit);
	}
	
	public CardCollection drawCardsFromPile(int numOfCards) throws IOException, ClassNotFoundException {
		return (CardCollection) clientInterface.retrieveClientResource(GameProtocol.SEND_CARDS_STRATEGICALLY_PILE, numOfCards);
	}
	
	
	
	// ______________PRIVATE______________
	
	private void firstTurn() throws ClassNotFoundException, IOException {
		// Player thread checks if it is its turn. If it's not, it goes into a waiting state
		// using roundOneLock. If it is its turn, it finishes its first turn and then uses
		// notifyAll to wake up all the other waiting Player threads. This loop runs again
		// in all the newly awoken threads and the Player thread with the next turn gets to
		// complete its first turn whilst the rest of threads go into a waiting state again.
		// Once the running thread completes its turn, it wakes up all the waiting threads 
		// again and this loop repeats itself. This ensures that the Player threads complete 
		// their turn in the assigned order.
		
		synchronized (roundOneLock) {
			while (turn != currentTurn) {
				try {
					roundOneLock.wait();
				} 
				catch (InterruptedException e) {
					System.out.println(this.name + "'s thread got interrupted!");
				}
			}
		
			sortHand();
			logger.logHeading(name + "'s first turn");
			logger.log("\n--" + name + "'s hand before first turn (" + retrievePlayerHandSize() + ")");
			logger.logCards(retrievePlayerHand());
			
			CardCollection dumpedCards = dumpCardsStrategically(CARDS_IN_FIRST_TURN);		
			logger.log("--Following five cards dumped into pile:");
			logger.logCards(dumpedCards);
			
			logger.log("--Pile before dumping:");
			logger.logCards(pile);
	
			pile.addCardCollection(dumpedCards);
			
			logger.log("--Pile after dumping:");
			logger.logCards(pile);
			
			// If last turn of first round
			if (turn == numOfPlayers) {
				currentTurn = 1;
			}
			else {
				currentTurn++;
			}	
			roundOneLock.notifyAll();
			
			sortHand();
			logger.log("--" + name + "'s hand after first turn (" + retrievePlayerHandSize() + ")");
			logger.logCards(retrievePlayerHand());
			logger.addNewLine();
		}	
	}
	
	private void secondTurn() throws IOException, ClassNotFoundException {
		// The same code is used as that in firstTurn(). Only the lock variable is changed.
		synchronized (roundTwoLock) {
			while (turn != currentTurn) {
				try {
					roundTwoLock.wait();
				} 
				catch (InterruptedException e) {
					System.out.println(this.name + "'s thread got interrupted!");
				}
			}
			logger.logHeading(name + "'s second turn");
			logger.log("\n--" + name + "'s hand before second turn (" + retrievePlayerHandSize() + ")");
			logger.logCards(retrievePlayerHand());
			
			CardCollection dumpedCards = dumpCardsStrategically(CARDS_IN_SECOND_TURN);
			logger.log("--Following two cards dumped into pile:");
			logger.logCards(dumpedCards);
			
			logger.log("--Pile before dumping:");
			logger.logCards(pile);
			
			pile.addCardCollection(dumpedCards);
			
			logger.log("--Pile after dumping:");
			logger.logCards(pile);
					
			// Send the client the current pile
			clientInterface.sendClientResource(GameProtocol.RECEIVE_CARD_COLLECTION, pile);
			
			// Tell client to draw 2 cards from the sent pile and add them to their hand and then send those
			// added cards back so the pile can be updated.
			CardCollection drawnCards = drawCardsFromPile(CARDS_IN_SECOND_TURN);;
			pile.removeCards(drawnCards);
				
			logger.log("--Cards drawn from the pile:");
			logger.logCards(drawnCards);
			
			logger.log("--Pile after drawing cards:");
			logger.logCards(pile);
			
			roundTwoLock.notifyAll();
			currentTurn++;		
			
			sortHand();
			logger.log("--" + name + "'s hand after second turn (" + retrievePlayerHandSize() + ")");
			logger.logCards(retrievePlayerHand());
		}	
	}
	
	private void waitForFirstRoundToComplete() {
		// As each player completes its first turn, it begins to wait on the barrier.
		// The barrier will only open when all the player threads call barrier.await().
		// The number of threads needed to trip the barrier is specified when the 
		// barrier is created. In our case, it is equal to the number of players, meaning
		// that only when all the players have completed their first turn will they
		// proceed towards their second turn. 
		try {
			barrier.await();
		} 
		catch (InterruptedException e) {
			System.out.println(this.name + "'s thread got interrupted!");
			e.printStackTrace();
		} 
		catch (BrokenBarrierException e) {
			System.out.println(this.name + " still waiting for completion of first round!");
			e.printStackTrace();
		}
	}

}
