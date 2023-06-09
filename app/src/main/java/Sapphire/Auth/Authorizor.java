package Sapphire.Auth;

import java.util.HashMap;
import Sapphire.*;
import Sapphire.Menu.Menu;
import java.sql.*;

public class Authorizor implements IAuthorizor{
    String tableName;
    String dbUsername;
    String dbPassword;
    String dbURL;
    
    private Connection conn;
    public Authorizor(){
        tableName = "Devices";
        dbUsername = StringReader.getString("dbUsername");
        dbPassword = StringReader.getString("dbPassword");
        dbURL = StringReader.getString("dbURL");

        try{
            conn = connect();
        }catch(Exception e){
            System.err.println(e );
            System.err.println("Connection to authentication database failed");
            Menu.waitForNextKeystroke();
        }
    }

    public boolean registerDevice(String deviceName, String authToken){
        if(!checkConnection(conn)){
            return false;
        }
        CallableStatement stmt = null;
        try{
            stmt = conn.prepareCall("INSERT INTO "+tableName+" (Name,AuthToken) VALUES (?,?);");
            stmt.setString(1, deviceName);
            stmt.setString(2, authToken);
            stmt.execute();

            stmt = conn.prepareCall("SELECT ID FROM "+tableName+" WHERE Name=?;");
            stmt.setString(1,deviceName);
            stmt.execute();
            if(stmt.getResultSet().next()){
                return true;
            } else{
                System.err.println("Failed to insert device, no errors");
                return false;
            }
        }catch(Exception e){
            System.err.println(e.getLocalizedMessage());
            return false;
        }
    }
    
    public boolean removeDeviceAuth(int deviceID){
        if(!checkConnection(conn)){
            return false;
        }
        CallableStatement stmt = null;
        try{
            stmt = conn.prepareCall("DELETE FROM "+tableName+" WHERE ID=?;");
            stmt.setInt(1, deviceID);
            stmt.execute();
            
            stmt = conn.prepareCall("ALTER TABLE "+tableName+" AUTO_INCREMENT=1;");
            stmt.execute();
            return !(checkDeviceExists(deviceID));
        }catch(Exception e){
            return false;
        }
    }

    public boolean checkDeviceExists(int deviceID){
        if(!checkConnection(conn)){
            return false;
        }
        CallableStatement stmt = null;
        try{
            stmt = conn.prepareCall("SELECT ID FROM "+tableName+" WHERE ID=?;");
            stmt.setInt(1, deviceID);
            stmt.execute();
            if(stmt.getResultSet().next()){
                return true;
            }
        }catch(Exception e){return false;}
        return false;
    }
    
    public int checkAuth(String authToken) throws Exception{
        if(!checkConnection(conn)){
            throw new Exception("Unable to connect to database");
        }
        CallableStatement stmt = null;
        ResultSet result = null;
        try{
            stmt = conn.prepareCall("SELECT ID FROM "+tableName+" WHERE AuthToken=?;");
            stmt.setString(1, authToken);
            stmt.execute();
            result = stmt.getResultSet();
            if(result.next()){
                int id = Integer.parseInt(result.getString("ID"));
                return id;
            }
        }catch(Exception e){
            System.out.println("checkAuthError: "+e.getLocalizedMessage());
        }
        throw new Exception("Id is not authorized");
    }
    
    public String getDeviceName(int deviceID) throws Exception{
        if(!checkConnection(conn)){
            throw new Exception("Unable to connect to database");
        }
        CallableStatement stmt = null;
        ResultSet result = null;
        try{
            stmt = conn.prepareCall("SELECT Name FROM "+tableName+" WHERE ID=?;");
            stmt.setInt(1, deviceID);
            stmt.execute();
            result = stmt.getResultSet();
            if(result.next()){
                return result.getString("Name");
            }
        }catch(Exception e){}
        throw new Exception("Unable to find device name");
    }
    
    public HashMap<Integer,String> showAllDevices() throws Exception{
        if(!checkConnection(conn)){
            throw new Exception("Unable to connect to database");
        }
        HashMap<Integer,String> hm = new HashMap<Integer,String>();
        CallableStatement stmt = null;
        ResultSet result = null;
        try{
            stmt = conn.prepareCall("SELECT ID,Name FROM "+tableName+";");
            stmt.execute();
            result = stmt.getResultSet();
            if(result.next()){ 
                //System.out.println("Results found");
                do{
                    Integer i = Integer.valueOf(result.getInt("ID"));
                    hm.put(i,result.getString("Name"));
                }while(result.next());
                return hm;
            }
        }catch(Exception e){
            System.err.println(e );
        }
        throw new Exception("Unable to find any devices");
    }

    private Connection connect()throws SQLException, Exception{
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch(ClassNotFoundException cnfe){
            throw new Exception("idk what happened here");
        }
        Connection conn = null;
        conn = DriverManager.getConnection(dbURL,dbUsername,dbPassword);
        return conn;
    }

    private boolean checkConnection(Connection conn){
        try{
            if(conn.isClosed()){
                connect();
            }
        }catch(SQLException sqle){
            System.err.println(sqle );
            return false;
        }catch(Exception e){
            System.err.println("Unable to reconnect");
            return false;
        }
        return true;
    }
}