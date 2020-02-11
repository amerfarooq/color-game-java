import java.util.Comparator;

public class CardComparator implements Comparator<Card>{
	
	@Override
	public int compare(Card card1, Card card2) {
		
		int compareSuit = card1.getSuit().compareTo(card2.getSuit());
		
		// If cards of the same suit
		if (compareSuit == 0) {
			// Compare card values
			return Integer.compare(card2.getCardType().getCardValue(), 
								   card1.getCardType().getCardValue());
		}
		else  {
			return compareSuit;
		}
	}
}
