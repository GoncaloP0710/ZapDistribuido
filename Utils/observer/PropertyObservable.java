package utils.observer;

import java.beans.PropertyChangeListener;

/**
 * 
 * An observable that can be observed by PropertyChangeListeners
 */
public interface PropertyObservable {
	/**
	 * Adds the given listener
	 * 
	 * @param listener to be added
	 */
	void addListener(PropertyChangeListener listener); 
	/**
	 * Removes the the given listener
	 * 
	 * @param listener to be removed
	 */
	void removeListener(PropertyChangeListener listener);
}