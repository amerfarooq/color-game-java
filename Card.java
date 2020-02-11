import java.io.Serializable;

public class Card implements Serializable {
	
	private static final long serialVersionUID = 123L;
	private CardType value;
	private Suit suit;
	private String name;
	
	public Card(Suit suit, CardType value) {
		this.suit = suit;
		this.value = value;
		setName();
	}
	
	public CardType getCardType() {
		return this.value;
	}
	
	public Suit getSuit() {
		return this.suit;
	}
	
	public String getName() {
		return this.name;
	}
	
	private void setName() {
		String aSuit = this.suit.toString();
		String aValue = this.value.toString();

		this.name = aValue + " of " + aSuit;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		
		 // If the object is compared with itself then return true   
        if (obj == this) {
            return true; 
        }
  
        // Cast obj to Card so that we can compare data members  
        final Card toCompare = (Card) obj; 
          
        return this.name.equals(toCompare.getName());
	}
}
