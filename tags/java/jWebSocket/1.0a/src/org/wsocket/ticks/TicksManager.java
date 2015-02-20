/*     */ package org.wsocket.ticks;
/*     */ 
import java.io.IOException;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
/*     */ import java.sql.CallableStatement;
/*     */ import java.sql.Connection;
/*     */ import java.sql.PreparedStatement;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.SQLException;
/*     */ import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Thread.State;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
        import org.custom.Functions;
        import org.custom.RunServer;
import static org.custom.RunServer.server;
import static org.custom.RunServer.tm;
        import org.custom.TestServer;
import org.http.Emailer;
        import org.http.HTTPURLConnection;
import static org.http.HTTPURLConnection.communicate2RandomOrg;
import static org.http.HTTPURLConnection.generateRandomNumberListInt;
import static org.http.HTTPURLConnection.generateRandomNumberListLong;
import static org.http.HTTPURLConnection.generatedNums;
import static org.http.HTTPURLConnection.receivedSeedNums;
import org.http.SimpleExpBackoff;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/*     */ import org.wsocket.jdbc.DBConnection;
          import quickfix.SessionID;
          
/*     */ public class TicksManager extends Thread{
/*     */ 
            public static /*final*/ int PRICE_PRECISION;// = 5; //digits after point: for o/p
            
/*  26 */   public static double real_price = 0.0D;
/*  27 */   public static double last_db_price = 0.0D;
/*  28 */   public static double price = 0.0D;
/*  29 */   public static double old_price = 0.0D;
/*  30 */   public static double mix_price = 0.0D;
            public static String totalOrders = null;
/*  33 */   public static int currBarId = 0;
/*  34 */   public static int position_amount = 0;
/*  35 */   public static int broker_status = 1;
/*  36 */   public static int trading_status = 1;
/*  37 */   public static String mode = "real";
            public SessionID tradeSession = null;    
/*     */ 
/*  39 */   private static DBConnection jdbc_connection = RunServer.jdbc;
            private static TestServer wsocketServer     = RunServer.server;
            
            Timer timer = new Timer();
            //private long mix_delay;// = 1*400;//how often to make new mix price
            
            public static long auto_delay;//how often to generate tick
            
            public boolean autoRate; //if auto enabled then tm.newtick will be called at certain circumstances
            
            //NEW connectorr to external random num generator server  (sleeptime, maxattempt, numListSize, min, max)
            //public static HTTPURLConnection randomOrg;            
            private static String dtFormat;
/*     */ 
/*     */   public TicksManager(Properties properties){
                System.out.println("TM:[INIT] Parsing variables");
                //init variables                
                //this.mix_delay = Long.parseLong(properties.getProperty("mix_price_delay"));
                
                this.PRICE_PRECISION = Integer.parseInt(properties.getProperty("price_output_precision"));                 
                //this.randomOrg = new HTTPURLConnection(properties);
                
                this.autoRate = Boolean.parseBoolean(properties.getProperty("auto_enabled"));
                this.auto_delay = Long.parseLong(properties.getProperty("mix_price_delay"));
                
                this.mode = properties.getProperty("tick_manager_mode");
                
                this.dtFormat = properties.getProperty("date_time_format");
                
                //connection checker; server saver
                timer.scheduleAtFixedRate(new Delayer(), 0, 1000);
            }

            /**
             * Sets received from broker new price
             * 
             * @param new_p 
             */
/*     */   public static void newTick(double new_p){              
/*  56 */     TicksManager.real_price = new_p;
              Functions.printLog("TM:[newTick] INF - New price["+new_p+"]");
/*     */   }
            
            /**
             * Generate new mix price based on facts
             * @param curr_price
             *          current price
             * @param real_price 
             *          real price from broker
             * @param point
             *          divider
             * @return mixPrice
             *          newly generated price with offset
             */ 
            private double mixPrice_old(double curr_price, double real_price, int point){
                int 
                    count = 0, 
                    offset = 0, 
                    diff = 0,
                    coeff = 0
                    ; 

/*  95 */           count = Functions.generateRandom(1000);
/*  96 */           offset = 0;
/*     */ 
/*  98 */           diff = new BigDecimal(Double.valueOf((curr_price - real_price) * 3.0D * point).doubleValue()).setScale(0, RoundingMode.HALF_DOWN).intValue();
/* 100 */           
                    if (diff == 0) {
/* 101 */             if (Functions.generateRandom(1) == 0) diff = -1;
/*     */             else diff = 1;
/*     */           }
/*     */ 
                    
/* 108 */           if (diff > 0){
/* 112 */             if (count < 60) offset = -1;
/* 113 */             if (count < 10) offset = -2;
/* 114 */             if (count < 1) offset = -3; 

/*     */           }else { 
                      diff = Math.abs(diff);
/* 117 */             if (count > 940) offset = 1;
/* 118 */             if (count > 990) offset = 2;
/* 119 */             if (count > 999) offset = 3;
/*     */ 
/*     */           }
/* 125 */           double offset_point = (double) offset / point;
                    curr_price += offset_point;
                    
                    //get rid of tail
                    curr_price = new BigDecimal(Double.valueOf(curr_price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
/*     */           //Functions.printLog("New mix price["+curr_price+"];"+TicksManager.activeCount()+";"  );      
              return curr_price;  
            }            

/*     */   @Override
            public void run() {
/*  61 */     Connection con = null;
/*     */ 
/*  65 */     int bar_id = 0;
/*  66 */     int count = 0;
/*  70 */     int order_id = 0;
/*  71 */     int i = 0;
              double amount = 0.0D;
/*  79 */     int clordid = 0;
/*  80 */     String query = null;

              totalOrders = getTotalOrders();
              
/*  75 */     currBarId = getCurrBarId();
/*  76 */     last_db_price = getLastDBPrice();              

/*     */     while (true){
                //In timer now
                //{#REGION check wsocket status}
                //checkSocketStatus();
                //{#ENDREGION check wsocket status}
                
                //System.out.format("TM:[Run] Getting new mix_price Beep! ");
                if ("mix".equals(TicksManager.mode)) {                    
                    //init value
                    if (mix_price < 0.1D) {
                        TicksManager.mix_price = TicksManager.real_price;
                    }

                    //get new mixPrice and round it
                    TicksManager.mix_price = new BigDecimal(Double.valueOf(
                               // mixPrice_new(TicksManager.mix_price, TicksManager.real_price )
                            mixPrice_old(TicksManager.mix_price, TicksManager.real_price, 10000)
                            ).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();

                 //not mix
                 }else {
                    TicksManager.mix_price = TicksManager.real_price;
                 }
                    
                 //is possible only if not mix
/* 134 */         if (TicksManager.mix_price > 0.1D) {
/* 135 */           TicksManager.price = TicksManager.mix_price;
                    //System.out.println("TicksManager.mix_price > 0.1D TicksManager.old_price != TicksManager.price="+(TicksManager.old_price != TicksManager.price));
/*     */         }
/*     */ 
/* 139 */         if (
                      (TicksManager.old_price != TicksManager.price) && 
                      (TicksManager.price > 0.1D) //FIXME already checked in previous statement
                     ){
/* 141 */           con = jdbc_connection.getConnection();

/* 142 */           if (con != null){
/*     */             try{
/* 147 */               query = "{call tick(?)}";
/* 148 */               CallableStatement pc = con.prepareCall(query);

/* 149 */               pc.setDouble(1, TicksManager.price);
/* 150 */               pc.executeUpdate();

/* 151 */               ResultSet rs = pc.getResultSet();

/* 152 */               while (rs.next()) {
/* 153 */                 String[] res = rs.getString("vResult").split(":");
/* 154 */                 if (res[0].equals("OK"))
/* 155 */                   bar_id = Integer.parseInt(res[1]);
/*     */                 else {
/* 157 */                   Functions.printLog("TM:[run] WRN - Error in call ticks: " + res[1]);
/*     */                 }
/*     */               }
/* 160 */               rs.close();
/* 161 */               pc.close();
/*     */ 
/* 164 */               Statement st = con.createStatement();
/* 165 */               query = "SELECT `id` FROM `orders` WHERE `status`='msg'";

/* 166 */               st.executeQuery(query);
                 
/* 167 */               rs = st.getResultSet();

/* 168 */               while (rs.next()) {
    
/* 169 */                 order_id = rs.getInt("id");

/*     */                 try {
/* 171 */                   query = "{call close_order_by_id(?)}";
/* 172 */                   pc = con.prepareCall(query);

/* 173 */                   pc.setInt(1, order_id);
/* 174 */                   pc.executeUpdate();

/* 175 */                   ResultSet o_rs = pc.getResultSet();

/* 176 */                   if (o_rs.next()) {
/* 177 */                     String[] res = o_rs.getString("vResult").split(":");
/* 178 */                     if (!res[0].equals("OK")) {
/* 179 */                       Functions.printLog("TM:[run] WRN - Error in call close_order_by_id: " + res[1]);
/*     */                     }
/*     */                   }

/* 182 */                   o_rs.close();
/* 183 */                   pc.close();
/*     */                 } catch (SQLException e) {
/* 185 */                   Functions.printLog(
                                "TM:[run] ERR - " + e.toString()
                                +"; " + query + " : " + order_id
                                );
/*     */                 }
/*     */               }//end of {while (rs.next())}
/*     */ 
/* 190 */               rs.close();
/* 191 */               st.close();
/*     */ 
/* 195 */               query = "{call `"+RunServer.jdbc_demo_name+"`.tick(?)}";
/* 196 */               pc = con.prepareCall(query);

/* 197 */               pc.setDouble(1, TicksManager.price);
/* 198 */               pc.executeUpdate();

/* 199 */               rs = pc.getResultSet();

/* 200 */               while (rs.next()) {
/* 201 */                 String[] res = rs.getString("vResult").split(":");

/* 202 */                 if (res[0].equals("OK"))
/* 203 */                   bar_id = Integer.parseInt(res[1]);
/*     */                 else {
/* 205 */                   Functions.printLog("TM:[run] WRN - Error in call ticks: " + res[1]);
/*     */                 }

/*     */               }

/* 208 */               rs.close();
/* 209 */               pc.close();
/*     */ 
/* 211 */               st = con.createStatement();
/* 212 */               query = "SELECT `id` FROM `"+RunServer.jdbc_demo_name+"`.`orders` WHERE `status`='msg'";
/* 213 */               
                        st.executeQuery(query);                        
                 
/* 214 */               rs = st.getResultSet();

/* 215 */               while (rs.next()) {
    
/* 216 */                 order_id = rs.getInt("id");

/*     */                 try {
/* 218 */                   query = "{call `"+RunServer.jdbc_demo_name+"`.close_order_by_id(?)}";
/* 219 */                   pc = con.prepareCall(query);

/* 220 */                   pc.setInt(1, order_id);
/* 221 */                   pc.executeUpdate();
                            
/* 222 */                   ResultSet o_rs = pc.getResultSet();

/* 223 */                   if (o_rs.next()) {
/* 224 */                     String[] res = o_rs.getString("vResult").split(":");

/* 225 */                     if (!res[0].equals("OK")) {
/* 226 */                       Functions.printLog("TM:[run] WRN - Error in call (demo) close_order_by_id: " + res[1]);
/*     */                     }

/*     */                   }

/* 229 */                   o_rs.close();
/* 230 */                   pc.close();

/*     */                 } catch (SQLException e) {
/* 232 */                   Functions.printLog(
                                "TM:[run] WRN - Error " + e.toString()
                                +"; " + query + " : " + order_id
                                );
/*     */                 }
/*     */               }
/*     */ 
/* 237 */               rs.close();
/* 238 */               st.close();
/*     */ 
/* 241 */               query = "{call get_for_market_open()}";
/* 242 */               pc = con.prepareCall(query);
/* 243 */               pc.executeUpdate();
                       
/* 244 */               rs = pc.getResultSet();

/* 245 */               while (rs.next()) {
/* 246 */                 count = rs.getInt("count");

/* 247 */                 if (count > 0) {
/* 248 */                   amount = rs.getDouble("amount");
/* 249 */                   clordid = rs.getInt("clordid");
/*     */                 }

/*     */               }

/* 252 */               rs.close();
/* 253 */               pc.close();
/*     */ 
/* 256 */               if (count > 0) {
                            if (this.getServerStatus() == 0) {
                              //FixProtocol.sendNewOrder_city(clordid, amount, this.tradeSession); ");
                              FixProtocol_dukas.sendNewOrder(clordid, amount);
                            }    
/*     */               }
/*     */ 
/* 263 */               st = con.createStatement();
/* 264 */               query = "SELECT `hash`, `check` FROM `accounts` WHERE `online` = 1";
/* 265 */               st.executeQuery(query);
                        
/* 266 */               rs = st.getResultSet();

                        Functions.printLog(
                                "TM:[run] INF - result from DB(online acc) [" +
                                rs.toString() + "]"
                                );

/* 267 */               while (rs.next()) {   
                           
    /* 268 */               for (String key : Users.UserList.keySet()) {
        
    /* 269 */                 Users user = (Users)Users.UserList.get(key);
    
                              if(user != null) if (user.hash.equals(rs.getString("hash"))) {
    /* 271 */                     user.check = rs.getInt("check");
    /* 272 */                     Users.UserList.replace(key, user);
    
                                  /*System.out.println(
                                    "TicksManager:[run] user.hash.equals(rs.getString(\"hash\")) [" +
                                    user + "]"
                                    );*/
    /*     */                 }
    
    /*     */               }
          
/*     */               }//end of while

/* 276 */               rs.close();
/* 277 */               st.close();
/*     */ 
/* 280 */               st = con.createStatement();
/* 281 */               query = "SELECT `hash`, `check` FROM `"+RunServer.jdbc_demo_name+"`.`accounts` WHERE `online` = 1";
/* 282 */               st.executeQuery(query);

                        
/* 283 */               rs = st.getResultSet();

                        Functions.printLog(
                                "TM:[run] result from DB(online demo.acc) [" +
                                rs.toString() + "]"
                                );

/* 284 */               while (rs.next()) {    
   
                            for (String key : Users.UserList.keySet()) {
                                
    /* 269 */                 Users user = (Users)Users.UserList.get(key);
                              if(user != null) if (user.hash.equals(rs.getString("hash"))) {
    /* 271 */                     user.check = rs.getInt("check");
    /* 272 */                     Users.UserList.replace(key, user);
    /*     */                 }
    
    /*     */               }
    
/*     */               }//end of while
                        //System.out.println();

/* 293 */               rs.close();
/* 294 */               st.close();
/*     */ 
/* 297 */               //TicksManager.totalOrdersReal = getTotalOrders();
/* 298 */               //TicksManager.totalOrdersDemo = getTotalOrders();
                        TicksManager.totalOrders = getTotalOrders();
                        
/* 299 */               TicksManager.currBarId = getCurrBarId();
/*     */ 
/* 302 */               jdbc_connection.freeConnection(con);

/*     */             } catch (SQLException e) {
/* 305 */               Functions.printLog(
                            "TM:[run] WRN - Error " + e.toString()
                            + "; " + query + " : " + order_id
                            );
/*     */             } 

/*     */           }else {
/* 310 */             Functions.printLog("TM:[run] ERR - TM failed to jdbc connect...");
/*     */           }
/*     */ 
/* 314 */           TicksManager.old_price = TicksManager.price;
/*     */ 
/* 317 */           if (Users.UserList.size() > 0) {
/* 319 */             for (String key : Users.UserList.keySet()) {
/* 320 */               Users user = (Users)Users.UserList.get(key);
/*     */               String msg;

                        if(user == null) continue;
                        
/* 322 */               if (user.hash.equals("unknown")) {
                            msg =
                                TicksManager.price + ":" + 
                                bar_id
                                ;
/*     */               }else{
                            msg = 
                                  TicksManager.price + ":" + 
                                  bar_id + ":" + 
                                  user.check + ":" + 
                                  TicksManager.totalOrders
                                  ;
/*     */               }
                        wsocketServer.sendMessage(user, msg);
/*     */             }//end of {for (String key : Users.UserList.keySet()}
/*     */           }// end of {if (Users.UserList.size() > 0) }

/*     */         }
/*     */ 
/* 346 */       if (i > 5) {
/* 347 */         setServerOnline(broker_status, trading_status);
/* 348 */         i = 0;
/*     */       } else {
/* 350 */         i++;
/*     */       }
/*     */ 
                try{
                    Thread.sleep(100l);
                    /*System.out.println(
                            "TICKSMANAGER:[run] Going to sleep for a while[100l]"
                            );*/

                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.out);
                    Functions.printLog(
                                  "TM:[run] ERR - " + ex.getLocalizedMessage()
                                  );

                }

/*     */     }
/*     */   }
/*     */ 
            public int getServerStatus () {
              int status = 0; 
              int id = 0;

              Connection con = jdbc_connection.getConnection();
/* 365 */     String query = null;

              if (con == null) return status;

/*     */     try {
/* 369 */         Statement st = con.createStatement();
/* 370 */         query = "SELECT `status`, `id` FROM `server_status` WHERE `status` = 1 and now() between `start` and `stop`";
/* 371 */         st.executeQuery(query);

/* 372 */         ResultSet rs = st.getResultSet();

/* 373 */         while (rs.next()) {
/* 374 */           status = rs.getInt("status");
                
                    Functions.printLog(
                        "TM:[getServerStatus] INF - server_status " + 
                        "[" + status+ "] id:" + rs.getInt("id")
                    );
/*     */         }

/* 376 */         rs.close();
/* 377 */         st.close();

/*     */       } catch (SQLException e) {
                    Functions.printLog(
                      "TM:[getServerStatus] ERR - " + e.getLocalizedMessage()+ 
                      "[" + query+ "]"
                    );
  /* 381 */         jdbc_connection.release();

/*     */       }
/* 383 */       jdbc_connection.freeConnection(con);
              
                return status;
            }
            
            public static String getServerTimeStatus () {
              String status = "";

              Connection con = jdbc_connection.getConnection();
/* 365 */     String query = null;

              if (con == null) return status;

/*     */     try {
/* 369 */         Statement st = con.createStatement();
/* 370 */         query = "SELECT `status`, `id` FROM `server_status` WHERE `status` = 1 and now() between `start` and `stop`";
/* 371 */         st.executeQuery(query);

/* 372 */         ResultSet rs = st.getResultSet();

/* 373 */         while (rs.next()) {
/* 374 */           status = rs.getString("status")+rs.getString("id")+"#";
                
                    Functions.printLog(
                        "TM:[getServerStatus] INF - server_status " + 
                        "[" + status+ "] id:" + rs.getInt("id")
                    );
/*     */         }

/* 376 */         rs.close();
/* 377 */         st.close();

/*     */       } catch (SQLException e) {
                    Functions.printLog(
                      "TM:[getServerStatus] ERR - " + e.getLocalizedMessage()+ 
                      "[" + query+ "]"
                    );
  /* 381 */         jdbc_connection.release();

/*     */       }
/* 383 */       jdbc_connection.freeConnection(con);
              
                return status;
            }
            
            public double getLastPrice () {
                double last_price = 0; 
                Connection con = jdbc_connection.getConnection();
/* 365 */     String query = null;

              if (con == null) return last_price;

/*     */       try {
/* 369 */         Statement st = con.createStatement();
/* 370 */         query = "SELECT `price` FROM `ticks` order by id desc limit 0,1";
/* 371 */         st.executeQuery(query);

/* 372 */         ResultSet rs = st.getResultSet();

/* 373 */         while (rs.next()) {
/* 374 */           last_price = rs.getDouble("price");

                    Functions.printLog(
                        "TM:[getCurrBar] INF - last_price=" + 
                        "[" + last_price+ "]"
                      );
/*     */         }

/* 376 */         rs.close();
/* 377 */         st.close();

/*     */       } catch (SQLException e) {        
/* 381 */         Functions.printLog(
                    "TM:[getLastPrice] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );

                  jdbc_connection.release();
/*     */       }
/* 383 */       jdbc_connection.freeConnection(con);
             
                return last_price;
            }
                    
/*     */   private int getCurrBarId() {
/* 363 */     int bar_id = 0;
/* 364 */     Connection con = jdbc_connection.getConnection();
/* 365 */     String query = null;

              if (con == null) return bar_id;

/*     */       try {
/* 369 */         Statement st = con.createStatement();
/* 370 */         query = "SELECT * FROM `bars` ORDER BY id DESC LIMIT 0, 1";
/* 371 */         st.executeQuery(query);
                  
/* 372 */         ResultSet rs = st.getResultSet();

/* 373 */         while (rs.next()) {
/* 374 */           bar_id = rs.getInt("id");

                    Functions.printLog(
                        "TM:[getCurrBar] INF - bar_id=" + 
                        "[" + bar_id+ "]"
                      );
                    
/*     */         }
/* 376 */         rs.close();
/* 377 */         st.close();

/*     */       } catch (SQLException e) {
                 Functions.printLog(
                    "TM:[getCurrBar] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
/* 381 */         jdbc_connection.release();

/*     */       }
/* 383 */       jdbc_connection.freeConnection(con);

/* 385 */     return bar_id;
/*     */   }

            /**
             * Gets total orders from real and demo
             * @return total=real+demo
             */
            private String getTotalOrders(){                
                Connection con = jdbc_connection.getConnection();
                int buyOrders = 0;
                int sellOrders = 0;
                String query = null;
 
                if (con == null) return String.valueOf(buyOrders) + ":" + String.valueOf(sellOrders);

                try {
                    Statement st = con.createStatement();
                    query = "SELECT (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'buy')+(SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'sell')+(SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'sell') AS sells;";
                    
                    //real: query = "SELECT (SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'sell') AS sells FROM ticks WHERE id = (SELECT MAX(id) FROM ticks);";
                    //demo: query = "SELECT (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'sell') AS sells FROM `"+RunServer.jdbc_demo_name+"`.ticks WHERE id = (SELECT MAX(id) FROM `"+RunServer.jdbc_demo_name+"`.ticks);";

                    st.executeQuery(query);
                  
                    ResultSet rs = st.getResultSet();

                    while (rs.next()) {
                      buyOrders = rs.getInt("buys");
                      sellOrders = rs.getInt("sells");
                               Functions.printLog(
                                   "TM:[getTotalOrders] INF - buyOrders=" + 
                                   "[" + buyOrders+ "]; sellOrders=["+ sellOrders+ "];"
                                 );
                    }

                    rs.close();
                    st.close();

                    jdbc_connection.freeConnection(con);

                }catch (SQLException e) {
                    Functions.printLog(
                      "TM:[getTotalOrders] ERR - " + e.getLocalizedMessage()+ 
                      "[" + query+ "]"
                    );

                    jdbc_connection.release();
                }

                return String.valueOf(buyOrders) + ":" + String.valueOf(sellOrders);                
            }

/*     */   private double getLastDBPrice() {
/* 516 */     Connection con = jdbc_connection.getConnection();
/* 517 */     double last_price = 0.0D;
/* 518 */     String query = null;
/*     */ 
              if (con == null) return last_price;

/*     */       try {
/* 522 */         Statement st = con.createStatement();
/* 523 */         query = "SELECT close AS last_price FROM `bars` ORDER BY `bars`.`timestamp` DESC LIMIT 0, 1";
/* 524 */         st.executeQuery(query);
                  
/* 525 */         ResultSet rs = st.getResultSet();

/* 526 */         while (rs.next()) {
/* 527 */           last_price = rs.getDouble("last_price");
                    Functions.printLog(
                        "TM:[getLastDBPrice] INF - last_price=" + 
                        "[" + last_price+ "]"
                      );
/*     */         }                 

/* 529 */         rs.close();
/* 530 */         st.close();
/*     */ 
/* 533 */         jdbc_connection.freeConnection(con);

/*     */       }catch (SQLException e) {
                  Functions.printLog(
                    "TM:[getLastDBPrice] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
/* 538 */         jdbc_connection.release();
/*     */       }
/*     */ 
/* 542 */     return last_price;
/*     */   }
/*     */ 
/*     */   static void setOrderFilled(String clordeID, String execid, double price){
/* 548 */     Connection con = jdbc_connection.getConnection();
/*     */ 
/* 550 */     String query = null;
/*     */ 
              if (con == null) return;

/*     */       try {
/* 554 */         query = "UPDATE `"+RunServer.jdbc_demo_name+"`.market_orders SET market_id = " + execid + ", market_price = " + price + ", status = 'opened' WHERE id = " + clordeID;
/* 555 */         PreparedStatement st = con.prepareStatement(query);
/* 556 */         st.setString(1, execid);
/* 557 */         st.setDouble(2, price);
/* 558 */         st.setInt(3, Integer.valueOf(clordeID).intValue());
/* 559 */         st.executeUpdate();
/* 560 */         st.close();

                  Functions.printLog(
                    "TM:[setOrderFilled] INF - execute [" + query + "]"
                    );
/*     */ 
/* 563 */         jdbc_connection.freeConnection(con);

/*     */       } catch (SQLException e) {
                    Functions.printLog(
                    "TM:[setOrderFilled] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
/* 568 */         jdbc_connection.release();
/*     */       }
/*     */   }
/*     */ 
/*     */   static void setServerOnline(int status, int trading){
/* 574 */     Connection con = jdbc_connection.getConnection();
/*     */ 
/* 576 */     String query = null;
/*     */ 
              if (con == null) return;
/*     */       try {
/* 580 */         Statement st = con.createStatement();
/* 581 */         query = "UPDATE websocket SET broker = " + status + ", trading = " + trading + ", websocket_online = NOW();";
/* 582 */         st.executeUpdate(query);
/* 583 */         st.close();
                  
/* 585 */         st = con.createStatement();
/* 586 */         query = "UPDATE `"+RunServer.jdbc_demo_name+"`.websocket SET broker = " + status + ", trading = " + trading + ", websocket_online = NOW();";
/* 587 */         st.executeUpdate(query);
/* 588 */         st.close();
/*     */ 
/* 591 */         jdbc_connection.freeConnection(con);

/*     */       }catch (SQLException e){
                    Functions.printLog(
                    "TM:[setServerOnline] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
/* 598 */         jdbc_connection.release();

/*     */       }
/*     */   }
/*     */ 
/*     */   static void setOrderFilled(String clordeID, String error){
/* 604 */     Connection con = jdbc_connection.getConnection();
/*     */ 
/* 606 */     String query = null;
/*     */ 
              if (con == null) return;
              
/*     */       try {
/* 610 */         query = "UPDATE `"+RunServer.jdbc_demo_name+"`.market_orders SET error = " + error + ", status = 'rejected' WHERE id = " + clordeID;
/* 611 */         PreparedStatement st = con.prepareStatement(query);
/* 612 */         st.setString(1, error);
/* 613 */         st.setInt(2, Integer.valueOf(clordeID).intValue());
/* 614 */         st.executeUpdate();
/* 615 */         st.close();

                  Functions.printLog(
                    "TM:[setOrderFilled] INF - execute [" + query + "]"
                    );
/*     */ 
/* 618 */         jdbc_connection.freeConnection(con);

/*     */       } catch (SQLException e) {
                  Functions.printLog(
                    "TM:[setOrderFilled] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
/* 623 */         jdbc_connection.release();
/*     */       }
/*     */   }

            /*
             * 
             */
            public String getOnOffDates(){
                Connection con = jdbc_connection.getConnection();
                String query = null;
                if (con == null) return "false#false#false";
                
                Statement st;
                try {                    
                    st = con.createStatement();
                    query = "select `id`, `start` as server_off_date, `stop` as server_on_date from `"+RunServer.jdbc_demo_name+"`.`server_status` where `status`=1 and now() between `start` and `stop`";
                    st.executeQuery(query);
                    ResultSet rs = st.getResultSet();
                    
                    query = "false#false#false";
                    
                    while (rs.next()) {                  
                        Functions.printLog(
                             "TM[getOnOffDates] INF - id:"+rs.getString("id")
                             + " off:" + rs.getString("server_off_date")
                             + " on:" + rs.getString("server_on_date")
                           );
                        query = rs.getString("id")+"#"+rs.getString("server_off_date")+"#"+rs.getString("server_on_date");
                    }                 

                 rs.close();
                 st.close();
                 jdbc_connection.freeConnection(con);
                } catch (SQLException ex) {
                    Functions.printLog(
                             "TM:[getOnOffDates] WRN - "
                           );
                    jdbc_connection.release();
                    return "false#false#false";
                }         
                return query;
            }

            /*
             * Updates server on\off dates
             */
            public void setOnOffDate(String server_start, String server_stop){
                Connection con = jdbc_connection.getConnection();
 
                String query = null;
                if (con == null) return;

                try {
                  //INSERT INTO `boarmachine_demo`.`server_status` (type, start, stop, msg, status) VALUES ('sys_weekend', now(), now(), null, 1)
                  query = "INSERT INTO `"+RunServer.jdbc_demo_name+"`.server_status (type, start, stop, msg, status) VALUES ('sys_weekend', '"+server_stop+"', '"+server_start+ "', null, 1)";
                  PreparedStatement st = con.prepareStatement(query);
                  st.executeUpdate();
                  st.close();
                  
                  query = "INSERT INTO server_status (type, start, stop, msg, status) VALUES ('sys_weekend', '"+server_stop+"', '"+server_start+ "', null, 1)";
                  st = con.prepareStatement(query);
                  st.executeUpdate();
                  st.close();

                    Functions.printLog(
                      "TM:[setOnOffDate] INF - execute [" + query + "]"
                      );

                  jdbc_connection.freeConnection(con);

                } catch (SQLException e) {
                           Functions.printLog(
                             "TM:[setOnOffDate] ERR - " + e.getLocalizedMessage()+ 
                             "[" + query+ "]"
                           );
                  jdbc_connection.release();
                }
            }
            /**
             * Tries to connect to webSocket
             *  if its unreachable restarts socket server
             */
            class Delayer extends TimerTask{
                DateTime server_start;
                DateTime server_stop;
                DateTime now;
                DateTimeFormatter fmt;
                
                Delayer(){
                    fmt = DateTimeFormat.forPattern(dtFormat);
                    String[] result = getOnOffDates().split("#");
                    
                    try{
                        server_start = fmt.parseDateTime(result[2]);
                        server_stop = fmt.parseDateTime(result[1]);
                        server_stop = server_stop.plusDays(1);
                        
                    }catch(IllegalArgumentException e){
                        Functions.printLog("TM[Delayer] - ERR Error parsing: " + result[2] + "; "+result[1]);
                        server_start = new DateTime();
                        server_stop  = new DateTime(); 
                        now          = new DateTime();
                    }
                }
        
                @Override
                public void run() {
                    now          = new DateTime();
                    Functions.printLog("TM[Delayer] stop:"
                            +server_stop.toString(dtFormat)
                            +"; now:"+ now.toString(dtFormat)
                            +"; start:"+server_start.toString(dtFormat)
                            +"; ServerStatus["+RunServer.serverStatus+"]"
                            );
                    
                    //server is online, but its time to go sleep
                    if((RunServer.serverStatus == 0) && server_stop.isBefore(now) && server_start.isAfter(now)){
                        RunServer.serverStatus = 1;
                        
                        Functions.printLog("SERVER-SET-OFFLINE");                         
                        new Emailer().sendEmail("SERVER-INFO", "["+new Date().toString()+"] SERVER-SET-OFFLINE");
                        
                         
                    //if server_start time is passed, but status is offline: go online, shedule next on\off time
                    }else if(now.isAfter(server_start) && RunServer.serverStatus == 1){
                        server_start = server_start.plusWeeks(1);
                        server_stop = server_stop.plusDays(6);
                        RunServer.serverStatus = 0;
                        
                        setOnOffDate(server_start.toString(dtFormat),server_stop.toString(dtFormat));
                        
                        Functions.printLog("SERVER-SET-ONLINE#NEW-ON-OFF-TIME-SHEDULED");
                        new Emailer().sendEmail("SERVER-INFO", "["+new Date().toString()+"] SERVER-SET-ONLINE. NEXT OFFON TIME:"+server_stop.toString(dtFormat)+";"+server_start.toString(dtFormat));
                        
                    //if server is online, but on\off time is weird, get them from database
                    //or off time is not met check socketServer
                    }else if(
                            (RunServer.serverStatus == 0 && (server_stop.isAfter(now) || now.isAfter(server_start)))
                            || (server_start.isEqual(server_stop) && server_start.isEqual(now))
                            ){
                        Functions.printLog("SERVER-WORKING");
                        
                        //check wsocket availability
                        checkSocketStatus();  
                        
                        //check onoff dates
                        fmt = DateTimeFormat.forPattern(dtFormat);
                        String[] result = getOnOffDates().split("#");

                        try{
                            server_start = fmt.parseDateTime(result[2]);
                            server_stop = fmt.parseDateTime(result[1]);
                            server_stop = server_stop.plusDays(1);
                            Functions.printLog("TM[Delayer] INF Sheduled on["+server_start.toString(dtFormat)+"]off["+server_stop.toString(dtFormat)+"]");

                        }catch(IllegalArgumentException e){
                            Functions.printLog("TM[Delayer] - ERR Error parsing: " + result[2] + "; "+result[1]);
                            server_start = new DateTime();
                            server_stop  = new DateTime(); 
                            now          = new DateTime();
                        }
                    }
                    
                    
                }
            }
            
            public void checkSocketStatus(){
                try{    
                    Socket sock = new Socket();
                    sock.connect(RunServer.wsocket_port, (int)TimeUnit.SECONDS.toMillis(5));
                    sock.close();                    
                    
                }catch(ConnectException e){
                    new Emailer().sendEmail("SERVER-ERROR", "["+new Date().toString()+"] WebSocket down");
                    
                    Functions.printLog(
                                "TM[Run] ERR - "
                                + e.getLocalizedMessage()
                                + " to port " + RunServer.wsocket_port.getPort()+";"
                                +" Trying to restart wsocket server."
                                );
                    try {                        
                        RunServer.server.stop();
                        RunServer.server = new TestServer(RunServer.wsocket_port);
                        RunServer.server.start();
                        new Emailer().sendEmail("SERVER-INFO", "["+new Date().toString()+"] WebSocket server up again");
                        
                    } catch (IOException ex) {
                        Functions.printLog("TM[WSocketRestart] ERR -" + ex.getLocalizedMessage());
                        
                    } catch (InterruptedException ex) {
                        Functions.printLog("TM[WSocketRestart] ERR -" + ex.getLocalizedMessage());
                        
                    }
                    
                } catch (IOException e) {
                    Functions.printLog("TM[Run] ERR - " + e.getLocalizedMessage());
                    
                    new Emailer().sendEmail("SERVER-ERROR", "["+new Date().toString()+"] WebSocket down");
                }
            }
/*     */ }