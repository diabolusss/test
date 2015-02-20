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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.util.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import wsocketserver.users.OrderData;


public class SimpleServer extends WebSocketServer{	
    static int connection_count = 0;
    String 
        JSON_MESSAGE_STATUS    = "{\"status\":\"",
        JSON_MESSAGE_END      = "]}",
        JSON_MESSAGE_MSG      = "\",\"msg\":["
        ;  
    
    static final int MAX_UNRESPONDED_PINGS = 6;
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
         * 
         * Sent in format:
         *      {"status":"none\logged", "msg":[
         *              {"type":"rate","msg":[{"price":1.111,"barid":11111,"check":1,"up":10,"down":12,"oopen":0}]},
         *              {"type":"sys", "msg":[{"tx":"some_text"},{"tx":"some_other_text"}]},
         *              {"type":"chat","msg":[{"from":"from","tx":"text","win":win,"ts":"timestamp"},....]},
         *              {"type":"pong","msg":0},
         *              {"type":"ping","msg":0},
         *              {"type":"team","msg":["arena":{sparring_data}, "team_data":{"id":id, "any_team_data"}]}
         *             ]}
	 */
	public void broadcastMessage( String message, int mode ) {    
            int unauthorized_users = 0;
            //System.out.println("DEBUG#in broadcastMessage:");
            
            //if no whom to sent message return
            if (Users.UserList.size() <= 0) {
                //System.out.println("DEBUG#in broadcastMessage:No users in list");
                return;
            }
            
            switch(mode){
                case 0: //fix price msg //received in form [price:bar_id:buys:sells]                    
                    //send in form [price:bar_id:check:buys:sells]  
                    String[] messageParts = message.split(":");     
                    
                    //System.out.println("DEBUG#in broadcastMessage:Parsing message");
                    
                    //for every user in list
                    for (String key : Users.UserList.keySet()) {                            
                        Users user = (Users)Users.UserList.get(key);                        
                        if(user == null) continue;
                        Users.printUser(user);
                        
                        Boolean json_override = user.json_enabled;

                        String check = null;
                        if(!user.hash.equals("unknown")){
                            if(user.type.equals("real")){
                                //NB REVISIONED
                                DBMakeQuery.checkUserOrderStatus(WSocketServer.jdbc, null, user.id); 

                            }else if(user.type.equals("demo")){
                                //NB REVISIONED
                                DBMakeQuery.checkUserOrderStatus(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                            }
                        }
                        //System.out.println("DEBUG#in broadcastMessage:Throws errors after here");

                        if(!user.type.equals("real") || !user.type.equals("demo") || !user.type.equals("sys")) unauthorized_users++;

                        String user_status = user.hash;

                        if(
                                //WSocketServer.json_response_enabled  || 
                                json_override){
                            String parsedRateMessage = "{\"type\":\"rate\"," +
                                    "\"price\":" + messageParts[0] + ","
                                    +"\"barid\":"+messageParts[1]
                                + (
                                    (user_status.equals("unknown")==true)?
                                        ("")
                                        :
                                        (
                                            ",\"check\":"    + user.check        + ","
                                            + "\"up\":"     + messageParts[2]   + ","
                                            + "\"down\":"   + messageParts[3]   + ","
                                            + "\"oopen\":"  + user.order_open
                                        )
                                  )
                                + "}";       

                            //System.out.println("DEBUG#in broadcastMessage:after parsedRateMessage");
                            //get any data if available
                            String parsedCheckMessage = ""; 

                            String result = null;                            
                            //get system messages
                            //{"type":"sys", "msg":[{"tx":"some_text"},{"tx":"some_other_text"}]},
                            if(user.type.equals("real")){
                                //NB REVISIONED
                                result =  DBMakeQuery.callGetSysMsgs(WSocketServer.jdbc, null, user.id) ;

                            }else if(user.type.equals("demo")){
                                //NB REVISIONED
                                result = DBMakeQuery.callGetSysMsgs(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                            }
                            if(result != null){
                                parsedCheckMessage = ",{\"type\":\"sys\", \"msg\":[" + result + "]}";
                            }
                            //System.out.println("DEBUG#in broadcastMessage:after callGetSysMsgs");

                            result = null;
                            //get chat messages
                            //{"type":"chat","msg":[{"from":"from","tx":"text","win":win,"ts":"timestamp"},....]},
                            if(user.type.equals("real")){
                                //NB REVISIONED
                                result =  DBMakeQuery.callGetChatMsgs(WSocketServer.jdbc, null, user.id) ;

                            }else if(user.type.equals("demo")){
                                //NB REVISIONED
                                result = DBMakeQuery.callGetChatMsgs(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                            }
                            if(result != null){
                                parsedCheckMessage += ",{\"type\":\"chat\", \"lang\":\""+user.lang+"\", \"msg\":[" + result + "]}";
                            }    
                            //System.out.println("DEBUG#in broadcastMessage:after callGetChatMsgs");

                            //get player team_id
                            if(user.type.equals("real")){
                                //NB REVISIONED
                                user.team_id = DBMakeQuery.getUserTeamID(WSocketServer.jdbc, null, user.id) ;

                            }else if(user.type.equals("demo")){
                                //NB REVISIONED
                                user.team_id = DBMakeQuery.getUserTeamID(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                            }
                            //System.out.println("DEBUG#in broadcastMessage:after getUserTeamID");

                            //get team trading data                                
                            //{"type":"team","msg":{"arena":{sparring_data}, "trading_data":[{"any_team_data"},...]}}
                            result = null;
                            if(user.team_id != -1) {
                                //System.out.println("DEBUG#in broadcastMessage: in team id");

                                ArrayList<OrderData> team_trading_data = null;
                                if(user.type.equals("real")){
                                    // NB REVISIONED
                                    team_trading_data = DBMakeQuery.getTeamTradingData(WSocketServer.jdbc, null, user.id) ;

                                }else if(user.type.equals("demo")){
                                    // NB REVISIONED
                                    team_trading_data = DBMakeQuery.getTeamTradingData(WSocketServer.jdbc, WSocketServer.jdbc_demo_name, user.id);
                                }
                                //System.out.println("DEBUG#in broadcastMessage:after getTeamTradingData");
                                
                                parsedCheckMessage += ",{\"type\":\"team\", \"msg\":{\"ts\":"+(new Date().getTime()/1000)+"";

                                if(team_trading_data != null || !team_trading_data.isEmpty()){
                                    //Functions.printLog("parsedCheckMessage: parsing team_trading_data");
                                    result = "";
                                    int i = 0;
                                    for(OrderData order : team_trading_data){
                                        //order.printOrderData();
                                        result += order.parse2JSON() + ((i++ > 0)?(","):(""));
                                    }
                                    if(!result.isEmpty()){
                                        Functions.printLog("parsedCheckMessage: parsed successfully team_trading_data");
                                        parsedCheckMessage += ",\"trading_data\":[" + result + "]";

                                        //parsedCheckMessage += ",{\"type\":\"team\", \"msg\":[{\"trading_data\":[" + result + "]}]}";
                                    }

                                }else{
                                    Functions.printLog("parsedCheckMessage: no team_trading_data");
                                }
                                parsedCheckMessage += "}}";

                            }

                            sendMessage(user, 
                                    JSON_MESSAGE_STATUS + 
                                    ((user_status.equals("unknown")==true)?("none"):("logged")) + 
                                    JSON_MESSAGE_MSG +
                                    parsedRateMessage + 
                                    parsedCheckMessage +    
                                    JSON_MESSAGE_END);
                            
                        }else{ //if json not enabled, send simple response
                            sendMessage(user,
                                    messageParts[0] + ":" + 
                                    messageParts[1] + (
                                        (user_status.equals("unknown")==true)?
                                            ("")
                                            :
                                            (
                                                ":"    + user.check
                                                + ":"  + messageParts[2] 
                                                + ":"  + messageParts[3] 
                                                + ":"  + user.order_open
                                            )
                                      )
                                    );  
                        }
                    }
                    
                    break;
                //END of case 0
                case 1://PING  
                    for (String key : Users.UserList.keySet()) {
                        if (!Users.UserList.get(key).type.equalsIgnoreCase("sys")) Users.UserList.get(key).dead++;
                        Users user = (Users)Users.UserList.get(key);
                        if(user == null) continue;

                        Boolean json_override = user.json_enabled;
                        
                        if(!user.type.equals("real") || !user.type.equals("demo") || !user.type.equals("sys")) unauthorized_users++;

                        String user_status = user.hash;

                        if(
                               //WSocketServer.json_response_enabled  || 
                                json_override){
                            sendMessage(user,
                                    JSON_MESSAGE_STATUS + 
                                    ((user_status.equals("unknown")==true)?("none"):("logged")) +
                                    JSON_MESSAGE_MSG +
                                    "{\"type\":\"" + message + "\", \"msg\":" + user.dead + ", \"ts\":"+(new Date().getTime()/1000) + "}" 
                                    + JSON_MESSAGE_END
                                    );
                        }else{
                            sendMessage(user,message+":"+user.dead+":"+(new Date().getTime()/1000));
                        }

                    }
                     
                    break;
                //END of case 1
                    
            }//END of switch          
            
            //NB REVISIONED
            DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, (unauthorized_users>0)?(unauthorized_users-1):(0), 1);
	}
        
        public void sendMessage( Users user, String message ) {
            //Nested locks can result in deadlocks quite easily if one is using wait/notify. 
            synchronized (user) {                    
                    if(user.connector != null && user.connector.isOpen() && user.dead < MAX_UNRESPONDED_PINGS){  
                        
                        Functions.printLog(
                        "SERVER:[sendMessage] INF - Message sent: [user:" + user.id + 
                        "; hash:" + user.hash +
                        "; ip:" + user.connector.getRemoteSocketAddress().getAddress().getHostAddress() +
                        "; key:" + user.key + 
                        "; MSG:" + message + ":MSG" +
                        "; user.dead="+user.dead + 
                        "; type: " + user.type
                        );    
                        if(user.response_encode){
                            message = new String(Base64.encodeBytesToBytes(message.getBytes()));
                        }
                        user.connector.send( message );
                   
                    }else{
                        Functions.printLog(
                        "SERVER:[sendMessage] WRN - Bad or ghost connection: [user:" + user.id + 
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
                            connector.close(counter, "Too much sys connections!");
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
            connection_count = (Users.UserList.size() > 0)?(Users.UserList.size()-1):(0);
            Functions.printLog( 
                    "SERVER:[onOpen] INF - " +
                    "Client[connector:" + connector + 
                    "; ip:" + connector.getRemoteSocketAddress().getAddress().getHostAddress() +
                    "; user:" + Users.getUser(key).toString() +
                    "; key:" + key + ";] connected"
                    + "Connection count["+connection_count+"]" + connector.getLocalSocketAddress().getAddress().getHostAddress()
                    );
            //NB REVISIONED
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
                return ;
            }

            String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();            
                            connector.toString().split("@")[1];
            Users.removeUser(key);
            
            //minus one connection <- fix client
            connection_count = (Users.UserList.size() > 0)?(Users.UserList.size()-1):(0);
            
            //NB REVISIONED
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
                return ;
                
            }
            
            // some errors like port binding failed may not be assignable to a specific websocket
            // String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();
            //            connector.toString().split("@")[1];
            //Users.removeUser(key);
            
            //minus one connection <- fix client
            //connection_count = Users.UserList.size()-1;
            
            //NB REVISIONED
            //DBMakeQuery.setServerUserCount(WSocketServer.jdbc, null, WSocketServer.server_host, connection_count, 0);
            
            //Functions.printLog("SERVER:[onError] INF - Total connection number:["+connection_count+"]");
            
	}
        
        private void processMessage(WebSocket connector, String msg){
            //String hash = null;
            //String type = null;
            String key = //connector.getRemoteSocketAddress().getAddress().getHostAddress();
                        connector.toString().split("@")[1];
            Users user = (Users)Users.UserList.get(key);
            
            Boolean response_encode = user.response_encode;
            Boolean json_override = user.json_enabled;
            
            //if response_encode is true, than first not encoded message was received already
            if(user.response_encode){
                try {
                    msg = new String(Base64.decode(msg.getBytes() ));                    
                } catch (IOException ex) {
                    Functions.printLog("SERVER:[onMessage] WRN - Failed to decode incoming message. E:"+ex.getLocalizedMessage());
                    return;
                }
            }
            
            Functions.printLog( 
                            "SERVER:[onMessage] INF - Client[key:" + key +
                            "; user:" + user.toString() + 
                            "] says: " + msg 
                            );
            
            //parse received message to json
            JSONObject json = null;
            try {
                json = (JSONObject) new JSONParser().parse(msg);

            } catch (ParseException ex) {
                Functions.printLog("SERVER:[onMessage] WRN - Failed to parse received JSON message["+msg+"]. E:"+ex.getLocalizedMessage());
                return;
            };

            String msg_type = (String) json.get("type");
            JSONObject msg_value = (JSONObject) json.get("msg");
            Functions.printLog("SERVER:[onMessage] Client json message type:"+msg_type.toString());        

            ////////////////////////////////////////////////
            /////////// START OF ///// JSON DATA PARSE /////
            ////////////////////////////////////////////////
            //if received //////pong message///////
            //example:
            //  "{\"type\":\"pong\",
            //      \"msg\":{}
            //   }"
            if(msg_type.equalsIgnoreCase("pong")){
                //if got answer then reset dead ping counter
                Functions.printLog("SERVER:[onMessage] INF - Client still alive");
                Users.getUser(key).dead = 0;

            }//END OF PARSE //////pong message///////
            //if received //////ping message///////
            //example:
            //  "{\"type\":\"ping\",
            //      \"msg\":{\"from\":\"WWSM\"}
            //   }"
            else if(msg_type.equalsIgnoreCase("ping")){
                String response = null;
                String date = DBMakeQuery.getServerTimeStatus(WSocketServer.jdbc, null);

                for (Object jsonEntryObj : msg_value.entrySet())        {
                    Map.Entry jsonEntry = (Map.Entry) jsonEntryObj;
                    String jsonEntryKey = (String) jsonEntry.getKey();
                    Object jsonEntryValue = jsonEntry.getValue();

                    response = "";

                    //ping from manager
                    if(jsonEntryKey.equalsIgnoreCase("from")){
                        String from = jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tping from:"+jsonEntryValue.toString());

                        if(from.equalsIgnoreCase("WWSM")){
                            //mark client as system if its not already
                            if(!user.type.equalsIgnoreCase("sys")){
                                Users.getUser(key).type = "sys";
                            }

                            //send simple pong
                            response = "pong";

                        }//ping from client
                        else if(from.equalsIgnoreCase("client")){                    
                            if(json_override){
                                response = 
                                        JSON_MESSAGE_STATUS + 
                                        ((user.hash.equals("unknown")==true)?("none"):("logged")) +
                                        JSON_MESSAGE_MSG +
                                        "{\"type\":\"pong\", \"msg\":" + ((date == null)?(0):(date))  + "}" 
                                        + JSON_MESSAGE_END;

                            }else{
                                response = "pong:" + ((date == null)?(0):(date));

                            }
                        }else{
                            Functions.printLog("SERVER:[onMessage] \tunknown ping source["+jsonEntryKey+"] value:"+jsonEntryValue.toString());
                            return;      
                        }//endif
                    }//endif
                }//end of for
                if(user.response_encode){
                    response = new String(Base64.encodeBytesToBytes(response.getBytes()));
                }
                connector.send(response );   

            }//END OF PARSE //////ping message///////        
            //if received //////dukasrate message///////
            //example:
            //  "{\"type\":\"dukasrate\",
            //      \"msg\":{\"barid\":1234567,\"buysell\":\"1:2\",\"buys\":1,\"sells\":2, \"price\":1.23334}
            //   }"
            else if(msg_type.equalsIgnoreCase("dukasrate")){
                //mark client as system if its not already
                if(!user.type.equalsIgnoreCase("sys")){
                    Users.getUser(key).type = "sys";
                }
                String price = "1.233334";
                String barid = "1234567";
                String buys = "1";
                String sells = "2";

                for (Object jsonEntryObj : msg_value.entrySet())        {
                    Map.Entry jsonEntry = (Map.Entry) jsonEntryObj;
                    String jsonEntryKey = (String) jsonEntry.getKey();
                    Object jsonEntryValue = jsonEntry.getValue();

                    //barid value
                    if(jsonEntryKey.equalsIgnoreCase("barid")){                    
                        barid = jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tbarid value:"+barid);

                    }//order buys:sells together
                    else if(jsonEntryKey.equalsIgnoreCase("buysell")){
                        buys=jsonEntryValue.toString().split(":")[0];
                        sells=jsonEntryValue.toString().split(":")[1];
                        Functions.printLog("SERVER:[onMessage] \tbuys value:"+buys+", sells value:"+sells);

                    }//order buys count
                    else if(jsonEntryKey.equalsIgnoreCase("buys")){
                        buys=jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tbuys value:"+buys);

                    }//order sells count
                    else if(jsonEntryKey.equalsIgnoreCase("sells")){
                        sells=jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tsells value:"+sells);

                    }//price
                    //can be transmuted to
                    // rate:{price:1.111, currency:usd, ...}
                    else if(jsonEntryKey.equalsIgnoreCase("price")){ 
                        price=jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tprice value:"+price);

                        /* 
                         * Uncomment this only if use rate format
                        //parse each variable separately
                        JSONObject settings = (JSONObject) jsonEntryValue;
                        for (Object settingsEntryObj : settings.entrySet()) {                    
                              Entry settingsEntry = (Entry) settingsEntryObj;
                              String settingsEntryKey = (String) settingsEntry.getKey();
                              Object settingsEntryValue = settingsEntry.getValue();

                              if(settingsEntryKey.equalsIgnoreCase("price")){
                                  price=jsonEntryValue.toString();
                                  System.out.println("\tprice value:"+price);
                              }else if(settingsEntryKey.equalsIgnoreCase("currency")){
                                  currency=jsonEntryValue.toString();
                                  System.out.println("\tcurrency value:"+currency);                              
                              }//endif
                        }//end of for 
                        */
                    }//endif
                }//end of for

                //broadcast dukasrate to all users
                broadcastMessage(price+":"+barid+":"+buys+":"+sells, 0);
            }//END OF PARSE //////dukasrate message///////
            //if received //////hash message///////
            //example: 
            //  "{\"type\":\"hash\",
            //      \"msg\":{\"hash\":\"H723&*H(4c\",\"online\":\"demo\",
            //          \"settings\":{\"json\":\"true\",\"encode\":\"true\",\"int\":6}
            //       }
            //   }"                
            else if(msg_type.equalsIgnoreCase("hash")){
                String type                     = "demo";
                String hash                     = "unknown";
                String setting_json_enabled     = "false";
                String setting_encoding_enabled = "false";

                for (Object jsonEntryObj : msg_value.entrySet())        {
                    Map.Entry jsonEntry = (Map.Entry) jsonEntryObj;
                    String jsonEntryKey = (String) jsonEntry.getKey();
                    Object jsonEntryValue = jsonEntry.getValue();

                    //found hash value
                    if(jsonEntryKey.equalsIgnoreCase("hash")){                    
                        hash = jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tUser hash value:"+hash);

                    }//user type
                    else if(jsonEntryKey.equalsIgnoreCase("online")){
                        type=jsonEntryValue.toString();
                        Functions.printLog("SERVER:[onMessage] \tUser type value:"+type);

                    }//handle client data processing settings
                    else if(jsonEntryKey.equalsIgnoreCase("settings")){                    
                        //System.out.println("Client settings value:"+jsonEntryValue.toString());

                        //parse each variable separately
                        JSONObject settings = (JSONObject) jsonEntryValue;
                        for (Object settingsEntryObj : settings.entrySet()) {                    
                              Map.Entry settingsEntry = (Map.Entry) settingsEntryObj;
                              String settingsEntryKey = (String) settingsEntry.getKey();
                              Object settingsEntryValue = settingsEntry.getValue();

                              if(settingsEntryKey.equalsIgnoreCase("json")){
                                  setting_json_enabled=settingsEntryValue.toString();
                                  Functions.printLog("SERVER:[onMessage] \t\tjson setting value:"+setting_json_enabled);

                              }else if(settingsEntryKey.equalsIgnoreCase("encode")){
                                  setting_encoding_enabled=settingsEntryValue.toString();
                                  Functions.printLog("SERVER:[onMessage] \t\tencode setting value:"+setting_encoding_enabled);

                              }else{
                                  Functions.printLog("SERVER:[onMessage] \t\tunknown setting["+settingsEntryKey+"] value:"+settingsEntryValue.toString());
                              }//endif
                        }//end of for
                    }//endif
                }//end of for

                //validate user
                Users.assignHash(key, hash, type);

                //set check=1 for online accounts    
                //NB REVISIONED
                DBMakeQuery.checkOnlineAccountsHash(WSocketServer.jdbc, null);
                DBMakeQuery.checkOnlineAccountsHash(WSocketServer.jdbc, WSocketServer.jdbc_demo_name); 

                //specify and save client specific settings
                //TODO catch parse errors
                user.json_enabled = Boolean.parseBoolean(setting_json_enabled);
                user.response_encode = Boolean.parseBoolean(setting_encoding_enabled);
                Users.UserList.replace(key, user);

            }//END OF PARSE //////hash message///////
            else{
                Functions.printLog("SERVER:[onMessage] \tNot implemented message parsing yet");
            }/////////////////////////////////////////////
            /////////// END OF ///// JSON DATA PARSE /////
            //////////////////////////////////////////////
        }
}