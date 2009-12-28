package trivia;

/**
 * No comment
 */
public class ManyParametersTest extends junit.framework.TestCase {
  
  /**
   * Executed before each testXXX().
   */
  protected void setUp() {
    //TODO: my setup code goes here.
  }
  
  /**
   * Executed after each testXXX().
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    //TODO: my tear down code goes here.
  }
  
  
  /**
   * Constructor
   */
  public ManyParametersTest(String pName) {
    super(pName);
  }
  
  /**
   * Easy access for aggregating test suite.
   */
  public static junit.framework.Test suite() {
    return new junit.framework.TestSuite(ManyParametersTest.class);
  }
  
  /**
   * Main
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(ManyParametersTest.class);
  }
}