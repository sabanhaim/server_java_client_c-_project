package protocol.tbgp.games;

import java.util.Set;

import protocol.tbgp.TBGPRoom;

public interface TBGPGameFactory {
	TBGPGame create(String gameName, TBGPRoom room);
	
	Set<String> getSupportedGames();
}

