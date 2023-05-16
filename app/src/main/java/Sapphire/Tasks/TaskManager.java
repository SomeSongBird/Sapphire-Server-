package Sapphire.Tasks;

import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import Sapphire.Auth.IAuthorizor;
import Sapphire.Logging.*;
import Sapphire.Networking.*;
import Sapphire.Tasks.Enums.TaskType;

import spark.Response;
import static spark.Spark.halt;

public class TaskManager implements ITaskManager {
    //#region init
    IAuthorizor auth;
    ILogger log;
    Task[] activeTasks;
    int nextTaskID = 1;
    long lastUpdate;

    public TaskManager(IAuthorizor authorizor, ILogger logger){
        auth = authorizor;
        log = logger;
        activeTasks = new Task[0];
        lastUpdate = System.currentTimeMillis();
    }
    //#endregion init
    
    //#region taskManagement
    private void addNewTask(Task t){
        int newlength = activeTasks.length+1;
        Task[] updatedActiveTasks = new Task[newlength];
        for(int i=0;i<activeTasks.length;i++){
            updatedActiveTasks[i] = activeTasks[i];
        }
        updatedActiveTasks[newlength-1] = t;

        activeTasks = updatedActiveTasks;
    }

    private void removeTask(Task t) throws Exception{
        int newlength = activeTasks.length-1;
        Task[] updatedActiveTasks = new Task[newlength];
        int index = 0;
        for(Task task:activeTasks){
            if(task.id!=t.id){
                updatedActiveTasks[index++] = task;
            }
        }
        if(index!=newlength){
            throw new Exception("Missing elements in task array");
        }
        activeTasks = updatedActiveTasks;
    }

    public int startNewTask(TaskType type, StructuredRequest req, Response res){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
            return -1;
        }
        Task t = null;
        try{
            switch(type){
                case fileTransfer:
                    //System.out.println("fileTransfer");
                    t = new TaskFileTransfer(nextTaskID++, req);
                    break;
                case directory:
                //System.out.println("directory");
                t = new TaskDirectory(nextTaskID++, req);
                break;
                case remoteStart:
                t = new TaskRemoteStart(nextTaskID++, req);
                break;
                case updateConnection: //not used, currently set to an error case
                return -1;
                //break;
                default: return -1; // error case
            }
            //System.out.println("add");
            addNewTask(t);
            //System.out.println("execute");
            t.executeStage(req);
        }catch(Exception e){
            System.out.println("TaskStart error: "+e );
        }
        if(res!=null){
            res.header("taskID", t.id);
        }
        return t.id;
    }

    public void timeoutTask(long currentTime){
        for(Task t : activeTasks){
            if(currentTime-t.lastUpdate>60000 || (t.delivered && t.step==Step.closing)){ //if it has been a minute or more or the task is finished, kill the task
                t.stopTask();
                try{removeTask(t);}
                catch(Exception e){/* log failure */ continue;}
            }
            }

    }

    //#endregion taskManagement

    //#region updates
    public Object updateClient(StructuredRequest req, Response res){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
        }
        try{
            res.header("taskID", "-1");
            for(int i=0;i<activeTasks.length;i++){
                Task task = activeTasks[i];
                if(task.nextClientID==req.clientID&&!task.delivered){
                    res.header("taskID",String.valueOf(task.id));
                    task.getOutput(res);
                    //System.out.println("what");
                    return null;
                }
            }
        }catch(Exception e){
            System.out.println("updateClientError: "+e );
        }
        return "None";
    }

    public boolean updateTasks(StructuredRequest req, Response res){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
            return false;
        }
        try{
            for(int i=0;i<activeTasks.length;i++){
                Task task = activeTasks[i];
                if(task.id==req.taskID){
                    res.header("taskID",String.valueOf(task.id));
                    task.executeStage(req);
                    return true;
                }
            }
        }catch(Exception e){
            System.out.println("Update Tasks Error: "+e );
        }
        return false;
    }

    public Object sendClientList(StructuredRequest req, Response res){
        String output = "None";
        try{
            req.clientID = auth.checkAuth(req.authToken);
            res.header("taskID", "-1");
            output = "<ClientList>\r\n";
            HashMap<Integer,String> clientList = auth.showAllDevices();
            for(int i : clientList.keySet()){
                if(i!=req.clientID){
                    if(!output.equals("<ClientList>\r\n")){
                        output = output+":";
                    }
                    output = output + i+","+clientList.get(i);
                }
            }
            output += "\r\n</ClientList>\r\n";

            PrintWriter writter = new PrintWriter(new BufferedOutputStream(res.raw().getOutputStream()));
            writter.append(output).flush();
            writter.close();
        }catch(Exception e){
            halt(401,"Unauthorized Access");
        }
        return null;
    }
    
    //#endregion updates

    public void shutdownSequence(){
        for(int i=0;i<activeTasks.length;i++){
            Task task = activeTasks[i];
            task.stopTask();
        }
    }
}
