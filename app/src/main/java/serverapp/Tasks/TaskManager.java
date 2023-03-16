package serverapp.Tasks;

import serverapp.Auth.IAuthorizor;
import serverapp.Logging.*;
import serverapp.Networking.*;
import serverapp.Tasks.Enums.TaskType;
import spark.Response;
import static spark.Spark.halt;

import java.security.spec.ECFieldF2m;

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
    }

    public int startNewTask(TaskType type, StructuredRequest req){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
            return -1;
        }
        Task t;
        switch(type){
            case fileTransfer:
                t = new TaskFileTransfer(nextTaskID++, req);
                break;
            case directory:
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
        addNewTask(t);
        t.executeStage(req);
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
    public Object updateClient(StructuredRequest req,Response res){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
        }

        for(Task task: activeTasks){
            if(task.nextClientID==req.clientID){
                return task.getOutput(res);
            }
        }
        return "none";
    }

    public boolean updateTasks(StructuredRequest req){
        try{
            req.clientID = auth.checkAuth(req.authToken);
        }catch(Exception e){
            //log failed attempt
            halt(401,"Unauthorized Access");
            return false;
        }
        for(Task task: activeTasks){
            if(task.id==req.taskID){
                task.executeStage(req);
                return true;
            }
        }
        return false;
    }

    //#endregion updates

    public void shutdownSequence(){
        for(Task t:activeTasks){
            t.stopTask();
        }
    }
}
