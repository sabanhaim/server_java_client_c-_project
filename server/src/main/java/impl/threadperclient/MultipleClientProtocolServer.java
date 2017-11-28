package impl.threadperclient;

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import protocol.AsyncServerProtocol;
import protocol.ServerProtocolFactory;
import protocol.tbgp.TBGPProtocol;
import protocol.tbgp.TBGPRoom;
import protocol.tbgp.TBGPServer;
import protocol.tbgp.games.TBGPGame;
import protocol.tbgp.games.TBGPGameFactory;
import protocol.tbgp.games.bluffer.Bluffer;
import tokenizer.FixedSeparatorMessageTokenizer;
import tokenizer.MessageTokenizer;
import tokenizer.StringMessage;
import tokenizer.TokenizerFactory;
 
class MultipleClientProtocolServer<T> implements Runnable {
    private ServerSocketChannel ssChannel;
    private int listenPort;
    private ServerProtocolFactory<T> protocolFactory;
    private TokenizerFactory<T> tokenizerFactory; 
    
    public MultipleClientProtocolServer(int port, ServerProtocolFactory<T> p, TokenizerFactory<T> t)
    {
        ssChannel = null;
        listenPort = port;
        protocolFactory = p;
        tokenizerFactory = t;
    }
    
    public void run()
    {
        try {
        	ssChannel = ServerSocketChannel.open();
        	ssChannel.socket().bind(new InetSocketAddress(listenPort));
            System.out.println("Listening...");
        }
        catch (IOException e) {
            System.out.println("Cannot listen on port " + listenPort);
        }
        
        while (true)
        {
            try {
                ConnectionHandler<T> newConnection = new ConnectionHandler<T>(ssChannel.accept(), 
                		protocolFactory.create(), tokenizerFactory.create());
                new Thread(newConnection).start();
            } catch (IOException e) {
                System.out.println("Failed to accept on port " + listenPort);
            }
        }
    }
    
 
    // Closes the connection
    public void close() throws IOException
    {
        ssChannel.close();
    }
    
    /** 
     * Runs a TBGP server in the given port. Can easily be changes for other protocols
     */
    public static void main(String[] args) throws IOException {
    	if (args.length != 2) {
    		System.err.println("Usage: server <port> <questionsDBPath>");
            System.exit(1);
    	}

    	try {
	    	int port = Integer.decode(args[0]).intValue();
	    	String questionsPath = args[1];
	    	
	    	TBGPGameFactory gameFactory = new TBGPGameFactory() {
	    		public TBGPGame create(String gameName, TBGPRoom room) {
	    			TBGPGame g = null;
	    			if (gameName.toLowerCase().equals("bluffer")) {
	    				g = new Bluffer(questionsPath, room);
	    			}
	    			return g;
	    		}
	    		
	    		public Set<String> getSupportedGames() {
	    			Set<String> supportedGames = new LinkedHashSet<String>();
	    			supportedGames.add("bluffer");
	    			return supportedGames;
	    		}
	    	};
	    	
	    	TBGPServer tbgpServer = new TBGPServer(gameFactory);
	    	
	    	ServerProtocolFactory<StringMessage> protocolMaker = new ServerProtocolFactory<StringMessage>() {
	    		
	            public AsyncServerProtocol<StringMessage> create() {
	                return new TBGPProtocol(tbgpServer);
	            }
	        };
	        
	        TokenizerFactory<StringMessage> tokenizerMaker = new TokenizerFactory<StringMessage>() {
	            public MessageTokenizer<StringMessage> create() {
	                return new FixedSeparatorMessageTokenizer("\n", Charset.forName("UTF-8"));
	            }
	        };
	    	
	    	MultipleClientProtocolServer<StringMessage> s = 
	    			new MultipleClientProtocolServer<>(port, protocolMaker, tokenizerMaker);
	    	
	    	Thread thread = new Thread(s);
	        thread.start();
	        thread.join();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    }
}
