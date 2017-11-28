package protocol.tbgp;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import protocol.ProtocolCallback;
import protocol.tbgp.games.TBGPGame;
import protocol.tbgp.games.TBGPGameFactory;
import tokenizer.StringMessage;

/**
 * Contains the needed information about the TBGP server - all of the connected users,
 * their rooms, every room's game and so on.
 */
public class TBGPServer {
	private Map<String, TBGPRoom> rooms;
	private Map<String, TBGPUser> users;
	
	private TBGPGameFactory gameFactory; 
	
	public TBGPServer(TBGPGameFactory gameFactory) {
		this.rooms = new HashMap<>();
		this.users = new HashMap<>();
		this.gameFactory = gameFactory;
	}
	
	public void removeUser(String nick) {
		synchronized(users) {
			TBGPUser user = this.users.get(nick);
			if (user != null) {
				TBGPRoom room = user.getRoom();
				if (room != null) {
					room.removeUser(user);
				}
				
				this.users.remove(nick);
			}
		}
	}
	
	public boolean addUser(String nick, ProtocolCallback<StringMessage> userCallback) {
		synchronized (users) {
			if (this.users.get(nick) != null) {
				return false;
			} else {
				this.users.put(nick, new TBGPUser(nick, userCallback));
				return true;
			}
		}
	}
	
	public boolean handleJoinRequest(String nick, String roomName) {
		TBGPUser user = getUserByNick(nick);
		if (user.getRoom() != null) {
			return false;
		}
		
		synchronized (rooms) {
			TBGPRoom room = rooms.get(roomName);
			if (room == null) {
				room = new TBGPRoom(roomName);
				rooms.put(roomName, room);
			}
			
			if (room.getGame() != null) {
				return false;
			}
			
			room.addUser(user);
			user.setRoom(room);
		}
		
		return true;
	}
	
	public boolean handleMsgRequest(String nick, String msg) {
		TBGPUser user = getUserByNick(nick);
		TBGPRoom room = user.getRoom();
		if (room == null) {
			return false;
		}
		
		synchronized (room) {
			TBGPMessage message = new TBGPMessage("USRMSG", user.getNickname() + ": " + msg); 
			Set<TBGPUser> users = room.getUsers();
			for (TBGPUser u : users) {
				if (u.getNickname() != nick) {
					u.sendMessage(message);
				}
			}
			return true;
		}
	}
	
	public boolean handleTextResponse(String nick, String response) {
		TBGPUser user = getUserByNick(nick);
		TBGPGame game = user.getRoom().getGame();
		if (game == null){
			return false;
		}
		synchronized (game) {
			return game.handleTextResponse(user, response);
		}
	}
	
	public boolean handleSelectResponse(String nick, int choice) {
		TBGPUser user = getUserByNick(nick);
		TBGPGame game = user.getRoom().getGame();
		if (game == null){
			return false;
		}
		synchronized (game) {
			return game.handleSelectResponse(user, choice);
		}
	}
	
	public String listGames() {
		String gameList = "[ ";
		Set<String> supportedGames = this.gameFactory.getSupportedGames();
		for (String name : supportedGames) {
			gameList += name + " ";
		}
		gameList += "]";
		return gameList;
	}
	
	public boolean startGame(String nick, String gameName) {
		if (this.gameFactory.getSupportedGames().contains(gameName) == false) {
			return false;
		}
		
		TBGPUser user = getUserByNick(nick);
		TBGPRoom room = user.getRoom();
		if (room == null || room.getGame() != null) {
			return false;
		}
		
		synchronized (room) {
			user.sendMessage(new TBGPMessage("SYSMSG", "STARTGAME ACCEPTED"));
			room.startGame(this.gameFactory.create(gameName, room));
			return true;
		}
	}
	
	private TBGPUser getUserByNick(String nick) {
		TBGPUser user = this.users.get(nick);
		if (user == null) {
			// This should never happen - the nick is not input given by the client
			throw new InvalidParameterException();
		}
		return user;
	}
}
