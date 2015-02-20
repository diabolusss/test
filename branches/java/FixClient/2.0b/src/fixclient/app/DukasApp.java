package fixclient.app;

import fixclient.protocol.DukasProtocol;
import functions.Functions;

//import org.custom.Functions;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
     
public class DukasApp extends Thread{
    public static SessionSettings settings = null;
    public SocketInitiator initiator;
    
    public boolean started = false;
    
    public DukasApp(SessionSettings settings){
        this.settings = settings;
    }
     
    @Override
    public void run() {
        while (true){
            try{
                if (!this.started) {
                    boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true")).booleanValue();
     
                    MessageStoreFactory messageStoreFactory = new FileStoreFactory(this.settings);
                    LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
                    DefaultMessageFactory messageFactory = new DefaultMessageFactory();
                    
                    Application application = null;
                    
                    try {
                        application = new DukasProtocol(DukasApp.settings);
                        
                    } catch (ConfigError ex){
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - [ConfigError0] " + ex.getMessage());                        
                    } catch (FieldConvertError ex) {
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - [FieldConvertError] " + ex.getLocalizedMessage());
                    }
                    
                    try{
                        this.initiator = new SocketInitiator(application, messageStoreFactory, this.settings, logFactory, messageFactory);
               
                    } catch (ConfigError ex) {
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - [ConfigError1]" + ex.getMessage());
                    }
     
                    try{
                        this.initiator.start();
                        this.started = true;
                        Functions.printLog("FIXAPPDUKAS:[run] INF - FixApplication Started!");
                    
                    } catch (ConfigError ex){
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - [ConfigError2]" + ex.getMessage());
                    }catch(RuntimeError ex) {
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - [RuntimeError]" + ex.getLocalizedMessage());                        
                    }
                }
     
                Thread.sleep(10000L);
            } catch (InterruptedException ex) {
                Functions.printLog("FIXAPPDUKAS:[run] ERR - [InterruptedException]" + ex.getLocalizedMessage());
            }
            
        }//end of while
    }
}