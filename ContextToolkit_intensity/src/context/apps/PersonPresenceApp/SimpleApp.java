// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.widget.WContactPresence;
import context.arch.widget.WPersonNamePresence;
import context.arch.widget.WPersonNamePresence2;
import context.arch.widget.WPersonPresence;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.storage.AttributeNameValues;
import context.arch.util.Constants;
import context.arch.util.ContextTypes;
import context.arch.util.Error;
import context.arch.widget.WDisplay;
import context.arch.handler.AsyncServiceHandler;
import context.arch.interpreter.IIButton2Name;
import java.util.Vector;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.Attributes;
import context.arch.storage.Conditions;
import context.arch.storage.Retrieval;
import context.arch.storage.RetrievalResults;
import context.arch.storage.Storage;
import context.arch.subscriber.Subscriber;


public class SimpleApp implements context.arch.handler.Handler, context.arch.handler.AsyncServiceHandler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "SimpleApp";

    private int numHandles = 0;

    private int port;

    private context.arch.BaseObject server;

    private java.lang.String userid = "16CF8F0800000076";

    public SimpleApp( java.lang.String location, int localport, java.lang.String ppwHost, int ppwPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        port = localport;
        java.lang.String widgetId = WPersonNamePresence2.CLASSNAME + "_" + location;
        context.arch.util.Error error5 = server.subscribeTo( this, port, SUBSCRIBER_ID, ppwHost, ppwPort, widgetId, WPersonNamePresence2.UPDATE, "presenceUpdate" );
    }

    public SimpleApp()
    {
        int localPort = 5001;
        server = new context.arch.BaseObject( localPort );
        java.lang.String widgetHost = "127.0.0.1";
        int widgetPort = WPersonNamePresence2.DEFAULT_PORT;
        java.lang.String widgetId = WPersonNamePresence2.CLASSNAME + "_" + "test";
        context.arch.util.Error error5 = server.subscribeTo( this, localPort, SUBSCRIBER_ID, widgetHost, widgetPort, widgetId, WPersonNamePresence2.UPDATE, "presenceUpdate" );
        System.out.println( "\r\nError:" + error5.getError() );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "presenceUpdate" )) {
            context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
            System.out.println( "subscription results are: " + atts );
        }
        return null;
    }

    public context.arch.comm.DataObject asynchronousServiceHandle( java.lang.String requestTag, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (requestTag.equals( "display" )) {
            System.out.println( data.toString() );
        }
        return null;
    }

    public static void main( java.lang.String[] argv )
    {
        if (argv.length == 4) {
            System.out.println( "Attempting to create a SimpleApp on " + argv[2] + " at " + argv[0] );
            context.apps.PersonPresenceApp.SimpleApp sa = new context.apps.PersonPresenceApp.SimpleApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
        } else {
            System.out.println( "USAGE: java SimpleApp <location> [port] <WPersonNamePresence2Host/ip> <WPersonNamePresence2Port>" );
        }
    }

}
