
package wsocketserver.users;

import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

import wsocketserver.WSocketServer;

import customjbdc.connection.DBConnection;
import customjbdc.queries.DBMakeQuery;

import functions.Functions;


public class Users{
    
    public static ConcurrentHashMap<String, Users> UserList = new ConcurrentHashMap();
    public String key;
    public String hash;
    public int check;
    public int id;
    public int     order_open;
    public long team_id;
    public String name;
    public String role;
    public String type;
    public String lang = "un";
    public WebSocket connector;
    private DBConnection jdbcDB = WSocketServer.jdbc; 
    public int dead = 0;
    public Boolean json_enabled = false;
    public Boolean response_encode = false;

   
    public Users(String key, WebSocket connector){    
        this.check      = 0;        
        this.key        = key;
        this.connector  = connector;
        this.id         = 0;
        this.order_open = 0;
        this.team_id    = -1;
        
        this.name = "Guest";
        this.role = "guest";
        this.type = "unknown";
        this.hash = "unknown";
    }
    
    public static void printUser(Users user){
         Functions.printLog("USERS:[printUser] id:"+user.id
                 +", role:"+user.role
                 +", name:"+user.name
                 +", lang:"+user.lang
                 +", type:"+user.type
                 +", check:"+user.check
                 +", dead:"+user.dead
                 +", order_open:"+user.order_open
                 +", encode:"+user.response_encode
                 +", team_id:"+user.team_id
                 +", json:"+user.json_enabled
                 +", hash:"+user.hash
                 +", key:"+user.key);
    }

    public static void addNewUser(String key, Users user){
        if (UserList.containsKey(key)) {
            Functions.printLog(
                    "USERS:[addNewUser] WRN - User with such key["+key+"] exist already"
                    );
            return ;
        }
    
        UserList.put(key, user);
        Functions.printLog(
                    "USERS:[addNewUser] INF - User added: [key:" + key 
                    + "; user:" + user.toString() + "; hash:" + user.hash +"]"
                    );
        
    }

    public static void removeUser(String key){
        if (!UserList.containsKey(key)){
            Functions.printLog("USERS:[removeUser] WRN - No such key: " + key);
            return ;
        } 
        
        Users user = (Users)UserList.get(key);

        if (user == null) {
            Functions.printLog("USERS:[removeUser] WRN - User with key: " + key + " is null");
            return;
        }

        DBMakeQuery.changeUserStatus(user.jdbcDB, 
                (
                    (user.type.equalsIgnoreCase("demo")==true)?(WSocketServer.jdbc_demo_name):(null)
                )
                , user.id, 0);
                  
        UserList.remove(key);
        
        Functions.printLog("USERS:[removeUser] INF - User deleted: " + key);

    }
    
   
    public static void assignHash(String key, String hash, String type){        
        Users user = null;
              
        Functions.printLog("USERS:[assignHash] INF - Key: "+key+"; hash: "+hash+"; Type: "+type);     

        //get this user
        user = (Users)UserList.get(key);

        //check for tthis user in remote DB
        if (user != null && user.checkUser(hash, type)) {
            UserList.replace(key, user);
            Functions.printLog("USERS:[assignHash] INF - user " + key + " updated with hash " + hash+"; "+UserList.get(key).lang);

        } else {
            Functions.printLog("USERS:[assignHash] WRN - hash not assigned");
        }
    }
    
    private boolean checkUser(String hash, String type_s){

        String result = DBMakeQuery.checkUserByHash(jdbcDB, 
                (
                    (type_s.equalsIgnoreCase("demo")==true)?(WSocketServer.jdbc_demo_name):(null)
                )
                , hash);
        
        if(result == null) {
            Functions.printLog(
                    "USERS:[checkUser:"+type_s+"] WRN - no such user in DB or hash outdated"
                    );
            return false;
        }        
        String[] resultSet = result.split("#");
        
        int id_s        = 0;        
        String name_s   = null;
        String role_s   = null;
        String ip_s     = null;
        int teamid_s    = 0;
        String lang_s   = null;
            
        try{
            id_s        = Integer.parseInt(resultSet[0]);        
            name_s      = (resultSet.length > 0)?(resultSet[1]):(null);
            role_s      = (resultSet.length > 1)?(resultSet[2]):(null);
            ip_s        = (resultSet.length > 2)?(resultSet[3]):(null);
            teamid_s    = (resultSet.length > 3)?(Integer.parseInt(resultSet[4])):(-1);
            lang_s      = (resultSet.length > 4)?(resultSet[5]):(null);
            
            Functions.printLog("USERS:[checkUser:"+type_s+"] INF - variables parsed("+id_s+","+name_s+","+role_s+","+ip_s+","+teamid_s+","+lang_s+")");
      
        }catch(Exception e){
            Functions.printLog("USERS:[checkUser:"+type_s+"] WRN - failed to parse variable. resultsetlength["+resultSet.length+"] E:"+ e.getLocalizedMessage());
      
        }
        
        String remoteIP = this.connector.getRemoteSocketAddress().getAddress().getHostAddress();
        
        if(!remoteIP.equals(ip_s)){
            Functions.printLog(
                    "USERS:[checkUser:"+type_s+"] WRN - wrong ip(remote:"+remoteIP+"; db:"+ip_s+")"
                    );
            //return false;
        }
        
        this.id         = id_s;
        this.hash       = hash;
        this.name       = name_s;
        this.role       = role_s;
        this.type       = type_s;
        this.team_id    = teamid_s;
        this.lang       = lang_s;
        
        //NB REVISIONED
        DBMakeQuery.changeUserStatus(this.jdbcDB, 
                (
                    (type_s.equalsIgnoreCase("demo")==true)?(WSocketServer.jdbc_demo_name):(null)
                )
                , id_s, 1);

        return true;
    }
  
    public static Users getUser(String key){
        if (!UserList.containsKey(key)) {
            return null;
        }
        
        Users user = (Users)UserList.get(key);
        return user;
    }
}