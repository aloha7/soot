package context.apps.InOutBoard;

import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.InvalidMethodException;
import context.arch.MethodException;


public class InOutBoardServer extends BaseObject {

	private final boolean DEBUG = true;
	
	private InOutBoard app = null;
	
	public InOutBoardServer (int port) {
		super (port);
	}
	
	public void setApp (InOutBoard theApp) {
	
		app = theApp;
	}
	
  public DataObject runUserMethod(DataObject data) throws InvalidMethodException, MethodException {
    
    if (DEBUG)
	  	System.out.println ("*** USER METHOD ***");

    String s = new String ("{");
    s = s + app.people[0].toString ();
    for (int i = 1; i < app.peopleCount; i++) {
      s = s + "," + app.people[i].toString ();
    }
    s = s + "}";
   		
    DataObject result = new DataObject ("hack", s);
    
    if (DEBUG) {
    	System.out.println ("*** RETURNING ***");
    	System.out.println (s);
    }
    
    return result;
  }

}