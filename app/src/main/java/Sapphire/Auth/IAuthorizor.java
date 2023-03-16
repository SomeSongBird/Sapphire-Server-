package Sapphire.Auth;

import java.util.HashMap;

public interface IAuthorizor {
    public boolean registerDevice(String deviceName, String authToken);
    public boolean removeDeviceAuth(int deviceID);    
    public boolean checkDeviceExists(int deviceID);
    public int checkAuth(String authToken) throws Exception;
    public String getDeviceName(int deviceID)throws Exception;
    public HashMap<Integer,String> showAllDevices()throws Exception;
}
