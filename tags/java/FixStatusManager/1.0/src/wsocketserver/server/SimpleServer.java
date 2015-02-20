package wsocketserver.server;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import wsocketserver.WSocketServer;
import wsocketserver.users.Users;

import customjbdc.queries.DBMakeQuery;
import emailer.Emailer;

import functions.Functions;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SimpleServer extends WebSocketServer{
    
    static int FIX_FAILURE_STATUS_FLAG = 0; // 0 - ok; 1 - failure
    
    static Timer pinger;
    
	/**
	 * Initializator. tries to open <var>port</var> 
	 * @param port
	 * @throws UnknownHostException
	 */
	public SimpleServer( int port ) throws UnknownHostException {		
            super( new InetSocketAddress( port ) );
            pinger = new Timer();
            pinger.scheduleAtFixedRate(new Pinger(), 0, TimeUnit.SECONDS.toMillis(WSocketServer.PRICE_CHECK_INTERVAL));
	}
	
	/**
	 * Initializator. Creates server using opened port address
	 * @param address
	 */
	public SimpleServer( InetSocketAddress address ) {            
            super( address );    
            pinger = new Timer();
            pinger.scheduleAtFixedRate(new Pinger(), 0, TimeUnit.SECONDS.toMillis(WSocketServer.PRICE_CHECK_INTERVAL));
	}
        

	/**  
	 * Called after an opening handshake has been performed 
	 *  and the given websocket is ready to be written on.
	 *  
	 * @param WebSocket
	 * @param ClientHandshake
	 */
	@Override
	public void onOpen(WebSocket connector, ClientHandshake handshake) {
		//System.out.println("SERVER: new connection: " + handshake.getResourceDescriptor() );
            if( connector == null ) { 
                Functions.printLog("Server:[onOpen] ERR - Connection is null");
                return ;
            }
            
            FIX_FAILURE_STATUS_FLAG = 0;
        }

	/**
	 * Callback for string messages received from the remote host
	 * 
	 * @see #onMessage(WebSocket, ByteBuffer)
	 **/
	@Override
	public void onMessage(WebSocket connector, String message) {
            if( connector == null ) { 
                Functions.printLog("Server:[onMsg] ERR - connection is null");
                return ;
            }
            processMessage(connector, message);		
	}

	/**
	 * Called after the websocket connection has been closed.
	 * 
	 * @param code
	 *            The codes can be looked up here: {@link CloseFrame}
	 * @param reason
	 *            Additional information string
	 * @param remote
	 *            Returns whether or not the closing of the connection was initiated by the remote host.
	 **/
	@Override	
	public void onClose(WebSocket connector, int code, String reason, boolean remote) {
            if( connector == null ) { 
                Functions.printLog("Server:[onClose] WRN - connection is null");
                //return ;
            }
	}

	/**
	 * Called when errors occurs. If an error causes the websocket connection to fail 
	 *  {@link #onClose(WebSocket, int, String, boolean)} will be called additionally.<br>
	 *  This method will be called primarily because of IO or protocol errors.<br>
	 *  If the given exception is an RuntimeException that probably means that you encountered a bug.<br>
	 * 
	 * @param con
	 *            Can be null if there error does not belong to one specific websocket. 
	 *            For example if the servers port could not be bound.
	 **/
	@Override
	public void onError( WebSocket connector, Exception ex ) {
            String err = ex.getMessage();
            Functions.printLog("SERVER:[onError] INF - " + err);
            
            if( connector == null ) { 
                Functions.printLog("Server:[onError] INF - Connection is null");
                return ;
                
            }
	}
        
        private void processMessage(WebSocket connector, String msg){
            if (msg.contains("FIXPrice:")){
                Functions.printLog("Server:[onMsg] INF - "+msg);
                FIX_FAILURE_STATUS_FLAG = 0;
            }
        }
        
        private static class Pinger extends TimerTask {

            public Pinger() {
            }

            @Override
            public void run() {
                if(FIX_FAILURE_STATUS_FLAG == 1){
                    try {
                        new Emailer().sendEmail(WSocketServer.app_uid,"<"+new Date().toString()+">:Running cmd["+WSocketServer.FIXCLIENTFAILURECOMMAND+"]");
                        Functions.printLog("DELAYER: INF - Running command["+WSocketServer.FIXCLIENTFAILURECOMMAND+"]");
                        
                        Runtime.getRuntime().exec(WSocketServer.FIXCLIENTFAILURECOMMAND);
                        FIX_FAILURE_STATUS_FLAG = 0;
                        
                    } catch (IOException ex) {
                        Functions.printLog("DELAYER: ERR - "+ex.getLocalizedMessage());}
                }else {
                    FIX_FAILURE_STATUS_FLAG = 1;
                    
                }
            }
        }
}