import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class GameLogger {

	private PrintWriter writer;
		
	/*
	 * Creates a log file of the name passed as the argument and throws an Exception if the
	 * file cannot be found.
	 */
	public GameLogger(String fileName) throws FileNotFoundException {
		try {
			writer = new PrintWriter("./src/" + fileName);
		} 
		catch (FileNotFoundException e) {
			throw new FileNotFoundException("Log file could not be created!");
		}
	}
	
	/*
	 * Logs the passed string in the log file. 
	 */
	public void log(String log) {
		this.writer.println(log);
	}
	
	/*
	 * Closes the the log file.
	 */
	public void close() {
		this.writer.close();
	}
	
	/*
	 * Adds a new line in the log file.
	 */
	public void addNewLine() {
		this.writer.println();
	}
	
	/*
	 * Logs all the cards in the passed collection into the log file.
	 */
	public void logCards(CardCollection cards) {
		Iterator<Card> iter = cards.iterator();
		
		while(iter.hasNext()) {
			Card card = iter.next();
			logCard(card);
		}
		addNewLine();
	}

	/*
	 * Logs the name of the passed card into the log file.
	 */
	public void logCard(Card card) {
		log("----" + card.getName());
	}

	/*
	 * Logs the passed string in a box i.e
	 * +---+
	 * | s |
	 * +---+
	 * Modified solution from: https://stackoverflow.com/questions/27977973/how-to-use-print-format-to-make-box-around-text-java
	 */
	public void logHeading(String msg) {
	    int boxWidth = msg.length();
	    String line = "+" + ("-").repeat(boxWidth + 2) + "+";
	    log(line);
	    writer.printf("| %s |%n", padString(msg, boxWidth));
	    log(line);
	}
	
	
	// __Helper methods__
	
	private String padString(String str, int len) {
	    StringBuilder sb = new StringBuilder(str);
	    String toAppend = (" ").repeat(len - str.length());
	    return sb.append(toAppend).toString();
	}
	
}
