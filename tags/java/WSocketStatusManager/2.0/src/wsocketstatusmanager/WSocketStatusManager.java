package wsocketstatusmanager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import wsocketstatusmanager.client.ExampleClient;
import functions.Functions;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author colt
 * 
 * TODO: Randomly close connection to check socket availability to others [with custom delay].
 */
public class WSocketStatusManager {
    public static String app_uid;
    
    //#security access data
    // Keystore with certificate created like so (in JKS format):
    //   keytool -genkey -validity 3650 -keystore "keystore.jks" -storepass "storepassword" -keypass "keypassword" -alias "default" -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
    private static String STORETYPE        = "JKS";
    private static String KEYSTORE         = "keystore.jks";
    private static String STOREPASSWORD    = "Kx716BFg";
    private static String KEYPASSWORD      = "Kx716BFg";
    public static  Boolean tls_enabled_server = false;
    
    public static SSLSocketFactory SecureSocketFactory;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Properties props;
        
        if (args.length == 0){
            props = Functions.getProperties(WSocketStatusManager.class.getResourceAsStream("/resources/manager.properties"));
        }else{
            props = Functions.getProperties(args[0]);
        }
        
        Long delay                      = Long.parseLong(props.getProperty("idletime_before_reset_signal"));
        String cmd                      = props.getProperty("cmd_run_on_reset");
        WSocketStatusManager.app_uid    = props.getProperty("application_uid");
        
        Properties server_props = Functions.getProperties(props.getProperty("wsocket_server_properties"));
        
        tls_enabled_server = Boolean.parseBoolean(server_props.getProperty("secured_socket"));
        STORETYPE = server_props.getProperty("tls_keystore_type");
        KEYSTORE = server_props.getProperty("tls_key");   
        String host                     = ((tls_enabled_server==true)?("wss://" ):("ws://"))+ server_props.getProperty("websocket_host_name")+":"+ server_props.getProperty("websocket_connection_port");
        
        /** SECURE PART GOES HERE **/
        if(tls_enabled_server){
            if(STORETYPE == null || KEYSTORE == null){
                Functions.printLog("MANAGER:[SECURITY] ERR - Not defined keystore or keystore type");
                return;
            }

            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance( STORETYPE );
            } catch (KeyStoreException ex) {
                Functions.printLog("KEYSTORE:[INIT] ERR - " + ex.getLocalizedMessage());
            }

            File kf = new File( KEYSTORE );
            try {
                FileInputStream keystore = new FileInputStream( kf );
                if(keystore == null){
                    Functions.printLog("KEYSTORE:[INIT] WRN - Failed to open external ks file. Opening from resources");
                    keystore = (FileInputStream) WSocketStatusManager.class.getResourceAsStream( KEYSTORE );
                }

                ks.load( keystore, STOREPASSWORD.toCharArray() );

            } catch (Exception ex) {
                Functions.printLog("KEYSTORE:[INIT] ERR - " + ex.getLocalizedMessage());
            } 

            KeyManagerFactory kmf = null;
            try {
                kmf = KeyManagerFactory.getInstance( "SunX509" );                
                kmf.init( ks, KEYPASSWORD.toCharArray() );

            } catch (Exception ex) {
                Functions.printLog("KEYMNGRFACTORY:[INIT] ERR - " + ex.getLocalizedMessage());
            }

            TrustManagerFactory tmf = null;
            try {
                tmf = TrustManagerFactory.getInstance( "SunX509" );
                tmf.init( ks );

            } catch (Exception ex) {
                Functions.printLog("TRUSTMNGRFACTORY:[INIT] ERR - " + ex.getLocalizedMessage());               
            } 

            SSLContext sslContext = null;
            
            try {
                sslContext = SSLContext.getInstance( "TLS" );            
                sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
                // sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

                Functions.printLog("SECURITY:[INIT] INF - SSL context initialized");                 
                
                SecureSocketFactory = sslContext.getSocketFactory();
                Functions.printLog("SSLFACTORY:[INIT] INF - SSL socket factory is ready");                
                

            } catch (Exception ex) {
                Functions.printLog("SSLCONTEXT:[INIT] ERR - " + ex.getLocalizedMessage());
                Functions.printLog("SECURITY:[INIT] FAILED - Secure connection is not available");                
            }            
        }
        /** SECURE PART GOES HERE **/
        
        Functions.printLog("WSocketStatusManager:[INIT] INF - cmd to exec on failure: " + cmd);
        // code application logic here
        try {     
            ExampleClient.start( new URI(host), cmd, delay);
            
        } catch (URISyntaxException ex) {
            Functions.printLog("WSocketStatusManager:[INIT] ERR - " + ex.getMessage() + "; Reason: " + ex.getReason() + "; Input[host:"+host+"]: "+ ex.getInput());
        }
        
        
    }
}