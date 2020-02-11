Color Game Simulation

You are to implement a simulation of card game called Colors. In any playing card game, the deck is a
collection of 52 playing cards each belonging to 4 suit (Hearts, Spades, Diamonds and Club). Each card
has a card name, suit and a value (the values for Ace is 14, King is 13, Queen is 12 and Jack is 11 and rest
number is the value of card. Card to has two types number card (2-10) and picture cards (J, Q , K , A).
Rules for card sorting in hand: Once card is distributed to any player, he/she usually sort card in their
hand. Usually all card are sorted by suit and then by type. Suit arrangement is Hearts, Spades, Diamonds
and Club whereas for types the pictures card are placed first.

The rules and game play is:
    • There are a minimal of two players and maximum of four players.
    • The game starts by first deciding number of players.
    • The game can be played with one to four decks of cards, once this is decided; the total decks
    become a pile.
    • Game starts by shuffling the pile and each player is given one card, and as soon as any player gets
    a Jack of any suit, distribution ends and the player with the jack is the one who is to pick any suit
    as color for the game.
    • Pile is reset and shuffled again and then its all cards are distributed equally to all players. Each
    player is given time to sort cards in their hand.
    • First turn is of the player that has chosen the color. He has to dump any five cards of his choice in
    an open pile. Others players have to dump five cards too.
    • After that each player can pick any two cards from this open pile and replace them with any cards
    of their choice.
    • Now players are asked to show their hand. The winner is the one with highest score in hand. The
    score of each player’s hand is calculated as sum of all cards (sum of values of cards of all 3 suits
    and sum of all cards of color suits * 2).
    
Implementation details:
    The game simulation has to be designed in such way one single class Game runs the simulation. The
    player’s choice should be come as random from the player. The input for the game, player names, player
    count, number of deck and any other input are to be taken from a start.txt file where they should be in a
    CVS format. (You can use java.util.Properties class too).
    The game should be implemented in multithreading. The players should be independent threads that
    should play their own game independently. Player must do its own work and game winning strategy, its
    sorting and then picking and dumping cards to win game. The main game thread should be coordinating
    thread, that should start and end the game.
    Beside that game tread should ensure that all card distribution, all decision are recorded into files, so the
    game play can be verified later. All game decisions and details of player’s hands should be stored in a
    score file, so that it can be checked later.
    The design and its implementation should be flexible enough to accommodate future changes and robust
    by ensuring that arguments are checked, exceptions are caught and thrown where required.
