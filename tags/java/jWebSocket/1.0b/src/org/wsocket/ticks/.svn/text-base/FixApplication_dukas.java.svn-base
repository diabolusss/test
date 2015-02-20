/*    */ package org.wsocket.ticks;
/*    */ 
import org.custom.Functions;
/*    */ import quickfix.Application;
/*    */ import quickfix.ConfigError;
/*    */ import quickfix.DefaultMessageFactory;
/*    */ import quickfix.FieldConvertError;
/*    */ import quickfix.FileStoreFactory;
/*    */ import quickfix.LogFactory;
/*    */ import quickfix.MessageStoreFactory;
/*    */ import quickfix.RuntimeError;
/*    */ import quickfix.ScreenLogFactory;
/*    */ import quickfix.SessionSettings;
/*    */ import quickfix.SocketInitiator;
/*    */ 
/*    */ public class FixApplication_dukas extends Thread{
    
/* 29 */   public static SessionSettings settings = null;
/*    */   public SocketInitiator initiator;
/*    */ 
/* 33 */   public boolean started = false;
/*    */ 
/*    */   public FixApplication_dukas(SessionSettings settings){
/* 40 */     this.settings = settings;
/*    */   }
/*    */ 
/*    */    @Override
            public void run() {
/*    */     while (true)
/*    */       try{
/* 48 */         if (!this.started) {
/* 49 */           boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true")).booleanValue();
/*    */ 
/* 51 */           MessageStoreFactory messageStoreFactory = new FileStoreFactory(this.settings);
/* 52 */           LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
/* 53 */           DefaultMessageFactory messageFactory = new DefaultMessageFactory();

/* 54 */           Application application = null;
/*    */           try {
/* 56 */             application = new FixProtocol_dukas(this.settings);

/*    */           } catch (ConfigError ex){
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
    
                   } catch (FieldConvertError ex) {
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
                     
/*    */           }
/*    */ 
/*    */           try{
/* 65 */             this.initiator = new SocketInitiator(application, messageStoreFactory, this.settings, logFactory, messageFactory);
/*    */           } catch (ConfigError ex) {
                     Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
/*    */           }
/*    */ 
/*    */           try
/*    */           {
/* 72 */             this.initiator.start();
/* 73 */             this.started = true;
/* 74 */             Functions.printLog("FIXAPPDUKAS:[run] INF - FixApplication Started!");

/*    */           } catch (ConfigError ex){
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
                        
                   }catch(RuntimeError ex) {
                        Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
                        
/*    */           }
/*    */ 
/*    */         }
/*    */ 
/* 83 */         Thread.sleep(10000L);
/*    */       } catch (InterruptedException ex) {
                 Functions.printLog("FIXAPPDUKAS:[run] ERR - " + ex.getLocalizedMessage());
/*    */       }
/*    */   }
/*    */ }