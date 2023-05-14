package Sapphire.Tasks;

import Sapphire.Networking.StructuredRequest;


/* region names used:
 * directory_path, directory_details */

public class TaskDirectory extends Task {

    public TaskDirectory(int id, StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        this.id = id;
        firstClientID = r.clientID;
        step = Step.requesting;
    }

    public void executeStage(StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        delivered = false;
        outputString = "<Task>Directory</Task>\r\n";
        switch(step){
            // get request > start new task > wait for other client to update > request directory structure > recieve dir > wait for first to update > send dir
            case requesting:
                nextClientID = r.targetID;
                outputString += "<directory_request>directory_request</directory_request>\r\n"; // extraDetails[0] will be the path of the dir to branch down from
                step = Step.responding;
                return;
            case responding:
                nextClientID = firstClientID;
                outputString += "<directory_details>"+r.extraDetails.get("directory_details")+"</directory_details>\r\n"; // in this case, extraDetails[0] will be the full dir structure from the other client
                outputString += "<target_client>"+r.clientID+"</target_client>\r\n";
                step = Step.closing;
                return;
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
