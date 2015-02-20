package functions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;

/**
 *
 * @author colt
 */
public class Functions {
    public static Long fileSize(String filename){
        return new File(filename).length();
    }
    
    /*
     * Fetches all available server properties 
     * @param path
     */
    public static Properties getProperties(String path){
        Properties newProperties = new Properties();
        try {
            InputStream input = new FileInputStream(path);
            newProperties.load(input);
        }
        catch (IOException ex){
            printLog("Functions:[getProperties] ERR - Cannot open and load server properties file. E: "+ex.getLocalizedMessage());
            return null;
        }
        return newProperties;                
    }
    
    public static Properties getProperties(InputStream input){
        Properties newProperties = new Properties();
        try {            
            newProperties.load(input);
        }
        catch (IOException ex){
            printLog("Functions:[getProperties] ERR - Cannot open and load server properties file. E: "+ex.getLocalizedMessage());
            return null;
        }
        return newProperties;                
    }
    
    /**
    * Outputs message with timestamp
    * @param msg 
    */
    public static void printLog(String msg){
       System.out.println( "<" + Calendar.getInstance().getTime().toString() + "> " + msg);
    }
    
    /**
     * Generate pseudo random integers in diapason[min,max]
     * @param min
     * @param max
     * @return 
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max, long seed) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random(seed);

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = (int) (rand.nextInt((max - min) + 1)) + min;

        return randomNum;
    }
    public static int randInt(int min, int max, Random rand) {

        // Usually this can be a field rather than a method variable
        //Random rand = new Random(seed);

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = (int) (rand.nextInt((max - min) + 1)) + min;

        return randomNum;
    }
    
    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = (int) (rand.nextInt((max - min) + 1)) + min;

        return randomNum;
    }
    
   /**
    * Returns a psuedo-random number between min and max, inclusive.
    * The difference between min and max can be at most
    * <code>Integer.MAX_VALUE - 1</code>.
    *
    * @param min Minimim value
    * @param max Maximim value.  Must be greater than min.
    * @return Integer between min and max, inclusive.
    * @see java.util.Random#nextDouble()
    */
    public static long randLong(long min, long max, long seed) {

       // Usually this can be a field rather than a method variable
       Random rand = new Random(seed);

       // nextInt is normally exclusive of the top value,
       // so add 1 to make it inclusive
       long randomNum = (long) (rand.nextDouble()*((max - min) + 1)) + min;

       return randomNum;
   }
    
   public static long randLong(long min, long max, Random rand) {

       // Usually this can be a field rather than a method variable
       //Random rand = new Random(seed);

       // nextInt is normally exclusive of the top value,
       // so add 1 to make it inclusive
       long randomNum = (long) (rand.nextDouble()*((max - min) + 1)) + min;

       return randomNum;
   }
   public static long randLong(long min, long max) {

       // Usually this can be a field rather than a method variable
       Random rand = new Random();

       // nextInt is normally exclusive of the top value,
       // so add 1 to make it inclusive
       long randomNum = (long) (rand.nextDouble()*((max - min) + 1)) + min;

       return randomNum;
   }
   
      //deprecated
    public static int generateRandom(int n) {
        Random random = new Random();

        if (n == 1) {   
            if (generateRandom(50) > 50) {
                return 1;
            }
            return 0;
        }
        return Math.abs(random.nextInt()) % n;
    }
    
   /**
     * Parses external ip from websites
     * @return string
     *              if ok
     *          null
     *              is smth went wrong
     */
    public static String getExternalIp() {
        String IP_GETTER1_URL = "http://api.externalip.net/ip";
        String IP_GETTER2_URL = "http://checkip.amazonaws.com/";
        
        BufferedReader reader = null;
        String line = "";
        int tries = 0;
        do {	
            try {
                    reader = new BufferedReader(new InputStreamReader(new URL(IP_GETTER1_URL).openStream()));
                    line = reader.readLine();
                    
            } catch (FileNotFoundException fne) {
                    printLog("Functions:[getExternalIp] ERR - File not found for url: " + IP_GETTER1_URL);
                    return null;
                    
            } catch (IOException ioe) {
                    printLog("Functions:[getExternalIp] ERR - Got IO Exception, tries = " + (tries + 1)+"; Message: " + ioe.getMessage());
                    tries++;
                    try {
                            Thread.currentThread().sleep(300000);
                    } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                    }
                    continue;
                    
            } catch (Exception exc) {
                    exc.printStackTrace(System.out);
            }
        } while (reader == null && tries < 5);

        if (line != null && line.length() > 0) {
            printLog("Functions:[getExternalIp] INF - Your external ip address is: " + line);
        
        }        else {
            printLog("Functions:[getExternalIp] INF - couldn't get your ip address");
        }

        return line;
    }
    
        public static String humanDayOfWeek(int DefaultDayOfWeek){
        if(DefaultDayOfWeek>0) DefaultDayOfWeek -= 1;
        String[] namesOfDays = { "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        return namesOfDays[DefaultDayOfWeek];        
    }
    
    public static int humanDayOfWeek(String DefaultDayOfWeek){
        
        if(DefaultDayOfWeek.equalsIgnoreCase("MON")) return 1;
        else if(DefaultDayOfWeek.equalsIgnoreCase("TUE")) return 2;
        else if(DefaultDayOfWeek.equalsIgnoreCase("WED")) return 3;
        else if(DefaultDayOfWeek.equalsIgnoreCase("THU")) return 4;
        else if(DefaultDayOfWeek.equalsIgnoreCase("FRI")) return 5;
        else if(DefaultDayOfWeek.equalsIgnoreCase("SAT")) return 6;
        else if(DefaultDayOfWeek.equalsIgnoreCase("SUN")) return 7;
        
        return 0;
        
    }
    
    public static int subtractHumanDay(Calendar startDay, Calendar endDay){
        int start, end;
        start = humanDayOfWeek(humanDayOfWeek(startDay.get(Calendar.DAY_OF_WEEK)));
        end = humanDayOfWeek(humanDayOfWeek(endDay.get(Calendar.DAY_OF_WEEK)));
        if(start > end) return (7 - (start - end));
        else return (end - start);
    }

}
