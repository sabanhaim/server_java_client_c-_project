package protocol.tbgp.games;

import protocol.tbgp.TBGPRoom;
import protocol.tbgp.TBGPUser;

public abstract class TBGPGame {
	private final String name;
	protected final TBGPRoom room;
	
	/**
	 * @param name A unique name that represents this game (should be determined by the inheriting class)
	 * @param room The room in which the game takes place
	 */
	public TBGPGame(String name, TBGPRoom room) {
		this.name = name;
		this.room = room;
	}
	
	public String getName() {
		return this.name;
	}
	
	/**
	 * Handles a response sent by the user (with TXTRESP)
	 * @return True if the response was expected, false if not
	 */
	public abstract boolean handleTextResponse(TBGPUser user, String response);
	
	/**
	 * Similar to handleTextResponse, but for SELECTRESP
	 */
	public abstract boolean handleSelectResponse(TBGPUser user, int choice);
}
