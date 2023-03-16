package Sapphire.Tasks;

import Sapphire.Networking.*;
import spark.Response;

public abstract class Task{
    long lastUpdate;  // to help put a maximum durration on a task
    int id;
    int nextClientID;   // the next client that will be contacted
    int firstClientID;  // the id of the client that started the task
    Step step;
    boolean delivered;
    String outputString;
    public abstract void executeStage(StructuredRequest r);
    public abstract void stopTask();
    
    public Object getOutput(Response res){
        lastUpdate = System.currentTimeMillis();
        res.header("TaskID",id);
        delivered = true;
        res.body(outputString);
        return null;
    }
}


enum Step{
    requesting,     // sending a request to a client
    sending,
    confirming,
    responding,     // sending the information to the client that it's meant for
    closing         // any closing actions such as logging
};
