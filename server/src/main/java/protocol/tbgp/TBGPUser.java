package protocol.tbgp;

import java.io.IOException;
import java.security.InvalidParameterException;

import protocol.ProtocolCallback;
import tokenizer.StringMessage;

public class TBGPUser {
	private final String nickname;
	private final ProtocolCallback<StringMessage> callback;
	private TBGPRoom room;

	public TBGPUser(String nickname, ProtocolCallback<StringMessage> callback) {
		super();
		this.nickname = nickname;
		this.callback = callback;
		this.room = null;
	}
	
	public void setRoom(TBGPRoom room) {
		if (this.room != null) {
			throw new InvalidParameterException();
		}
		this.room = room;
	}

	public String getNickname() {
		return nickname;
	}
	
	public TBGPRoom getRoom() {
		return this.room;
	}
	
	public void sendMessage(TBGPMessage msg) {
		try {
			callback.sendMessage(new StringMessage(msg.toString()));
		} catch (IOException e) {
			System.out.print("IOException occurred when sending message to client " + this.nickname + ": ");
			e.printStackTrace();
		}
	}
}
