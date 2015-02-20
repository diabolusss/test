package fixclient.protocol;

import customjbdc.queries.DBMakeQuery;

import fixclient.FixClient;
import fixclient.ticks.TicksManager;

import functions.Functions;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.Message.Header;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.ClOrdID;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MassStatusReqID;
import quickfix.field.MassStatusReqType;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntries;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Password;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.field.Username;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderMassStatusRequest;
import quickfix.fix44.QuoteRequest;
import quickfix.fix44.component.Instrument;

public class DukasProtocol extends MessageCracker implements Application{
    //private static Logger log = Logger.getLogger(DukasProtocol.class);
    private static double market_point = 10000.0D;
    private String username = null;
    private String password = null;
    private static boolean subscribed = false;
    private final SessionSettings settings;
    private static SessionID trade_sid;

    public DukasProtocol(SessionSettings settings) throws ConfigError, FieldConvertError{
        this.username = settings.getString("Username");
        this.password = settings.getString("Password");
        this.settings = settings;
    }

    @Override
    public void onCreate(SessionID sid){
        Functions.printLog("FIXPROTODUKAS:[OnCreate] INF - " + sid.toString());
    }
    
    @Override
    public void onLogon(SessionID sid){
        subscribed = false;
        Functions.printLog("FIXPROTODUKAS:[onLogon] INF - " + sid.toString());
    }

    @Override
    public void onLogout(SessionID sid){
        subscribed = false;
        Functions.printLog("FIXPROTODUKAS:[onLogout] INF - "  + sid.toString());
        //TicksManager.broker_status = 0;
    }

    @Override
    public void toAdmin(Message msg, SessionID sid){
        if (isMessageOfType(msg, "A")) {
            addLogonField(msg);
        }

        if ((isMessageOfType(msg, "0")) && (!isTrade(sid)))
            if (!subscribed){   
                MDReqID mdreqid = new MDReqID();
                mdreqid.setValue("quotesrequest");
                
                MarketDataRequest req = new MarketDataRequest(mdreqid, new SubscriptionRequestType('1'), new MarketDepth(1));

                MarketDataRequest.NoMDEntryTypes group = new MarketDataRequest.NoMDEntryTypes();
                group.setField(new MDEntryType('0'));
                req.addGroup(group);
                
                group.setField(new MDEntryType('1'));
                req.addGroup(group);

                QuoteRequest.NoRelatedSym related = new QuoteRequest.NoRelatedSym();
                related.set(new Instrument(new Symbol("EUR/USD")));
                req.addGroup(related);
                req.set(new MDUpdateType(0));
                req.setField(new MsgSeqNum(3));
                
                try{
                    Session.sendToTarget(req, sid);
                    subscribed = true;
                    TicksManager.broker_status = 1;
                
                } catch (SessionNotFound ex) {
                    Functions.printLog("FIXPROTODUKAS:[toAdmin] WRN - Error " + ex.getLocalizedMessage());
                }
            
            } else {
                TicksManager.broker_status = 1;
            }
    }

    @Override
    public void fromAdmin(Message msg, SessionID sid) 
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon{        
         Functions.printLog("FIXPROTODUKAS:[fromAdmin] INF - " + msg.toString().replace('\001', '|'));
    }

    @Override
    public void toApp(Message msg, SessionID sid)
    throws DoNotSend{
        Functions.printLog("FIXPROTODUKAS:[toApp] INF - " + msg.toString().replace('\001', '|'));
    }

    @Override
    public void fromApp(Message msg, SessionID sid)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType{
        //MarketDataSnapshotFullRefresh
        if (isMessageOfType(msg, "W")) {
            crack(msg, sid);
        }
        //ExecutionReport
        else if (isMessageOfType(msg, "8")) {
            crack(msg, sid);
        }        

        else{
            //h-TradingSessionStatus, 
            //AI-QuoteStatusReport, 
            //3 - Reject, g - TradingSessionStatusRequest, u{1,2,3}
            if (isMessageOfType(msg, "U3") || isMessageOfType(msg, "U1") || isMessageOfType(msg, "U2")){
                crack(msg, sid);
                
            } else{
                Functions.printLog("FIXPROTODUKAS:[fromApp] WRN - unimplemented msg: "+msg.toString().replace('\001', '|'));
            }
        }
    }
    
    @Override 
    public void onMessage(quickfix.Message msg, SessionID sid) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        
        // Instrument Position Info (MsgType = ‘U3’)
        if (isMessageOfType(msg, "U3")){
            Functions.printLog("FIXPROTODUKAS:[fromApp] INF - U3: " + msg.toString().replace('\001', '|')); 
            DBMakeQuery.updateCurrentStake(FixClient.jdbc, null, msg.getString(7008), msg.getString(44));
        }
        
        //Notification - The message is used for client information about current account state.
        else if(isMessageOfType(msg, "U1")){
            Functions.printLog("FIXPROTODUKAS:[fromApp] INF - U1: " + msg.toString().replace('\001', '|'));
        }
        
        //Account Info - The message is used for client information about current account state.
        else if(isMessageOfType(msg, "U2")){
            Functions.printLog("FIXPROTODUKAS:[fromApp] INF - U2: " + msg.toString().replace('\001', '|'));
            DBMakeQuery.updateAccountStatusInfo(FixClient.jdbc, null, msg.getString(7005), msg.getString(7006), msg.getString(7007));
        }     
        
    }
    
    @Override
    public void onMessage(MarketDataSnapshotFullRefresh msg, SessionID sid) throws FieldNotFound{
        msg.getField(new NoMDEntries());
        MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();

        MDEntryPx MDEntryPx = new MDEntryPx();

        msg.getGroup(1, group);
        double offer = group.get(MDEntryPx).getValue();

        msg.getGroup(2, group);
        double bid = group.get(MDEntryPx).getValue();

        Double price = (offer + bid) / 2.0D;

        //get rid of tail after operation
        price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
               
        //round to defined digit count
        price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(TicksManager.PRICE_PRECISION, RoundingMode.HALF_DOWN).doubleValue();
        
        TicksManager.newTick(price);
        DBMakeQuery.updateCurrentPrice(FixClient.jdbc, null, price.toString());
        
    }

    @Override
    public void onMessage(ExecutionReport msg, SessionID sid)
    throws FieldNotFound{
        String clordeID = msg.getClOrdID().getValue();
        OrdStatus ordstatus = msg.getOrdStatus();
        double price = msg.getAvgPx().getValue();
        String execid = msg.getExecID().getValue();        

        if (ordstatus.valueEquals('2')) {
            DBMakeQuery.setOrderFilled(FixClient.jdbc, null,clordeID, execid, price);
        }
        
        if (ordstatus.valueEquals('8')) {
            String error = msg.getText().getValue();
            DBMakeQuery.setOrderFilled(FixClient.jdbc, null,clordeID, error);
        }
        Functions.printLog("FIXPROTODUKAS:[onMessage] INF - ExecutionReport: " + msg.toString().replace('\001', '|'));
    }

    private void addLogonField(Message message){
        message.getHeader().setField(new Username(this.username));
        message.getHeader().setField(new Password(this.password));
    }

    private boolean isMessageOfType(Message message, String type) {
        try {
            return type.equals(message.getHeader().getField(new MsgType()).getValue()); 
        } catch (FieldNotFound e) {
        }
        return false;
    }

    private boolean isTrade(SessionID sid){
        try {
            String str = this.settings.getString(sid, "SessionName");
            if (str.equals("TRADE")) {
                trade_sid = sid;
                return true;
            }
        } catch (ConfigError ex){ 
            Functions.printLog("FIXPROTODUKAS:[isTrade] WRN - Error " + ex.getLocalizedMessage());
        } catch (FieldConvertError ex){
            Functions.printLog("FIXPROTODUKAS:[isTrade] WRN - Error " + ex.getLocalizedMessage());
        }
        return false;
    }
    
    public static void sendNewOrder(int order_id, double amount){
        Side side = null;
        if (amount < 0.0D)
            side = new Side('2');
        else {
            side = new Side('1');
        }

        double market_amount = 0.0D;
        market_amount = Math.abs(amount * market_point);//(int)Math.pow(10, TicksManager.PRICE_PRECISION));

        NewOrderSingle message = new NewOrderSingle(new ClOrdID(String.valueOf(order_id)), side, new TransactTime(new Date()), new OrdType('1'));

        message.set(new Instrument(new Symbol("EUR/USD")));
        message.set(new OrderQty(market_amount));

        Session session = Session.lookupSession(trade_sid);
        int seq = session.getExpectedSenderNum();

        try{
            session.setNextSenderMsgSeqNum(seq > 0 ? seq + 1 : 1);
        
        } catch (IOException ex) {
            Functions.printLog("FIXPROTODUKAS:[sendNewOrder] WRN - Error " + ex.getLocalizedMessage());
        }

        try{
            Session.sendToTarget(message, trade_sid);
            Functions.printLog("FIXPROTODUKAS:[sendNewOrder] INF - Sending new order " + order_id + " : " + amount);

        } catch (SessionNotFound ex) {
            Functions.printLog("FIXPROTODUKAS:[sendNewOrder] WRN - Error " + ex.getLocalizedMessage());
        }
        
        //System.out.println("FIXPROTODUKAS:[sendNewOrder] Order sended!");
    }
    
    public static void sendOrderMassStatusRequest(){   
        Long MSRID = Functions.randLong(35000, 350000);
        OrderMassStatusRequest msg               = new OrderMassStatusRequest(
                new MassStatusReqID(MSRID.toString()),
                new MassStatusReqType(7)
                );
        
        Session session = null;
        int seq = 0;
        
        try{
            session = Session.lookupSession(trade_sid);
            seq = session.getExpectedSenderNum();
            
        }catch(NullPointerException ex){
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }
        
        try{
            session.setNextSenderMsgSeqNum(seq > 0 ? seq + 1 : 1);
            
        }catch(IOException ex){
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }
        
        try{
            Session.sendToTarget(msg, trade_sid);
            Functions.printLog("FIXPROTODUKAS:[testFix] INF - Sending new msg ");
            
        }catch (SessionNotFound ex) {
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }        
    }
    
    public static void sendAccountInfoRequest(){
        quickfix.Message msg = new quickfix.Message();
        
        //8=FIX.4.4
        //1=
        msg.getHeader().setString(MsgType.FIELD, "U7");
        
        Long MSRID = Functions.randLong(35000, 350000);
        Session session = null;
        int seq = 0;
        
        try{
            session = Session.lookupSession(trade_sid);
            seq = session.getExpectedSenderNum();
            
        }catch(NullPointerException ex){
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }
        
        try{
            session.setNextSenderMsgSeqNum(seq > 0 ? seq + 1 : 1);
            
        }catch(IOException ex){
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }
        
        try{
            Session.sendToTarget(msg, trade_sid);
            Functions.printLog("FIXPROTODUKAS:[testFix] INF - Sending new msg ");
            
        }catch (SessionNotFound ex) {
            Functions.printLog("FIXPROTODUKAS:[testFix] ERR - " + ex.getLocalizedMessage());
            return;
        }
    }
}