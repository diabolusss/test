/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package customjbdc.queries;

import customjbdc.connection.DBConnection;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import functions.Functions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import wsocketserver.users.Users;

/**
 *
 * @author colt
 */
public class DBMakeQuery {
    public static void test(DBConnection jdbc_connection, String dbName){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;        
        if(dbName == null || dbName.isEmpty()) query = "SELECT `hash`, `check` FROM `accounts` WHERE `online` = 1";
        else query = "SELECT `hash`, `check` FROM `"+dbName+"`.`accounts` WHERE `online` = 1";
        
        try {
            Statement st = con.createStatement();
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[test:"+dbName+"] ERR - result is null"
                        );
                return ;                
            }       
            
            
            while(rs.next()){
                ResultSetMetaData rs_meta = rs.getMetaData();
                Functions.printLog("getColumnCount="
                            +rs_meta.getColumnCount()
                        );
                for(int i=1; i<rs_meta.getColumnCount()+1; i++){
                    System.out.println(
                            "ColumnName:"+rs_meta.getColumnName(i)
                            +"; Value:"+rs.getString(rs_meta.getColumnName(i))
                            );

                }
            }           
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[test:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            return ;
        }
    }
    
    public static String checkUserByHash(DBConnection jdbc_connection, String dbName, String hash){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return null;
        
        String query = null;        
        if(dbName == null || dbName.isEmpty()) query = "SELECT accounts.`hash`, user.`id`, user.`name`, user.`role`, user.`ip` FROM  `user` INNER JOIN `accounts` ON user.`id` = accounts.`id` WHERE  accounts.`hash` = '" + hash + "'";
        else query = "SELECT accounts.`hash`, user.`id`, user.`name`, user.`role`, user.`ip` FROM  "+dbName+".`user` INNER JOIN "+dbName+".`accounts` ON user.`id` = accounts.`id` WHERE  accounts.`hash` = '" + hash + "'";
        
        try {
            Statement st = con.createStatement();
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[checkUserByHash:"+dbName+"] ERR - result is null"
                        );
                return null;                
            }  
            
            query = null;
            if (rs.next()) {
                query = rs.getInt("id")+"#"+rs.getString("name")+"#"+rs.getString("role")+"#"+rs.getString("ip");
                Functions.printLog(
                        "MKQUERY:[checkUserByHash:"+dbName+"] INF - result[(id,name,role,ip):"+query+"]"
                     );
            }
            
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[checkUserByHash:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            return null;
        }
        
        return query;
    }
    
    public static ConcurrentHashMap<String, ArrayList<ArrayList<Double>>> getOpenOrders(DBConnection jdbc_connection, String dbName1, String dbName2){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return null;
        
        String query = null;      
        if(dbName2 == null || dbName2.isEmpty()) query = "select type, stop_loss, take_profit from orders where status=\"open\" union select type, stop_loss, take_profit from `"+dbName1+"`.orders where status=\"open\" order by type, stop_loss asc";
        else query = "select type, stop_loss, take_profit from `"+dbName2+"`.orders where status=\"open\" union select type, stop_loss, take_profit from `"+dbName1+"`.orders where status=\"open\" order by type, stop_loss asc";
        //if(dbName == null || dbName.isEmpty()) query = "select type, stop_loss, take_profit from orders where status=\"open\" order by type, stop_loss desc";
        //else query = "select type, stop_loss, take_profit from `"+dbName+"`.orders where status=\"open\" order by type, stop_loss desc";
        ConcurrentHashMap<String, ArrayList<ArrayList<Double>>> totalOrderTypeBreakPoints = new ConcurrentHashMap();
        
        try {
            Statement st = con.createStatement();
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getOpenOrders:"+dbName1+":"+dbName2+"] ERR - result is null"
                        );
                return null;                
            }              
            
            ArrayList<ArrayList<Double>> sellTypeBreakPoints = new ArrayList<ArrayList<Double>>();
            ArrayList<ArrayList<Double>> buyTypeBreakPoints = new ArrayList<ArrayList<Double>>();
            
            /*String type */query = null;
            Double 
                    loss    = .0,
                    profit  = .0
                    ;
            
            while (rs.next()) {
                try{
                    query   = rs.getString("type");
                    loss    = rs.getDouble("stop_loss");
                    profit  = rs.getDouble("take_profit");
                    
                    if(query.equalsIgnoreCase("buy")){
                        buyTypeBreakPoints.add(new ArrayList<Double>(Arrays.asList(loss, profit)));
                        
                    }else if(query.equalsIgnoreCase("sell")){
                        sellTypeBreakPoints.add(new ArrayList<Double>(Arrays.asList(loss, profit)));
                    }
                    
                }catch(SQLException ex){
                    Functions.printLog("MKQUERY:[getOpenOrders:"+dbName1+":"+dbName2+"] ERR(type:"+query+",loss:"+loss+",profit:"+profit+") - " + ex.toString());
                }                
            }
            if ( !buyTypeBreakPoints.isEmpty() ){
                totalOrderTypeBreakPoints.put("buy", buyTypeBreakPoints);
            }
            if ( !sellTypeBreakPoints.isEmpty() ){
                totalOrderTypeBreakPoints.put("sell", sellTypeBreakPoints);
            }
            if ( totalOrderTypeBreakPoints.isEmpty() ){
                Functions.printLog("MKQUERY:[getOpenOrders:"+dbName1+":"+dbName2+"] INF - No active orders.");
                return null;
            }
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[getOpenOrders:"+dbName1+":"+dbName2+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            return null;
        }
        
        return totalOrderTypeBreakPoints;
    }
    
    public static void checkUserOrderStatus(DBConnection jdbc_connection, String dbName, int id){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;        
        if(dbName == null || dbName.isEmpty()) query = "Select accounts.check, orders.id, count(1) as opened from accounts,orders where orders.account_id="+id+" and orders.status='open' and orders.account_id=accounts.id";
        else query = "Select accounts.check,orders.id,count(1) as opened from "+dbName+".accounts,"+dbName+".orders where orders.account_id="+id+" and orders.status='open' and orders.account_id=accounts.id";
        
        try {
            Statement st = con.createStatement();
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[checkUserOrderStatus:"+dbName+"] ERR - result is null"
                        );
                return ;                
            }
            
            if (rs.next()) { 
                
                
                for (String key : Users.UserList.keySet()) {
                    Users user = (Users)Users.UserList.get(key);
                    
                    if(user != null) if(user.id == id){
                        user.check      = rs.getInt("check");
                        user.order_open = rs.getInt("id");                       
                        
                        Functions.printLog(
                                "MKQUERY:[checkUserOrderStatus:"+dbName+"] INF - result ["
                                +"id:"+user.id
                                +"; check:"+user.check
                                +"; order_open:"+user.order_open
                                + "]"
                                );
                        
                        Users.UserList.replace(key, user);   
                        break;
                    }
                }                
            }
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[checkUserOrderStatus:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            return ;
        }
    }
    
    public static void checkOnlineAccountHash(DBConnection jdbc_connection, String dbName){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;        
        if(dbName == null || dbName.isEmpty()) query = "SELECT `hash`, `check` FROM `accounts` WHERE `online` = 1";
        else query = "SELECT `hash`, `check` FROM `"+dbName+"`.`accounts` WHERE `online` = 1";
        
        try {
            Statement st = con.createStatement();
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[checkOnlineAccountHash:"+dbName+"] ERR - result is null"
                        );
                return ;                
            }
            
            while (rs.next()) { 
                /*Functions.printLog(
                                "MKQUERY:[checkOnlineAccountHash:"+dbName+"] INF - result [check:"
                                +rs.getInt("check")
                                +rs.getString("hash")
                                + "]"
                                );*/
                
                for (String key : Users.UserList.keySet()) {
                    Users user = (Users)Users.UserList.get(key);
                    if(user != null) if (user.hash.equals(rs.getString("hash"))) {
                        user.check = rs.getInt("check");
                        Users.UserList.replace(key, user);                        
                    }
                }                
            }
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[checkOnlineAccountHash:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            return ;
        }
    }
    
    public static String callGetForMarketOpen(DBConnection jdbc_connection, String dbName){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return null;
        
        int count = 0;
        
        String query = null;        
        if(dbName == null || dbName.isEmpty()) query = "{call get_for_market_open()}";
        else query = "{call `"+dbName+"`.get_for_market_open()}";
        
        try {
            CallableStatement pc = con.prepareCall(query);
            pc.executeUpdate();
                    
            ResultSet rs = pc.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[callGetForMarketOpen:"+dbName+"] ERR - result is null"
                        );
                return null;                
            }
            
            query = null;
            while (rs.next()) {
                count = rs.getInt("count");
                if (count > 0) {
                    query = rs.getString("count")+"#"+rs.getString("amount")+"#"+rs.getString("clordid");
                }
                
                Functions.printLog(
                                "MKQUERY:[callGetForMarketOpen:"+dbName+"] INF - result [" 
                    +query
                    + "]"
                                );
            }
            
            rs.close();
            pc.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[callGetForMarketOpen:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query
                    );
            jdbc_connection.release();
            
            return null;            
        }
        
        return query;
    }
    
    public static void forceCloseOrdersByStatus(DBConnection jdbc_connection, String dbName, String status){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;
        int order_id = 0;
        
        try {
            Statement st = con.createStatement();
            
            if(dbName == null || dbName.isEmpty()) query = "SELECT `id` FROM `orders` WHERE `status`='"+status+"'";
            else query = "SELECT `id` FROM `"+dbName+"`.`orders` WHERE `status`='"+status+"'";
            
            st.executeQuery(query);
            
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[force_close_order_by_id:"+dbName+"] ERR - result1 is null"
                        );
                return ;                
            }
            
            while (rs.next()) {
                order_id = rs.getInt("id");
                
                if(dbName == null || dbName.isEmpty()) query = "{call force_close_order_by_id(?)}";
                else query = "{call `"+dbName+"`.force_close_order_by_id(?)}";
                
                CallableStatement pc = con.prepareCall(query);                
                pc.setInt(1, order_id);
                pc.executeUpdate();
                
                ResultSet o_rs = pc.getResultSet();
                
                if (o_rs == null) {
                    Functions.printLog(
                            "MKQUERY:[force_close_order_by_id:"+dbName+"] ERR - result2 is null"
                            );
                    return ;                
                }
                
                Functions.printLog(
                                "MKQUERY:[force_close_order_by_id:"+dbName+"] INF - result [id:" 
                    +order_id+ "]"
                                );
                
                o_rs.close();
                pc.close();
            }
            
            rs.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[force_close_order_by_id:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query + " : " + order_id
                    );
            jdbc_connection.release();
        }
        
        
    }
    
    public static void closeOrdersByStatus(DBConnection jdbc_connection, String dbName, String status){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;
        int order_id = 0;
        
        try {
            Statement st = con.createStatement();
            
            if(dbName == null || dbName.isEmpty()) query = "SELECT `id` FROM `orders` WHERE `status`='"+status+"'";
            else query = "SELECT `id` FROM `"+dbName+"`.`orders` WHERE `status`='"+status+"'";
            
            st.executeQuery(query);
            
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[closeOrdersByStatus:"+dbName+"] ERR - result1 is null"
                        );
                return ;                
            }
            
            while (rs.next()) {
                order_id = rs.getInt("id");
                
                if(dbName == null || dbName.isEmpty()) query = "{call close_order_by_id(?)}";
                else query = "{call `"+dbName+"`.close_order_by_id(?)}";
                
                CallableStatement pc = con.prepareCall(query);                
                pc.setInt(1, order_id);
                pc.executeUpdate();
                
                ResultSet o_rs = pc.getResultSet();
                
                if (o_rs == null) {
                    Functions.printLog(
                            "MKQUERY:[closeOrdersByStatus:"+dbName+"] ERR - result2 is null"
                            );
                    return ;                
                }
                
                /* No processing here. DO WE REALLY NEED THIS PART?!
                if (o_rs.next()) {
                    try{
                        String[] res = o_rs.getString("vResult").split(":");
                        if (!res[0].equals("OK")) {
                            Functions.printLog("MKQUERY:[closeOrdersByStatus:"+dbName+"] WRN - Error in call close_order_by_id: " + res[1]);
                        }
                    }catch(SQLException ex){
                        Functions.printLog(
                        "MKQUERY:[closeOrdersByStatus:"+dbName+"] ERR - " + ex.toString()
                        +"; " + query + " : " + order_id
                        );
                    }
                }*/
                
                Functions.printLog(
                                "MKQUERY:[closeOrdersByStatus:"+dbName+"] INF - result [id:" 
                    +order_id+ "]"
                                );
                
                o_rs.close();
                pc.close();
            }
            
            rs.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog(
                    "MKQUERY:[closeOrdersByStatus:"+dbName+"] ERR - " + ex.toString()
                    +"; " + query + " : " + order_id
                    );
            jdbc_connection.release();
        }
        
        
    }
    
    public static int callTick(DBConnection jdbc_connection, String dbName, double price){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return 0;
        
        int bar_id = 0;
        
        String query = null;
        
        if(dbName == null || dbName.isEmpty()) query = "{call tick(?)}";
        else query = "{call `"+dbName+"`.tick(?)}";
        
        try {
            CallableStatement pc = con.prepareCall(query);
            pc.setDouble(1, price);
            pc.executeUpdate();
            
            ResultSet rs = pc.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[callTick:"+dbName+"] ERR - result is null"
                        );
                return 0;                
            }
            
            while (rs.next()) {
                String[] res = rs.getString("vResult").split(":");
                if (res[0].equals("OK"))
                    bar_id = Integer.parseInt(res[1]);
                else {
                    Functions.printLog("MKQUERY:[callTick:"+dbName+"] WRN - Error in call ticks: " + res[1]);
                }
                
                Functions.printLog(
                                "MKQUERY:[callTick:"+dbName+"] INF - result [vResult:" 
                    +rs.getString("vResult")
                    + "]"
                                );
            }
            
            rs.close();
            pc.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog("MKQUERY:[callTick:"+dbName+"] ERR - " + ex.getLocalizedMessage());
            jdbc_connection.release();
            
            return 0;            
        }
        
        return bar_id;
        
    }
    
    public static void callAutoCloseOrders(DBConnection jdbc_connection, String dbName, double price){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return ;
        
        String query = null;
        
        if(dbName == null || dbName.isEmpty()) query = "{call auto_close_orders(?)}";
        else query = "{call `"+dbName+"`.auto_close_orders(?)}";
        
        try {
            CallableStatement pc = con.prepareCall(query);
            pc.setDouble(1, price);
            pc.executeUpdate();
            
            ResultSet rs = pc.getResultSet();
            
            if (rs == null) {
                Functions.printLog("MKQUERY:[callAutoCloseOrders:"+dbName+"] ERR - result is null");
                return ;                
            }
            
            while (rs.next()) {
                Functions.printLog("MKQUERY:[callAutoCloseOrders:"+dbName+"] INF - Result["+rs.getString("vResult")+"]");
            }
            
            rs.close();
            pc.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog("MKQUERY:[callAutoCloseOrders:"+dbName+"] ERR - " + ex.getLocalizedMessage());
            jdbc_connection.release();
            
            return ;            
        }
        
        return ;          
    }
    
    public static int callIdleTick(DBConnection jdbc_connection, String dbName, double price){
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return 0;
        
        int bar_id = 0;
        
        String query = null;
        
        if(dbName == null || dbName.isEmpty()) query = "{call idle_tick(?)}";
        else query = "{call `"+dbName+"`.idle_tick(?)}";
        
        try {
            CallableStatement pc = con.prepareCall(query);
            pc.setDouble(1, price);
            pc.executeUpdate();
            
            ResultSet rs = pc.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[callIdleTick:"+dbName+"] ERR - result is null"
                        );
                return 0;                
            }
            
            while (rs.next()) {
                String[] res = rs.getString("vResult").split(":");
                
                if (res[0].equals("OK")){
                    bar_id = Integer.parseInt(res[1]);
                    Functions.printLog("MKQUERY:[callIdleTick:"+dbName+"] INF - Result OK");
                }else {
                    Functions.printLog("MKQUERY:[callIdleTick:"+dbName+"] WRN - Error in call new tick: " + res[1]);
                }                
                
            }
            
            rs.close();
            pc.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException ex) {
            Functions.printLog("MKQUERY:[callTick:"+dbName+"] ERR - " + ex.getLocalizedMessage());
            jdbc_connection.release();
            
            return 0;            
        }
        
        return bar_id;          
    }
     
    public static int getServerStatus (DBConnection jdbc_connection, String dbName) {
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return 0;
        
        int status = 0;         
        int id = 0;        
        String query = null;          
        
        try {          
            Statement st = con.createStatement();
            
            if(dbName == null || dbName.isEmpty()) query = "SELECT `status`, `id` FROM `server_status` WHERE `status` = 1 and UTC_TIMESTAMP between `start` and `stop`";
/* 370 */   
            else query = "SELECT `status`, `id` FROM `"+dbName+"`.`server_status` WHERE `status` = 1 and UTC_TIMESTAMP between `start` and `stop`";
            
            st.executeQuery(query);
            
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getServerStatus:"+dbName+"] ERR - result is null"
                        );
                return 0;                
            }
            
            while (rs.next()) {
                status = rs.getInt("status");
                
                Functions.printLog(
                                "MKQUERY:[getServerStatus:"+dbName+"] INF - result [status:" 
                    +rs.getInt("status")
                    + "]"
                                );
            }
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException e) {        
            Functions.printLog(
                      "MKQUERY:[getServerStatus:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                      "[" + query+ "]"
                    );
            jdbc_connection.release();
            
            return 0;            
        }             
        
        return status;       
    }
            
    
    public static String getServerTimeStatus (DBConnection jdbc_connection, String dbName) {
        Connection con = jdbc_connection.getConnection();        
        if (con == null) return null;
      
        String query = null;  
        
        try {
            Statement st = con.createStatement();
            if(dbName == null || dbName.isEmpty()) query = "SELECT `status`, `id` FROM `server_status` WHERE `status` = 1 and UTC_TIMESTAMP between `start` and `stop`";
/* 371 */   
            else query = "SELECT `status`, `id` FROM `"+dbName+"`.`server_status` WHERE `status` = 1 and UTC_TIMESTAMP between `start` and `stop`";

            st.executeQuery(query);
            
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getServerStatus:"+dbName+"] ERR - result is null"
                        );
                
                return null;                
            }
            
            query = null;
            while (rs.next()) {
                query = rs.getString("status")+rs.getString("id")+"#";
                
                Functions.printLog(
                                "MKQUERY:[getServerStatus:"+dbName+"] INF - result [status:" 
                    +rs.getString("status") + "; id:"
                    +rs.getString("id")
                    + "]"
                                );
            }
            
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        } catch (SQLException e) { 
            jdbc_connection.release();
            Functions.printLog(
                      "MKQUERY:[getServerStatus:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                      "[" + query+ "]"
                    );
            
            return null;
        }
        
        return query;        
    }
            
            
    public static double getLastPrice (DBConnection jdbc_connection, String dbName) {
        Connection con = jdbc_connection.getConnection();
        if (con == null) return 0;
        
        double last_price = 0; 
        String query = null;

/*     */       try {
/* 369 */         Statement st = con.createStatement();
/* 370 */         if(dbName == null || dbName.isEmpty()) query = "SELECT `price` FROM `ticks` order by id desc limit 0,1";
/* 371 */         else query = "SELECT `price` FROM `"+dbName+"`.`ticks` order by id desc limit 0,1";

                  st.executeQuery(query);

/* 372 */         ResultSet rs = st.getResultSet();

                if (rs == null) {
                    Functions.printLog(
                            "MKQUERY:[getLastPrice:"+dbName+"] ERR - result is null"
                            );
                    return 0;                
                }

/* 373 */         while (rs.next()) {
/* 374 */           last_price = rs.getDouble("price");

                    Functions.printLog(
                                "MKQUERY:[getLastPrice:"+dbName+"] INF - result [" 
                            +  rs.getDouble("price") + "]"
                                );
/*     */         }

/* 376 */         rs.close();
/* 377 */         st.close();
                  jdbc_connection.freeConnection(con);
                  
        } catch (SQLException e) { 
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[getLastPrice:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
            
            return 0;
        }             
                
        return last_price;            
    }
    
    public static int getCurrBarId(DBConnection jdbc_connection, String dbName) {
        Connection con = jdbc_connection.getConnection();              
        if (con == null) return 0;
        
        int bar_id = 0;              
        String query = null;
        
        try {  
            Statement st = con.createStatement();
            if(dbName == null || dbName.isEmpty()) query = "SELECT * FROM `bars` ORDER BY id DESC LIMIT 0, 1";
            else query = "SELECT * FROM `"+dbName+"`.`bars` ORDER BY id DESC LIMIT 0, 1";
                  
            st.executeQuery(query);
            
            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getCurrBar:"+dbName+"] ERR - result is null"
                        );
                return 0;                
            }
            
            while (rs.next()) {         
                bar_id = rs.getInt("id");    
                
                Functions.printLog(
                                "MKQUERY:[getCurrBar:"+dbName+"] INF - result [id:" 
                    + rs.getInt("id")
                    + "]"
                                );
            }         
            
            rs.close();       
            st.close();
            jdbc_connection.freeConnection(con);
       
        } catch (SQLException e) {  
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[getCurrBar:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
   
            return 0;
        }
     
        return bar_id;   
    }

    /**
     * Gets total orders from real and demo
     * @return total=real+demo
     */
    public static String getTotalOrders(DBConnection jdbc_connection, String dbName1, String dbName2){  
        Connection con = jdbc_connection.getConnection();
        if (con == null) return null;

        String query = null;

        try {
            Statement st = con.createStatement();
            
            if(dbName2 == null || dbName2.isEmpty()) query = "SELECT (SELECT COUNT(id) FROM `"+dbName1+"`.orders WHERE status = 'open' AND type = 'buy')+(SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM `"+dbName1+"`.orders WHERE status = 'open' AND type = 'sell')+(SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'sell') AS sells;";
            else query = "SELECT (SELECT COUNT(id) FROM `"+dbName1+"`.orders WHERE status = 'open' AND type = 'buy')+(SELECT COUNT(id) FROM `"+dbName2+"`.orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM `"+dbName1+"`.orders WHERE status = 'open' AND type = 'sell')+(SELECT COUNT(id) FROM `"+dbName2+"`.orders WHERE status = 'open' AND type = 'sell') AS sells;";
 
            //real: query = "SELECT (SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM orders WHERE status = 'open' AND type = 'sell') AS sells FROM ticks WHERE id = (SELECT MAX(id) FROM ticks);";
            //demo: query = "SELECT (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'buy') AS buys, (SELECT COUNT(id) FROM `"+RunServer.jdbc_demo_name+"`.orders WHERE status = 'open' AND type = 'sell') AS sells FROM `"+RunServer.jdbc_demo_name+"`.ticks WHERE id = (SELECT MAX(id) FROM `"+RunServer.jdbc_demo_name+"`.ticks);";

            st.executeQuery(query);

            ResultSet rs = st.getResultSet(); 
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getTotalOrders:"+dbName1+dbName2+"] ERR - result is null"
                        );
                return null;                
            }

            query = null;
            while (rs.next()) {              
              query = rs.getString("buys") + ":" + rs.getString("sells");              
              Functions.printLog(
                                "MKQUERY:[getTotalOrders:"+dbName1+dbName2+"] INF - result [buys:" 
                    +rs.getString("buys") + "; Sells:"
                    +rs.getString("sells")+ "]"
                                );
            }

            rs.close();
            st.close();

            jdbc_connection.freeConnection(con);

        }catch (SQLException e) {
            jdbc_connection.release();
            Functions.printLog(
              "MKQUERY:[getTotalOrders:"+dbName1+dbName2+"] ERR - " + e.getLocalizedMessage()+ 
              "[" + query+ "]"
            );
            
            return null;
        }

        return query;                
    }

/*     */   
    public static double getLastDBPrice(DBConnection jdbc_connection, String dbName) {
        Connection con = jdbc_connection.getConnection();
        if (con == null) return 0;
        
        double last_price = 0.0D;
        
        String query = null;
        
        try {  
            Statement st = con.createStatement();
            if(dbName == null || dbName.isEmpty()) query = "SELECT close AS last_price FROM `bars` ORDER BY `timestamp` DESC LIMIT 0, 1";
/* 524 */   
            else query = "SELECT close AS last_price FROM `"+dbName+"`.`bars` ORDER BY `timestamp` DESC LIMIT 0, 1";

            st.executeQuery(query);

            ResultSet rs = st.getResultSet();
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getLastDBPrice:"+dbName+"] ERR - result is null"
                        );
                return 0;                
            }

            while (rs.next()) {
                last_price = rs.getDouble("last_price"); 
                Functions.printLog(
                                "MKQUERY:[getLastDBPrice:"+dbName+"] INF - result [" 
                    +rs.getDouble("last_price")                                
                    + "]"
                                );
            }                 
 
            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
            
        }catch (SQLException e) {
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[getLastDBPrice:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );            
            
            return 0;
        }
    
        return last_price;  
    }
    
    /*
    * 
    */
    public static String getOnOffDates(DBConnection jdbc_connection, String dbName){ 
        Connection con = jdbc_connection.getConnection();
        if (con == null) return null;
        
        String query = null;

        Statement st;
        try {                    
            st = con.createStatement();
            
            if(dbName == null || dbName.isEmpty()) query = "select `id`, `start` as server_off_date, `stop` as server_on_date from `server_status` where `status`=1 and UTC_TIMESTAMP between `start` and `stop`";
            else query = "select `id`, `start` as server_off_date, `stop` as server_on_date from `"+dbName+"`.`server_status` where `status`=1 and UTC_TIMESTAMP between `start` and `stop`";

                
            st.executeQuery(query);
            ResultSet rs = st.getResultSet();     
            
            if (rs == null) {
                Functions.printLog(
                        "MKQUERY:[getOnOffDates:"+dbName+"] ERR - result is null"
                        );
                return null;                
            }
            
            query = null;            
            while (rs.next()) { 
                query = rs.getString("id")+"#"+rs.getString("server_off_date")+"#"+rs.getString("server_on_date");
                Functions.printLog(
                                "MKQUERY:[getOnOffDates:"+dbName+"] INF - result [id:" 
                    +rs.getString("id")+"#off:"
                    +rs.getString("server_off_date")+"#on:"
                    +rs.getString("server_on_date")
                    + "]"
                                );
            }                 

            rs.close();
            st.close();
            jdbc_connection.freeConnection(con);
         
        } catch (SQLException ex) {
            jdbc_connection.release();
            
            Functions.printLog(
                     "MKQUERY:[getOnOffDates:"+dbName+"] WRN - "
                    + ex.getLocalizedMessage()
                   );
            
            return null;
        }         
        
        return query;
    }

/******************************************************************************/  
/**  All setters goes down here   |     ^                                    **/
/**                              \|/   /|\                                   **/
/**                               V     |   All getters goes up there        **/
/**                                                                          **/ 
/******************************************************************************/ 
    public static void setSocketAvailable(DBConnection jdbc_connection, String dbName, String host, int status){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        
        try { 
            if(dbName == null || dbName.isEmpty()) query = "update `wsocket_availabness` set `up`="+status+" where `host`=\""+host+"\"";
            else query = "update "+dbName+".`wsocket_availabness` set `up`="+status+" where `host`=\""+host+"\"";
  
            PreparedStatement st = con.prepareStatement(query);
            st.executeUpdate(query);
            
            st.close();
            jdbc_connection.freeConnection(con);
            
            Functions.printLog(
                    "MKQUERY:[setSocketAvailable:"+dbName+"] INF - execute [" + query + "]"
                    ); 
            
        } catch (SQLException e) {
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[setSocketAvailable:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
            
                    );
        }
    }
    
    public static void changeUserStatus(DBConnection jdbc_connection, String dbName, int id, int status){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        
        try { 
            if(dbName == null || dbName.isEmpty()) query = "UPDATE `accounts` SET `online`="+status+" WHERE `id` = " + id;
            else query = "UPDATE "+dbName+".`accounts` SET `online`="+status+" WHERE `id` = " + id;
  
            PreparedStatement st = con.prepareStatement(query);
            st.executeUpdate(query);
            
            st.close();
            jdbc_connection.freeConnection(con);
            
            Functions.printLog(
                    "MKQUERY:[changeUserStatus:"+dbName+"] INF - execute [" + query + "]"
                    ); 
            
        } catch (SQLException e) {
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[changeUserStatus:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
            
                    );
        }
    }
    
    public static void setOrderFilled(DBConnection jdbc_connection, String dbName, String clordeID, String execid, double price){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        
        try { 
            if(dbName == null || dbName.isEmpty()) query = "UPDATE market_orders SET market_id = " + execid + ", market_price = " + price + ", status = 'opened' WHERE id = " + clordeID;
            else query = "UPDATE `"+dbName+"`.market_orders SET market_id = " + execid + ", market_price = " + price + ", status = 'opened' WHERE id = " + clordeID;
  
            PreparedStatement st = con.prepareStatement(query);
            st.setString(1, execid);
            st.setDouble(2, price);
            st.setInt(3, Integer.valueOf(clordeID).intValue());
            st.executeUpdate();
            
            st.close();
            jdbc_connection.freeConnection(con);
            
            Functions.printLog(
                    "MKQUERY:[setOrderFilled:"+dbName+"] INF - execute [" + query + "]"
                    ); 
            
        } catch (SQLException e) {
            jdbc_connection.release();
            Functions.printLog(
                    "MKQUERY:[setOrderFilled:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
            
                    );
        }
    }
    
    public static void setServerOnline(DBConnection jdbc_connection, String dbName, int status, int trading){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        
        try {
            Statement st = con.createStatement();
            if(dbName == null || dbName.isEmpty()) query = "UPDATE websocket SET broker = " + status + ", trading = " + trading + ", websocket_online = NOW();";
/* 582 */  
            else query = "UPDATE `"+dbName+"`.websocket SET broker = " + status + ", trading = " + trading + ", websocket_online = NOW();";

            st.executeUpdate(query);
            st.close(); 
            jdbc_connection.freeConnection(con);
            
            /*Functions.printLog(
                    "MKQUERY:[setServerOnline:"+dbName+"] INF - execute [" + query + "]"
                    );*/
            
        }catch (SQLException e){
            jdbc_connection.release();
            
            Functions.printLog(
                    "MKQUERY:[setServerOnline:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );            
        }
    }
    
    public static void setOrderFilled(DBConnection jdbc_connection, String dbName, String clordeID, String error){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        
        try {
/* 610 */         
            if(dbName == null || dbName.isEmpty()) query = "UPDATE market_orders SET error = " + error + ", status = 'rejected' WHERE id = " + clordeID;
            else query = "UPDATE `"+dbName+"`.market_orders SET error = " + error + ", status = 'rejected' WHERE id = " + clordeID;
/* 611 */   
            PreparedStatement st = con.prepareStatement(query);
            st.setString(1, error);
            st.setInt(2, Integer.valueOf(clordeID).intValue());
            st.executeUpdate();
            
            st.close();            
            jdbc_connection.freeConnection(con);
            
            Functions.printLog(
                    "MKQUERY:[setOrderFilled:"+dbName+"] INF - execute [" + query + "]"
                    );      
            
        } catch (SQLException e) {
            jdbc_connection.release();
            
            Functions.printLog(
                    "MKQUERY:[setOrderFilled:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                    "[" + query+ "]"
                  );
        }  
    }            

    /*
     * Updates server on\off dates
     */
    public static void setOnOffDate(DBConnection jdbc_connection, String dbName, String server_start, String server_stop){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        try {
            
            if(dbName == null || dbName.isEmpty()) query = "INSERT INTO server_status (type, start, stop, msg, status) VALUES ('sys_weekend', '"+server_stop+"', '"+server_start+ "', null, 1)";
            else query = "INSERT INTO `"+dbName+"`.server_status (type, start, stop, msg, status) VALUES ('sys_weekend', '"+server_stop+"', '"+server_start+ "', null, 1)";

            PreparedStatement st = con.prepareStatement(query);
            st.executeUpdate();
            st.close();

            Functions.printLog(
              "MKQUERY:[setOnOffDate:"+dbName+"] INF - execute [" + query + "]"
              );

            jdbc_connection.freeConnection(con);

        } catch (SQLException e) {
                   Functions.printLog(
                     "MKQUERY:[setOnOffDate:"+dbName+"] ERR - " + e.getLocalizedMessage()+ 
                     "[" + query+ "]"
                   );
          jdbc_connection.release();
        }
    }
    
    public static void setTimezone(DBConnection jdbc_connection, String timezone ){
        Connection con = jdbc_connection.getConnection();
        if (con == null) return;
        
        String query = null;
        try {
            query = "SET time_zone = '"+timezone+"'";

            PreparedStatement st = con.prepareStatement(query);
            st.executeUpdate();
            st.close();

            Functions.printLog(
              "MKQUERY:[setTimezone] INF - execute [" + query + "]"
              );

            jdbc_connection.freeConnection(con);

        } catch (SQLException e) {
                   Functions.printLog(
                     "MKQUERY:[setTimezone] ERR - " + e.getLocalizedMessage()+ 
                     "[" + query+ "]"
                   );
          jdbc_connection.release();
        }
    }
}
