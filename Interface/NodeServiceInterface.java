package Interface;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import utils.observer.Listener;
import Events.*;

public interface NodeServiceInterface extends Listener<NodeEvent>, PropertyChangeListener{
    
    // TODO: Add other methods

    /**
	 * Reaction to property change events, namely those emitted by the player
	 * (can affect the selected song and song being played)
	 */
	@Override
	void propertyChange(PropertyChangeEvent evt);
	
	/**
	 * Reaction to events, namely those emitted by the music library that 
	 * backs up this playlist (can affect the content of the playlist)
	 */
	@Override
	void processEvent(NodeEvent e);
}
