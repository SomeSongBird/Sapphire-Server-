package Sapphire.Networking;

import static spark.Spark.*;


import spark.Response;
import Sapphire.Tasks.*;
import Sapphire.Tasks.Enums.TaskType;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;

public class Routing implements IRouting{
    ITaskManager taskManager;

    public Routing(ITaskManager tm){
        taskManager = tm;
    }

    public void startRouting(){
        //secure("C:/keystoreDIR/keystore", "strongKeystorePassword","","");
        port(44344);
       
        //get("/fileTest", (req,res) -> getFile(res)); // testing purposes
        get("/ping", (req, res) -> "pong");     // testing purposes
        post("/update", (req, res) -> taskManager.updateClient(new StructuredRequest(req,"GET"), res));   // recieves pings from clients to check for any requests
        post("/client_list", (req, res) -> taskManager.sendClientList(new StructuredRequest(req,"GET"), res));
       
        // get request > start new task > wait for other client to update > request directory structure > recieve dir > wait for first to update > send dir
        post("/update_directory/request", (req,res)-> taskManager.startNewTask(TaskType.directory, new StructuredRequest(req,"POST"),res));
        post("/update_directory/compliance", (req,res)-> taskManager.updateTasks(new StructuredRequest(req,"POST"),res));

        // get request > start new task > wait for other client to update > send requested file name > wait for other client to return location of file > wait for first client to update > send file location to first client
        post("/remote_start/request", (req,res)-> taskManager.startNewTask(TaskType.remoteStart, new StructuredRequest(req,"POST"),res));
        post("/remote_start/compliance", (req,res)-> taskManager.updateTasks(new StructuredRequest(req,"POST"),res));
        // get request > start new task > wait for other client to update 
        // (if no file included) > request file from that client > recieve file > store file localy > wait for first client to update > send file to first client
        // (if file included) > recieve file > store file localy > wait for other client to update > send file to other client
        post("/file_transfer/request",(req,res)-> taskManager.startNewTask(TaskType.fileTransfer, new StructuredRequest(req,"POST"),res));  
        post("/file_transfer/compliance",(req,res)-> taskManager.updateTasks(new StructuredRequest(req,"POST"),res));
    }

    public void stopRouting(){ 
        stop();
    }


    private static Object getFile(Response res){
        try{
            OutputStream bos = new BufferedOutputStream(res.raw().getOutputStream());
            PrintWriter writer = new PrintWriter(bos);
            
            /* compress file */
            String inputFile = "/home/usr/example.png";
            File zipped = new File(zipfile(inputFile));
                        
            writer.append("<File>").flush();
            Files.copy(zipped.toPath(),bos);
            bos.flush();    
            writer.append("</File>\r\n").flush();
            
            writer.close();
            bos.close();
            zipped.delete();
        }catch(Exception e){
            halt(405,"server error");
        }
        return null;
    }

    private static String zipfile(String filename){
        File infile = new File(filename);
        String outputFileName = filename+".zip";
        File outfile = new File(outputFileName);
        try{
            outfile.createNewFile();
            FileInputStream fis = new FileInputStream(infile);
            ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(outfile));
            
            ZipEntry zipEntry = new ZipEntry("entry");
            zOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zOut.write(bytes, 0, length);
            }
            zOut.flush();
            zOut.closeEntry();
            fis.close();
            zOut.close();
        }catch(IOException e){
            System.err.println(e );
            return ("E: "+ e );
        }
        
        return outputFileName;
    }
}
