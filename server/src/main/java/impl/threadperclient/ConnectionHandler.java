package impl.threadperclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import protocol.AsyncServerProtocol;
import tokenizer.MessageTokenizer;

class ConnectionHandler<T> implements Runnable {
    
	private static final int BUFFER_SIZE = 1024;
	
    private SocketChannel clientChannel;
    private final AsyncServerProtocol<T> protocol;
    private final MessageTokenizer<T> tokenizer;
    
    public ConnectionHandler(SocketChannel acceptedChannel, AsyncServerProtocol<T> p, MessageTokenizer<T> t)
    {
        clientChannel = acceptedChannel;
        protocol = p;
        tokenizer = t;
        System.out.println("Accepted connection from " + acceptedChannel.socket().getRemoteSocketAddress());
    }
    
    public void run()
    {   
        try {
            initialize();
        }
        catch (IOException e) {
            System.out.println("Error in initializing I/O");
        }
 
        try {
            process();
        } 
        catch (IOException e) {
            System.out.println("Error in I/O");
        } 
        
        System.out.println("Connection closed: " + clientChannel.socket().getRemoteSocketAddress());
        close();
 
    }
    
    public void process() throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
        boolean terminated = false;
        
        while (!terminated && !protocol.shouldClose()) {
		
	        // Receive a whole message..
			while (!tokenizer.hasMessage()) {
				buf.clear();
				int numBytesRead = 0;
				try {
					numBytesRead = clientChannel.read(buf);
				} catch (IOException e) {
					numBytesRead = -1;
				}
				// is the channel closed?
				if (numBytesRead == -1) {
					// No more bytes can be read from the channel
					System.out.println("Client on " + clientChannel.socket().getRemoteSocketAddress() + 
							" has disconnected unexpectedly");
					protocol.connectionTerminated();
					terminated = true;
					break;
				}
				
				buf.flip();
				tokenizer.addBytes(buf);
			}
        
			// Process the message
	        protocol.processMessage(tokenizer.nextMessage(), (m) -> clientChannel.write(tokenizer.getBytesForMessage(m)));
        }
        
        close();
    }
    
    // Starts listening
    public void initialize() throws IOException
    {
        // Initialize I/O
        System.out.println("I/O initialized");
    }
    
    // Closes the connection
    public void close()
    {
        try {            
            clientChannel.close();
        }
        catch (IOException e)
        {
            System.out.println("Exception in closing I/O");
        }
    }
    
}