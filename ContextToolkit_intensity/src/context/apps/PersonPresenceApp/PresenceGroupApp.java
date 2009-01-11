// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.widget.WPresenceGroup;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.subscriber.Subscriber;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.storage.AttributeNameValues;
import context.arch.util.Error;
import java.util.Vector;


public class PresenceGroupApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "PresenceGroupApp";

    public PresenceGroupApp( java.lang.String location, java.lang.String prgwHost, int prgwPort )
    {
        this( location, DEFAULT_PORT, prgwHost, prgwPort );
    }

    public PresenceGroupApp( java.lang.String location, int localport, java.lang.String prgwHost, int prgwPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        context.arch.util.Error done = server.subscribeTo( (context.arch.handler.Handler) this, localport, SUBSCRIBER_ID, prgwHost, prgwPort, WPresenceGroup.CLASSNAME + "_" + location, WPresenceGroup.UPDATE, "PresenceGroupUpdate" );
        System.out.println( "done was " + done );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "PresenceGroupUpdate" )) {
            System.out.println( "\n\n\n**********************************************\n" );
            context.arch.storage.AttributeNameValues anvs = new context.arch.storage.AttributeNameValues( data );
            java.lang.String who = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroup.USERID ).getValue();
            java.lang.String group = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroup.GROUP ).getValue();
            java.lang.String when = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroup.TIMESTAMP ).getValue();
            java.lang.String where = (java.lang.String) anvs.getAttributeNameValue( WPresenceGroup.LOCATION ).getValue();
            System.out.print( who + " is in the group " );
            System.out.print( group + " at the location " );
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
            System.out.println( "Attempting to create a PresenceGroupApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.PresenceGroupApp prga = new context.apps.PersonPresenceApp.PresenceGroupApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a PresenceGroupApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.PresenceGroupApp prga = new context.apps.PersonPresenceApp.PresenceGroupApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java PresenceGroupApp <username> [port] <WPresenceGroupHost/ip> <WPresenceGroupPort>" );
            }
        }
    }

}
