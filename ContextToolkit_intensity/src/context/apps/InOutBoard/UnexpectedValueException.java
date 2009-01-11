package context.apps.InOutBoard;

/**
 * This class implements the UnexpectedValueException.  This exception is
 * thrown if an unexpected value is encountered during processing.
 *
 */
public class UnexpectedValueException extends Exception {

  private String message = "";

  /** 
   * Basic constructor for UnexpectedValueException with no message
   */
  public UnexpectedValueException() { 
    super();
  }

  /** 
   * Constructor for UnexpectedValueException with error message
   *
   * @param message Error message
   */
  public UnexpectedValueException(String message) { 
    super(message);
    this.message = message;
  }

  /**
   * Returns the error message
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }
}

