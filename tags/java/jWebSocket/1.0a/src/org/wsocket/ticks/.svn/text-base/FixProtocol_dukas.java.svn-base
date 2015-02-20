/*     */ package org.wsocket.ticks;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
/*     */ import java.util.Date;
/*     */ import org.apache.log4j.Logger;
import org.custom.Functions;
import org.custom.RunServer;
          import quickfix.Application;
/*     */ import quickfix.ConfigError;
/*     */ import quickfix.DoNotSend;
/*     */ import quickfix.FieldConvertError;
/*     */ import quickfix.FieldNotFound;
/*     */ import quickfix.IncorrectDataFormat;
/*     */ import quickfix.IncorrectTagValue;
/*     */ import quickfix.Message;
/*     */ import quickfix.RejectLogon;
/*     */ import quickfix.Session;
/*     */ import quickfix.SessionID;
/*     */ import quickfix.SessionNotFound;
/*     */ import quickfix.SessionSettings;
/*     */ import quickfix.UnsupportedMessageType;
/*     */ import quickfix.field.ClOrdID;
/*     */ import quickfix.field.MDEntryPx;
/*     */ import quickfix.field.MDEntryType;
/*     */ import quickfix.field.MDReqID;
/*     */ import quickfix.field.MDUpdateType;
/*     */ import quickfix.field.MarketDepth;
/*     */ import quickfix.field.MsgSeqNum;
/*     */ import quickfix.field.MsgType;
/*     */ import quickfix.field.NoMDEntries;
/*     */ import quickfix.field.OrdStatus;
/*     */ import quickfix.field.OrdType;
/*     */ import quickfix.field.OrderQty;
/*     */ import quickfix.field.Password;
/*     */ import quickfix.field.Side;
/*     */ import quickfix.field.SubscriptionRequestType;
/*     */ import quickfix.field.Symbol;
/*     */ import quickfix.field.TransactTime;
/*     */ import quickfix.field.Username;
/*     */ import quickfix.fix44.ExecutionReport;
/*     */ import quickfix.fix44.MarketDataRequest;
/*     */ import quickfix.fix44.MarketDataSnapshotFullRefresh;
/*     */ //import quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries;
/*     */ import quickfix.fix44.MessageCracker;
/*     */ import quickfix.fix44.NewOrderSingle;
/*     */ //import quickfix.fix44.QuoteRequest.NoRelatedSym;
          import quickfix.fix44.QuoteRequest;
/*     */ import quickfix.fix44.component.Instrument;
/*     */ 
/*     */ class FixProtocol_dukas extends MessageCracker
/*     */   implements Application{
    
/*  54 */   private static Logger log = Logger.getLogger(FixProtocol_dukas.class);
/*  55 */   private static double market_point = 10000.0D;
/*  56 */   private String username = null;
/*  57 */   private String password = null;
/*  58 */   private TicksManager tm= RunServer.tm;
/*     */ 
/*  60 */   private static boolean subscribed = false;
/*     */   private final SessionSettings settings;
/*     */   private static SessionID trade_sid;
/*     */ 
/*     */   public FixProtocol_dukas(SessionSettings settings)
/*     */     throws ConfigError, FieldConvertError{
    
/*  66 */     this.username = settings.getString("Username");
/*  67 */     this.password = settings.getString("Password");
/*  73 */     this.settings = settings;

              //this.tm = new TicksManager(settings.getString("Mode"));
/*  78 */     //this.tm.start();
/*     */   }
/*     */ 
/*     */   @Override
            public void onCreate(SessionID sid){
/*  83 */     //log.debug("On Create: " + sid.toString());
              Functions.printLog("FIXPROTODUKAS:[OnCreate] INF - " + sid.toString());
/*     */   }
/*     */ 
/*     */   @Override
            public void onLogon(SessionID sid){
/*  88 */     subscribed = false;
/*  89 */     //log.debug("On login: " + sid.toString());
              Functions.printLog("FIXPROTODUKAS:[onLogon] INF - " + sid.toString());
/*     */   }
/*     */ 
/*     */   @Override
            public void onLogout(SessionID sid){
/*  94 */     subscribed = false;
/*  95 */     TicksManager.broker_status = 0;
/*  96 */     //log.debug("On logout: " + sid.toString());
              Functions.printLog("FIXPROTODUKAS:[onLogout] INF - "  + sid.toString());
/*     */   }
/*     */ 
/*     */   @Override
            public void toAdmin(Message msg, SessionID sid){
/* 103 */     if (isMessageOfType(msg, "A")) {
/* 104 */       addLogonField(msg);
/*     */     }
/*     */ 
/* 108 */     if ((isMessageOfType(msg, "0")) && (!isTrade(sid)))
/* 109 */       if (!subscribed){
    
/* 112 */         MDReqID mdreqid = new MDReqID();
/* 113 */         mdreqid.setValue("quotesrequest");
/*     */ 
/* 115 */         MarketDataRequest req = new MarketDataRequest(mdreqid, new SubscriptionRequestType('1'), new MarketDepth(1));
/*     */ 
/* 120 */         MarketDataRequest.NoMDEntryTypes group = new MarketDataRequest.NoMDEntryTypes();
/* 121 */         group.setField(new MDEntryType('0'));
/* 122 */         req.addGroup(group);
/*     */ 
/* 124 */         group.setField(new MDEntryType('1'));
/* 125 */         req.addGroup(group);
/*     */ 
/* 127 */         QuoteRequest.NoRelatedSym related = new QuoteRequest.NoRelatedSym();
/* 128 */         related.set(new Instrument(new Symbol("EUR/USD")));
/* 129 */         req.addGroup(related);
/*     */ 
/* 131 */         req.set(new MDUpdateType(0));
/*     */ 
/* 133 */         req.setField(new MsgSeqNum(3));
/*     */         try{
/* 136 */           Session.sendToTarget(req, sid);
/* 137 */           subscribed = true;
/* 138 */           TicksManager.broker_status = 1;

/*     */         } catch (SessionNotFound ex) {
                    Functions.printLog("FIXPROTODUKAS:[toAdmin] WRN - Error " + ex.getLocalizedMessage());
/*     */         }

/*     */       } else {
/* 143 */         TicksManager.broker_status = 1;
/*     */       }
/*     */   }
/*     */ 
/*     */   @Override
            public void fromAdmin(Message msg, SessionID sid)
/*     */     throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon{
/* 152 */     //log.debug("From admin: " + msg.toString().replace('\001', '|'));
              Functions.printLog("FIXPROTODUKAS:[fromAdmin] INF - " + msg.toString().replace('\001', '|'));
            }
/*     */ 
/*     */   @Override
            public void toApp(Message msg, SessionID sid)
/*     */     throws DoNotSend{
/* 158 */     //log.debug("To app: " + msg.toString().replace('\001', '|'));
              Functions.printLog("FIXPROTODUKAS:[toApp] INF - " + msg.toString().replace('\001', '|'));
/*     */   }
/*     */ 
/*     */   @Override
            public void fromApp(Message msg, SessionID sid)
/*     */     throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType{
    
/* 164 */     if (isMessageOfType(msg, "W")) {
/* 165 */       crack(msg, sid);
/*     */     }
/*     */ 
/* 169 */     if (isMessageOfType(msg, "8")) {
/* 170 */       crack(msg, sid);
/*     */     }
/*     */ 
/* 173 */     if (isMessageOfType(msg, "U3")){
/* 174 */       //log.debug(
                Functions.printLog("FIXPROTODUKAS:[fromApp] INF - U3: " + msg.toString().replace('\001', '|'));
              }
/*     */   }
/*     */ 
/*     */   @Override
            public void onMessage(MarketDataSnapshotFullRefresh msg, SessionID sid)
/*     */     throws FieldNotFound{
    
/* 186 */     msg.getField(new NoMDEntries());
/* 187 */     MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
/*     */ 
/* 190 */     MDEntryPx MDEntryPx = new MDEntryPx();
/*     */ 
/* 192 */     msg.getGroup(1, group);
/* 193 */     double offer = group.get(MDEntryPx).getValue();
              
/* 195 */     msg.getGroup(2, group);
/* 196 */     double bid = group.get(MDEntryPx).getValue();
/*     */ 
/* 199 */     double price = (offer + bid) / 2.0D;
              
              //get rid of tail after operation
/* 200 */     price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
              //round to defined digit count
              price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();

/* 203 */     TicksManager.newTick(price);
/*     */   }
/*     */ 
/*     */   @Override
            public void onMessage(ExecutionReport msg, SessionID sid)
/*     */     throws FieldNotFound{
    
/* 209 */     String clordeID = msg.getClOrdID().getValue();
/* 210 */     OrdStatus ordstatus = msg.getOrdStatus();
/* 211 */     double price = msg.getAvgPx().getValue();
/* 212 */     String execid = msg.getExecID().getValue();
/* 213 */     String error = msg.getText().getValue();
/*     */ 
/* 215 */     if (ordstatus.valueEquals('2')) {
/* 216 */       TicksManager.setOrderFilled(clordeID, execid, price);
/*     */     }
/*     */ 
/* 219 */     if (ordstatus.valueEquals('8')) {
/* 220 */       TicksManager.setOrderFilled(clordeID, error);
/*     */     }
              Functions.printLog("FIXPROTODUKAS:[onMessage] INF - ExecutionReport: " + msg.toString().replace('\001', '|'));
/*     */   }
/*     */ 
/*     */   private void addLogonField(Message message){
    
/* 228 */     message.getHeader().setField(new Username(this.username));
/* 229 */     message.getHeader().setField(new Password(this.password));
/*     */   }
/*     */ 
/*     */   private boolean isMessageOfType(Message message, String type) {
/*     */     try {
/* 234 */       return type.equals(message.getHeader().getField(new MsgType()).getValue()); 
              } catch (FieldNotFound e) {
/*     */     }
/* 236 */     return false;
/*     */   }
/*     */ 
/*     */   private boolean isTrade(SessionID sid){
/*     */     try {
/* 242 */       String str = this.settings.getString(sid, "SessionName");
/* 243 */       if (str.equals("TRADE")) {
/* 244 */         trade_sid = sid;
/* 245 */         return true;
/*     */       }
/*     */     } catch (ConfigError ex){ 
                Functions.printLog("FIXPROTODUKAS:[isTrade] WRN - Error " + ex.getLocalizedMessage());
    
              } catch (FieldConvertError ex){
                Functions.printLog("FIXPROTODUKAS:[isTrade] WRN - Error " + ex.getLocalizedMessage());
                
/*     */     }
/*     */ 
/* 253 */     return false;
/*     */   }
/*     */ 
/*     */   public static void sendNewOrder(int order_id, double amount){
    
/* 258 */     Side side = null;
/* 259 */     if (amount < 0.0D)
/* 260 */       side = new Side('2');
/*     */     else {
/* 262 */       side = new Side('1');
/*     */     }
/*     */ 
/* 266 */     double market_amount = 0.0D;
/* 267 */     market_amount = Math.abs(amount * market_point);
/*     */ 
/* 270 */     NewOrderSingle message = new NewOrderSingle(new ClOrdID(String.valueOf(order_id)), side, new TransactTime(new Date()), new OrdType('1'));
/*     */ 
/* 277 */     message.set(new Instrument(new Symbol("EUR/USD")));
/* 278 */     message.set(new OrderQty(market_amount));
/*     */ 
/* 281 */     Session session = Session.lookupSession(trade_sid);
/* 282 */     int seq = session.getExpectedSenderNum();

/*     */     try{
/* 285 */       session.setNextSenderMsgSeqNum(seq > 0 ? seq + 1 : 1);

/*     */     } catch (IOException ex) {
                Functions.printLog("FIXPROTODUKAS:[sendNewOrder] WRN - Error " + ex.getLocalizedMessage());
/*     */     }

/*     */     try{
/* 291 */       Session.sendToTarget(message, trade_sid);
/* 292 */       //log.debug("
                Functions.printLog("FIXPROTODUKAS:[sendNewOrder] INF - Sending new order " + order_id + " : " + amount);
/*     */     
              } catch (SessionNotFound ex) {
/* 294 */       //log.debug(ex.toString());
                Functions.printLog("FIXPROTODUKAS:[sendNewOrder] WRN - Error " + ex.getLocalizedMessage());

/*     */     }
              //System.out.println("FIXPROTODUKAS:[sendNewOrder] Order sended!");
/*     */   }
/*     */ }