package Sapphire;

import Sapphire.Menu.Menu;
import Sapphire.Networking.*;
import Sapphire.Tasks.*;
import Sapphire.Logging.*;
import Sapphire.Auth.*;

import java.io.FileNotFoundException;

public class MainController {
    
    public static StringReader sr;
    private static ILogger log; 
    private static IAuthorizor auth; 
    private static ITaskManager task;
    private static IRouting rout;
    private static Menu menu;
    
    private static boolean setup(){
        try{
            sr = new StringReader();
        }catch(FileNotFoundException e){
            System.err.println("Could not read string resources file, check that the file exists and is properly setup then try again");
            return false;
        }

        log = new Logger();
        auth = new Authorizor(sr);
        task = new TaskManager(auth,log);
        rout = new Routing(task);
        menu = new Menu(task,auth,log,sr);
        
        rout.startRouting();
        return true;
    }
    
    private static void mainLoop(){
        Thread menuThread = new Thread(menu);

        menuThread.start();
        while(true){
            task.timeoutTask(System.currentTimeMillis());
            if(menu.shutdown){
                rout.stopRouting();
                task.shutdownSequence();
                return;
            }
            try{Thread.sleep(500);} // litterally just so this doesn't kill my computer waiting for something to happen
            catch(Exception e){}
        }
        
    }


    public static void main(String[]args){
        if(setup()){
            mainLoop();
        }
        return;
    }
}
