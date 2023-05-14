package Sapphire.Tasks;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import Sapphire.Networking.*;
import spark.Response;

public abstract class Task{
    long lastUpdate;  // to help put a maximum durration on a task
    int id;
    int nextClientID;   // the next client that will be contacted
    int firstClientID;  // the id of the client that started the task
    Step step;
    boolean delivered;
    String outputString = "";
    public abstract void executeStage(StructuredRequest r);
    public abstract void stopTask();
    
    public Object getOutput(Response res){
        lastUpdate = System.currentTimeMillis();
        res.header("TaskID",id);
        try{
            PrintWriter writter = new PrintWriter(new BufferedOutputStream(res.raw().getOutputStream()));
            writter.append(outputString).flush();
            writter.close();
        }catch(IOException ioe){
            System.out.println("Error writing task output: "+ ioe.getMessage());
            return null;
        }
        delivered = true;
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
