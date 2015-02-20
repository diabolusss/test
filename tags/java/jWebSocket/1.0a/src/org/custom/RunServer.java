package org.custom;

//import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
//import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.http.Emailer;
import org.java_websocket.WebSocketImpl;
import org.wsocket.jdbc.DBConnection;
import org.wsocket.ticks.FixApplication_dukas;
import org.wsocket.ticks.TicksManager;
import quickfix.ConfigError;
import quickfix.SessionSettings;


public class RunServer {
	//private static int wsocket_port;//   = 8880;// 843 flash policy port 
        public static InetSocketAddress wsocket_port = new InetSocketAddress(0);
        
        //# db connection settings
        private static /*final*/    String  jdbc_real_name;
        public static /*final*/     String  jdbc_demo_name;
        private static /*final*/    String  jdbc_URL;
        private static /*final*/    String  db_user;
        private static /*final*/    String  db_pass;
        private  static /*final*/   int     db_max_conn;
        
        private static final String DEFAULT_SERVER_PROPERTIES_PATH = "/resources/server.properties";
        private static final String DEFAULT_FIX_PROPERTIES_PATH = "/resources/quickfix.cfg";
        
        public static Properties serverProperties = new Properties();
        
        public static DBConnection jdbc;
        public static TestServer server;
        public static TicksManager tm;
        
        public static int serverStatus = 0;
        
        //FixApplication_citygroup app = new FixApplication_citygroup(settings, tm);
        public static FixApplication_dukas dukasFixApp;
        
        public static Timer serverAutoTimer = new Timer();

	public static void main(String[] args) throws IOException, InterruptedException {		
            //debugging mode
            WebSocketImpl.DEBUG = false;

            InputStream inputStream = null;
            
            //#REGION get settings
            if (args.length == 0){
                serverProperties = getDefaultProperties(DEFAULT_SERVER_PROPERTIES_PATH);
                if(serverProperties!=null) inputStream = RunServer.class.getResourceAsStream(DEFAULT_FIX_PROPERTIES_PATH);
            }else{
                serverProperties = getPropertiesFromFile(args[0]);
                if(serverProperties!=null) inputStream = new FileInputStream(serverProperties.getProperty("quickfix_configuration"));
            }

            if(serverProperties == null || inputStream == null){
                Functions.printLog("SERVER:[GETPROP] ERR - No properties were found. Halt");
                return;
            }           
            
                SessionSettings fixSettings = null;
                
                //parse settings
                /*
                 * added 3 libss
                 * 
                 *  slf4j-log4j12
                 *  slf4j-api
                 *  log4j
                 * 
                 * otherwise throws errors on try
                 * 
                 */
                try {
                    fixSettings = new SessionSettings(inputStream);
                    //Functions.printLog("SERVER:[GETPROP] INF - Settings parsed");
                    
                    //close stream
                    try {
                        inputStream.close();

                    } catch (IOException ex) {
                        //ex.printStackTrace(System.out);
                        Functions.printLog(
                                "SERVER:[CLOSEPROP] WRN - " +
                                ex.getLocalizedMessage()
                                );

                    }
                    
                } catch (ConfigError ex) {
                    //ex.printStackTrace(System.out);
                    Functions.printLog(
                            "SERVER:[PARSEPROP] ERR - " +
                            ex.getLocalizedMessage()
                            );
                    return;                    
                }                
                
            initVariables(serverProperties);
            Functions.printLog("SERVER:[PARSEPROP] INF - Properties parsed successfully");
            //#ENDREGION get settings		

                //System.out.println("SERVER:[INIT] Creating server");
                server = new TestServer(wsocket_port);
		
		//System.out.println("SERVER:[INIT] Starting server");
		server.start();
		
		Functions.printLog( "SERVER:[INIT] INF - Server started on port: " + server.getPort() );
		
                Functions.printLog(
                        "SERVER:[INIT] Creating jdbc connection" +
                        "[URL:" + jdbc_URL +
                        "; User:" + db_user + 
                        "; Max_conn:" + db_max_conn +
                        "]"
                        );
                
                jdbc = DBConnection.getInstance(jdbc_URL, db_user, db_pass, db_max_conn);
                Connection con = jdbc.getConnection();
                jdbc.freeConnection(con);               
                
                //System.out.println("SERVER:[INIT] Creating and starting new TicksManager");
                tm = new TicksManager(serverProperties);                
                tm.start();
                
                dukasFixApp = new FixApplication_dukas(fixSettings);
                dukasFixApp.start();
                
                //serverAutoTimer.scheduleAtFixedRate(new RunServer.Delayer(tm.getLastPrice()), 0, tm.auto_delay);
                new Emailer().sendEmail("SERVER-INFO", "["+new Date().toString()+"] Server started");
        }
       
    /*
     * Fetches all available server properties 
     * @param path
     */
    static Properties getPropertiesFromFile(String path){
        Properties newProperties = new Properties();
        try {
            InputStream input = new FileInputStream(path);
            newProperties.load(input);
        }
        catch (IOException ex){
            System.out.println("[FETCHCONFIG] ERROR Cannot open and load server properties file.");
            return null;
        }
        return newProperties;                
    }
    
    static Properties getDefaultProperties(String path){
        Properties newProperties = new Properties();
        try {
            InputStream input = RunServer.class.getResourceAsStream(path);
            newProperties.load(input);
        }
        catch (IOException ex){
            System.out.println("[FETCHCONFIG] ERROR Cannot open and load server properties file.");
            return null;
        }
        return newProperties;                
    }
    
    /*
     * Sets properties 
     * 
     */
    private static void initVariables(Properties properties){
        //RunServer.wsocket_port   = Integer.parseInt(properties.getProperty("websocket_connection_port"));
        RunServer.wsocket_port = new InetSocketAddress(
                Integer.parseInt(properties.getProperty("websocket_connection_port"))
                );
        
        //# db connection settings
        RunServer.jdbc_real_name = properties.getProperty("jdbc_real_database");
        RunServer.jdbc_demo_name = properties.getProperty("jdbc_demo_database");
        RunServer.jdbc_URL       = properties.getProperty("jdbc_host")+jdbc_real_name;
        RunServer.db_user        = properties.getProperty("jdbc_username");
        RunServer.db_pass        = properties.getProperty("jdbc_password");
        RunServer.db_max_conn    = Integer.parseInt(properties.getProperty("jdbc_max_connections"));
        
    }
                 
}
