package org.custom;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.wsocket.ticks.TicksManager;
import org.wsocket.ticks.Users;


public class TestServer extends WebSocketServer{
	private static int counter = 0;
	
	/**
	 * Initializator. tries to open <var>port</var> 
	 * @param port
	 * @throws UnknownHostException
	 */
	public TestServer( int port ) throws UnknownHostException {		
		super( new InetSocketAddress( port ) );
	}
	
	/**
	 * Initializator. Creates server using opened port address
	 * @param address
	 */
	public TestServer( InetSocketAddress address ) {
		super( address );
	}
	
	/**
	 * Sends <var>message</var> to all currently connected WebSocket clients.
	 * 
	 * @param message
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void broadcastMessage( String message ) {
                
                System.out.println(
                        "TESTSERVER: [broadcastMessage]" + 
                        message
                        );
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( message );
			}
		}
	}
        
        public void sendMessage( Users user, String message ) {
            synchronized ( user ) {           
                if(user.connector.isOpen()){  
                    user.connector.send( message );
                    Functions.printLog(
                    "SERVER:[sendMessage] INF - Message sent: [user:" + user + 
                    "; hash:" + user.hash + "]" +
                    "; key:" + user.key + "]" +
                    "; msg:" + message + "]"
                    );
                }else{
                    Functions.printLog(
                    "SERVER:[sendMessage] WRN - Message failed: [user:" + user + 
                    "; hash:" + user.hash + "]" +
                    "; key:" + user.key + "]" +
                    "; msg:" + message + "]"
                    );
                }
            }
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
            
            //one user more
            counter++;
            
            Functions.printLog("SERVER:[onOpen] INF - Connection count " + counter );
              
            //get uniq key
            String key = connector.toString().split("@")[1];
                
            //add new user to user list    
            Users user = new Users(connector.toString().split("@")[1], connector);
            Users.addNewUser(key, user);
                
            Functions.printLog( 
                    "SERVER:[onOpen] INF - " +
                    "Client[connector:" + connector + 
                    "; ip:" + connector.getRemoteSocketAddress().getAddress().getHostAddress() +
                    "; user:" + Users.getUser(key).toString() +
                    "; key:" + key + ";] connected"
                    );
	}

	/**
	 * Callback for string messages received from the remote host
	 * 
	 * @see #onMessage(WebSocket, ByteBuffer)
	 **/
	@Override
	public void onMessage(WebSocket connector, String message) {
		// TODO Auto-generated method stub
            String hash = null;
            String type = null;
            String key = connector.toString().split("@")[1];
            
            Functions.printLog( 
                            "SERVER:[onMessage] INF - Client[connector:" + connector +
                            "; user:" + Users.getUser(key).toString() + 
                            "] says: " + message 
                            );
            
            //price : bar_id : {1 or 0:boolean} : buys : sells
            if (message.contains("online:")) {
                type = message.substring(7,11);
                hash = message.substring(12);
                Users.assignHash(key, hash, type);

                connector.send(
                    String.valueOf(TicksManager.last_db_price) + 
                    ":" + String.valueOf(TicksManager.currBarId) + 
                    ":0:" + TicksManager.totalOrders
                );

                Functions.printLog(
                    "SERVER:[onMessage] INF - Send: " +
                    String.valueOf(TicksManager.last_db_price) + 
                    ":" + String.valueOf(TicksManager.currBarId) + 
                    ":0:" + TicksManager.totalOrders
                );
            }
            
            if (message.equals("ping")){
                /*System.out.println(
                                "SERVER:[onMessage] ponging back"
                                );*/
                connector.send("pong:"+TicksManager.getServerTimeStatus());
            }

		
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
		// TODO Auto-generated method stub		
		counter--;
                
		String key = connector.toString().split("@")[1];
                
		System.out.println(
				"SERVER:[onClose] Connection " + 
				connector + " was ended[Reason: " +
				reason +
				"]. Total connection number: " + 
				counter
				);
                
                Users.removeUser(key);
	}
	
	@Override
	public void onFragment( WebSocket conn, Framedata fragment ) {
		System.out.println( 
				"SERVER:[onFragment] Received fragment[" + 
				fragment + "] from "+
				conn
				);
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
	public void onError( WebSocket conn, Exception ex ) {
		System.out.println(
				"SERVER:[onError] ERROR " + ex.getMessage()
				
				);
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

}
