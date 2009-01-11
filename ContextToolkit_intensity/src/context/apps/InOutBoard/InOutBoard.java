// This is mutant program.
// Author : ysma

package context.apps.InOutBoard;


import context.arch.widget.WPNIOSonic;
import context.arch.comm.DataObject;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.util.Error;
import context.arch.util.XMLURLReader;
import context.arch.util.Configuration;
import context.arch.util.ConfigObjects;
import context.arch.util.ConfigObject;
import context.arch.util.ContextTypes;
import context.arch.util.Assert;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.AttributeFunction;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Conditions;
import context.arch.storage.Storage;
import context.arch.storage.Retrieval;
import context.arch.storage.RetrievalResults;
import context.arch.widget.WidgetHandles;
import context.arch.widget.WidgetHandle;
import context.arch.interpreter.Interpreter;
import context.arch.interpreter.IIButton2NameExt;
import context.arch.comm.language.DecodeException;
import cmp.QuickSort.QuickSort;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.net.MalformedURLException;


public class InOutBoard implements context.arch.handler.Handler
{

    private static final boolean DEBUG = false;

    public java.lang.String VERSION_NUMBER = "1.1";

    public static final int MAX_PEOPLE = 20;

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "InOutBoard";

    private static final java.lang.String DefaultConfigURL = "http://www.cc.gatech.edu/fce/contexttoolkit/test.xml";

    private static final java.lang.String TestConfigURL = "http://fire.cc.gt.atl.ga.us/context/config/inout-app-test.xml";

    private final int kOvertimeLimit = 18;

    public static final java.lang.String RFID_ID = "RFIDP_Daniel";

    public static final java.lang.String IN = WPNIOSonic.IN;

    public static final java.lang.String OUT = WPNIOSonic.OUT;

    private static final boolean TESTUI = false;

    private static final boolean USERFID = false;

    private context.apps.InOutBoard.BoardWatcherThread bwt = null;

    private context.apps.InOutBoard.InOutBoardUI ui = null;

    public context.apps.InOutBoard.InOutRecord[] people;

    public int peopleCount = 0;

    private java.util.Hashtable peopleSeen = new java.util.Hashtable();

    private context.apps.InOutBoard.InOutBoardServer server = null;

    public InOutBoard( java.lang.String location, int localport, context.arch.widget.WidgetHandles whs, java.lang.String rfidHost, int rfidPort, java.lang.String rfidID, java.lang.String i2nxHost, int i2nxPort, java.lang.String i2nxID )
    {
        server = new context.apps.InOutBoard.InOutBoardServer( localport );
        server.setApp( this );
        if (!TESTUI) {
            for (int i = 0; i < whs.numWidgetHandles(); i++) {
                context.arch.widget.WidgetHandle wh = whs.getWidgetHandleAt( i );
                context.arch.util.Error error = server.subscribeTo( (context.arch.handler.Handler) this, localport, SUBSCRIBER_ID, wh.getHostName(), wh.getPort(), wh.getId(), WPNIOSonic.UPDATE, "presenceUpdate" );
                if (DEBUG) {
                    System.out.println( "error was " + error );
                }
            }
        }
        if (USERFID) {
            context.arch.util.Error error = server.subscribeTo( (context.arch.handler.Handler) this, localport, SUBSCRIBER_ID, rfidHost, rfidPort, rfidID, WPNIOSonic.UPDATE, "RFIDPresenceUpdate" );
            if (DEBUG) {
                System.out.println( "error for RFID was " + error );
            }
        }
        ui = new context.apps.InOutBoard.InOutBoardUI( this );
        context.arch.comm.DataObject ul = server.runComponentMethod( i2nxHost, i2nxPort, i2nxID, IIButton2NameExt.LISTUSERSNAMES, null, null );
        context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( ul );
        int nPeople = atts.numAttributeNameValues();
        people = new context.apps.InOutBoard.InOutRecord[nPeople];
        context.arch.widget.WidgetHandle wh = whs.getWidgetHandleAt( 0 );
        context.arch.storage.AttributeFunctions afs = new context.arch.storage.AttributeFunctions();
        afs.addAttributeFunction( WPNIOSonic.TIMESTAMP, Attribute.LONG, AttributeFunction.FUNCTION_MAX );
        afs.addAttributeFunction( WPNIOSonic.INOUT );
        for (int i = 0; i < nPeople; i++) {
            java.lang.String name = (java.lang.String) atts.getAttributeNameValueAt( i ).getValue();
            if (DEBUG) {
                System.out.println( "read = " + name );
            }
            addNewUser( name );
        }
        QuickSort.sort( people, people[0] );
        for (int i = 0; i < people.length; i++) {
            peopleSeen.put( people[i].getName(), new java.lang.Integer( i ) );
        }
        ui.start( peopleCount, people );
        for (int i = 0; i < nPeople; i++) {
            java.lang.String name = (java.lang.String) atts.getAttributeNameValueAt( i ).getValue();
            context.arch.storage.Conditions conds = new context.arch.storage.Conditions();
            conds.addCondition( WPNIOSonic.USERNAME, Storage.EQUAL, name );
            context.arch.storage.Retrieval retrieval = new context.arch.storage.Retrieval( afs, conds );
            context.arch.comm.DataObject retrieve = server.retrieveDataFrom( wh.getHostName(), wh.getPort(), wh.getId(), retrieval );
            java.lang.String retrieveError = (new context.arch.util.Error( retrieve )).getError();
            if (retrieveError.equals( Error.NO_ERROR )) {
                context.arch.storage.RetrievalResults retrieveData = new context.arch.storage.RetrievalResults( retrieve );
                if (retrieveData != null) {
                    context.arch.storage.AttributeNameValues anvs = retrieveData.getAttributeNameValuesAt( 0 );
                    context.arch.storage.AttributeNameValue when = anvs.getAttributeNameValue( WPNIOSonic.TIMESTAMP );
                    context.arch.storage.AttributeNameValue status = anvs.getAttributeNameValue( WPNIOSonic.INOUT );
                    userSighting( name, "CRB", (java.lang.String) when.getValue(), (java.lang.String) status.getValue() );
                }
            }
        }
    }

    public void setTestData()
    {
    }

    private java.lang.String now()
    {
        java.lang.Long now = new java.lang.Long( System.currentTimeMillis() );
        return now.toString();
    }

    private void addNewUser( java.lang.String who, java.lang.String what, java.lang.String when )
    {
        context.apps.InOutBoard.InOutRecord newUser = new context.apps.InOutBoard.InOutRecord( who, what, when );
        people[peopleCount++] = newUser;
    }

    private void addNewUser( java.lang.String who, java.lang.String when )
    {
        addNewUser( who, OUT, when );
    }

    private void addNewUser( java.lang.String who )
    {
        addNewUser( who, now() );
    }

    public context.arch.comm.DataObject userSighting( java.lang.String who, java.lang.String where, java.lang.String when, java.lang.String status )
    {
        java.lang.Integer userIndex = (java.lang.Integer) peopleSeen.get( who );
        int ix;
        context.arch.comm.DataObject result = null;
        if (userIndex != null) {
            ix = userIndex.intValue();
            context.apps.InOutBoard.InOutRecord userRecord = people[ix];
            userRecord.setStatus( status );
            userRecord.setInfo( when );
        } else {
            addNewUser( who, when );
            ix = peopleCount - 1;
        }
        if (DEBUG) {
            System.out.println( "about to update UI for: " + ix );
        }
        ui.setInOut( ix, people[ix] );
        return result;
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        context.arch.comm.DataObject result = null;
        if (callback.equals( "presenceUpdate" ) || callback.equals( "RFIDPresenceUpdate" )) {
            context.arch.storage.AttributeNameValues anvs = new context.arch.storage.AttributeNameValues( data );
            context.arch.storage.AttributeNameValue who = anvs.getAttributeNameValue( WPNIOSonic.USERNAME );
            context.arch.storage.AttributeNameValue where = anvs.getAttributeNameValue( WPNIOSonic.LOCATION );
            context.arch.storage.AttributeNameValue when = anvs.getAttributeNameValue( WPNIOSonic.TIMESTAMP );
            context.arch.storage.AttributeNameValue status = anvs.getAttributeNameValue( WPNIOSonic.INOUT );
            if (DEBUG) {
                System.out.println( "\n\n\n**********************************************\n" );
                System.out.print( who.getValue() + " was seen going " );
                System.out.println( status.getValue() + " " );
                System.out.print( where.getValue() + " at time " );
                System.out.println( when.getValue() + "\n" );
                System.out.println( "**********************************************\n" );
            }
            result = userSighting( (java.lang.String) who.getValue(), (java.lang.String) where.getValue(), (java.lang.String) when.getValue(), (java.lang.String) status.getValue() );
            if (DEBUG) {
                System.out.println( "userSighting returned: " + result );
            }
            return result;
        } else {
            throw new context.arch.InvalidMethodException( Error.UNKNOWN_CALLBACK_ERROR );
        }
    }

    public void quitApp()
    {
        System.exit( 0 );
    }

    public void midnightRefresh()
    {
        ui.refreshAll();
    }

    public void checkOvertime()
    {
        for (int i = 0; i < people.length; i++) {
            context.apps.InOutBoard.InOutRecord p = people[i];
        }
    }

    public static void main( java.lang.String[] argv )
    {
        java.lang.String location = null;
        int localPort = -1;
        java.lang.String rfidHost = null;
        int rfidPort = -1;
        java.lang.String rfidID = null;
        java.lang.String ii2nxHost = null;
        int ii2nxPort = -1;
        java.lang.String ii2nxID = null;
        context.arch.widget.WidgetHandles whs = new context.arch.widget.WidgetHandles();
        try {
            context.arch.util.XMLURLReader cfgr;
            if (TESTUI) {
                cfgr = new context.arch.util.XMLURLReader( TestConfigURL );
            } else {
                if (argv.length == 1) {
                    cfgr = new context.arch.util.XMLURLReader( argv[0] );
                } else {
                    cfgr = new context.arch.util.XMLURLReader( DefaultConfigURL );
                }
            }
            context.arch.comm.DataObject cfg = cfgr.getParsedData();
            context.arch.util.Configuration config = new context.arch.util.Configuration( cfg );
            if (DEBUG) {
                System.out.println( cfg );
            }
            context.arch.storage.AttributeNameValues params = config.getParameters();
            if (params != null) {
                context.arch.storage.AttributeNameValue loc = params.getAttributeNameValue( "LOCATION" );
                if (loc != null) {
                    location = (java.lang.String) loc.getValue();
                }
                context.arch.storage.AttributeNameValue port = params.getAttributeNameValue( "LOCALPORT" );
                if (port != null) {
                    localPort = Integer.parseInt( (java.lang.String) port.getValue() );
                }
            }
            context.arch.util.ConfigObjects widgets = config.getWidgetConfigurations();
            if (widgets != null) {
                for (java.util.Enumeration e = widgets.getEnumeration(); e.hasMoreElements();) {
                    context.arch.util.ConfigObject widget = (context.arch.util.ConfigObject) e.nextElement();
                    if (DEBUG) {
                        System.out.println( "Widget: " + widget );
                    }
                    if (widget.getId() != null) {
                        if (widget.getId().equals( RFID_ID )) {
                            rfidID = widget.getId();
                            if (widget.getHost() != null) {
                                rfidHost = widget.getHost();
                            }
                            if (widget.getPort() != null) {
                                rfidPort = Integer.parseInt( widget.getPort() );
                            }
                        } else {
                            if (widget.getHost() != null && widget.getPort() != null) {
                                whs.addWidgetHandle( widget.getId(), widget.getHost(), Integer.parseInt( widget.getPort() ) );
                            }
                        }
                    }
                }
            }
            context.arch.util.ConfigObjects interpreters = config.getInterpreterConfigurations();
            if (interpreters != null) {
                context.arch.util.ConfigObject interpreter = (context.arch.util.ConfigObject) interpreters.getEnumeration().nextElement();
                if (interpreter != null) {
                    if (interpreter.getHost() != null) {
                        ii2nxHost = interpreter.getHost();
                    }
                    if (interpreter.getPort() != null) {
                        ii2nxPort = Integer.parseInt( interpreter.getPort() );
                    }
                    if (interpreter.getId() != null) {
                        ii2nxID = interpreter.getId();
                    }
                }
            }
        } catch ( java.net.MalformedURLException mue ) {
            System.out.println( "InOutBoard MalformedURL: " + mue );
        } catch ( context.arch.comm.language.DecodeException de ) {
            System.out.println( "InOutBoard Decode: " + de );
        }
        if (DEBUG) {
            System.out.println( "Attempting to create an InOutBoard. Configuration is:" );
            System.out.println( "location = " + location );
            System.out.println( "localPort = " + localPort );
            for (int i = 0; i < whs.numWidgetHandles(); i++) {
                context.arch.widget.WidgetHandle wh = whs.getWidgetHandleAt( i );
                System.out.println( "wpnioHost " + i + " = " + wh.getHostName() );
                System.out.println( "wpnioPort " + i + " = " + wh.getPort() );
                System.out.println( "wpnioID " + i + " = " + wh.getId() );
            }
            System.out.println( "rfidHost = " + rfidHost );
            System.out.println( "rfidPort = " + rfidPort );
            System.out.println( "rfidID = " + rfidID );
            System.out.println( "ii2nxHost = " + ii2nxHost );
            System.out.println( "ii2nxPort = " + ii2nxPort );
            System.out.println( "ii2nxID = " + ii2nxID );
        }
        context.apps.InOutBoard.InOutBoard iobApp = new context.apps.InOutBoard.InOutBoard( location, localPort, whs, rfidHost, rfidPort, rfidID, ii2nxHost, ii2nxPort, ii2nxID );
    }

}
