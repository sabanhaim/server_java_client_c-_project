package protocol.tbgp;

import java.security.InvalidParameterException;
import java.util.LinkedHashSet;
import java.util.Set;

import protocol.tbgp.games.TBGPGame;

public class TBGPRoom {
	private final String name;
	
	private Set<TBGPUser> users;
	private TBGPGame game;
	
	public TBGPRoom(String name) {
		this.name = name;
		this.users = new LinkedHashSet<>();
		this.game = null;
	}
	
	public TBGPGame getGame() {
		return game;
	}

	public String getName() {
		return name;
	}

	public void addUser(TBGPUser user) {
		users.add(user);
	}
	
	public void removeUser(TBGPUser user) {
		users.remove(user);
	}
	
	public Set<TBGPUser> getUsers() {
		return users;
	}
	
	public void startGame(TBGPGame game) {
		if (this.game != null) {
			throw new InvalidParameterException();
		}
		this.game = game;
	}
	
	public void stopGame() {
		if (this.game == null) {
			throw new InvalidParameterException();
		}
		this.game = null;
	}
}
