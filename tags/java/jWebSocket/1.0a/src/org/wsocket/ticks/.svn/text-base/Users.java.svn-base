/*     */ package org.wsocket.ticks;
/*     */ 
/*     */ import java.sql.Connection;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.SQLException;
/*     */ import java.sql.Statement;
/*     */ import java.util.concurrent.ConcurrentHashMap;
import org.custom.Functions;
          import org.custom.RunServer;
          import org.java_websocket.WebSocket;
/*     */ import org.wsocket.jdbc.DBConnection;
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
/*     */   public static void addNewUser(String key, Users user)
/*     */   {
/*  86 */     if (!UserList.containsKey(key)) {
/*  87 */       UserList.put(key, user);
/*  88 */       Functions.printLog(
                    "USERS:[addNewUser] INF - User added: [key:" + key 
                    + "; user:" + user.toString() + "; hash:" + user.hash +"]"
                    );
/*     */     }else{
                Functions.printLog(
                    "USERS:[addNewUser] ERR - User with such key exist already"
                    );
              }
/*     */   }
/*     */ 
/*     */   public static void removeUser(String key){
/*  97 */     if (UserList.containsKey(key))
/*     */       try {
/*  99 */         Users user = (Users)UserList.get(key);
/* 100 */         user.setUserOffline(user.id);
/* 101 */         UserList.remove(key);

/* 102 */         Functions.printLog("USERS:[removeUser] INF - User deleted: " + key);

/*     */       } catch (SQLException e) {
                  Functions.printLog("USERS:[removeUser] ERR - " + e.getLocalizedMessage());
/*     */       }
/*     */   }
/*     */ 
/*     */   public static void assignHash(String key, String hash, String type){
              Functions.printLog("USERS:[assignHash] INF - Key: "+key+"; hash: "+hash+"; Type: "+type); 
/* 117 */     Users user = null;
                
              //if user wih such key exist
/* 118 */     if (UserList.containsKey(key)){
    
/* 120 */       for (String key_id : UserList.keySet()) {
    
/* 121 */         user = (Users)UserList.get(key_id);

                  //if eist user wih such hash 
/* 122 */         if (user != null && user.hash.equals(hash)) {
/* 123 */           System.out.println("USERS:[assignHash] Replace hash on key: " + key_id + " : " + user.hash);
/* 124 */           user.hash = "unknown";
/* 125 */           UserList.replace(key_id, user);
/*     */         }

/*     */       }///end of for

/*     */       try {
    
                  //get this user
/* 130 */         user = (Users)UserList.get(key);
                    
                  //check for tthis user in remote DB
/* 131 */         if (user != null && user.checkUser(hash, type)) {
/* 132 */           UserList.replace(key, user);
/* 133 */           System.out.println("USERS:[assignHash] user " + key + " updated with hash " + hash);

/*     */         } else {
/* 135 */           System.out.println("USERS:[assignHash] hash not assigned - user not found...");
/*     */         }

/*     */       } catch (SQLException ex) {
/* 138 */         ex.printStackTrace(System.out);
                  System.out.println("USERS:[assignHash] ERROR " + ex.getLocalizedMessage());
                  
/*     */       }
/*     */     }else{
                System.out.println("USERS:[assignHash] No user with such key[" + key + "]");
    
              }

/*     */   }
/*     */ 
/*     */   private boolean checkUser(String hash, String type_s) throws SQLException {
/* 144 */     boolean valid = false;
/* 145 */     int id_s = 0;
/* 146 */     Connection con = this.jdbcDB.getConnection();
/* 147 */     String name_s = null;
/* 148 */     String role_s = null;
/* 149 */     String ip_s = null;
/* 150 */     String query = null;

/* 151 */     if (con != null){
    
/*     */       try {
/* 154 */         Statement ps = con.createStatement();

/* 155 */         if (type_s.equals("real")) {
/* 156 */           query = "SELECT accounts.`hash`, user.`id`, user.`name`, user.`role`, user.`ip` FROM  `user` INNER JOIN `accounts` ON user.`id` = accounts.`id` WHERE  accounts.`hash` = '" + hash + "'";
/*     */ 
/* 161 */           ps.executeQuery(query);
/*     */         } else {
/* 163 */           query = "SELECT a.`hash`, u.`id`, u.`name`, u.`role`, u.`ip` FROM  `"+RunServer.jdbc_demo_name+"`.`user` AS u INNER JOIN `"+RunServer.jdbc_demo_name+"`.`accounts` AS a ON u.`id` = a.`id` WHERE a.`hash` = '" + hash + "'";
/*     */ 
/* 169 */           ps.executeQuery(query);
/*     */         }

/* 171 */         ResultSet rs = ps.getResultSet();

/* 172 */         if (rs.next()) {
/* 173 */           id_s = rs.getInt("id");
/* 174 */           name_s = rs.getString("name");
/* 175 */           role_s = rs.getString("role");
/* 176 */           ip_s = rs.getString("ip");
                    
                    System.out.println(
                            "USERS:[checkUser] ResultSet id_s = " + id_s +
                            "; name_s = " + name_s + 
                            "; role_s = " + role_s + 
                            "; ip_s = " + ip_s
                            
                            );
/*     */         }else{
                    System.out.println(
                            "USERS: [checkUser] query["+
                            query +
                            "] - No such entry in remote DB");
    
                  }
/*     */ 
/* 179 */         rs.close();
/* 180 */         ps.close();
/*     */       } catch (SQLException e) {
/* 182 */         System.out.println("USERS:[checkUser] "+e);
/* 183 */         System.out.println("USERS:[checkUser] ERROR: " + query);
/* 184 */         this.jdbcDB.release();
/*     */       }
/* 186 */       this.jdbcDB.freeConnection(con);
/*     */     } else {
/* 188 */       throw new SQLException("Error in check user: connection is null...");
/*     */     }
/*     */ 
/* 191 */     String remoteIP = this.connector.getRemoteSocketAddress().getAddress().getHostAddress();
/* 192 */     if ((id_s != 0) && (remoteIP.equals(ip_s))) {
/* 193 */       valid = true;
/*     */ 
/* 196 */       this.hash = hash;
/* 197 */       this.id = id_s;
/* 198 */       this.name = name_s;
/* 199 */       this.role = role_s;
/* 200 */       this.type = type_s;
/*     */ 
/* 202 */       setUserOnline(id_s);
/*     */     }
/* 204 */     return valid;
/*     */   }
/*     */ 
/*     */   private void setUserOnline(int id) throws SQLException {
/* 208 */     Connection con = this.jdbcDB.getConnection();
/* 209 */     String query = null;
/* 210 */     if (con != null)
/*     */     {
/*     */       try {
/* 213 */         Statement st = con.createStatement();
/* 214 */         query = "UPDATE `accounts` SET `online` = 1 WHERE `id` = " + id;
/* 215 */         st.executeUpdate(query);
/* 216 */         st.close();
/*     */       } catch (SQLException e) {
/* 218 */         System.out.println("USERS:[setUserOnline] "+e);
/* 219 */         System.out.println("USERS:[setUserOnline] ERROR: " + query);
/* 220 */         this.jdbcDB.release();
/*     */       }
/* 222 */       this.jdbcDB.freeConnection(con);
/*     */     } else {
/* 224 */       throw new SQLException("Error in setOnline user: connection is null...");
/*     */     }
/*     */   }
/*     */ 
/*     */   private void setUserOffline(int id) throws SQLException
/*     */   {
/* 230 */     Connection con = this.jdbcDB.getConnection();
/* 231 */     String query = null;
/* 232 */     if (con != null)
/*     */     {
/*     */       try {
/* 235 */         Statement st = con.createStatement();
/* 236 */         query = "UPDATE `accounts` SET `online` = 0 WHERE `id` = " + id;
/* 237 */         st.executeUpdate(query);
/* 238 */         st.close();
/*     */       } catch (SQLException e) {
/* 240 */         System.out.println("USERS:[setUserOffline] "+e);
/* 241 */         System.out.println("USERS:[setUserOffline] ERROR: " + query);
/* 242 */         this.jdbcDB.release();
/*     */       }
/* 244 */       this.jdbcDB.freeConnection(con);
/*     */     } else {
/* 246 */       throw new SQLException("Error in setOffline user: connection is null...");
/*     */     }
/*     */   }
/*     */ 
/*     */   public static Users getUser(String key)
/*     */   {
/* 257 */     if (UserList.containsKey(key)) {
/* 258 */       Users user = (Users)UserList.get(key);
/* 259 */       return user;
/*     */     }
/* 261 */     return null;
/*     */   }
/*     */ }