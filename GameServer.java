import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
	
	private static final int PORT = 9231;

	public static void main(String[] args) throws Exception {
			
		CardGame game;
		try {
			game = new CardGame("./src/start.txt");
		}
		catch(FileNotFoundException | InvalidGameParameterException e) {
			System.err.println(e.getMessage());
			return;
		}
		
		try (ServerSocket welcomeSocket = new ServerSocket(PORT);) {
			System.out.println("Server waiting for players to connect\n");
		    int currentPlayers = 0;
		    int totalPlayers = game.getNumberOfPlayers();
		    
		    while(currentPlayers < totalPlayers) {
		    	Socket newPlayerSocket = welcomeSocket.accept();  
		    	game.addPlayer(newPlayerSocket);
	            ++currentPlayers;
		    }
		}	
		game.run();
	}
}
