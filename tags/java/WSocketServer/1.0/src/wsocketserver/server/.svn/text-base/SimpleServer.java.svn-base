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

import functions.Functions;


public class SimpleServer extends WebSocketServer{	
    static int connection_count = 0;
    
    static final int MAX_UNRESPONDED_PINGS = 4;
	/**
	 * Initializator. tries to open <var>port</var> 
	 * @param port
	 * @throws UnknownHostException
	 */
	public SimpleServer( int port ) throws UnknownHostException {		
		super( new InetSocketAddress( port ) );
	}
	
	/**
	 * Initializator. Creates server using opened port address
	 * @param address
	 */
	public SimpleServer( InetSocketAddress address ) {            
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
	public void broadcastMessage( String message, int mode ) {    
            int unauthorized_users = 0;
            
            switch(mode){
                case 0: //fix price msg //received in form [FIXPrice:price:bar_id:buys:sells]
                    //send in form [price:bar_id:check:buys:sells]
                    String[] messageParts = message.split(":");     
                    
                    if (Users.UserList.size() > 0) {    
                        for (String key : Users.UserList.keySet()) {                            
                            Users user = (Users)Users.UserList.get(key);
                            if(user == null) continue;

                            if(!user.hash.equals("unknown")){
                                if(user.type.equals("real")){
                                    DBMakeQuery.checkUserOrderStatus(WSocketServer.jdbc, null, user.id);

                                }else if(user.type.equals("demo")){
                                    DBMakeQuery.checkUserOrderStatus(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                                }
                            }
                            
                            if(!user.type.equals("real") || !user.type.equals("demo") || !user.type.equals("sys")) unauthorized_users++;
                            
                            String parsedMessage = messageParts[0]+":"+messageParts[1]
                                + (
                                    (user.hash.equals("unknown")==true)?
                                        ("")
                                        :
                                        (":" + user.check + ":" + messageParts[2]+":"+messageParts[3]+":"+user.order_open)
                                  )
                                ;                        

                            sendMessage(user, parsedMessage);
                        }
                    }
                    break;
                //END of case 0
                case 1:
                    if (Users.UserList.size() > 0) {    
                        for (String key : Users.UserList.keySet()) {
                            if (!Users.UserList.get(key).type.equalsIgnoreCase("sys")) Users.UserList.get(key).dead++;
                            Users user = (Users)Users.UserList.get(key);
                            if(user == null) continue;
                            
                            if(!user.type.equals("real") || !user.type.equals("demo") || !user.type.equals("sys")) unauthorized_users++;
                            
                            sendMessage(user, message + ":" + user.dead);
                        }
                    }     
                    break;
                //END of case 1                    
                    
            }
            
            DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, unauthorized_users-1, 1);
	}
        
        public void sendMessage( Users user, String message ) {
            //Nested locks can result in deadlocks quite easily if 
            // one is using wait/notify. 
            synchronized (user) {                    
                    if(user.connector != null && user.connector.isOpen() && user.dead < MAX_UNRESPONDED_PINGS){  
                        user.connector.send( message );
                        Functions.printLog(
                        "SERVER:[sendMessage] INF - Message sent: [user:" + user.id + 
                        "; hash:" + user.hash + "]" +
                        "; ip:" + user.connector.getRemoteSocketAddress().getAddress().getHostAddress() +
                        "; key:" + user.key + "]" +
                        "; msg:" + message + "]"+
                        "; user.dead="+user.dead + "]" + 
                        "; type: " + user.type
                        );                  
                   
                    }else{
                        Functions.printLog(
                        "SERVER:[sendMessage] WRN - Message failed or ghost connection: [user:" + user.id + 
                        "; hash:" + user.hash + "]" +
                        "; key:" + user.key + "]" +
                        "; msg:" + message + "]"+
                        "; user.dead="+user.dead
                        );

                        if(user.connector.isOpen()) user.connector.close();
                        Users.removeUser(user.key);                        
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
            if( connector == null ) { 
                Functions.printLog("Server:[onOpen] ERR - Connection is null");
                return ;
            }
              
            /* check connection count from one ip */
            if (Users.UserList.size() > 0) {    
                int counter = 0;
                Functions.printLog("SERVER:[onOpen] WRN - Multiple connections from one ip: ");
                
                for (String _key : Users.UserList.keySet()) {
                    Users user = (Users)Users.UserList.get(_key);
                    
                    if(user == null) continue;
                    if(user.connector.getRemoteSocketAddress().getAddress().getHostAddress().equals(connector.getRemoteSocketAddress().getAddress().getHostAddress())){
                        //if system user and more than 2 from one ip
                        //dont add
                        if(counter > 3 && user.type.equals("sys")){
                            connector.close(counter, "Too much connections!");
                            return;
                        }else counter++;
                    }
                    System.out.print(counter+", ");
                }
                System.out.println();
            }/* end */
            
            //get uniq key
            String key = 
                    //connector.getRemoteSocketAddress().getAddress().getHostAddress();
                    connector.toString().split("@")[1];
                
            //add new user to user list    
            Users user = new Users(key, connector);
            Users.addNewUser(key, user);
                
            //minus one connection <- fix client
            connection_count = Users.UserList.size()-1;
            Functions.printLog( 
                    "SERVER:[onOpen] INF - " +
                    "Client[connector:" + connector + 
                    "; ip:" + connector.getRemoteSocketAddress().getAddress().getHostAddress() +
                    "; user:" + Users.getUser(key).toString() +
                    "; key:" + key + ";] connected"
                    + "Connection count["+connection_count+"]" + connector.getLocalSocketAddress().getAddress().getHostAddress()
                    );
            DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, connection_count, 0);
	}

	/**
	 * Callback for string messages received from the remote host
	 * 
	 * @see #onMessage(WebSocket, ByteBuffer)
	 **/
	@Override
	public void onMessage(WebSocket connector, String message) {
            if( connector == null ) { 
                System.out.println("Server:[onMsg] ERR - connection is null");
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

            String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();            
                            connector.toString().split("@")[1];
            Users.removeUser(key);
            
            //minus one connection <- fix client
            connection_count = Users.UserList.size()-1;
            
            DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, connection_count, 0);
            
            Functions.printLog(
                            "SERVER:[onClose] INF - Connection " + 
                            key + " was ended[Reason: " +
                            reason +
                            "; code:"+code+"; remote:"+remote+"]. Total connection number:["+connection_count+"]" 
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
	public void onError( WebSocket connector, Exception ex ) {
            String err = ex.getMessage();
            Functions.printLog("SERVER:[onError] INF - " + err);
            
            if( connector == null ) { 
                Functions.printLog("Server:[onError] INF - Connection is null");
                //return ;
                
            }
            
            // some errors like port binding failed may not be assignable to a specific websocket
            String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();
                        connector.toString().split("@")[1];
            Users.removeUser(key);
            
            //minus one connection <- fix client
            connection_count = Users.UserList.size()-1;
            
            DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, connection_count, 0);
            
            Functions.printLog("SERVER:[onError] INF - Total connection number:["+connection_count+"]");
            
	}
        
        private void processMessage(WebSocket connector, String msg){
            String hash = null;
            String type = null;
            String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();
                        connector.toString().split("@")[1];
            
            Functions.printLog( 
                            "SERVER:[onMessage] INF - Client[key:" + key +
                            "; user:" + Users.getUser(key).toString() + 
                            "] says: " + msg 
                            );
            
            //price : bar_id : {1 or 0:boolean} : buys : sells
            //if handshake message from client -> add new user
            if (msg.contains("online:")) {
                type = msg.substring(7,11);
                hash = msg.substring(12);
                Users.assignHash(key, hash, type);
                
                //validate users
                DBMakeQuery.checkOnlineAccountHash(WSocketServer.jdbc, null);
                DBMakeQuery.checkOnlineAccountHash(WSocketServer.jdbc, WSocketServer.jdbc_demo_name);
                           
            //if hearthbeat message from client -> send pong
            }else if (msg.startsWith("ping")){
                
                if(msg.contains("WSSM")){
                    if(!Users.getUser(key).type.equalsIgnoreCase("sys")){
                        Users.getUser(key).type = "sys";
                    }
                    connector.send("pong");
                    
                }else{
                    connector.send("pong:"+DBMakeQuery.getServerTimeStatus(WSocketServer.jdbc, null));
                    
                }     
             
            //got answer from client
            }else if (msg.startsWith("pong")){
                //if got answer then reset dead ping counter
                Functions.printLog("SERVER:[onMessage] INF - Still alive");
                Users.getUser(key).dead = 0;
                
            //if price update message from fix -> broadcast new price to clients
            //receive in form [FIXPrice:price:bar_id:buys:sells]
            //send [price:bar_id:check:buys:sells]
            }else if (msg.contains("FIXPrice:")){
                if(!Users.getUser(key).type.equalsIgnoreCase("sys")){
                    Users.getUser(key).type = "sys";
                }
                String fixMsg = msg.substring(9);
                broadcastMessage(fixMsg, 0);
            }
        }
}