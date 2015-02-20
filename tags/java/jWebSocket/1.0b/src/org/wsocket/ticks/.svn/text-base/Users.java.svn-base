/*     */ package org.wsocket.ticks;
/*     */ 
/*     */ import java.util.concurrent.ConcurrentHashMap;
import org.custom.Functions;
          import org.custom.RunServer;
          import org.java_websocket.WebSocket;
/*     */ import org.wsocket.jdbc.DBConnection;
import org.wsocket.jdbc.DBMakeQuery;
/*     */ 
/*     */ public class Users{
/*  25 */   public static ConcurrentHashMap<String, Users> UserList = new ConcurrentHashMap();
/*     */   public String key;
/*     */   public String hash;
/*     */   public int check;
/*     */   public int id;
/*     */   public String name;
/*     */   public String role;
/*     */   public String type;
/*     */   public WebSocket connector;
/*  60 */   private DBConnection jdbcDB = RunServer.jdbc;
             
/*     */ 
/*     */   public Users(String key, WebSocket connector){
    
/*  70 */     this.check = 0;
/*  71 */     this.hash = "unknown";
/*  72 */     this.key = key;
/*  73 */     this.connector = connector;
/*  74 */     this.id = 0;
/*  75 */     this.name = "Guest";
/*  76 */     this.role = "guest";
/*  77 */     this.type = "demo";
/*     */   }
/*     */ 
/*     */   

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
            return ;
        } 
        
        Users user = (Users)UserList.get(key);

        if (user == null) {
            Functions.printLog("USERS:[removeUser] WRN - User with key: " + key + " is null");
            return;
        }

        DBMakeQuery.changeUserStatus(user.jdbcDB, 
                (
                    (user.type.equalsIgnoreCase("demo")==true)?(RunServer.jdbc_demo_name):(null)
                )
                , user.id, 0);
                  
        UserList.remove(key);
        
        Functions.printLog("USERS:[removeUser] INF - User deleted: " + key);

    }
    
/*     */   
    public static void assignHash(String key, String hash, String type){        
        Users user = null;
              
        Functions.printLog("USERS:[assignHash] INF - Key: "+key+"; hash: "+hash+"; Type: "+type);     

        //get this user
        user = (Users)UserList.get(key);

        //check for tthis user in remote DB
        if (user != null && user.checkUser(hash, type)) {
            UserList.replace(key, user);
            Functions.printLog("USERS:[assignHash] INF - user " + key + " updated with hash " + hash);

        } else {
            Functions.printLog("USERS:[assignHash] WRN - hash not assigned");
        }
    }
    
    private boolean checkUser(String hash, String type_s){

        String result = DBMakeQuery.checkUserByHash(jdbcDB, 
                (
                    (type_s.equalsIgnoreCase("demo")==true)?(RunServer.jdbc_demo_name):(null)
                )
                , hash);
        
        if(result == null) {
            Functions.printLog(
                    "USERS:[checkUser:"+type_s+"] WRN - no such user in DB"
                    );
            return false;
        }        
        String[] resultSet = result.split("#");
        
        int id_s        = Integer.parseInt(resultSet[0]);        
        String name_s   = resultSet[1];
        String role_s   = resultSet[2];
        String ip_s     = resultSet[3];
        
        String remoteIP = this.connector.getRemoteSocketAddress().getAddress().getHostAddress();
        
        if(!remoteIP.equals(ip_s)){
            Functions.printLog(
                    "USERS:[checkUser:"+type_s+"] WRN - wrong ip(remote:"+remoteIP+"; db:"+ip_s+")"
                    );
            return false;
        }
        
        this.id = id_s;
        this.hash = hash;
        this.name = name_s;
        this.role = role_s;
        this.type = type_s;
        DBMakeQuery.changeUserStatus(this.jdbcDB, 
                (
                    (type_s.equalsIgnoreCase("demo")==true)?(RunServer.jdbc_demo_name):(null)
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