package context.arch.handler;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class maintains a list of context widget handlers, allows additions and 
 * removals of individual handlers.
 */
public class Handlers extends Hashtable {

  /**
   * Basic empty constructor
   */
  public Handlers() {
    super(5);
  }

  /**
   * Adds a handler to the handler list
   *
   * @param handlerInfo container for handler info
   * @see context.arch.handler.HandlerInfo
   */
  public synchronized void addHandler(HandlerInfo info) {
    put(info.getSubId()+info.getCallback(), info);
  }

  /**
   * Removes a handler from the handler list
   *
   * @param handlerInfo HandlerInfo object to remove
   * @see context.arch.handler.HandlerInfo
   */
  public synchronized void removeHandler(HandlerInfo info) {
    remove(info.getSubId()+info.getCallback());
  }

  /**
   * Returns a handler that matches the given key
   *
   * @param key String that matches handler info
   * @return context widget handler that matches the given key
   */
  public synchronized Handler getHandler(String key) {
    HandlerInfo info = (HandlerInfo)get(key);
    if (info != null) {
      return info.getHandler();
    }
    return null;
  }

  /**
   * Returns an enumeration containing all the handlers in the list
   */
  public synchronized Enumeration getHandlers() {
    return elements();
  }

  /**
   * Returns the number of handlers in the list
   */
  public synchronized int numHandlers() {
    return size();
  }

}
