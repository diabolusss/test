/*     */ package org.wsocket.ticks;
/*     */ 
import java.io.IOException;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
        import org.custom.Functions;
        import org.custom.RunServer;
        import org.custom.TestServer;
import org.http.Emailer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/*     */ import org.wsocket.jdbc.DBConnection;
import org.wsocket.jdbc.DBMakeQuery;
          import quickfix.SessionID;
          
/*     */ public class TicksManager extends Thread{
    
            long serverStatusCheckPeriod;
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
/*  39 */   public static DBConnection jdbc_connection = RunServer.jdbc;
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
                this.serverStatusCheckPeriod = Long.parseLong(
                        properties.getProperty("websocket_connection_check_interval"));
                
                //connection checker; server saver
                timer.scheduleAtFixedRate(new Delayer(), 0, serverStatusCheckPeriod);
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

/*     */   
    @Override            
    public void run() {
        int bar_id_real = 0;
        int bar_id_demo = 0;
        int i = 0;

              
        totalOrders   = DBMakeQuery.getTotalOrders(jdbc_connection, RunServer.jdbc_demo_name, null);
        currBarId     = DBMakeQuery.getCurrBarId(jdbc_connection, null);
        last_db_price = DBMakeQuery.getLastDBPrice(jdbc_connection, null);              

        while (true){       
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
                      (TicksManager.price > 0.1D) 
                     ){
    
                    //real
                    bar_id_real = DBMakeQuery.callTick(jdbc_connection, null, TicksManager.price);
                    DBMakeQuery.closeOrdersByStatus(jdbc_connection, null, "msg");
                    
                    //demo
                    bar_id_demo = DBMakeQuery.callTick(jdbc_connection, RunServer.jdbc_demo_name, TicksManager.price);
                    DBMakeQuery.closeOrdersByStatus(jdbc_connection, RunServer.jdbc_demo_name, "msg");

                    //send orders to fix
                    String tResult = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                    if(tResult != null){
                        int count = Integer.parseInt(tResult.split("#")[0]);
                        double amount = Double.parseDouble(tResult.split("#")[1]);
                        int clordid = Integer.parseInt(tResult.split("#")[2]);

                       if (DBMakeQuery.getServerStatus(jdbc_connection, null) == 0) {
                           FixProtocol_dukas.sendNewOrder(clordid, amount);
                       }
                    }
                    
                    //real
                    DBMakeQuery.checkOnlineAccountHash(jdbc_connection, null);
                    //demo
                    DBMakeQuery.checkOnlineAccountHash(jdbc_connection, RunServer.jdbc_demo_name);

                    TicksManager.totalOrders = DBMakeQuery.getTotalOrders(jdbc_connection, RunServer.jdbc_demo_name, null);                        
                    TicksManager.currBarId = DBMakeQuery.getCurrBarId(jdbc_connection, null);

                    TicksManager.old_price = TicksManager.price; 
  
                    if (Users.UserList.size() > 0) {
    
/* 319 */             for (String key : Users.UserList.keySet()) {
/* 320 */               Users user = (Users)Users.UserList.get(key);
/*     */               String msg;

                        if(user == null) continue;
                        
                        msg =
                                TicksManager.price + ":" 
                                + (
                                    //(user.type.equalsIgnoreCase("demo")==true)?(bar_id_demo):(bar_id_real)
                                    bar_id_demo
                                  )
                                + (
                                    (user.hash.equals("unknown")==true)?
                                        (""):(":" + user.check + ":" + TicksManager.totalOrders)
                                  )
                                ;

                        wsocketServer.sendMessage(user, msg);
/*     */             }//end of {for (String key : Users.UserList.keySet()}
/*     */           }// end of {if (Users.UserList.size() > 0) }

/*     */         }

                //START only for test puproses; delete fro real
                //broker_status = 0;
                //END
                
/* 346 */       if (i > 5) {
/* 347 */         DBMakeQuery.setServerOnline(jdbc_connection, null, broker_status, trading_status);//trading_status);
                  DBMakeQuery.setServerOnline(jdbc_connection, RunServer.jdbc_demo_name, broker_status, trading_status);//trading_status);
/* 348 */         i = 0;
/*     */       } else {
/* 350 */         i++;
/*     */       }
            
                try{
                    if(RunServer.serverStatus == 1){
                        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                    }
                    else{
                        Thread.sleep(100l);
                    }

                } catch (InterruptedException ex) {
                    Functions.printLog(
                                  "TM:[run] ERR - " + ex.getLocalizedMessage()
                                  );

                }

/*     */     }//end of endless loop
/*     */   }
/*     */ 
           
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
                    
                    server_start = new DateTime();
                    server_stop  = new DateTime(); 
                    now          = new DateTime();                    
                }
        
                @Override
                public void run() {
                    now          = new DateTime();
                    /*Functions.printLog("TM[Delayer] INF");*/
                    //END
                    
                    //server is online, but its time to go sleep
                    if((RunServer.serverStatus == 0) && server_stop.isBefore(now) && server_start.isAfter(now)){
                        RunServer.serverStatus = 1;
                        
                        broker_status = 0;
                        trading_status = 0;
                        DBMakeQuery.setServerOnline(jdbc_connection, null, broker_status, trading_status);//trading_status);
                        DBMakeQuery.setServerOnline(jdbc_connection, RunServer.jdbc_demo_name, trading_status, broker_status);//trading_status);

                        
                        //close all open orders
                        DBMakeQuery.closeOrdersByStatus(jdbc_connection, RunServer.jdbc_demo_name, "open");
                        DBMakeQuery.closeOrdersByStatus(jdbc_connection, null, "open");
                        
                        //close all open order in fix real
                        String tResult_real = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                        int clordid_real = 0;
                        double amount_real = 0;
                        if(tResult_real != null){
                            int count = Integer.parseInt(tResult_real.split("#")[0]);
                            amount_real = Double.parseDouble(tResult_real.split("#")[1]);
                            clordid_real = Integer.parseInt(tResult_real.split("#")[2]);                           
                            FixProtocol_dukas.sendNewOrder(clordid_real, amount_real);
                            Functions.printLog("[callGetForMarketOpen:real]: "+tResult_real);
                        }
                        //demo
                        String tResult_demo = DBMakeQuery.callGetForMarketOpen(jdbc_connection, RunServer.jdbc_demo_name);
                        int clordid_demo = 0;
                        double amount_demo = 0;
                        if(tResult_real != null){
                            int count = Integer.parseInt(tResult_demo.split("#")[0]);
                            amount_demo = Double.parseDouble(tResult_demo.split("#")[1]);
                            clordid_demo = Integer.parseInt(tResult_demo.split("#")[2]);                           
                            FixProtocol_dukas.sendNewOrder(clordid_demo, amount_demo);
                            Functions.printLog("[callGetForMarketOpen:demo]: "+tResult_demo);
                        }
                        
                        try{
                            //wait until fix starts if its not already
                            Thread.sleep(10000L);
                            
                            if(tResult_real != null) FixProtocol_dukas.sendNewOrder(clordid_real, amount_real);
                            
                            //and close connection
                            RunServer.dukasFixApp.initiator.stop();                            
                            
                        }catch(NullPointerException ex){
                            Functions.printLog(
                                    "TM[Delayer] WRN - Failed to stop Fix"
                                    );
                        } catch (InterruptedException ex) {
                            Functions.printLog(
                                    "TM[Delayer] WRN - Failed to sleep"
                                    );
                        }
                        
                        Functions.printLog("SERVER-SET-OFFLINE");                         
                        new Emailer().sendEmail("SERVER-INFO["+RunServer.serverID+"]", "["+new Date().toString()+"] SERVER-SET-OFFLINE");
                        
                         
                    //if server_start time is passed, but status is offline: go online, shedule next on\off time and start fix
                    }else if(now.isAfter(server_start) && RunServer.serverStatus == 1){
                        //change flag to reset Fix
                        RunServer.dukasFixApp.started = false;
                        
                        broker_status = 1;
                        trading_status = 1;
                        
                        //set wsocket up
                        RunServer.serverStatus = 0;
                        
                        //schedule new onoff time
                        server_start = server_start.plusWeeks(1);
                        server_stop = server_stop.plusDays(6);                        
                        
                        DBMakeQuery.setOnOffDate(jdbc_connection, null, server_start.toString(dtFormat),server_stop.toString(dtFormat));
                        DBMakeQuery.setOnOffDate(jdbc_connection, RunServer.jdbc_demo_name, server_start.toString(dtFormat),server_stop.toString(dtFormat));
                        
                        Functions.printLog("SERVER-SET-ONLINE#NEW-ON-OFF-TIME-SHEDULED");
                        new Emailer().sendEmail("SERVER-INFO["+RunServer.serverID+"]", "["+new Date().toString()+"] SERVER-SET-ONLINE. NEXT OFFON TIME:"+server_stop.toString(dtFormat)+";"+server_start.toString(dtFormat));
                                            
                      
                    //if server is online, but on\off time is weird, get them from database
                    //or off time is not met check socketServer
                    }else if(
                            (RunServer.serverStatus == 0 && (server_stop.isAfter(now) || now.isAfter(server_start)))
                            || (server_start.isEqual(server_stop) && server_start.isEqual(now))
                            ){
                        Functions.printLog("SERVER-WORKING stop:"
                                +server_stop.toString(dtFormat)
                            +"; now:"+ now.toString(dtFormat)
                            +"; start:"+server_start.toString(dtFormat)
                            +"; ServerStatus["+RunServer.serverStatus+"]" 
                            );
                        
                        //check wsocket availability
                        //checkSocketStatus();  
                        
                        //check onoff dates                        
                        String result = DBMakeQuery.getOnOffDates(jdbc_connection, null);
                        
                        if (result != null){                            
                            String[] resultSet = result.split("#");                            
                            server_start = new DateTime();
                            server_stop  = new DateTime(); 
                            now          = new DateTime();
                            
                            try{
                                server_start = fmt.parseDateTime(resultSet[2]);
                                server_stop = fmt.parseDateTime(resultSet[1]);
                                server_stop = server_stop.plusDays(1);
                                Functions.printLog("TM[Delayer] INF - Sheduled on["+server_start.toString(dtFormat)+"]off["+server_stop.toString(dtFormat)+"]");

                            }catch(IllegalArgumentException e){
                                Functions.printLog("TM[Delayer] ERR - Error parsing["+result+"]: " + resultSet[2] + "; "+resultSet[1]);
                                
                            }catch(ArrayIndexOutOfBoundsException ex){
                                Functions.printLog("TM[Delayer] ERR - Error parsing["+result+"]");                                
                            }
                        }
                    }
                    
                    //always check wsocket availability
                    checkSocketStatus();
                    
                }
            }
            
            public void checkSocketStatus(){
                boolean catchedError = false;
                try{    
                    Socket sock = new Socket();
                    sock.connect(RunServer.wsocket_port, (int)TimeUnit.SECONDS.toMillis(5));
                    sock.close();                    
                    
                }catch(ConnectException e){
                    catchedError = true;
                    Functions.printLog("TM[checkSocketStatus] ERR - " + e.getLocalizedMessage());
                    new Emailer().sendEmail("SERVER-ERROR["+RunServer.serverID+"]", "["+new Date().toString()+"] WebSocket down[REASON: ConnectException - "+e.getLocalizedMessage()+"] ONLINE:"+TestServer.counter);
                                    
                    
                } catch (IOException e) {
                    catchedError = true;
                    Functions.printLog("TM[checkSocketStatus] ERR - " + e.getLocalizedMessage());                    
                    new Emailer().sendEmail("SERVER-ERROR["+RunServer.serverID+"]", "["+new Date().toString()+"] WebSocket down[REASON: IOException - "+e.getLocalizedMessage()+"] ONLINE:"+TestServer.counter);
                }
                
                if(catchedError){
                    Functions.printLog(
                                "TM[checkSocketStatus] INF - Trying to restart wsocket server."
                                );
                    try {                        
                        RunServer.server.stop();
                        RunServer.server = new TestServer(RunServer.wsocket_port);
                        RunServer.server.start();
                        new Emailer().sendEmail("SERVER-INFO["+RunServer.serverID+"]", "["+new Date().toString()+"] WebSocket server up again");
                        Functions.printLog("TM[WSocketRestart] INF - WebSocket server up again");
                        
                    } catch (IOException ex) {
                        Functions.printLog("TM[WSocketRestart] ERR -" + ex.getLocalizedMessage());
                        
                    } catch (InterruptedException ex) {
                        Functions.printLog("TM[WSocketRestart] ERR -" + ex.getLocalizedMessage());
                        
                    }
                }
            }
/*     */ }