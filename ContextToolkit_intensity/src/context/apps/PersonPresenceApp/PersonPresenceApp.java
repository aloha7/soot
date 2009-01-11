// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.widget.WPersonPresence;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Retrieval;
import context.arch.storage.Conditions;
import context.arch.storage.StorageObject;
import context.arch.storage.Storage;
import context.arch.storage.RetrievalResults;
import context.arch.util.Error;
import context.arch.widget.WTourRegistration;
import java.util.Vector;
import java.util.Hashtable;


public class PersonPresenceApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "PersonPresenceApp";

    private int numHandles = 0;

    private int port;

    public PersonPresenceApp( java.lang.String location, java.lang.String ppwHost, int ppwPort )
    {
        this( location, DEFAULT_PORT, ppwHost, ppwPort );
    }

    public PersonPresenceApp( java.lang.String location, int localport, java.lang.String ppwHost, int ppwPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        port = localport;
        java.lang.String widgetId = WPersonPresence.CLASSNAME + "_" + location;
        System.out.println( "**********************************************" );
        context.arch.comm.DataObject version = server.getVersion( ppwHost, ppwPort, WTourRegistration.CLASSNAME + "_CRB" );
        java.lang.String versionError = (new context.arch.util.Error( version )).getError();
        java.lang.String versionNumber = null;
        if (versionError.equals( Error.NO_ERROR )) {
            versionNumber = (java.lang.String) version.getDataObject( WPersonPresence.VERSION ).getValue().firstElement();
        }
        System.out.println( "Version with invalid id: " + version + "\n" );
        System.out.println( "error = " + versionError + ", version number = " + versionNumber );
        System.out.println( "**********************************************\n" );
        System.out.println( "**********************************************" );
        context.arch.comm.DataObject ping = server.pingComponent( ppwHost, ppwPort, widgetId + "blah" );
        java.lang.String pingError = (new context.arch.util.Error( ping )).getError();
        System.out.println( "error = " + pingError );
        System.out.println( "**********************************************\n" );
        System.out.println( "**********************************************" );
        context.arch.comm.DataObject ping2 = server.getVersion( ppwHost, ppwPort, widgetId );
        java.lang.String pingError2 = (new context.arch.util.Error( ping2 )).getError();
        System.out.println( "error = " + pingError2 );
        System.out.println( "**********************************************\n" );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "presenceUpdate" )) {
            context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
            System.out.println( "\n\n\n**********************************************\n" );
            context.arch.storage.AttributeNameValue idAtt = atts.getAttributeNameValue( WPersonPresence.USERID );
            if (idAtt != null) {
                java.lang.String userid = (java.lang.String) idAtt.getValue();
                System.out.print( userid + " was seen at " );
            } else {
                System.out.print( "null was seen at " );
            }
            context.arch.storage.AttributeNameValue locAtt = atts.getAttributeNameValue( WPersonPresence.LOCATION );
            if (locAtt != null) {
                java.lang.String locationValue = (java.lang.String) locAtt.getValue();
                System.out.print( locationValue + " at time " );
            } else {
                System.out.print( "null at time " );
            }
            context.arch.storage.AttributeNameValue timeAtt = atts.getAttributeNameValue( WPersonPresence.TIMESTAMP );
            if (timeAtt != null) {
                java.lang.String time = (java.lang.String) timeAtt.getValue();
                System.out.print( time + "\n" );
            } else {
                System.out.print( "null\n" );
            }
            System.out.println( "**********************************************\n" );
            numHandles++;
        } else {
            numHandles++;
            throw new context.arch.InvalidMethodException( Error.UNKNOWN_CALLBACK_ERROR );
        }
        return null;
    }

    public static void main( java.lang.String[] argv )
    {
        if (argv.length == 3) {
            System.out.println( "Attempting to create a PersonPresenceApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.PersonPresenceApp pnpa = new context.apps.PersonPresenceApp.PersonPresenceApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a PersonPresenceApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.PersonPresenceApp pnpa = new context.apps.PersonPresenceApp.PersonPresenceApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java PersonPresenceApp <location> [port] <WPersonPresenceHost/ip> <WPersonPresencePort>" );
            }
        }
    }

}
