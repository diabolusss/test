/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wsocketserver.users;

import functions.Functions;

/**
 *
 * @author colt
 */
public class OrderData {
    public  long 
            orderID     = 0,
            userID      = 0,
            sparringID  = 0
            ;
    public   String 
            orderType   = null,
            gameType    = null
            ;
    public  double 
            openPrice   = 0,
            lot         = 0,
            profit      = 0,
            stopLoss    = 0,
            takeProfit  = 0,
            profitpp    = 0
            ;
    
    public OrderData(){        
    }
    
    public  void printOrderData(){
        Functions.printLog("OrderData: INF - (oid, uid, sid, ot, gt, op, lot, p, sl, tp, ppp):("+orderID+","+userID+","+sparringID+","+orderType+","+gameType+","+openPrice+","+lot+","+profit+","+stopLoss+","+takeProfit+","+profitpp+","+")");
    }
    
    /**
     * 
     * @return json string
     */
    public String parse2JSON(){        
        return "{\"oid\":"+orderID+", \"uid\":"+userID+", \"sid\":"+sparringID+", \"ot\":"+orderType+", \"gt\":"+gameType+", \"op\":"+openPrice+", \"lot\":"+lot+", \"p\":"+profit+", \"sl\":"+stopLoss+", \"tp\":"+takeProfit+", \"ppp\":"+profitpp+"}";
    }
    
}
