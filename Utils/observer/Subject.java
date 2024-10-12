package Utils.observer;

/**
 *
 * @param <E>
 * Represents objects that are observed by listeners of events of type E
 * 
 */

 public interface Subject<E extends Event> {

	/**
	 * Emits a given event to the listeners
	 * 
	 * @param e event that occurred
	 */
	void emitEvent(E e);

}
