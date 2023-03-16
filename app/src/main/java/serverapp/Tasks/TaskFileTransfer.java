package serverapp.Tasks;

import serverapp.Networking.StructuredRequest;
import spark.Response;
import static spark.Spark.halt;
import java.io.*;
import java.nio.file.Files;

/* region names used:
 * final_path, requested_file_path, file_location, confirmation, File */

public class TaskFileTransfer extends Task{
    String final_path = "";
    public TaskFileTransfer(int id, StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        this.id = id;
        firstClientID = r.clientID;
        final_path = r.extraDetails.get("final_path");

        // if there's a file, send and confirm
        // else recieve and respond
        if(r.extraDetails.get("file_location") == null){
            step = Step.sending;
        }else step = Step.requesting;
    }

    public void executeStage(StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        delivered = false;
        outputString = "<Task>FileTransfer</Task>\r\n";
        switch(step){
            case requesting: 
                nextClientID = r.targetID;
                outputString += "<requested_file_path>"+r.extraDetails.get("requested_file_path")+"</requested_file_path>\r\n";
                step = Step.responding;
                return;
            case sending:
                nextClientID = r.targetID;
                outputString += r.extraDetails.get("file_location");
                step = Step.confirming;
                return;
            case responding:
                nextClientID = this.firstClientID;
                outputString += r.extraDetails.get("file_location");
                step = Step.closing;
                return;
            case confirming:
                nextClientID = this.firstClientID;
                outputString += "<confirmation>"+final_path+"</confirmation>\r\n";
                step = Step.closing;
                return;
            default:
                return;
        }
    }
    
    public Object getOutput(Response res){
        if((step==Step.closing || step==Step.confirming) && !(outputString=="<Task>FileTransfer</Task>\r\n"+"<confirmation>"+final_path+"</confirmation>\r\n")){
            // if it's the final step and the output isn't just confirmation
            delivered = true;
            // header stuff
            File file = new File(outputString);
            res.header("TaskID", id);
            
            try(OutputStream bos = new BufferedOutputStream(res.raw().getOutputStream());
                PrintWriter writer = new PrintWriter(bos);
            ){
                writer.append("<final_path>"+final_path+"</final_path>").flush();
                
                writer.append("<File>").flush();
                Files.copy(file.toPath(),bos);
                bos.flush();
                writer.append("</File>\r\n").flush();

                //close everything
                writer.close();
                bos.close();
                file.delete();
            }catch(IOException ioe){
                halt(405,"server error");
            }
            return null;
        }
        return super.getOutput(res);
    }

    public void stopTask(){
        /* 
         * delete the stored file (if any)
         * put current state into the log
         */
    }
}
