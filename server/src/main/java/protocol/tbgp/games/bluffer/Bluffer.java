package protocol.tbgp.games.bluffer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;

import protocol.tbgp.TBGPMessage;
import protocol.tbgp.TBGPRoom;
import protocol.tbgp.TBGPUser;
import protocol.tbgp.games.TBGPGame;

public class Bluffer extends TBGPGame {
	// The number of questions to be asked before finishing the game
	private static final int NUM_OF_QUESTIONS = 3;
	
	/** 
	 * Represents the type of answer we're currently expecting - a full answer or choices
	 */
	private enum ExpectedAnswerState {
		NONE,
		TEXT,
		CHOICES
	}
	
	/**
	 * Represents the format expected from the questions json.
	 */
	private class QuestionsInfo {
		OpenBlufferQuestion[] questions;
	}
	
	private String questionsDBPath;
	private LinkedList<OpenBlufferQuestion> questionsAsked;
	private ExpectedAnswerState currentState;
	private ChoicesBlufferQuestion choicesQuestion;
	private Map<String, Integer> userScores;
	
	public Bluffer(String questionsDBPath, TBGPRoom room) {
		super("Bluffer", room);
		this.questionsDBPath = questionsDBPath;
		this.questionsAsked = new LinkedList<>();
		this.currentState = ExpectedAnswerState.NONE;
		this.choicesQuestion = null;
		this.userScores = new HashMap<>();
		
		Set<TBGPUser> users = room.getUsers();
		for (TBGPUser user : users) {
			this.userScores.put(user.getNickname(), 0);
		}
		
		beginNextQuestion();
	}
	
	@Override
	public boolean handleTextResponse(TBGPUser user, String response) {
		if (currentState != ExpectedAnswerState.TEXT) {
			return false;
		}
		
		synchronized (questionsAsked) {
			OpenBlufferQuestion q = questionsAsked.getLast();
			q.addAnswer(user.getNickname(), response);
			
			user.sendMessage(new TBGPMessage("SYSMSG", "TXTRESP ACCEPTED"));
			
			if (q.getNumOfAnswers() >= room.getUsers().size()) {
				beginChoicesStage();
			}
		}
		
		return true;
	}

	@Override
	public boolean handleSelectResponse(TBGPUser user, int choice) {
		if (currentState != ExpectedAnswerState.CHOICES) {
			return false;
		}
		
		final String nickname = user.getNickname();
		
		synchronized (this.choicesQuestion) {
			List<String> answerers = this.choicesQuestion.getOriginalAnswerers(choice);
			if (answerers == null) {
				return false;
			}
			
			if (!this.choicesQuestion.addAnswerer(nickname)) {
				return false;
			}
			
			user.sendMessage(new TBGPMessage("SYSMSG", "SELECTRESP ACCEPTED"));
			
			user.sendMessage(new TBGPMessage("GAMEMSG", "The correct answer is: " + 
											 choicesQuestion.getCorrectAnswer()));
			
			if (choice == this.choicesQuestion.getCorrectChoice()) {
				user.sendMessage(new TBGPMessage("GAMEMSG", "correct! +10pts"));
				userScores.put(nickname, new Integer(userScores.get(nickname) + 10));
			} else {
				user.sendMessage(new TBGPMessage("GAMEMSG", "wrong! +0pts"));
			}
			
			for (String answerer : answerers) {
				if (answerer != nickname) {
					userScores.put(answerer, new Integer(userScores.get(answerer) + 5));
				}
			}
			
			if (this.choicesQuestion.getNumOfAnswerers() >= room.getUsers().size()) {
				beginNextQuestion();
			}
			
			return true;
		}
	}
	
	private void beginNextQuestion() {
		this.currentState = ExpectedAnswerState.NONE;
		
		if (questionsAsked.size() >= NUM_OF_QUESTIONS) {
			String scores = "Summary: ";
			Set<TBGPUser> users = room.getUsers();
			for (TBGPUser user : users) {
				scores += user.getNickname() + ": " + userScores.get(user.getNickname()) + " ";
			}
			
			sendMessageToRoom(new TBGPMessage("GAMEMSG", scores));
			this.room.stopGame();
		} else {
			OpenBlufferQuestion question = readQuestion(questionsAsked);
			questionsAsked.add(question);
			currentState = ExpectedAnswerState.TEXT;
			
			sendMessageToRoom(new TBGPMessage("ASKTXT", question.getQuestionText()));
		}
	}
	
	private void beginChoicesStage() {
		OpenBlufferQuestion q = questionsAsked.getLast();
		Map<String, List<String>> answers = q.getAnswersByUsers();
		String correctAnswer = q.getRealAnswer().toLowerCase();
		
		Set<String> possibleAnswers = new HashSet<>(answers.keySet());
		possibleAnswers.add(correctAnswer);
		
		List<String> shuffledAnswers = new ArrayList<String>(possibleAnswers);
		Collections.shuffle(shuffledAnswers);
		
		// Create a map between the choice and the users that gave that answer
		LinkedHashMap<Integer, List<String>> choices = new LinkedHashMap<>();
		String questionStr = "";
		for (int i = 0; i < shuffledAnswers.size(); i++) {
			final String answer = shuffledAnswers.get(i);
			final List<String> answerers = answers.get(answer);
			if (answerers == null) {
				choices.put(i, new LinkedList<String>());
			} else {
				choices.put(i, answerers);
			}
			questionStr += i + "." + answer + " ";
		}
		
		this.choicesQuestion = new ChoicesBlufferQuestion(choices, 
				shuffledAnswers.indexOf(correctAnswer), correctAnswer);
		this.currentState = ExpectedAnswerState.CHOICES;
		
		TBGPMessage msg = new TBGPMessage("ASKCHOICES", questionStr);
		sendMessageToRoom(msg);
	}
	
	private OpenBlufferQuestion readQuestion(List<OpenBlufferQuestion> questionsAsked) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(this.questionsDBPath));
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException();
		}
		
		Gson gson = new Gson();
		QuestionsInfo questionsInfo = gson.fromJson(br, QuestionsInfo.class);
		ArrayList<OpenBlufferQuestion> questions = new ArrayList<>(Arrays.asList(questionsInfo.questions));
		Collections.shuffle(questions);
		
		for (int i = 0; i < questions.size(); i++) {
			if (!questionsAsked.contains(questions.get(i))) {
				return questions.get(i);
			} 
		}
		throw new InvalidParameterException();
	}
	
	private void sendMessageToRoom(TBGPMessage msg) {
		Set<TBGPUser> users = room.getUsers();
		for (TBGPUser user : users) {
			user.sendMessage(msg);
		}
	}
}
