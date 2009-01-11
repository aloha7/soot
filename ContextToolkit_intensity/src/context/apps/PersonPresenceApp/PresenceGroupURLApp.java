// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.comm.DataObject;
import context.arch.widget.WPresenceGroupURL;
import context.arch.handler.Handler;
import context.arch.subscriber.Subscriber;
import context.arch.storage.AttributeNameValues;
import context.arch.MethodException;
import context.arch.InvalidMethodException;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.util.Error;
import java.util.Vector;
import context.arch.BaseObject;


public class PresenceGroupURLApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "PresenceGroupURLApp";

    public PresenceGroupURLApp( java.lang.String location, java.lang.String prgwuHost, int prgwuPort )
    {
        this( location, DEFAULT_PORT, prgwuHost, prgwuPort );
    }

    public PresenceGroupURLApp( java.lang.String location, int localport, java.lang.String prgwuHost, int prgwuPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        context.arch.util.Error done = server.subscribeTo( (context.arch.handler.Handler) this, localport, SUBSCRIBER_ID, prgwuHost, prgwuPort, WPresenceGroupURL.CLASSNAME + "_" + location, WPresenceGroupURL.UPDATE, "PresenceGroupURLUpdate" );
        System.out.println( "done was " + done );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "PresenceGroupURLUpdate" )) {
            System.out.println( "\n\n\n**********************************************\n" );
            context.arch.storage.AttributeNameValues anvs = new context.arch.storage.AttributeNameValues( data );
            java.lang.String who = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroupURL.USERID ).getValue();
            java.lang.String url = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroupURL.URL ).getValue();
            java.lang.String when = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroupURL.TIMESTAMP ).getValue();
            java.lang.String where = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroupURL.LOCATION ).getValue();
            System.out.print( who + " has the url " );
            System.out.print( url + " at the location " );
            System.out.print( where + " at time " );
            System.out.println( when + "\n" );
            System.out.println( "**********************************************\n" );
        } else {
            throw new context.arch.InvalidMethodException( Error.UNKNOWN_CALLBACK_ERROR );
        }
        return null;
    }

    public static void main( java.lang.String[] argv )
    {
        if (argv.length == 3) {
            System.out.println( "Attempting to create a PresenceGroupURLApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.PresenceGroupURLApp prgua = new context.apps.PersonPresenceApp.PresenceGroupURLApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a PresenceGroupURLApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.PresenceGroupURLApp prgua = new context.apps.PersonPresenceApp.PresenceGroupURLApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java PresenceGroupURLApp <username> [port] <WPresenceGroupURLHost/ip> <WPresenceGroupURLPort>" );
            }
        }
    }

}
