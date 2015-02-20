 package fixclient.ticks;
 
import customjbdc.connection.DBConnection;
import customjbdc.queries.DBMakeQuery;
import fixclient.FixClient;
import static fixclient.FixClient.WSConnectionList;
import fixclient.protocol.DukasProtocol;
import fixclient.wsocket.SimpleClient;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
import java.net.URI;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
      //  import org.custom.Functions;
import emailer.Emailer;
import functions.Functions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
//import org.wsocket.jdbc.DBConnection;
//import org.wsocket.jdbc.DBMakeQuery;
         
public class TicksManager extends Thread{
    public static /*final*/ int PRICE_PRECISION;

    public static double    real_price      = 0.0D;
    public static double    price           = 0.0D;
    public static double    old_price       = 0.0D;
    public static double    mix_price       = 0.0D;
    public static int       broker_status   = 1;
    public static int       trading_status  = 1;
    public static String    mode            = "real";
    public static int       mix_spread      = 3;
            
    public static DBConnection jdbc_connection = FixClient.jdbc;
            
    //# connection checker
    static Timer 
            scheduleTimer,
            connTimer
            ;
    private static String totalOrders;
    
    public static String dtFormat;
    
    public static int serverStatus=0;
    
    //vars for checking tick occurence frequency
    static ArrayList<DateTime> tick_time_list = new ArrayList<DateTime>();
    static /*final*/ Integer TICK_COUNT_PER_MIN = 10;
    static /*final*/ Integer TIME_PER_TICK = 900; //1000ms -100ms
    static boolean mix_on = true;    
    
    
    public TicksManager(Properties properties){
        
        Functions.printLog("TM:[INIT] INF - Parsing variables");

        this.PRICE_PRECISION    = Integer.parseInt(properties.getProperty("price_output_precision"));
        
        DBMakeQuery.configFixPrecision(jdbc_connection, null, TicksManager.PRICE_PRECISION );
        DBMakeQuery.configFixPrecision(jdbc_connection, FixClient.jdbc_demo_name, TicksManager.PRICE_PRECISION );
        
        DBMakeQuery.configGameTypes(jdbc_connection, null, null, TicksManager.PRICE_PRECISION);
        DBMakeQuery.configGameTypes(jdbc_connection, null, FixClient.jdbc_demo_name, TicksManager.PRICE_PRECISION);
        
        this.mode               = properties.getProperty("tick_manager_mode");
        this.dtFormat           = properties.getProperty("date_time_format");
        this.TICK_COUNT_PER_MIN    = Integer.parseInt(properties.getProperty("tick_count_per_minute"));
        this.TIME_PER_TICK    = Integer.parseInt(properties.getProperty("time_per_tick"));
        this.mix_spread         = Integer.parseInt(properties.getProperty("mix_spread"));
        
        Long schedule_time_check_interval = Long.parseLong(
                        properties.getProperty("schedule_time_check_interval"));
        
        Long delay_to_restore_conn = Long.parseLong(
                        properties.getProperty("delay_to_restore_conn"));
        
        
        scheduleTimer = new Timer();
        scheduleTimer.scheduleAtFixedRate(new Scheduler(), 0, schedule_time_check_interval);
        
        connTimer = new Timer();
        connTimer.scheduleAtFixedRate(new SocketChecker(), 0, delay_to_restore_conn);
    }

    /**
     * Sets received from broker new price
     * 
     * @param new_p 
     */
    public synchronized static void newTick(double new_p){ 
        TicksManager.real_price = new BigDecimal(Double.valueOf(
                new_p                 
            ).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();        
       
        TicksManager.price = TicksManager.real_price;

        Functions.printLog("TM:[newTick] INF - New price["+price+"]");
        
        Integer avgTime = getTickAvgTime();
        
        if(avgTime == 0) return;
        
        if(avgTime <= TIME_PER_TICK){ 
            mix_on = false;
            Functions.printLog("TM:[newTick] INF - Mix on:"+mix_on+". AVG:"+avgTime);            
            
        }else if(avgTime > (TIME_PER_TICK + (TIME_PER_TICK/10))){
            mix_on = true;
            Functions.printLog("TM:[newTick] INF - Mix on:"+mix_on+". AVG:"+avgTime);            
            
        }else{
            Functions.printLog("TM:[newTick] INF - Mix status:"+mix_on+". AVG:"+avgTime);
            
        }
    }
            
    private static Integer getTickAvgTime(){
        Long avg = 0L;
        if(tick_time_list.size() == TICK_COUNT_PER_MIN){
            DateTime prev = tick_time_list.get(0);
            Long diff = 0L;
            
            for(int i = 1; i < tick_time_list.size(); i++){
                DateTime curr = tick_time_list.get(i);
                
                diff = (curr.getMillis() - prev.getMillis());
                
                avg += diff;
                prev = curr;
                
            }
            avg /= (tick_time_list.size()-1);
            tick_time_list.clear();
            
        }else{
            tick_time_list.add(new DateTime());
        }
        
        return avg.intValue();        
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
    private double mixPrice_old(double curr_price, double real_price, int point, int spread){
        int 
            count = 0, 
            offset = 0, 
            diff = 0,
            coeff = 0
            ; 

           count = Functions.generateRandom(1000);
           offset = 0;
 
           diff = new BigDecimal(Double.valueOf((curr_price - real_price) * spread * point).doubleValue()).setScale(0, RoundingMode.HALF_DOWN).intValue();
           
                    if (diff == 0) {
             if (Functions.generateRandom(1) == 0) diff = -1;
             else diff = 1;
           }
 
                    
           if (diff > 0){
             if (count < 60) offset = -1;
             if (count < 10) offset = -2;
             if (count < 1) offset = -3; 

           }else { 
                      diff = Math.abs(diff);
             if (count > 940) offset = 1;
             if (count > 990) offset = 2;
             if (count > 999) offset = 3;
 
           }
           double offset_point = (double) offset / point;
                    curr_price += offset_point;
                    
                    //get rid of tail
                    curr_price = new BigDecimal(Double.valueOf(curr_price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
           //Functions.printLog("New mix price["+curr_price+"];"+TicksManager.activeCount()+";"  );      
        return curr_price;  
    }  
            
    @Override            
    public void run() {
        int bar_id_real = 0;
        int bar_id_demo = 0;
        int i = 0;
        
        //stores open orders profit and loss points to control procedure call
        ConcurrentHashMap<String, ArrayList<ArrayList<Double>>> totalOrderTypeBreakPoints = new ConcurrentHashMap();
        
        while (true){ 
            //Functions.printLog("TM:[run] INF");            
            
            //System.out.format("TM:[Run] Getting new mix_price Beep! ");
           if ("mix".equals(TicksManager.mode) && mix_on && (TicksManager.serverStatus == 0)) {                 
                //init value
                if (mix_price < 0.1D) {
                    TicksManager.mix_price = TicksManager.real_price;
                }

                //get rounded new mixPrice
                TicksManager.mix_price = new BigDecimal(Double.valueOf(
                        mixPrice_old(TicksManager.mix_price, TicksManager.real_price, (int)Math.pow(10, TicksManager.PRICE_PRECISION), mix_spread) //;
                    ).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();
                
                //System.out.println("mix="+mix_price+"; real="+real_price);
                
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
                //System.out.println("old="+old_price+"; price="+price);
                //round
                /*TicksManager.price = new BigDecimal(Double.valueOf(
                        TicksManager.price                     
                    ).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();
                */
                TicksManager.old_price = TicksManager.price; 
  
                //get open order break points(loss,profit)
                totalOrderTypeBreakPoints = DBMakeQuery.getOpenOrders(jdbc_connection, FixClient.jdbc_demo_name, null);
                
                bar_id_demo = DBMakeQuery.callIdleTick(jdbc_connection, FixClient.jdbc_demo_name, TicksManager.price);//DBMakeQuery.callTick(jdbc_connection, FixClient.jdbc_demo_name, TicksManager.price);
                bar_id_real = DBMakeQuery.callIdleTick(jdbc_connection, null, TicksManager.price);
                                
                //check
                if(isBreakPoint(totalOrderTypeBreakPoints, TicksManager.price)){
                    DBMakeQuery.callAutoCloseOrders(jdbc_connection, null, price);
                    DBMakeQuery.callAutoCloseOrders(jdbc_connection, FixClient.jdbc_demo_name, price);
                }
                
                DBMakeQuery.closeOrdersByStatus(jdbc_connection, null, "msg");
                DBMakeQuery.closeOrdersByStatus(jdbc_connection, FixClient.jdbc_demo_name, "msg");
                
                //send orders to fix
                String tResult = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                if(tResult != null){
                    int count = Integer.parseInt(tResult.split("#")[0]);
                    double amount = Double.parseDouble(tResult.split("#")[1]);
                    int clordid = Integer.parseInt(tResult.split("#")[2]);
                    DukasProtocol.sendNewOrder(clordid, amount);
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
                //if(TicksManager.serverStatus == 0) 
                    Thread.sleep(145L);
                //else Thread.sleep(TimeUnit.SECONDS.toMillis(1));

            } catch (InterruptedException ex) {
                Functions.printLog(
                    "TM:[run] ERR - " + ex.getLocalizedMessage()
                    );

            }
              
        }//end of endless loop
        
    }
    
    boolean isBreakPoint(ConcurrentHashMap<String, ArrayList<ArrayList<Double>>> totalOrderTypeBreakPoints, double price){
        if(totalOrderTypeBreakPoints == null) return false;
        
        for(String type:totalOrderTypeBreakPoints.keySet()){
            ArrayList<ArrayList<Double>> price_set = totalOrderTypeBreakPoints.get(type);
            Iterator<ArrayList<Double>> iter = price_set.iterator();
            
            int i = 0;
            
            while(iter.hasNext()){                
                Functions.printLog("TM:[isBreakPoint] INF - "+type+" - [loss:"+price_set.get(i).get(0)+", profit:"+price_set.get(i).get(1)+"] currPrice: "+price);
                
                //if buy then if price >= profit or price <= loss then return true
                if(type.equalsIgnoreCase("buy") && (price_set.get(i).get(0) >= price || price >= price_set.get(i).get(1)) ){
                    return true;
                    
                //if sell then if price <= profit or price >= loss then return true
                } else if(type.equalsIgnoreCase("sell") && (price_set.get(i).get(1) >= price || price >= price_set.get(i).get(0)) ){
                    return true;
                    
                }
                iter.next();
                i++;
            }
        }
        
        return false;
    }
    
    class SocketChecker extends TimerTask{

        @Override
        public void run() {   
            for (URI serv_uri:WSConnectionList.keySet()){
                //Functions.printLog("DELAYER:[run] INF - Check connection to "+serv_uri.toString());
                SimpleClient conn = (SimpleClient) WSConnectionList.get(serv_uri);
                
                if(!conn.isOpen()){                    
                    Functions.printLog("DELAYER:[run] INF - Restoring connection to "+serv_uri.toString());
                    SimpleClient client = new SimpleClient(serv_uri);
                    client.connect();
                    WSConnectionList.replace(serv_uri, client);
                }
            }
            
        }
        
    }
    
    class Scheduler extends TimerTask{
        DateTime server_start;
        DateTime server_stop;
        DateTime now;
        DateTimeFormatter fmt;
        
        int i = 61; 

        Scheduler(){
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
            if(FixClient.dukasFix.started) {
                DukasProtocol.sendOrderMassStatusRequest();
                DukasProtocol.sendAccountInfoRequest();
            }

            //server is online, but its time to go sleep
            if((TicksManager.serverStatus == 0) && server_stop.isBefore(now) && server_start.isAfter(now)){                
                
                TicksManager.serverStatus = 1;

                broker_status = 0;
                trading_status = 0;
                
                DBMakeQuery.setServerOnline(jdbc_connection, null, broker_status, trading_status);
                DBMakeQuery.setServerOnline(jdbc_connection, FixClient.jdbc_demo_name, broker_status, trading_status);
                
                //saving last fix tick price for autoticker
                DBMakeQuery.setClosingPrice(jdbc_connection, null, real_price);
                
                //wait until fix starts if its not already
                try {                            
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException ex) {
                    Functions.printLog(
                            "TM[Delayer] WRN - Failed to sleep"
                            );
                }

                //close all open orders
                DBMakeQuery.forceCloseOrdersByStatus(jdbc_connection, FixClient.jdbc_demo_name, "open");
                DBMakeQuery.forceCloseOrdersByStatus(jdbc_connection, null, "open");
                System.out.println("alive - 2");
                
                //close all open order in dukasfix real
                String tResult_real = DBMakeQuery.callGetForMarketOpen(jdbc_connection, null);
                if(tResult_real != null){
                    System.out.println("alive - 3");
                    int count = Integer.parseInt(tResult_real.split("#")[0]);
                    double amount_real = Double.parseDouble(tResult_real.split("#")[1]);
                    int clordid_real = Integer.parseInt(tResult_real.split("#")[2]);                           
                    DukasProtocol.sendNewOrder(clordid_real, amount_real);
                    Functions.printLog("[callGetForMarketOpen:real]: "+tResult_real);
                }
                
                //wait until fix finishes all processing
                try {                            
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                } catch (InterruptedException ex) {
                    Functions.printLog(
                            "TM[Delayer] WRN - Failed to sleep"
                            );
                }

                try{  
                    //and close connection
                    System.out.println("alive - 4");
                    FixClient.dukasFix.initiator.stop();                            

                }catch(NullPointerException ex){
                    Functions.printLog(
                            "TM[Delayer] WRN - Failed to stop Fix"
                            );
                }                         

                Functions.printLog("FIX SNOOZES Zzz-zz-z");                      
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

                Functions.printLog("FIX-UP#NEXT OFFON TIME:"+server_stop.toString(dtFormat)+";"+server_start.toString(dtFormat));
                new Emailer().sendEmail(FixClient.app_id, "["+new Date().toString()+"] FIX UP. NEXT OFFON TIME:"+server_stop.toString(dtFormat)+";"+server_start.toString(dtFormat));
                
                server_stop = server_stop.plusDays(1);
            //if server is online, but on\off time is weird, get them from database
            //or off time is not met check socketServer
            }else if(
                    (TicksManager.serverStatus == 0 && (server_stop.isAfter(now) || now.isAfter(server_start)))
                    || (server_start.isEqual(server_stop) && server_start.isEqual(now))
                    ){
                
                if(FixClient.dukasFix.started) {
                    if(!FixClient.dukasFix.initiator.isLoggedOn()){
                        Functions.printLog("SHEDULER ERR - dukasFix isn't loggedOn[maybe stunnel is down]"); 
                        
                        if(i > 60 ){
                            new Emailer().sendEmail(FixClient.app_id, "["+new Date().toString()+"] SHEDULER ERR - dukasFix isn't loggedOn[maybe stunnel is down])" );
                            i=0;
                        }else i++;
                        
                        TicksManager.serverStatus =  0;
                        TicksManager.mix_on = false;
                    }
                }
                    
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