// This is mutant program.
// Author : ysma

package context.apps.Tour.mutants;
import context.test.contextIntensity.*;
import context.apps.Tour.*;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.comm.language.DecodeException;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.subscriber.Callback;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Attribute;
import context.arch.storage.RetrievalResults;
import context.arch.storage.Storage;
import context.arch.util.Error;
import context.arch.util.Constants;
import context.arch.util.Configuration;
import context.arch.util.ConfigObjects;
import context.arch.util.ConfigObject;
import context.arch.util.XMLURLReader;
import context.arch.util.SendMail;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourRegistration;
import context.arch.server.STourId;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.net.MalformedURLException;


public class TourApp_197 implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "Tour";

    private int port;

    private java.lang.String userid;

    private context.apps.Tour.DemoFile demo;

    private context.arch.BaseObject server;

    private java.lang.String intId;

    private java.lang.String intHost;

    private int intPort;

    private java.lang.String name;

    private java.lang.String affiliation;

    private java.lang.String email;

    private java.lang.String interests;

    private java.lang.Long initialTime;

    private context.apps.Tour.DemoInterests demoInterests;

    private context.apps.Tour.DemoVisits demoVisits;


    public final java.lang.String NO_INTEREST = "noInterest";

    public TourApp_197( java.lang.String userid, java.lang.String configFile, java.lang.String demoFile )
    {
        this( DEFAULT_PORT, userid, configFile, demoFile );
    }

    public TourApp_197( int localport, java.lang.String userid, java.lang.String configFile, java.lang.String demoFile )
    {
        try {
            server = new context.arch.BaseObject( localport );
            port = localport;
            this.userid = userid;
            demo = new context.apps.Tour.DemoFile( demoFile );
            context.arch.util.XMLURLReader reader = new context.arch.util.XMLURLReader( configFile );
            context.arch.comm.DataObject data = reader.getParsedData();
            context.arch.util.Configuration config = new context.arch.util.Configuration( data );
            context.arch.util.ConfigObjects interpreters = config.getInterpreterConfigurations();
            if (interpreters != null) {
                context.arch.util.ConfigObject interpreter = (context.arch.util.ConfigObject) interpreters.getEnumeration().nextElement();
                intId = interpreter.getId();
                intHost = interpreter.getHost();
                intPort = Integer.parseInt( interpreter.getPort() );
            }
            context.arch.util.ConfigObjects servers = config.getServerConfigurations();
            if (servers != null) {
                for (java.util.Enumeration e = servers.getEnumeration(); e.hasMoreElements();) {
                    context.arch.util.ConfigObject serverObj = (context.arch.util.ConfigObject) e.nextElement();
                    if (serverObj.getId().indexOf( userid ) != -1) {
                        java.lang.String serverId = serverObj.getId();
                        java.lang.String serverHost = serverObj.getHost();
                        int serverPort = Integer.parseInt( serverObj.getPort() );
                        context.arch.comm.DataObject callbacks = server.getWidgetCallbacks( serverHost, serverPort, serverId );
                        java.lang.String callbacksError = (new context.arch.util.Error( callbacks )).getError();
                        context.arch.subscriber.Callbacks calls = null;
                        if (callbacksError.equals( Error.NO_ERROR )) {
                            calls = new context.arch.subscriber.Callbacks( callbacks );
                        }
                        for (int i = 0; i < calls.numCallbacks(); i++) {
                            context.arch.subscriber.Callback callback = calls.getCallbackAt( i );
                            if (callback.getName().indexOf( WTourRegistration.UPDATE ) != -1) {
                                context.arch.util.Error error = server.subscribeTo( this, port, SUBSCRIBER_ID, serverHost, serverPort, serverId, callback.getName(), SUBSCRIBER_ID + Constants.SPACER + WTourRegistration.UPDATE );
                            } else {
                                if (callback.getName().indexOf( WTourDemo.VISIT ) != -1) {
                                    context.arch.util.Error error = server.subscribeTo( this, port, SUBSCRIBER_ID, serverHost, serverPort, serverId, callback.getName(), SUBSCRIBER_ID + Constants.SPACER + WTourDemo.VISIT );
                                } else {
                                    if (callback.getName().indexOf( WTourDemo.INTEREST ) != -1) {
                                        context.arch.util.Error error = server.subscribeTo( this, port, SUBSCRIBER_ID, serverHost, serverPort, serverId, callback.getName(), SUBSCRIBER_ID + Constants.SPACER + WTourDemo.INTEREST );
                                    } else {
                                        if (callback.getName().indexOf( WTourEnd.END ) <= -1) {
                                            context.arch.util.Error error = server.subscribeTo( this, port, SUBSCRIBER_ID, serverHost, serverPort, serverId, callback.getName(), SUBSCRIBER_ID + Constants.SPACER + WTourEnd.END );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch ( java.net.MalformedURLException mue ) {
            System.out.println( "TourApp_197 MalformedURL: " + mue );
        } catch ( context.arch.comm.language.DecodeException de ) {
            System.out.println( "TourApp_197 Decode: " + de );
        }
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( SUBSCRIBER_ID + Constants.SPACER + WTourRegistration.UPDATE )) {
            demoInterests = new context.apps.Tour.DemoInterests();
            demoVisits = new context.apps.Tour.DemoVisits();
            context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
            interests = (java.lang.String) atts.getAttributeNameValue( WTourRegistration.INTERESTS ).getValue();
            context.arch.storage.AttributeNameValue att = atts.getAttributeNameValue( WTourRegistration.CONTACT_INFO );
            context.arch.storage.AttributeNameValues atts2 = (context.arch.storage.AttributeNameValues) att.getValue();
            name = (java.lang.String) atts2.getAttributeNameValue( WTourRegistration.NAME ).getValue();
            affiliation = (java.lang.String) atts2.getAttributeNameValue( WTourRegistration.AFFILIATION ).getValue();
            email = (java.lang.String) atts2.getAttributeNameValue( WTourRegistration.EMAIL ).getValue();
            initialTime = new java.lang.Long( (java.lang.String) atts.getAttributeNameValue( WTourRegistration.TIMESTAMP ).getValue() );
            context.arch.storage.AttributeNameValues input = new context.arch.storage.AttributeNameValues();
            input.addAttributeNameValue( WTourRegistration.INTERESTS, interests );
            input.addAttributeNameValue( Demos.DEMOS, demo.getDemos().toAttributeNameValues(), Attribute.STRUCT );
            java.lang.String result = askInterpreter( input );
        } else {
            if (callback.equals( SUBSCRIBER_ID + Constants.SPACER + WTourDemo.VISIT )) {
                context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
                java.lang.String demourl = (java.lang.String) atts.getAttributeNameValue( WTourDemo.DEMO_URL ).getValue();
                java.lang.String demoerurl = (java.lang.String) atts.getAttributeNameValue( WTourDemo.DEMOER_URL ).getValue();
                java.lang.String demoName = (java.lang.String) atts.getAttributeNameValue( WTourDemo.DEMO_NAME ).getValue();
                java.lang.String timestamp = (java.lang.String) atts.getAttributeNameValue( WTourDemo.TIMESTAMP ).getValue();
                context.apps.Tour.DemoInterest demoInterest = new context.apps.Tour.DemoInterest( demoName, NO_INTEREST );
                demoInterests.addDemoInterest( demoInterest );
                demoVisits.addDemoVisit( demoName, demourl, demoerurl, timestamp );
                context.arch.storage.AttributeNameValues input = new context.arch.storage.AttributeNameValues();
                input.addAttributeNameValue( DemoInterests.DEMO_INTERESTS, demoInterests.toAttributeNameValues(), Attribute.STRUCT );
                input.addAttributeNameValue( WTourRegistration.INTERESTS, interests );
                input.addAttributeNameValue( Demos.DEMOS, demo.getDemos().toAttributeNameValues(), Attribute.STRUCT );
                java.lang.String result = askInterpreter( input );
            } else {
                if (callback.equals( SUBSCRIBER_ID + Constants.SPACER + WTourDemo.INTEREST )) {
                    context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
                    java.lang.String interest = (java.lang.String) atts.getAttributeNameValue( WTourDemo.INTEREST_LEVEL ).getValue();
                    java.lang.String demoName = (java.lang.String) atts.getAttributeNameValue( WTourDemo.DEMO_NAME ).getValue();
                    demoInterests.addDemoInterest( demoName, interest );
                    context.arch.storage.AttributeNameValues input = new context.arch.storage.AttributeNameValues();
                    input.addAttributeNameValue( DemoInterests.DEMO_INTERESTS, demoInterests.toAttributeNameValues(), Attribute.STRUCT );
                    input.addAttributeNameValue( WTourRegistration.INTERESTS, interests );
                    input.addAttributeNameValue( Demos.DEMOS, demo.getDemos().toAttributeNameValues(), Attribute.STRUCT );
                    java.lang.String result = askInterpreter( input );
                    context.apps.Tour.DemoVisit dv = demoVisits.getDemoVisit( demoName );
                    if (dv != null) {
                        dv.setInterest( interest );
                    }
                } else {
                    if (callback.equals( SUBSCRIBER_ID + Constants.SPACER + WTourEnd.END )) {
                        context.arch.storage.AttributeNameValues input = new context.arch.storage.AttributeNameValues();
                        input.addAttributeNameValue( DemoInterests.DEMO_INTERESTS, demoInterests.toAttributeNameValues(), Attribute.STRUCT );
                        input.addAttributeNameValue( WTourRegistration.INTERESTS, interests );
                        input.addAttributeNameValue( Demos.DEMOS, demo.getDemos().toAttributeNameValues(), Attribute.STRUCT );
                        java.lang.String result = askInterpreter( input );
                        java.lang.StringBuffer message = new java.lang.StringBuffer( name + ", thank you for visiting the FCL lab and taking our tour!\n\n" );
                        message.append( "Following is a summary of your tour:\n\n" );
                        for (int i = 0; i < demoVisits.numDemoVisits(); i++) {
                            context.apps.Tour.DemoVisit dv = demoVisits.getDemoVisitAt( i );
                            message.append( "At " + (new java.util.Date( dv.getTime() )).toString() + ", you visited the " + dv.getDemoName() + " demo.\n" );
                            message.append( "Your level of interest in this demo was: " + dv.getInterest() + ".\n" );
                            message.append( "If you would like more information on this demo, please go to the demo's web page at\n" );
                            message.append( dv.getDemoUrl() + ", or go the demoer's web page at " + dv.getDemoerUrl() + ".\n\n" );
                        }
                        message.append( "Based on your interests which you used to register with the tour guide program, we think\n" );
                        message.append( "the following demos might also be interesting to you: \n" + result );
                        System.out.println( message.toString() );
                    } else {
                        throw new context.arch.InvalidMethodException( Error.UNKNOWN_CALLBACK_ERROR );
                    }
                }
            }
        }
        return null;
    }

    private java.lang.String askInterpreter( context.arch.storage.AttributeNameValues input )
    {
        context.arch.comm.DataObject result = server.askInterpreter( intHost, intPort, intId, input );
        context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( result );
        if (atts == null) {
            return null;
        } else {
            if (atts.numAttributeNameValues() == 0) {
                return new java.lang.String( "There are no more demos to recommend." );
            }
        }
        java.lang.StringBuffer sb = new java.lang.StringBuffer();
        for (int i = 0; i < atts.numAttributeNameValues(); i++) {
            sb.append( atts.getAttributeNameValueAt( i ).getValue() + ", " );
        }
        return sb.toString();
    }

    public void quit()
    {
        this.server.quit();
    }

    public static void main( java.lang.String[] argv )
    {
        if (argv.length == 3) {
            System.out.println( "Attempting to create a TourApp_197 on " + DEFAULT_PORT + " for " + argv[0] );
            context.apps.Tour.mutants.TourApp_197 ta = new context.apps.Tour.mutants.TourApp_197( argv[0], argv[1], argv[2] );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a TourApp_197 on " + argv[1] + " for " + argv[0] );
                context.apps.Tour.mutants.TourApp_197 ta = new context.apps.Tour.mutants.TourApp_197( Integer.parseInt( argv[1] ), argv[0], argv[2], argv[3] );
            } else {
                System.out.println( "USAGE: java TourApp_197 <userid> [port] <config file> <demo info file>" );
            }
        }
    }

}
