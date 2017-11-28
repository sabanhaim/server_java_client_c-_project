package impl.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import protocol.*;
import tokenizer.*;

/**
 * This class supplies some data to the protocol, which then processes the data,
 * possibly returning a reply. This class is implemented as an executor task.
 * 
 */
public class ProtocolTask<T> implements Runnable {

	private final ServerProtocol<T> _protocol;
	private final MessageTokenizer<T> _tokenizer;
	private final ConnectionHandler<T> _handler;

	public ProtocolTask(final AsyncServerProtocol<T> protocol, final MessageTokenizer<T> tokenizer, final ConnectionHandler<T> h) {
		this._protocol = protocol;
		this._tokenizer = tokenizer;
		this._handler = h;
	}

	// we synchronize on ourselves, in case we are executed by several threads
	// from the thread pool.
	public synchronized void run() {
	  final ProtocolCallback<T> callback = new ProtocolCallback<T>() {
		@Override
		public void sendMessage(T msg) throws IOException {
			try {
				_handler.addOutData(_tokenizer.getBytesForMessage(msg));
			} catch (CharacterCodingException e) { e.printStackTrace(); }
		}
	  };
		
      // go over all complete messages and process them.
      while (_tokenizer.hasMessage()) {
         T msg = _tokenizer.nextMessage();
         this._protocol.processMessage(msg, callback);
      }
	}

	public void addBytes(ByteBuffer b) {
		_tokenizer.addBytes(b);
	}
}
