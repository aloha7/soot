package context.apps.InOutBoard;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import context.apps.InOutBoard.InOutBoard;	// only for MAX_PEOPLE, IN and OUT constants
import context.apps.InOutBoard.InOutRecord;

	
class InOutBoardUI extends Frame implements WindowListener {

	private InOutBoard app = null;	// pointer to our master app
	
	private final int WIDTH  = 640;
	private final int HEIGHT = 480;
	private final int HGAP   = 20;
	private final int VGAP   = 5;
	private final int MGAP 	 = 5;
	private final int NCOLS  = 2;		// make sure NCOLS*NROWS >= MAX_PEOPLE
	private final int NROWS  = 10;


	private final Color  outStatusColor  = Color.red;
	private final Color  inStatusColor   = Color.green;
	private final String outStatusString = InOutBoard.OUT;
	private final String inStatusString  = InOutBoard.IN;
	
	private final Color	 bgColor = Color.cyan;
	
	private int h, w;
	private int ncols, nrows;
	private int hgap, vgap;
	
	private String title = "FCL In/Out Board";
	private int boxCount;	// the # of InOutBox displayed
	private InOutBox boxes[] = new InOutBox[InOutBoard.MAX_PEOPLE];
	
	private final boolean DEBUG = false;
	

	public InOutBoardUI (InOutBoard app) {
	
		super();

		this.app = app;
            this.addWindowListener(this);
		
		h = HEIGHT;
		w = WIDTH;
		ncols = NCOLS;
		nrows = NROWS;
		hgap = HGAP;
		vgap = VGAP;
		
		boxCount = 0;
		
		setSize (w, h);
		
		setTitle (title);
		
		setBackground (bgColor);
		
		setLayout(new BoardLayout(nrows, ncols, hgap, vgap, MGAP));
	}
	
	
	public void start (int count, InOutRecord params[]) {
	
		// fill in new boxes for the records in params
		for (int i = 0; i < count; i++) {
			InOutRecord v = params [i];
			InOutBox newBox = makeBox (v);
		}
		
		// add empty boxes for the rest of the rows/columns 
		// so that the layout manager doesn't resize the boxes (workaround)
		//addDummyBoxes ();

		//this.doLayout ();
		// display the frame
		this.show ();
	}
	
	
	public void setInOut (int id, InOutRecord rec) {
	
		if (id >= boxCount) {		// if this is a non-existent box, create it
			/*if (boxCount > 0) { 	// nothing to remove the first time
				removeDummyBoxes ();	
			}*/
			InOutBox newBox = makeBox (rec);
			//addDummyBoxes ();
		}
		else {									// it already exists, just update its contents
			updateBox (id, rec);
		}
		this.doLayout ();
	}


	private InOutBox makeBox (InOutRecord rec) {
		Color c = null;
		String info = null;
		
		if (DEBUG)
			System.out.println ("makeBox " + boxCount + "\n");
		
		try {		// convert the status string to a color
			c = statusColor (rec.getStatus());
		}
		catch (IllegalArgumentException e) {
			System.out.println ("Couldn't make box for " + rec + ". Got exception: " + e);
		}
		
		info = massageInfoString (rec.getInfo (), rec.getStatus());
		
		// create the box
		InOutBox newBox = new InOutBox (rec.getName(), c, info);

		// keep track of the new box
		boxes[boxCount] = newBox;
		boxCount++;
		
		// add new box to display hierarchy
		add (newBox);
		
		return newBox;
		 
	}	

/*
	private void addDummyBoxes () {
		// create empty boxes to make the layout manager happy
		for (int i = boxCount; i < InOutBoard.MAX_PEOPLE; i++) {
			makeDummyBox (i);
		}
	}
	
	
	private void removeDummyBoxes () {
		// remove empty boxes when we create a new real box
		for (int i = boxCount; i < InOutBoard.MAX_PEOPLE; i++) {
			remove (boxes[i]);
		}
	}
	
	
	private InOutBox makeDummyBox (int i) {
		
		InOutBox newBox = new InOutBox (null, null, null);
		
		boxes[i] = newBox;	// keep dummy boxes to replace them with real boxes
												// don't modify the actual count. These are _dummy_.
		add (newBox);
		
		return newBox;
		 
	}	

*/

	private InOutBox updateBox (int id, InOutRecord rec) {
		Color c = null;
		String info = null;
		
		if (DEBUG)
			System.out.println ("makeBox\n");
		
		try {
			c = statusColor (rec.getStatus());
		}
		catch (IllegalArgumentException e) {
			System.out.println ("Couldn't update box for " + rec + ". Got exception: " + e);
		}
		
		info = massageInfoString (rec.getInfo (), rec.getStatus());
		
		boxes[id].updateContent (rec.getName(), c, info);

		return boxes[id];
		 
	}	
	
	private Color statusColor (String status) throws IllegalArgumentException {
				// more dialogue adaptation
		Color c;
		
		if (status.equals (inStatusString)) {
			c = inStatusColor;
		} else {
			if (status.equals (outStatusString)) {
				c = outStatusColor;
			} else {
				throw new IllegalArgumentException ("InOutBoardUI::statusColor got an unexpected value for status: " + status);
			}
		}
		
		return c;
	}

	private String massageInfoString (String i, String s) {
	
		// convert time
		AgeSensitiveDate dt = new AgeSensitiveDate (i);
		String dtStr = dt.getShortDateTime ();
		
		// massage the info string
		if (s.equals (inStatusString)) {
			i = "In " + dtStr;
		} else {
			if (s.equals (outStatusString)) {
				i = "Out " + dtStr;
			} else {
				throw new IllegalArgumentException ("InOutBoardUI::massageInfoString got an unexpected value for status: " + s);
			}
		}
		
		return i;
		
	}


	// called by the watcher thread thru the app to force a refresh every night
	public void refreshAll () {
		repaint ();
	}
	
	
      public void windowClosing(WindowEvent e) {
        this.setVisible(false);
        this.dispose();
        app.quitApp ();
      }
  
      public void windowOpened(WindowEvent e) {
      }

      public void windowClosed(WindowEvent e) {
      }

      public void windowIconified(WindowEvent e) {
      }

      public void windowDeiconified(WindowEvent e) {
      }

      public void windowActivated(WindowEvent e) {
      }

      public void windowDeactivated(WindowEvent e) {
      }

}


