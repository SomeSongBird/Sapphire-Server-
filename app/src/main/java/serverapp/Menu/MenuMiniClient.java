package serverapp.Menu;

//#region imports
import serverapp.Tasks.*;
import serverapp.Tasks.Enums.TaskType;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import serverapp.Networking.StructuredRequest;
import serverapp.Networking.StructuredResponse;
//#endregion imports

public class MenuMiniClient implements Runnable{
    //#region init
    int ClientID = 0;
    String authToken;
    String temporaryFilePath;
    boolean shutdown = false;
    ITaskManager taskManager;
    String[] authorizedDirectories;

    public MenuMiniClient(ITaskManager tm, serverapp.StringReader sr){
        // read authToken
        authToken = sr.getString("MainMenuAuthToken");
        temporaryFilePath = sr.getString("temporaryFilePath");
        this.taskManager = tm;
        authorizedDirectories = new String[]{sr.getString("DefaultAuthorizedDirectories")};
    }
    public void run(){
        while(!shutdown){
            update();
            try{
                Thread.sleep(1000); //update every second
            }catch(Exception e){}
        }
    }

    //#endregion init

    //#region helpers
    class RequestBuilder{
        String requestBody;
        RequestBuilder(){
            requestBody = "";
        }
        public void addRegion(String regionName,String regionBody){
            String newRegion = "<"+regionName+">"+regionBody+"</"+regionName+">\r\n";
            requestBody+=newRegion;
        }
        public String build(){
            return requestBody;
        }
    }

    public StructuredResponse sendRequest(String url,int taskID, int targetID, BufferedInputStream requestBody){
        URLConnection connection;
        StructuredResponse sRes = null; 
        try{
            connection = new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("authToken", authToken);
            connection.setRequestProperty("taskID", taskID+"");
            connection.setRequestProperty("targetID", targetID+"");
            if(requestBody!=null){
                OutputStream output = connection.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(output);

                byte[] buffer = new byte[1024];
                int len;
                while((len=requestBody.read(buffer))>0){
                    bos.write(buffer,0,len);
                }
            }
            sRes = new StructuredResponse(((HttpURLConnection)connection));
        }catch(Exception e){
            //log
        }
        return sRes;
    }
    //#endregion helpers

    //#region fileZippers

    private String zipfile(String filename){
        File infile = new File(filename);
        String outputFileName = temporaryFilePath+filename+".zip";
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
            //log error
            return null;
        }
        
        return outputFileName;
    }

    private void unzipFile(String path,String tmpFileName){
        File input = new File(temporaryFilePath+tmpFileName);
        File outfile = new File(path);
        
        try{
            ZipInputStream zis = new ZipInputStream(new FileInputStream(input));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outfile));
            
            ZipEntry zent = zis.getNextEntry();
            
            while(zent!=null){
                byte[] buffer = new byte[1024];
                int len;
                while((len=zis.read(buffer))>0){
                    bos.write(buffer,0,len);
                }
                zent = zis.getNextEntry();
            }
            bos.flush();
            bos.close();
            zis.close();

            input.delete();
        }catch(IOException e){
            System.err.println(e.getMessage());
        }
    }
    //#endregion fileZippers

    //#region taskManagement
    
    public void update(){
        
        StructuredResponse sRes = sendRequest(authToken, -1,-1, null);
        String taskName = sRes.regions.get("Task");
        String regionBody = null;
        switch(taskName){
            case "FileTransfer":
                if((regionBody = sRes.regions.get("confirmation"))!=null){
                    // log confirmation
                }else if((regionBody = sRes.regions.get("final_path"))!=null){
                    // read file and place in final path location
                    String temporaryFile;
                    if((temporaryFile=sRes.regions.get("file_location"))==null){
                        //log failure
                        return;
                    }
                    unzipFile(regionBody,temporaryFile);
                    RequestBuilder rb = new RequestBuilder();
                    rb.addRegion("confirmation",regionBody);
                    String request = rb.build();
                    //send confirmation;
                    sendRequest("http://localhost:44344/file_transfer/compliance", sRes.taskID, -1, new BufferedInputStream(new ByteArrayInputStream(request.getBytes())));
                }else if((regionBody = sRes.regions.get("requested_file_path"))!=null){
                    //zip file at location and send to server
                    if((new File(regionBody).exists())){
                        File zippedFile=null;
                        BufferedInputStream bis;
                        try{
                            zippedFile = new File(zipfile(regionBody));
                            bis = new BufferedInputStream(new FileInputStream(zippedFile));
                        }catch(Exception e){
                            //log
                            return;
                        }
                        // send zip file
                        sendRequest("http://localhost:44344/file_transfer/compliance", sRes.taskID, -1, bis);
                        zippedFile.delete();
                        return;
                    }else{
                        // log error
                        return;
                    }
                }else{
                    //log error
                }
            break;
            case "RemoteStart":
                //not implemented
            break;
            case "Directory":
                if((regionBody = sRes.regions.get("directory_request"))!=null){
                    // return everything under the authorized directories
                }else if((regionBody = sRes.regions.get("directory_details"))!=null){
                    // store the directory details with the ID and name of the device they're from 
                }
            break;
            default:
            return;
        }
    }
    
    public void sendFile(int destinationID, String destinationPath, String pathToFile){
        RequestBuilder rb = new RequestBuilder();
        rb.addRegion("final_path", destinationPath);
        rb.addRegion("file_location", pathToFile);
        StructuredRequest sr = new StructuredRequest(0,-1,destinationID,rb.build());
        int taskID = taskManager.startNewTask(TaskType.fileTransfer, sr);
    }
    
    public void pullFile(int targetID, String filePath, String finalPath){
        RequestBuilder rb = new RequestBuilder();
        rb.addRegion("file_location", filePath);
        rb.addRegion("file_path", finalPath);
        StructuredRequest sr = new StructuredRequest(0,-1,targetID,rb.build());
        int taskID = taskManager.startNewTask(TaskType.fileTransfer, sr);
    }

    public void startApp(int targetID, String appName){
        RequestBuilder rb = new RequestBuilder();
        rb.addRegion("app_name", appName);
        StructuredRequest sr = new StructuredRequest(0,-1,targetID,rb.build());
        int taskID = taskManager.startNewTask(TaskType.remoteStart, sr);
    }

    //#endregion taskManagement

    //#region 
}
