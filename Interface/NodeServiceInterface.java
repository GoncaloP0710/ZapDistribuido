package Interface;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import utils.observer.Listener;
import Events.*;

public interface NodeServiceInterface extends Listener<NodeEvent> {
	
	/**
	 * Reaction to events, namely those emitted by the music library that 
	 * backs up this playlist (can affect the content of the playlist)
	 */
	@Override
	void processEvent(NodeEvent e);
}
