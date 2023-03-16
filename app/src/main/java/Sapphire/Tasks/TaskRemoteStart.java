package Sapphire.Tasks;

import Sapphire.Networking.StructuredRequest;

public class TaskRemoteStart extends Task {
    public TaskRemoteStart(int id, StructuredRequest r){
        this.id = id;
    }

    public void executeStage(StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        outputString = "<Task>RemoteStart</Task>\r\n";
        switch(step){
            
            default:
                return;
        }
    }

    public void stopTask(){
        /*
         * put task state into log
         */
    }
}
