import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class CardCollection implements Serializable {

	private static final long serialVersionUID = 19645L;
	
	/*
	 * CopyOnWriteArrayList is used for thread safety purposes as a normal ArrayList
	 * will throw ConcurrentModificationException if multiple threads try to modify it.
	 */
	private CopyOnWriteArrayList<Card> cards = new CopyOnWriteArrayList<>();
	
	private int deckLimit;
	private Boolean isLimited = false;		// Indicates whether a limit on the number of cards in the collection has been set.
	
	// ______________PUBLIC______________
	
	
	// __Constructors__
	
	public CardCollection() {}
	
	public CardCollection(int numOfDecks) {
		addDecks(numOfDecks);
	}
	
	public CardCollection(int numOfDecks, int deckLimit) {
		this.deckLimit = deckLimit;
		isLimited = true;
		enforceTotalCardLimit(numOfDecks * Deck.DECK_SIZE);
		addDecks(numOfDecks);
	}
	
	public CardCollection(CardCollection aCollec) {
		addCardCollection(aCollec);
	}
	
	public CardCollection(CardCollection aCollec, int deckLimit) {
		this.deckLimit = deckLimit;
		isLimited = true;
		enforceTotalCardLimit(aCollec.size());
		addCardCollection(aCollec);
	}
	

	// __Methods__
	
	/*
	 * Add a single card to the collection.
	 */
	public void addCard(Card card) {
		if (isLimited) {
			enforceTotalCardLimit(1);
		}
		cards.add(card);
	}
	
	/*
	 * Add all the cards in the passed collection into this collection.
	 */
	public void addCardCollection(CardCollection collection) {
		if (isLimited) {
			enforceTotalCardLimit(collection.size());
		}
		cards.addAll(collection.cards);
	}
	
	/*	
	 * Add the given number of 52 card standard decks to the collection.
	 */
	public void addDecks(int numOfDecks) {	
		if (isLimited) {
			enforceTotalCardLimit(numOfDecks * Deck.DECK_SIZE);
		}
		for(int i = 0; i < numOfDecks; ++i)
			addDeck();
	}
	
	/*
	 * Shuffle the card collection.
	 */
	public void shuffle() {
		Collections.shuffle(cards);
	}
	
	/*
	 * Print the names of all the cards in the collection on the console.
	 */
	public void printCollection() {
		for (Card card : cards) {
			System.out.println(card.getName());
		}
	}
	
	/*
	 * Removes a random card from the collection and returns it.
	 */
	public Card drawRandomCard() {
		int randomIndex = new Random().nextInt(this.cards.size());
		return cards.remove(randomIndex);
	}
	
	/*
	 * Removes the highest score card from the collection and returns it.
	 */
	public Card drawMaxScoreCard(Suit selectedSuit) {
		Card maxCard = null;
		int maxValue = 0;
		
		for(Card card : cards) {
			int cardValue = card.getCardType().getCardValue();
			
			// If the card's suit is the selected suit, then the 
			// card's score doubles.
			if (card.getSuit() == selectedSuit)
				cardValue *= 2;
			
			if (cardValue > maxValue) {
				maxCard = card;
				maxValue = cardValue;
			}
		}
		cards.remove(maxCard);
		return maxCard;
	}
	
	/*
	 * Removes the lowest score card from the collection and returns it.
	 */
	public Card drawMinScoreCard(Suit selectedSuit) {
		Card minCard = null;
		int minValue = 30;
		
		for(Card card : cards) {
			int cardValue = card.getCardType().getCardValue();
			
			if (card.getSuit() == selectedSuit)
				cardValue *= 2;
			
			if (cardValue < minValue) {
				minCard = card;
				minValue = cardValue;
			}
		}
		cards.remove(minCard);
		return minCard;
	}
	
	/*
	 * Removes the passed card from the collection.
	 */
	public Boolean removeCard(Card card) {
		return cards.remove(card);
	}
	
	public Boolean removeCards(CardCollection aCollec) {
		if (hasCards(aCollec)) {
			Iterator<Card> iter = aCollec.iterator();
			
			while(iter.hasNext()) 
				removeCard(iter.next());
			
			return true;
		}
		else {
			return false;
		}
	}
	
	/*
	 * Returns an iterator for the ArrayList of Cards in the collection.
	 */
	public Iterator<Card> iterator() {
		return cards.iterator();
	}
	
	public Boolean hasCard(Card aCard) {
		return cards.contains(aCard);
	}
	
	public Boolean hasCards(CardCollection aCollec) {	
		Iterator<Card> iter = aCollec.iterator();
		
		while(iter.hasNext()) {
			if (!hasCard(iter.next())) return false;
		}
		return true;
	}

	/*
	 * Sorts the hand according to suit and card value. Sorting criteria specified in 
	 * the assignment file.
	 */
	public void sort() {
		Collections.sort(cards, new CardComparator());
	}
	
	public void clear() {
		cards.clear();
	}
	
	public int size() {
		return cards.size();
	}
	
	public Boolean isEmpty() {
		return cards.isEmpty();
	}
	
	
	
	// ______________PRIVATE______________
	
	/*
	 * Add a standard 52 card deck to the card collection.
	 */
	private void addDeck() {
		Deck defaultDeck = new Deck();
		Iterator<Card> iter = defaultDeck.iterator();
		
		while(iter.hasNext()) {
			cards.add(iter.next());
		}
	}

	/*
	 * Throws an exception if adding the passed number of cards exceeds the total card limit
	 * of the CardCollection.
	 */
	private void enforceTotalCardLimit(int numOfCards) {
		if (!isLimited)
			throw new IllegalStateException("CardCollection is not limited!");
	
		if (cards.size() + numOfCards > (deckLimit * Deck.DECK_SIZE)) {
			throw new IllegalStateException("Number of cards being added to the pile exceeds"
					+ " the DECK_LIMIT which has been set to " + deckLimit);
		}
	}
}
