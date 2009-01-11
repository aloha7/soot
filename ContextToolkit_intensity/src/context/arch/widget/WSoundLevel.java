package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.InvalidMethodException;
import context.arch.generator.PositionIButton;
import context.arch.generator.IButtonData;
import context.arch.subscriber.Callbacks;
import context.arch.service.Services;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

/**
 * This class contains definitions for a context widget 
 * that provides information on 
 * the ambient sound level in a particular location.  The
 * information is in the form of a location and a volume value.  It
 * has the following callbacks: UPDATE. It supports polling and
 * subscriptions.  Currently it only uses the SoundLevel widget
 * written in Frontier and itself relying on the Sonic Mac application
 * (ie, the generator).
 * It handles only a single SoundLevel instance.
 * Since the widget is entirely implemented in Frontier, this class
 * only contains definitions useful for the application programmer wishing
 * to use the SoundLevel widget.
 *
 * @see context.arch.widget.Widget
 */
public class WSoundLevel extends Widget {

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Tag for user id
   */
  public static final String VOLUME = ContextTypes.VOLUME;

  /**
   * Tag for user location 
   */
  public static final String LOCATION = ContextTypes.LOCATION;

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "SoundLevel";

  /** 
   * Dummy constructor to allow compilation
   */
  public WSoundLevel() {
    super(CLASSNAME);
    setVersion(VERSION_NUMBER);
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    empty
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    return new Attributes();
  }
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    empty
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    return new Callbacks();
  }

  /**
   * This method implements the abstract method Widget.setServices().
   * It currently has no services and returns an empty Services object.
   *
   * @return the Services provided by this widget
   */
  protected Services setServices() {
    return new Services();
  }
  
  /**
   * This method implements the abstract method Widget.queryGenerator().
   * It just returns null.
   *
   * @return the AttributeNameValues from this widget's generator
   */
  protected AttributeNameValues queryGenerator() {
    return null;
  }

}
