package protocol.tbgp;

import java.io.IOException;

import protocol.AsyncServerProtocol;
import protocol.ProtocolCallback;
import tokenizer.StringMessage;

/**
 * a simple implementation of the server protocol interface
 */
public class TBGPProtocol implements AsyncServerProtocol<StringMessage> {
	
	private enum CmdResult {
		ACCEPTED,
		REJECTED,
		UNIDENTIFIED,
		DONT_ANSWER
	}

	private boolean _shouldClose = false;
	private boolean _connectionTerminated = false;
	
	private boolean _isConnected = false;
	private String _connectedUserNick = null;
	private TBGPServer _tbgpServer;
	
	public TBGPProtocol(TBGPServer server) {
		super();
		this._tbgpServer = server;
	}

	/**
	 * processes a message<BR>
	 * this simple interface prints the message to the screen, then composes a simple
	 * reply and sends it back to the client
	 *
	 * @param msg the message to process
	 * @return the reply that should be sent to the client, or null if no reply needed
	 */
	@Override
	public void processMessage(StringMessage msg, ProtocolCallback<StringMessage> callback) {        
		if (this._connectionTerminated) {
			return;
		}

		CmdResult res;
		StringBuilder optionalInfo = new StringBuilder();
		TBGPMessage cmd = new TBGPMessage(msg.toString());
		if (this.isEnd(new StringMessage(cmd.getCommand()))) {
			this._shouldClose = true;
			connectionTerminated();
			res = CmdResult.ACCEPTED;
		} else {
			res = handleCmd(cmd, callback, optionalInfo);
		}
		
		if (res != CmdResult.DONT_ANSWER) {
			TBGPMessage message = new TBGPMessage("SYSMSG", cmd.getCommand() + " " + res.name() + 
					" " + optionalInfo.toString());
			this.sendMessage(callback, message);
		}
	}

	/**
	 * determine whether the given message is the termination message
	 *
	 * @param msg the message to examine
	 * @return false - this simple protocol doesn't allow termination...
	 */
	@Override
	public boolean isEnd(StringMessage msg) {
		return msg.toString().toUpperCase().equals("QUIT");
	}

	/**
	 * Is the protocol in a closing state?.
	 * When a protocol is in a closing state, it's handler should write out all pending data, 
	 * and close the connection.
	 * @return true if the protocol is in closing state.
	 */
	@Override
	public boolean shouldClose() {
		return this._shouldClose;
	}

	/**
	 * Indicate to the protocol that the client disconnected.
	 */
	@Override
	public void connectionTerminated() {
		if (this._connectedUserNick != null) {
			this._tbgpServer.removeUser(this._connectedUserNick);
		}
		this._connectionTerminated = true;
	}
	
	private CmdResult handleCmd(TBGPMessage cmd, ProtocolCallback<StringMessage> callback, StringBuilder optionalInfo) {		
		if (_isConnected) {
			boolean succeeded;
			switch (cmd.getCommand()) {
			case "JOIN":
				succeeded = _tbgpServer.handleJoinRequest(this._connectedUserNick, cmd.getParam());
				break;
			case "MSG":
				succeeded = _tbgpServer.handleMsgRequest(this._connectedUserNick, cmd.getParam());
				break;
			case "LISTGAMES":
				String gameList = _tbgpServer.listGames();
				optionalInfo.append(gameList);
				succeeded = true;
				break;
			case "STARTGAME":
				succeeded = _tbgpServer.startGame(this._connectedUserNick, cmd.getParam());
				if (succeeded) {
					return CmdResult.DONT_ANSWER;
				}
				break;
			case "TXTRESP":
				succeeded = _tbgpServer.handleTextResponse(this._connectedUserNick, cmd.getParam());
				if (succeeded) {
					return CmdResult.DONT_ANSWER;
				}
				break;
			case "SELECTRESP":
				try {
					succeeded = _tbgpServer.handleSelectResponse(this._connectedUserNick, Integer.decode(cmd.getParam()));
					if (succeeded) {
						return CmdResult.DONT_ANSWER;
					}
				} catch (NumberFormatException ex) {
					succeeded = false;
				}
				break;
			case "NICK":
				// The client cannot set his nick more than once. 
				succeeded = false;
				break;
			default:
				return CmdResult.UNIDENTIFIED;
			}
			
			if (succeeded) {
				return CmdResult.ACCEPTED;
			} else {
				return CmdResult.REJECTED;
			}
		} else {
			if (cmd.getCommand().equals("NICK")) {
				if (_tbgpServer.addUser(cmd.getParam(), callback)) {
					this._isConnected = true;
					this._connectedUserNick = cmd.getParam(); 
					return CmdResult.ACCEPTED;
				} else {
					return CmdResult.REJECTED;
				}
			} else {
				return CmdResult.UNIDENTIFIED;
			}
		}
	}
	
	private void sendMessage(ProtocolCallback<StringMessage> callback, TBGPMessage msg) {
		try {
			callback.sendMessage(new StringMessage(msg.toString()));
		} catch (IOException e) {
			this._shouldClose = true;
			System.out.print("IOException occurred when sending message to client:");
			e.printStackTrace();
		}
	}
}
