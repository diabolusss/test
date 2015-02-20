/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fixclient.app;

import fixclient.ticks.TicksManager;
import functions.Functions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.http.SimpleExpBackoff;
import org.joda.time.DateTime;

/**
 *
 * @author colt
 * 
 * num	[1,1e4]	The number of integers requested.
 * min	[-1e9,1e9]	The smallest value allowed for each integer.
 * max	[-1e9,1e9]	The largest value allowed for each integer.
 * col	[1,1e9]	The number of columns in which the integers will be arranged.
 * 
 */
public class FixAutoRate {
    private static /*final*/ long RAW_ROUND_PRECISION;// = (long)Math.pow(10, 5); //offset 5 point precision
    
    private static /*final*/ String USER_AGENT;// = "hodiko.v@inbox.lv";
    private static /*final*/ String SERVICE_PROVIDER_URL;// = "qhttp://www.random.org/integers/";
    private static /*final*/ String SERVICE_PROVIDER_QUOTA_URL;// = "http://www.random.org/quota/";
    
    //generated numbers for offset
    public static  int randomNumberListSize;
    public static  int maxRandomNumberListSize;
    public static  int minRandomNumberListSize;
    public static  int minRandomNumber;
    public static  int maxRandomNumber;
    
    // use this numbers to seed internal generator
    // and for setting generated numberList size
    public static /*final*/ int randomSeedListSize; // = 2000;
    public static final int minSeedNumber = (int) -1e9;
    public static final int maxSeedNumber = (int) 1e9;
    
    public static Random randomCore = new Random();
    
    public static List<Integer> revertNums      = new ArrayList<Integer>(); 
    public static List<Integer> generatedNums   = new ArrayList<Integer>();   
    public static List<Long> receivedSeedNums   = new ArrayList<Long>();    
    
    //for 'šmarter' timeout if connection is down
    private static SimpleExpBackoff backoff;
    
    private AutoRateBody delayer = new AutoRateBody();
    private Timer timer = new Timer();
    
    private static long mix_delay;// = 1*400;
    
    public static double price = 0;
    public static double realPrice;
    public static double totalOffset = 0;
    
    public static Long generatedOffset = 0L;
    public static int sessionOffset = 0;
    
    //DateTime server_start;
    DateTime auto_rate_stop;
    DateTime autoReverse;
    //DateTime now;
    public static String dtFormat;
    
    public int running = 0;

    public FixAutoRate() {
        //To change body of generated methods, choose Tools | Templates.
    }
    
    class AutoRateBody extends TimerTask{
        
        @Override
        public void run() {
            Functions.printLog("NEW TICK: BEEP!");
            DateTime now          = new DateTime();

            //if there is some offset numbers
            //creating course
            if(now.isBefore(autoReverse)){
                Functions.printLog("Forcing course!");
                
                if(!generatedNums.isEmpty()){
                    int index = Functions.randInt(0, generatedNums.size(), new Random(System.currentTimeMillis()).nextLong());
                    if(index>0)--index;
                    int num = generatedNums.get(index);                
                    double randomOffset = (double) ((int)num)/RAW_ROUND_PRECISION;

                    revertNums.add(num);

                    System.out.printf("[size:%d]randNum[%d]=%d; total_offset=%1.6f; Saved number for future use[size:%d]\n", generatedNums.size(), index, generatedNums.get(index),totalOffset,revertNums.size());

                    price += randomOffset; 
                    totalOffset += randomOffset;

                    //get rid of tail
                    price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();

                    //Functions.printLog("New price="+price);
                    TicksManager.newTick(price);

                    generatedNums.remove(index);
                    
                }else{
                    //if there is no seed numbers
                    if(receivedSeedNums.isEmpty()){
                       //get them and if not succeeeded generate own numbers
                        if(!communicate2RandomOrg()){
                            receivedSeedNums = generateRandomNumberListLong(minSeedNumber, maxSeedNumber, randomSeedListSize);
                        }

                    }

                    Functions.printLog("seedNumArrSize[" + receivedSeedNums.size() +"] ");

                    //now set new seed at random index
                    int index = Functions.randInt(0, receivedSeedNums.size(), new Random(System.currentTimeMillis()).nextLong());                
                    if(index>0)--index;
                    randomCore.setSeed(receivedSeedNums.get(index));  
                    Functions.printLog("Seed[" +receivedSeedNums.get(index)+ "] ");

                    //remove bad seed
                    receivedSeedNums.remove(index);

                    //and set new number list size
                    index = Functions.randInt(0, receivedSeedNums.size(), new Random(System.currentTimeMillis()).nextLong());                
                    if(index>0)--index;
                    randomNumberListSize = minRandomNumberListSize + (int)(Math.abs(receivedSeedNums.get(index) % maxRandomNumberListSize));                
                    Functions.printLog("NumListSize["+randomNumberListSize+"]");
                    //remove bad seed
                    receivedSeedNums.remove(index);

                    //now we are ready to generate own numbers
                    generatedNums = generateRandomNumberListInt(minRandomNumber, maxRandomNumber, randomNumberListSize);

                    sessionOffset += generatedOffset;

                    //Functions.printLog("Session total offset: "+sessionOffset);
                }
                
            //if there is some offset numbers
            //reversing course
            }else if(!now.isBefore(autoReverse) && now.isBefore(auto_rate_stop)){
                Functions.printLog("Reverting course!");
                
                if(!revertNums.isEmpty()){
                    int index = Functions.randInt(0, revertNums.size(), new Random(System.currentTimeMillis()).nextLong());
                    if(index>0)--index;
                    int num = revertNums.get(index);                
                    double randomOffset = (double) ((int)num)/RAW_ROUND_PRECISION;

                    System.out.printf("Reversing price[size:%d]randNum[%d]=%d; total_offset=%1.6f\n", revertNums.size(), index, revertNums.get(index),totalOffset);

                    price -= randomOffset; 
                    totalOffset -= randomOffset;

                    //get rid of tail
                    price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();

                    //Functions.printLog("New price="+price);
                    TicksManager.newTick(price);

                    revertNums.remove(index);
                    
                }else{
                    int index = Functions.randInt(0, receivedSeedNums.size(), new Random(System.currentTimeMillis()).nextLong());                
                    if(index>0)--index;
                    randomNumberListSize = minRandomNumberListSize + (int)(Math.abs(receivedSeedNums.get(index) % maxRandomNumberListSize));                
                    //Functions.printLog("NumListSize["+randomNumberListSize+"]");
                    //remove bad seed
                    receivedSeedNums.remove(index);

                    //now we are ready to generate own numbers
                    revertNums = generateRandomNumberListInt(minRandomNumber, maxRandomNumber, randomNumberListSize);

                    sessionOffset += generatedOffset;

                    //Functions.printLog("Session total offset: "+sessionOffset);
                }
            
            }else if (now.isAfter(auto_rate_stop)){
                Functions.printLog("STOP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                stop();
                
            }
        }   
        
    }
    
    //init
    public FixAutoRate(Properties randomORGProperties, double closing_price, DateTime auto_rate_start, DateTime auto_rate_stop){
        this.backoff = new SimpleExpBackoff(
                Long.parseLong(randomORGProperties.getProperty("init_sleep_before_try")),
                Long.parseLong(randomORGProperties.getProperty("restore_connection_max_tries"))
                );
        
        this.maxRandomNumberListSize = Integer.parseInt(randomORGProperties.getProperty("max_mix_offset_block_size"));
        this.minRandomNumberListSize = Integer.parseInt(randomORGProperties.getProperty("min_mix_offset_block_size"));
        this.minRandomNumber = Integer.parseInt(randomORGProperties.getProperty("negative_mix_price_offset"));
        this.maxRandomNumber = Integer.parseInt(randomORGProperties.getProperty("positive_mix_price_offset"));
        
        this.RAW_ROUND_PRECISION = (long)Math.pow(10, Integer.parseInt(randomORGProperties.getProperty("price_output_precision")));
        this.mix_delay = Long.parseLong(randomORGProperties.getProperty("mix_price_delay"));
        
        this.USER_AGENT = randomORGProperties.getProperty("user_agent");
        this.SERVICE_PROVIDER_URL = randomORGProperties.getProperty("service_provider_host");
        this.SERVICE_PROVIDER_QUOTA_URL = randomORGProperties.getProperty("service_provider_quota");
        
        this.randomSeedListSize = Integer.parseInt(randomORGProperties.getProperty("true_seed_block_size"));
                
        this.auto_rate_stop = auto_rate_stop;
        this.realPrice = closing_price;
        this.price = this.realPrice;   
        this.autoReverse = auto_rate_stop.minus((auto_rate_stop.getMillis()-auto_rate_start.getMillis())/2);
        
        this.dtFormat           = randomORGProperties.getProperty("date_time_format");
        
        Functions.printLog("FAR:[Init] INF - Starting auto rate with starting price of "+realPrice+
                "\n\t\tStarting date: " + auto_rate_start.toString(dtFormat)+
                "\n\t\tEnding date: " + auto_rate_stop.toString(dtFormat)+
                "\n\t\tReversing to initial price after "+ (auto_rate_stop.getMillis()-auto_rate_start.getMillis())/2+
                " millis: " + autoReverse.toString(dtFormat)       
                );    
        
        this.running = 1;
   }  
    
    public void start(){
        timer.scheduleAtFixedRate(new AutoRateBody(), 0, mix_delay);
    }
    
    public void stop(){
        timer.cancel();
    }
    
    /**
     * Tries to connect to service provider and get generated numbers
     * Are used for internal random(seed) as seed and generated array length
     * @param listSize
     *          number of random integers
     * @param min 
     *          minimal border
     * @param max
     *          max border
     * 
     * @return randomNum list
     *          null if smth went wrong 
     */    
    public static List<Long> getRandomLongList(int listSize, int min, int max) {
        URL _url = null;
        HttpURLConnection con = null;
        int responseCode;

        try {
            _url = new URL(SERVICE_PROVIDER_URL+"?num="+listSize+"&min="+min+"&max="+max+"&col="+listSize+"&base=10&format=plain&rnd=new");
            con = (HttpURLConnection) _url.openConnection();
            
            // optional default is GET
            con.setRequestMethod("GET");
            
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            
            responseCode = con.getResponseCode();

        } catch (IOException ex) {
            Functions.printLog("FAR:[GETRANDOMORGINT] ERR - " + ex.getLocalizedMessage());
            return new ArrayList<Long>();
        }
        
        //Functions.printLog("\nSending 'GET' request to URL : " + url);
        //Functions.printLog("Response Code : " + responseCode);
        switch(responseCode){
            case 200: //OK
                break;
            case 301:
            case 503: Functions.printLog("FAR:[GETRANDOMORGINT] WRN -Status Code 503\\301 Service Unavailable");
                return new ArrayList<Long>();
        }
        
        StringBuffer response = null;
        
        List<String> items;
        
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
            }
            
            //print result
            //Functions.printLog(response.toString());
            items = Arrays.asList(response.toString().split("\\s*\t\\s*"));
            //Functions.printLog(items.toString());
            
        } catch (IOException ex) {
            Functions.printLog("FAR:[GETRANDOMORGINT] ERR - " + ex.getLocalizedMessage());
            return new ArrayList<Long>();
        }
        
        List<Long> randomNums = new ArrayList<Long>();        
        for(int i = 0; i < items.size(); i++){
            try{
                randomNums.add(Long.parseLong(items.get(i)));

            }catch(NumberFormatException ex){
                Functions.printLog("FAR:[GETRANDOMORGINT] ERR - " + ex.getLocalizedMessage());

            }
        }
        
        if(randomNums.size() > 0) return randomNums;
        else return new ArrayList<Long>();
    }
    
    /**
     * Get available quota for this ip from random.org
     * @return quota
     *              -1 if failed
     */
    public static long getQuota(){
        URL _url = null;
        HttpURLConnection con = null;
        int responseCode;

        try {
            _url = new URL(SERVICE_PROVIDER_QUOTA_URL+"?format=plain");
            con = (HttpURLConnection) _url.openConnection();
            
            // optional default is GET
            con.setRequestMethod("GET");
            
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            
            responseCode = con.getResponseCode();

        } catch (IOException ex) {
            Functions.printLog("FAR:[GETQUOTA] ERR - " + ex.getLocalizedMessage());
            return -1;
        }
        
        //Functions.printLog("\nSending 'GET' request to URL : " + url);
        //Functions.printLog("Response Code : " + responseCode);
        switch(responseCode){
            case 200: //OK
                break;
            case 301:
            case 503: Functions.printLog("FAR:[GETRANDOMORGINT] WRN - Status Code 503\\301 Service Unavailable");
                return -1;
        }
        
        StringBuffer response = null;
        
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
            }
            
        } catch (IOException ex) {
            Functions.printLog("FAR:[GETQUOTA] ERR - " + ex.getLocalizedMessage());
            return -1;
        }
        
        long randomNum = -1;
        
        try{
            randomNum = Long.parseLong(response.toString());

        }catch(NumberFormatException ex){
            Functions.printLog("FAR:[GETQUOTA] ERR - " + ex.getLocalizedMessage());

        }   
        
        return randomNum;
    }
    
    /**
     * Implements exponential back off if server is unavailable or max quota exceeds
     * otherwise gets set of random numbers
     * @return true
     *              if succeed otherwise false
     * 
     */
    public static boolean communicate2RandomOrg(){        
        long quota = -1;
        
        //if not exceeded max attempt number
        if(backoff.isFailed()){
            Functions.printLog("FAR:[comm2RandOrg] ERR - EXCEEDED MAX TRIES COUNT[" + backoff.maxTries + "]");
            return false;
        }
            
        //if previously failed to get quota or its negative and now is possible to make another retry
        if(backoff.isRetryOk() && quota < 0){                
            quota = getQuota();

            //if quota negative 
            if(quota < 0){
                backoff.backoff();
                Functions.printLog("FAR:[comm2RandOrg] BACKOFFING ERR - Server not available or quota negative");
                return false;
            }
            
            long availableQuota = quota;

            receivedSeedNums = getRandomLongList(randomSeedListSize, minSeedNumber, maxSeedNumber);
            
            if(!receivedSeedNums.isEmpty()){
                backoff.reset();
                Functions.printLog("FAR:[comm2RandOrg] INF - Received positive quota["+availableQuota+"]. Received random numbers. Resetting backoff counter.");
                return true;

            }else{
                backoff.backoff();
                Functions.printLog("FAR:[comm2RandOrg] ERR - Failed to get number list from server. Received quota["+availableQuota);
                return false;
            }

        }
        return false;
    }    
    
    /**
     * If server is not available generate numbers at your own
     * @return 
     */
    public static List<Long> generateRandomNumberListLong(long min, long max, int size){
    //public static void generateRandomNumberList(){
        Long localOffset=0L;
        Long tNum;
        
        List<Long> randomList = new ArrayList<Long>();
        
        for(int i = 0; i < size; i++){
            
            tNum=Functions.randLong(min, max, randomCore);
            
            localOffset += tNum;
            
            randomList.add(tNum);
        }
        Functions.printLog("FAR:[generateRandomNumberListLong] INF - Generated offset "+localOffset);
        
        generatedOffset = localOffset;
        
        return randomList;
    }  
    /**
     * If server is not available generate numbers at your own
     * @return 
     */
    public static List<Integer> generateRandomNumberListInt(int min, int max, int size){
    //public static void generateRandomNumberList(){
        Integer localOffset=0;
        int tNum;
        
        List<Integer> randomList = new ArrayList<Integer>();
        
        for(int i = 0; i < size; i++){
            
            tNum=Functions.randInt(min, max, randomCore);
            
            localOffset += tNum;
            
            randomList.add(tNum);
        }
        
        Functions.printLog("FAR:[generateRandomNumberListInt] INF - Generated offset "+localOffset);
        
        generatedOffset = localOffset.longValue();
        
        return randomList;
    }
    
}
