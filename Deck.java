import java.util.*;

/*
 * Decks will either contain all 52 cards or none at all. They are not used for drawing
 * cards and are instead used for initializing and "containing" the 52 standard playing cards.
 * Decks are added to piles and cards are drawn from these piles, not the deck itself.
 */

public class Deck {
	
	private CardCollection cards = new CardCollection();
	public static final int DECK_SIZE = 52;
	
	public Deck() {
		initializeDeck();
	}
	
	/*
	 * Add 52 standard playing cards to the Deck.
	 */
	private void initializeDeck() {
		// Loop over the 4 card suits
		for (Suit suit : Suit.values()) {
			// Loop over 13 card types
			for (CardType value : CardType.values()) {
				cards.addCard(new Card(suit, value));
			}
		}
	}
	
	public void printDeck() {
		cards.printCollection();
	}

	public void shuffle() {
		cards.shuffle();
	}
	
	public Iterator<Card> iterator() {
		return cards.iterator();
	}

	public void clear() {
		cards.clear();
	}

	public Boolean isEmpty() {
		return cards.isEmpty();
	}	
}
