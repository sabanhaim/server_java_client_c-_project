package protocol.tbgp.games.bluffer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChoicesBlufferQuestion {	
	/**
	 * Maps between a choice and the users that gave that answer. 
	 */
	private final Map<Integer, List<String>> choices;
	
	private final int correctChoice;
	
	private final String correctAnswer;
	
	private List<String> answerers; 
	
	public ChoicesBlufferQuestion(Map<Integer, List<String>> choices, 
			int correctChoice, String correctAnswer) {		
		this.choices = choices;
		this.correctChoice = correctChoice;		
		this.correctAnswer = correctAnswer;
		this.answerers = new LinkedList<>();
	}
	
	public String getCorrectAnswer() {
		return this.correctAnswer;
	}

	public int getCorrectChoice() {
		return this.correctChoice;
	}
	
	/**
	 * Adds the given choice to the given user.
	 * @return True if successful, or false if the user has already chosen something
	 */
	public boolean addAnswerer(String nick) {
		if (this.answerers.contains(nick)) {
			return false;
		} else {
			this.answerers.add(nick);
			return true;
		}
	}
	
	/** 
	 * Given a choice, this function returns a list of the nicknames of users
	 * that originally gave this answer.
	 */
	public List<String> getOriginalAnswerers(int choice) {
		return choices.get(choice);
	}
	
	public int getNumOfAnswerers() {
		return this.answerers.size();
	}
}
