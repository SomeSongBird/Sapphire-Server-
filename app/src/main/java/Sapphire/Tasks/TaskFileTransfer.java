package Sapphire.Tasks;

import Sapphire.Networking.StructuredRequest;
import spark.Response;
import static spark.Spark.halt;
import java.io.*;
import java.nio.file.Files;


public class TaskFileTransfer extends Task{
    String final_path = "";
    String temporaryFile = "";
    public TaskFileTransfer(int id, StructuredRequest r){
        lastUpdate = System.currentTimeMillis();
        this.id = id;
        firstClientID = r.clientID;
        final_path = r.extraDetails.get("final_path");

        // if there's a file, send and confirm
        // else recieve and respond
        if(r.extraDetails.get("temporary_file_location") != null){
            step = Step.sending;
        }else step = Step.requesting;
    }

    public void executeStage(StructuredRequest r){
        System.out.println("Executing Stage "+step);
        lastUpdate = System.currentTimeMillis();
        outputString = "<Task>\r\nFileTransfer\r\n</Task>\r\n";
        switch(step){
            case requesting: 
                delivered = false;
                nextClientID = r.targetID;
                final_path = r.extraDetails.get("file_path").strip();
                outputString += "<requested_file_path>\r\n"+r.extraDetails.get("file_location")+"\r\n</requested_file_path>\r\n";
                step = Step.responding;
                return;
            case sending:
                delivered = false;
                nextClientID = r.targetID;
                final_path = r.extraDetails.get("final_path").strip();
                temporaryFile = r.extraDetails.get("temporary_file_location");
                step = Step.confirming;
                return;
            case responding:
                delivered = false;
                nextClientID = this.firstClientID;
                temporaryFile = r.extraDetails.get("temporary_file_location");
                step = Step.closing;
                return;
            case confirming:
                nextClientID = this.firstClientID;
                outputString += "<confirmation>\r\n"+final_path+"\r\n</confirmation>\r\n";
                step = Step.closing;
                return;
            default:
                return;
        }
    }
    
    public Object getOutput(Response res){
        if((step==Step.closing || step==Step.confirming) && !delivered){
            // if it's the final step and the output isn't just confirmation
            // header stuff
            File file = null;
            try {
                file = new File(temporaryFile);
                if(!file.exists()){
                    System.out.println("temporary file not found | "+ id);
                    return "temporary file not found | "+ id;
                }
            } catch (Exception e) {
                System.out.println("Trying to find it: "+e );    
            }
            
            try{
                OutputStream bos = new BufferedOutputStream(res.raw().getOutputStream());
                PrintWriter writer = new PrintWriter(bos);
                writer.append(outputString).flush();
                writer.append("<final_path>\r\n"+final_path+"\r\n</final_path>\r\n").flush();
                
                writer.append("<File>\r\n").flush();
                Files.copy(file.toPath(),bos);
                bos.flush();
                writer.append("\r\n</File>\r\n").flush();
                
                //close everything
                writer.close();
                bos.close();
                file.delete();
                delivered = true;
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
