// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.widget.WPersonNamePresence;
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
import context.arch.storage.Attributes;
import context.arch.util.Error;
import java.util.Vector;
import context.arch.storage.Attribute;


public class PersonNamePresenceApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "PersonNamePresenceApp";

    public PersonNamePresenceApp( java.lang.String location, java.lang.String pnpwHost, int pnpwPort )
    {
        this( location, DEFAULT_PORT, pnpwHost, pnpwPort );
    }

    public PersonNamePresenceApp( java.lang.String location, int localport, java.lang.String pnpwHost, int pnpwPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        context.arch.util.Error done = server.subscribeTo( (context.arch.handler.Handler) this, localport, SUBSCRIBER_ID, pnpwHost, pnpwPort, WPersonNamePresence.CLASSNAME + "_" + location, WPersonNamePresence.UPDATE, "presenceUpdate" );
        System.out.println( "\r\nError:" + done.getError() );
        context.arch.comm.DataObject d3 = server.getVersion( pnpwHost, pnpwPort, WPersonNamePresence.CLASSNAME + "_" + location );
        System.out.println( "Version is: " + d3 + "\n\n" );
        context.arch.comm.DataObject data = pollPersonNamePresenceWidget( server, location, pnpwHost, pnpwPort );
        if (data != null) {
            java.lang.String error = (new context.arch.util.Error( data )).getError();
            if (!error.equals( Error.NO_ERROR )) {
                System.out.println( "Error in polling: " + error );
            } else {
                System.out.println( "\n\n\n**********************************************\n" );
                context.arch.storage.AttributeNameValues anvs = new context.arch.storage.AttributeNameValues( data );
                java.lang.String who = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.USERNAME ).getValue();
                java.lang.String where = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.LOCATION ).getValue();
                java.lang.String when = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.TIMESTAMP ).getValue();
                System.out.print( who + " was seen at " );
                System.out.print( where + " at time " );
                System.out.println( when + "\n" );
                System.out.println( "**********************************************\n" );
            }
        } else {
            System.out.println( "results of polling was null!!!" );
        }
    }

    private context.arch.comm.DataObject pollPersonNamePresenceWidget( context.arch.BaseObject server, java.lang.String location, java.lang.String host, int port )
    {
        context.arch.comm.DataObject widget = new context.arch.comm.DataObject( WPersonNamePresence.ID, WPersonNamePresence.CLASSNAME + "_" + location );
        context.arch.storage.Attributes atts = new context.arch.storage.Attributes();
        atts.addAttribute( WPersonNamePresence.USERNAME );
        atts.addAttribute( WPersonNamePresence.LOCATION );
        atts.addAttribute( WPersonNamePresence.TIMESTAMP, Attribute.LONG );
        java.util.Vector v = new java.util.Vector();
        v.addElement( widget );
        v.addElement( atts.toDataObject() );
        context.arch.comm.DataObject query = new context.arch.comm.DataObject( WPersonNamePresence.QUERY, v );
        try {
            return server.userRequest( query, WPersonNamePresence.QUERY, host, port );
        } catch ( context.arch.comm.language.DecodeException de ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() Decode: " + de );
        } catch ( context.arch.comm.language.EncodeException ee ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() Encode: " + ee );
        } catch ( context.arch.comm.language.InvalidDecoderException ide ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() InvalidDecoder: " + ide );
        } catch ( context.arch.comm.language.InvalidEncoderException iee ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() InvalidEncoder: " + iee );
        } catch ( context.arch.comm.protocol.InvalidProtocolException ipe ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() InvalidProtocol: " + ipe );
        } catch ( context.arch.comm.protocol.ProtocolException pe ) {
            System.out.println( "PersonPresenceApp pollPersonNamePresenceWidget() Protocol: " + pe );
        }
        return null;
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "presenceUpdate" )) {
            System.out.println( "\n\n\n**********************************************\n" );
            context.arch.storage.AttributeNameValues anvs = new context.arch.storage.AttributeNameValues( data );
            java.lang.String who = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.USERNAME ).getValue();
            java.lang.String when = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.TIMESTAMP ).getValue();
            java.lang.String where = (java.lang.String) anvs.getAttributeNameValue( WPersonNamePresence.LOCATION ).getValue();
            System.out.print( who + " was seen at " );
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
            System.out.println( "Attempting to create a PersonNamePresenceApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.PersonNamePresenceApp pnpa = new context.apps.PersonPresenceApp.PersonNamePresenceApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a PersonNamePresenceApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.PersonNamePresenceApp pnpa = new context.apps.PersonPresenceApp.PersonNamePresenceApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java context.apps.PersonPresenceApp.PersonNamePresenceApp <location> [port] <WPersonNamePresenceHost/ip> <WPersonNamePresencePort>" );
            }
        }
    }

}
