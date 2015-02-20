/*     */ package fixclient.ticks;
/*     */ 
import customjbdc.connection.DBConnection;
import customjbdc.queries.DBMakeQuery;
import fixclient.FixClient;
import static fixclient.FixClient.WSConnectionList;
import fixclient.protocol.DukasProtocol;
import fixclient.wsocket.SimpleClient;
import java.io.IOException;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
import java.net.URI;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
      //  import org.custom.Functions;
import emailer.Emailer;
import functions.Functions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
//import org.wsocket.jdbc.DBConnection;
//import org.wsocket.jdbc.DBMakeQuery;
         
public class TicksManager extends Thread{
    public static /*final*/ int PRICE_PRECISION;// = 5; //digits after point: for o/p

    public static double    real_price      = 0.0D;
    public static double    price           = 0.0D;
    public static double    old_price       = 0.0D;
    public static double    mix_price       = 0.0D;
    public static int       broker_status   = 1;
    public static int       trading_status  = 1;
    public static String    mode            = "real";
            
    public static DBConnection jdbc_connection = FixClient.jdbc;
            
    //# connection checker
    static Timer 
            scheduleTimer,
            connTimer
            ;
    private static String totalOrders;
    
    public static String dtFormat;
    
    public static int serverStatus=0;
    
    public TicksManager(Properties properties){
        
        Functions.printLog("TM:[INIT] INF - Parsing variables");

        this.PRICE_PRECISION    = Integer.parseInt(properties.getProperty("price_output_precision"));
        this.mode               = properties.getProperty("tick_manager_mode");
        this.dtFormat           = properties.getProperty("date_time_format");
        
        Long schedule_time_check_interval = Long.parseLong(
                        properties.getProperty("schedule_time_check_interval"));
        
        Long delay_to_restore_conn = Long.parseLong(
                        properties.getProperty("delay_to_restore_conn"));
        
        scheduleTimer = new Timer();
        scheduleTimer.scheduleAtFixedRate(new Delayer(), 0, schedule_time_check_interval);
        
        connTimer = new Timer();
        connTimer.scheduleAtFixedRate(new Delayer1(), 0, delay_to_restore_conn);
    }

    /**
     * Sets received from broker new price
     * 
     * @param new_p 
     */
    public synchronized static void newTick(double new_p){ 
        TicksManager.real_price = new_p;
        Functions.printLog("TM:[newTick] INF - New price["+new_p+"]");
    }
            
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
            
    @Override            
    public void run() {
        int bar_id_real = 0;
        int bar_id_demo = 0;
        int i = 0;
        
        while (true){ 
            //Functions.printLog("TM:[run] INF");
                        
            //System.out.format("TM:[Run] Getting new mix_price Beep! ");
            if ("mix".equals(TicksManager.mode)) {                    
                //init value
                if (mix_price < 0.1D) {
                    TicksManager.mix_price = TicksManager.real_price;
                }

                //get new mixPrice and round it
                TicksManager.mix_price = new BigDecimal(Double.valueOf(
                        mixPrice_old(TicksManager.mix_price, TicksManager.real_price, 10000)
                        ).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();

             //not mix
             }else {
                TicksManager.mix_price = TicksManager.real_price;
             }
                    
             //is possible only if not mix
            if (TicksManager.mix_price > 0.1D) {
                TicksManager.price = TicksManager.mix_price;
                //System.out.println("TicksManager.mix_price > 0.1D TicksManager.old_price != TicksManager.price="+(TicksManager.old_price != TicksManager.price));
            }
            
            //broadcast price values
            if (
                (TicksManager.old_price != TicksManager.price) && 
                (TicksManager.price > 0.1D) 
               ){
                TicksManager.old_price = TicksManager.price; 
  
                //real
                bar_id_real = DBMakeQuery.callTick(jdbc_connection, null, TicksManager.price);
                DBMakeQuery.closeOrdersByStatus(jdbc_connection, null, "msg");

                //demo
                bar_id_demo = DBMakeQuery.callTick(jdbc_connection, FixClient.jdbc_demo_name, TicksManager.price);
                DBMakeQuery.closeOrdersByStatus(jdbc_connection, FixClient.jdbc_demo_name, "msg");

                //send orders to fix
                String tResult = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                if(tResult != null){
                    int count = Integer.parseInt(tResult.split("#")[0]);
                    double amount = Double.parseDouble(tResult.split("#")[1]);
                    int clordid = Integer.parseInt(tResult.split("#")[2]);

                   //if (DBMakeQuery.getServerStatus(jdbc_connection, null) == 0) {
                       DukasProtocol.sendNewOrder(clordid, amount);
                   //}
                }
                
                TicksManager.totalOrders = DBMakeQuery.getTotalOrders(jdbc_connection, FixClient.jdbc_demo_name, null);                        
                                    
                //broadcast message to connections
                for (URI serv_uri:WSConnectionList.keySet()){
                    SimpleClient conn = (SimpleClient) WSConnectionList.get(serv_uri);
                    
                    if(conn.isOpen()){
                        String msg = "FIXPrice:"+TicksManager.price
                                +":"+bar_id_demo
                                +":"+TicksManager.totalOrders;
                        Functions.printLog("TM:[run] INF - Sending msg["+msg+"] to ["+serv_uri.toString()+"]");
                        conn.send(msg);
                    }
                }
                
            }
            
            if (i > 5) {
                DBMakeQuery.setServerOnline(jdbc_connection, null, broker_status, trading_status);   
                DBMakeQuery.setServerOnline(jdbc_connection, FixClient.jdbc_demo_name, broker_status, trading_status);
                i = 0;
            } else {
                i++;
            }
            
            try{                        
                Thread.sleep(100L);

            } catch (InterruptedException ex) {
                Functions.printLog(
                    "TM:[run] ERR - " + ex.getLocalizedMessage()
                    );

            }
              
        }//end of endless loop
        
    }
    
    class Delayer extends TimerTask{

        @Override
        public void run() {   
            for (URI serv_uri:WSConnectionList.keySet()){
                //Functions.printLog("DELAYER:[run] INF - Check connection to "+serv_uri.toString());
                SimpleClient conn = (SimpleClient) WSConnectionList.get(serv_uri);
                if(!conn.isOpen()){
                    //DBMakeQuery.setSocketAvailable(jdbc_connection, null, serv_uri.toString().substring(5), 0);
                    
                    Functions.printLog("DELAYER:[run] INF - Restoring connection to "+serv_uri.toString());
                    SimpleClient client = new SimpleClient(serv_uri);
                    client.connect();
                    WSConnectionList.replace(serv_uri, client);
                }
            }
            
        }
        
    }
    
    class Delayer1 extends TimerTask{
                DateTime server_start;
                DateTime server_stop;
                DateTime now;
                DateTimeFormatter fmt;
                
                Delayer1(){
                    fmt = DateTimeFormat.forPattern(TicksManager.dtFormat);
                    
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
                    if((TicksManager.serverStatus == 0) && server_stop.isBefore(now) && server_start.isAfter(now)){
                        TicksManager.serverStatus = 1;
                        
                        broker_status = 0;
                        trading_status = 0;
                        DBMakeQuery.setServerOnline(jdbc_connection, null, broker_status, trading_status);//trading_status);
                        DBMakeQuery.setServerOnline(jdbc_connection, FixClient.jdbc_demo_name, trading_status, broker_status);//trading_status);
                       
                        //wait until fix starts if its not already
                        try {                            
                            Thread.sleep(10000L);
                        } catch (InterruptedException ex) {
                            Functions.printLog(
                                    "TM[Delayer] WRN - Failed to sleep"
                                    );
                        }
                        
                        //close all open orders
                        DBMakeQuery.forceCloseOrdersByStatus(jdbc_connection, FixClient.jdbc_demo_name, "open");
                        DBMakeQuery.forceCloseOrdersByStatus(jdbc_connection, null, "open");
                        
                        //close all open order in fix real
                        String tResult_real = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                        int clordid_real = 0;
                        double amount_real = 0;
                        if(tResult_real != null){
                            int count = Integer.parseInt(tResult_real.split("#")[0]);
                            amount_real = Double.parseDouble(tResult_real.split("#")[1]);
                            clordid_real = Integer.parseInt(tResult_real.split("#")[2]);                           
                            DukasProtocol.sendNewOrder(clordid_real, amount_real);
                            Functions.printLog("[callGetForMarketOpen:real]: "+tResult_real);
                        }
                        //demo
                        String tResult_demo = DBMakeQuery.callGetForMarketOpen(jdbc_connection, FixClient.jdbc_demo_name);
                        int clordid_demo = 0;
                        double amount_demo = 0;
                        if(tResult_real != null){
                            int count = Integer.parseInt(tResult_demo.split("#")[0]);
                            amount_demo = Double.parseDouble(tResult_demo.split("#")[1]);
                            clordid_demo = Integer.parseInt(tResult_demo.split("#")[2]);                           
                            DukasProtocol.sendNewOrder(clordid_demo, amount_demo);
                            Functions.printLog("[callGetForMarketOpen:demo]: "+tResult_demo);
                        }    
                        
                        try{                           
                            if(tResult_real != null) DukasProtocol.sendNewOrder(clordid_real, amount_real);
                            
                            //and close connection
                            FixClient.dukasFix.initiator.stop();                            
                            
                        }catch(NullPointerException ex){
                            Functions.printLog(
                                    "TM[Delayer] WRN - Failed to stop Fix"
                                    );
                        }                         
                        
                        Functions.printLog("SERVER-SET-OFFLINE");                         
                        new Emailer().sendEmail(FixClient.app_id, "["+new Date().toString()+"] FIX SNOOZES Zzz-zz-z");
                        
                         
                    //if server_start time is passed, but status is offline: go online, shedule next on\off time and start fix
                    }else if(now.isAfter(server_start) && TicksManager.serverStatus == 1){
                        //change flag to reset Fix
                        FixClient.dukasFix.started = false;
                        
                        broker_status = 1;
                        trading_status = 1;
                        
                        //set wsocket up
                        TicksManager.serverStatus = 0;
                        
                        //schedule new onoff time
                        server_start = server_start.plusWeeks(1);
                        server_stop = server_stop.plusDays(6);                        
                        
                        DBMakeQuery.setOnOffDate(jdbc_connection, null, server_start.toString(dtFormat),server_stop.toString(dtFormat));
                        DBMakeQuery.setOnOffDate(jdbc_connection, FixClient.jdbc_demo_name, server_start.toString(dtFormat),server_stop.toString(dtFormat));
                        
                        Functions.printLog("SERVER-SET-ONLINE#NEW-ON-OFF-TIME-SHEDULED");
                        new Emailer().sendEmail(FixClient.app_id, "["+new Date().toString()+"] FIX UP. NEXT OFFON TIME:"+server_stop.toString(dtFormat)+";"+server_start.toString(dtFormat));
                                         
                 
                      
                    //if server is online, but on\off time is weird, get them from database
                    //or off time is not met check socketServer
                    }else if(
                            (TicksManager.serverStatus == 0 && (server_stop.isAfter(now) || now.isAfter(server_start)))
                            || (server_start.isEqual(server_stop) && server_start.isEqual(now))
                            ){
                        Functions.printLog("SERVER-WORKING stop:"
                                +server_stop.toString(dtFormat)
                            +"; now:"+ now.toString(dtFormat)
                            +"; start:"+server_start.toString(dtFormat)
                            +"; ServerStatus["+TicksManager.serverStatus+"]" 
                            ); 
                        
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
                }
            }
    
}
