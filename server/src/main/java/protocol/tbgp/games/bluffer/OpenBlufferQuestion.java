package protocol.tbgp.games.bluffer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Saves data about a question in Bluffer, including all of its answers
 * All answers (correct and not) are saved in lower case to allow comparison
 */
public class OpenBlufferQuestion {
	private final String questionText;
	private final String realAnswer;
	
	// Maps between a user and his answer
	private Map<String, String> answerers;
	
	/** Empty constructor for Gson */
	public OpenBlufferQuestion() {
		this.answerers = new LinkedHashMap<>();
		this.questionText = null;
		this.realAnswer = null;
	}
	
	public OpenBlufferQuestion(String question, String correctAnswer) {
		this.questionText = question;
		this.realAnswer = correctAnswer.toLowerCase();
		this.answerers = new LinkedHashMap<>();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final OpenBlufferQuestion other = (OpenBlufferQuestion)obj;
		return questionText.equals(other.getQuestionText());
	}
	
	/**
	 * Saves the answer by the given user
	 * @param user The user that sent the answer 
	 * @param answer The answer
	 * @return True if successful, or false if the user has already answered.
	 */
	public boolean addAnswer(String user, String answer) {
		if (this.answerers.containsKey(user)) {
			return false;
		}
		
		this.answerers.put(user, answer.toLowerCase());
		return true;
	}
	
	public int getNumOfAnswers() {
		return this.answerers.size();
	}
	
	/**
	 * @return a map between an answer, and the list of users who answered it.
	 */
	public Map<String, List<String>> getAnswersByUsers() {
		// Get a Set of unique answers chosen by the users
		Set<String> answers = new HashSet<String>(this.answerers.values());
		
		// Convert it to a map between an answer and the list of users who gave it.
		Map<String, List<String>> answersByUsers = new HashMap<>();
		for (String answer : answers) {
			List<String> answerers = new LinkedList<>();
			for (Map.Entry<String, String> entry : this.answerers.entrySet()) {
				if (entry.getValue().equals(answer)) {
					answerers.add(entry.getKey());
				}
			}
			answersByUsers.put(answer,  answerers);
		}
		
		return answersByUsers;
	}
	
	public String getRealAnswer() {
		return this.realAnswer;
	}
	
	public String getQuestionText() {
		return this.questionText;
	}
	
}
