
public enum GameProtocol {
	
	SEND_CARDS_RANDOMLY_HAND,		// Randomly send specified number of Card's from the hand
	SEND_CARDS_STRATEGICALLY_HAND,	// Strategically send specified number of Card's from the hand
	SEND_CARDS_STRATEGICALLY_PILE,	// Strategically send specified number of Card's from the hand
	SEND_HAND,
	SEND_NAME,
	SEND_SUIT,
	SEND_HAND_SIZE,
	SORT_HAND,
	GAME_OVER,
	RECEIVE_CARD,
	RECEIVE_CARD_COLLECTION,
	RECEIVE_SUIT,
	YOU_WIN,
	YOU_LOSE,
	GAME_TIED
}
