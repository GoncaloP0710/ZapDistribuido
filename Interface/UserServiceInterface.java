package Interface;

import Utils.observer.Listener;
import Events.*;

public interface UserServiceInterface extends Listener<NodeEvent> {

	// TODO: Add the other methods that belong to the NodeService
	
	/**
	 * Reaction to events, namely those emitted by the music library that 
	 * backs up this playlist (can affect the content of the playlist)
	 */
	@Override
	void processEvent(NodeEvent e);
}
