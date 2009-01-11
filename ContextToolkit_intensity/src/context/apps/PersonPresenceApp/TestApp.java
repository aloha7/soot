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
import context.arch.interpreter.IIButton2Name;
import context.arch.util.Error;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Retrieval;
import context.arch.storage.Conditions;
import context.arch.storage.StorageObject;
import context.arch.storage.Storage;
import context.arch.storage.RetrievalResults;
import java.util.Vector;
import java.util.Hashtable;


public class TestApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "PersonPresenceApp";

    private int numHandles = 0;

    private int port;

    private java.lang.String userid = "16CF8F0800000076";

    public TestApp( java.lang.String location, java.lang.String ppwHost, int ppwPort, java.lang.String iib2nHost, int iib2nPort )
    {
        this( location, DEFAULT_PORT, ppwHost, ppwPort, iib2nHost, iib2nPort );
    }

    public TestApp( java.lang.String location, int localport, java.lang.String ppwHost, int ppwPort, java.lang.String iib2nHost, int iib2nPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        port = localport;
        java.lang.String widgetId = WPersonPresence.CLASSNAME + "_" + location;
        context.arch.storage.AttributeNameValues preInterpretAtts3 = new context.arch.storage.AttributeNameValues();
        preInterpretAtts3.addAttributeNameValue( IIButton2Name.IBUTTONID, userid, Attribute.STRING );
        context.arch.comm.DataObject interpret3 = server.askInterpreter( iib2nHost, iib2nPort, IIButton2Name.CLASSNAME, preInterpretAtts3 );
        java.lang.String interpretError3 = (new context.arch.util.Error( interpret3 )).getError();
        context.arch.storage.AttributeNameValues interpretAtts3 = null;
        if (interpretError3.equals( Error.NO_ERROR )) {
            interpretAtts3 = new context.arch.storage.AttributeNameValues( interpret3 );
        }
        System.out.println( "Interpret with valid attribute: " + interpret3 + "\n" );
        System.out.println( "error = " + interpretError3 + ", interpreted atts = " + interpretAtts3 );
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
                userid = (java.lang.String) idAtt.getValue();
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
        if (argv.length == 5) {
            System.out.println( "Attempting to create a PersonPresenceApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.TestApp ta = new context.apps.PersonPresenceApp.TestApp( argv[0], argv[1], Integer.parseInt( argv[2] ), argv[3], Integer.parseInt( argv[4] ) );
        } else {
            if (argv.length == 6) {
                System.out.println( "Attempting to create a PersonPresenceApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.TestApp ta = new context.apps.PersonPresenceApp.TestApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ), argv[4], Integer.parseInt( argv[5] ) );
            } else {
                System.out.println( "USAGE: java TestApp <location> [port] <WPersonPresenceHost/ip> <WPersonPresencePort> <IIButton2NameHost/ip> <IIButton2NamePort>" );
            }
        }
    }

}
