package Sapphire.Tasks;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import Sapphire.Networking.*;
import spark.Response;

public abstract class Task{
    long lastUpdate;  // to help put a maximum durration on a task
    int id = -1;
    int nextClientID = -1;   // the next client that will be contacted
    int firstClientID = -1;  // the id of the client that started the task
    Step step;
    boolean delivered;
    String outputString = "";
    public abstract void executeStage(StructuredRequest r);
    public abstract void stopTask();
    
    public Object getOutput(Response res){
        
        try{
            lastUpdate = System.currentTimeMillis();
            PrintWriter writter = new PrintWriter(new BufferedOutputStream(res.raw().getOutputStream()));
            writter.append(outputString).flush();
            writter.close();
        }catch(IOException e){
            System.out.println("Error writing task output: "+ e.getMessage());
            return null;
        }catch(Exception e2){
            System.out.println("idk man lol "+e2.getLocalizedMessage());
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
