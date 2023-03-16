package Sapphire.Menu;

//#region imports
import java.util.HashMap;
import java.util.Scanner;
import Sapphire.Tasks.ITaskManager;
import Sapphire.Auth.IAuthorizor;
import Sapphire.Logging.ILogger;
import Sapphire.*;
//#endregion imports

public class Menu implements Runnable{
    //#region init
    Scanner userInput = new Scanner(System.in);
    public Boolean shutdown = false;
    ITaskManager taskManager;
    IAuthorizor authorizor;
    ILogger logger;
    MenuMiniClient mc;
    public Menu(ITaskManager tm,IAuthorizor auth,ILogger log, StringReader sr){
        taskManager = tm;
        authorizor = auth;
        logger = log;
        mc = new MenuMiniClient(tm,sr);
    }

    public void run(){
        mainMenu();
        return;
    }
    //#endregion init

    //#region helpers
    private boolean cancel(String userInput){
        String ui = userInput.toLowerCase();
        if(ui.equals("cancel")||ui.equals("exit")){
            return true;
        }
        return false;
    }

    private String getUserInput(){
        String in = userInput.next();
        return in;
    }

    public static void waitForNextKeystroke(){
        try{
            while(System.in.available()==0){
                Thread.sleep(100);
            }
            System.in.read();
        }catch(Exception e){}
    }

    private static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    } 
    //#endregion helpers

    //#region MainMenu 
    private void mainMenu(){
        System.out.println("Welcome to the server's GUI");
        boolean printMenu = true;
        while(true){
            if(printMenu){    
                clearScreen();
                System.out.println("Enter the number of your action:");
                System.out.println("\t1. Manage Registered Devices");
                System.out.println("\t2. View Activity Logs");
                System.out.println("\t3. Make New Request");
                printMenu = false;
            }
            
            switch(getUserInput()){
                case "1":
                    manageRegisteredDevices();
                    break;
                case "2":
                    viewActivityLog();
                    break;
                case "3":
                    makeNewRequest();
                    break;
                case "quit": // replace with return statement if resume execution is implemented
                case "shutdown":
                    shutdown = true;
                    return;
                case "h":
                case "H":
                case "help":
                case "Help":
                    break;
                default:
                continue;
            }
            printMenu = true;
        }
    }
    //#endregion MainMenu

    //#region registerDevices
    private void manageRegisteredDevices(){
        Boolean printMenu = true;
        while(true){
            if(printMenu){
                clearScreen();
                System.out.println("Enter the number of your action:");
                System.out.println("\t1. Register a New Device");
                System.out.println("\t2. Remove a Device");
                System.out.println("\t3. Show Registered Devices");
                System.out.println("\t4. Return to Main Menu");
                printMenu = false;
            }
            switch(getUserInput()){
                case "1":
                    // register new device
                    registerDevice();
                    return;
                case "2":
                    removeDevice();
                    return;
                case "3":
                    showDevices();
                    return;
                case "4":
                    return;
                case "h":
                case "H":
                case "help":
                case "Help":
                    break;
                default:
                    continue;
            }
            printMenu = true;
        }
    }

    private void registerDevice(){
        String newDeviceName = null;
        String newAuthToken = null;
        boolean nameSelected = false;
        boolean deviceAdded = false;
        clearScreen();
        do{
            System.out.print("Enter the name of the new device: ");
            System.out.flush();
            newDeviceName = getUserInput();
            System.out.print("Device name: "+newDeviceName+"\nIs this correct(Y/N)? ");
            System.out.flush(); // just to make sure
            String confirm = getUserInput();
            if(cancel(confirm)) return;
            switch(confirm.toLowerCase()){
                case "yes":
                case "y":
                    nameSelected = true;
                    break;
                case "no":
                case "n":
                    continue;
                default:
                    System.out.print("Device name: "+newDeviceName+"\nIs this correct? ");
                    System.out.flush(); // just to make sure
            }
        }while(!nameSelected);
        clearScreen();
        do{
            System.out.print("Enter the authentication token of the new device:\n\t");
            System.out.flush(); // just to make sure
            newAuthToken = getUserInput();
            if(cancel(newAuthToken)) return;
            
            if(newDeviceName==null||newAuthToken==null){
                System.out.println("Something's going wrong: "+newDeviceName+","+newAuthToken);
                waitForNextKeystroke();
                return;
            }

            deviceAdded = authorizor.registerDevice(newDeviceName, newAuthToken);
            //clearScreen();
            if(deviceAdded){
                System.out.println("Successfully added new device: "+newDeviceName);
            }else{
                System.out.println("Failed to register new device");
            }
            waitForNextKeystroke();
            clearScreen();
        }while(!deviceAdded);
    }

    private void removeDevice(){
        String deviceName = "";
        int deviceID = 0;

        clearScreen();
        while(true){
            System.out.println("Enter the ID of the device you want to remove");
            String sdeviceID = getUserInput();
            if(cancel(sdeviceID)) return;
            try{
                deviceID=Integer.parseInt(sdeviceID);
            }catch(Exception e){
                System.out.println("Invalid input");
                continue;
            }
            if(authorizor.checkDeviceExists(deviceID)){
                try{deviceName = authorizor.getDeviceName(deviceID);}
                catch(Exception e){
                    System.err.println("Device name not found despite id being found");
                }
                System.out.println("Are you sure you want to remove: "+deviceName+"? (Y/N)");
                String confirm = getUserInput();
                if(cancel(confirm)) return;
                switch(confirm.toLowerCase()){
                    case "yes":
                    case "y":
                        break;
                    case "no":
                    case "n":
                        return;
                    default:
                        System.out.println("Are you sure you want to remove: "+deviceName+"? (Y/N)");
                }
                clearScreen();
                if(authorizor.removeDeviceAuth(deviceID)){
                    System.out.println("Device: "+deviceID+"|"+deviceName+" Successfully removed\nPress Enter to return");
                }else{
                    System.out.println("Failed to remove device "+deviceID+"|"+deviceName+"\nPress Enter to return");    
                }
                waitForNextKeystroke();
                return;
            }
        }
    }

    private void showDevices(){
        clearScreen();
        HashMap<Integer,String> devices = null;
        System.out.println("All currently registered devices {Device_ID|Device_Name}\nPress Enter to return");
        try{
            devices = authorizor.showAllDevices();
        }catch(Exception e){
            System.err.println(e.getMessage());
            waitForNextKeystroke();
            return;    
        }
        for(int deviceid : devices.keySet()){
            System.out.println("{"+deviceid+"|"+devices.get(deviceid)+"}");
        }
        waitForNextKeystroke();
    }
    //#endregion registerDevices

    //#region activityLog
    private void viewActivityLog(){
        /* idk hwo I'm going to view data from the logger, hell I dont even know how I'm storing that yet */
        clearScreen();
        System.out.println("Not yet Implemented");
        waitForNextKeystroke();
        return;
    }

    //#endregion activityLog

    //#region customRequests
    private void makeNewRequest(){
        /*  */
        boolean printMenu = true;
        while(true){
            if(printMenu){
                clearScreen();
                System.out.println("Enter the number of your action:");
                System.out.println("\t1. Update Directory Structure");
                System.out.println("\t2. Fetch File");
                System.out.println("\t3. Send File");
                System.out.println("\t4. Return to Main Menu");
                printMenu = false;
            }
            try{
                switch(getUserInput()){
                    case "1":
                    System.out.println("not implemented\nPress Enter to return");
                    waitForNextKeystroke();
                    break;
                    case "2":
                    pullFile();
                    break;
                    case "3":
                        sendFile();
                        break;
                    case "4":
                    return;
                    case "h":
                    case "H":
                    case "help":
                    case "Help":
                    break;
                    default:
                    continue;
                }    
            }catch(Exception e){
                System.out.println("An error occured, try again another time");
                waitForNextKeystroke();
            }
            printMenu = true;
        }
    }

    private void pullFile() throws Exception{
        int deviceID = -1;
        String filename = "";
        String localName = "";
        clearScreen();
        while(true){
            System.out.println("What device would you like to pull from?");
            // display all 
            HashMap<Integer,String> devices = authorizor.showAllDevices();
            if(devices==null) throw new Exception();
            for(int deviceid : devices.keySet()){
                System.out.print("{"+deviceid+"|"+devices.get(deviceid)+"}, ");
            }
            String sdeviceID = getUserInput();
            if(cancel(sdeviceID)){
                return;
            }
            try{
                deviceID = Integer.parseInt(sdeviceID);
                clearScreen();
                break;
            }catch(Exception e){
                clearScreen();
                System.out.println("Invalid device ID");
            }
        }
        while(true){
            System.out.println("Selected device: "+deviceID);
            // replace with dir parser
            System.out.println("Enter the full path and name of the file you want to pull");
            // for now, assume input is correct
            filename = getUserInput();
            if(cancel(filename)){
                return;
            }
            break;
        }
        clearScreen();
        while(true){
            System.out.println("Selected device: "+deviceID+"\nSelected file: "+filename);
            System.out.println("Enter local destination and name");
            localName = getUserInput();
            if(cancel(localName)){
                return;
            }
            break;
        }
        mc.pullFile(deviceID, filename, localName);
    }

    private void sendFile() throws Exception{
        int deviceID = -1;
        String filename = "";
        String desinationName = "";
        clearScreen();
        while(true){
            System.out.println("What device would you like to send to?");
            // display all 
            HashMap<Integer,String> devices = authorizor.showAllDevices();
            if(devices==null) throw new Exception();
            for(int deviceid : devices.keySet()){
                System.out.print("{"+deviceid+"|"+devices.get(deviceid)+"}, ");
            }
            String sdeviceID = getUserInput();
            if(cancel(sdeviceID)){
                return;
            }
            try{
                deviceID = Integer.parseInt(sdeviceID);
                clearScreen();
                break;
            }catch(Exception e){
                clearScreen();
                System.out.println("Invalid device ID");
            }
        }
        while(true){
            System.out.println("Selected device: "+deviceID);
            // replace with dir parser
            System.out.println("Enter the full path and name of the file you want to send");
            // for now, assume input is correct
            filename = getUserInput();
            if(cancel(filename)){
                return;
            }
            break;
        }
        clearScreen();
        while(true){
            System.out.println("Selected device: "+deviceID+"\nSelected file: "+filename);
            System.out.println("Enter destination path and name");
            desinationName = getUserInput();
            if(cancel(desinationName)){
                return;
            }
            break;
        }
        mc.sendFile(deviceID, filename, desinationName);
    }
    
    //#endregion customRequests
}
