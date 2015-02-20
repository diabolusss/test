package fixclient.wsocket;

import customjbdc.queries.DBMakeQuery;
import fixclient.FixClient;
import static fixclient.FixClient.WSConnectionList;
import functions.Functions;
import java.net.URI;
//import org.custom.Functions;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

/** This example demonstrates how to create a websocket connection to a server. Only the most important callbacks are overloaded. */
public class SimpleClient extends WebSocketClient {
	public SimpleClient( URI serverUri , Draft draft ) {
		super( serverUri, draft );
	}

	public SimpleClient( URI serverURI ) {
		super( serverURI );
	}

        // if you plan to refuse connection based on ip or httpfields overload: 
        //   onWebsocketHandshakeReceivedAsClient
	@Override
	public void onOpen( ServerHandshake handshakedata ) {
            Functions.printLog(this.uri.toString().toUpperCase()+":[onOpen] INF - opened connection");
            //NB REVISIONED
            DBMakeQuery.setSocketAvailable(FixClient.jdbc, null, this.uri.toString().substring( ((FixClient.tls_enabled_server)?(6):(5))  ), 1);
        }

	@Override
	public void onMessage( String message ) {
            Functions.printLog(this.uri.toString().toUpperCase()+":[onMessage] INF - message["+message+"]");
	}

	@Override
	public void onClose( int code, String reason, boolean remote ) {
            // The codecodes are documented in class org.java_websocket.framing.CloseFrame
            Functions.printLog(this.uri.toString().toUpperCase()+":[onClose] INF - Connection closed by " + ( remote ? "remote peer" : "us" ) );
            //NB REVISIONED
            DBMakeQuery.setSocketAvailable(FixClient.jdbc, null, this.uri.toString().substring(((FixClient.tls_enabled_server)?(6):(5))), 0);
        }

	@Override
	public void onError( Exception ex ) {
            // if the error is fatal then onClose will be called additionally
            Functions.printLog(this.uri.toString().toUpperCase()+":[onError] ERR - "+ex.getLocalizedMessage());
            //DBMakeQuery.setSocketAvailable(FixClient.jdbc, null, this.uri.toString().substring(((FixClient.tls_enabled_server)?(6):(5))), 0);	
        }

}