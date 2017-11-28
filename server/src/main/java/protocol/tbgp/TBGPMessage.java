package protocol.tbgp;

/**
 * Represents a command in TBGP
 */
public class TBGPMessage {
	private final String command;
	private final String param;
	
	/**
	 * Constructs a TBGPCommand based on the given message string
	 */
	public TBGPMessage(String msg) {
		msg = msg.replaceAll("(\\r|\\n)", "");
		int spacePos = msg.indexOf(' ');
		if (spacePos == -1) { 
			this.command = msg.toUpperCase();
			this.param = null;
		} else { 
			this.command = msg.substring(0, spacePos).toUpperCase();
			this.param = msg.substring(spacePos+1).toLowerCase();
		}
	}
	
	/**
	 * Constructors a TBGPCommand according to the given command and parameter
	 */
	public TBGPMessage(String command, String param) {
		this.command = command;
		this.param = param;
	}

	public String getCommand() {
		return command;
	}

	public String getParam() {
		return param;
	}
	
	public String toString() {
		return getCommand() + " " + getParam();
	}
}
