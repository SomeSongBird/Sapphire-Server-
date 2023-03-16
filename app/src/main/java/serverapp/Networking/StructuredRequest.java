package serverapp.Networking;

import spark.Request;
import java.util.regex.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

public class StructuredRequest {
    //#region init
    private static long fileID = 0;
    public String authToken;
    public int clientID; // -1 to start a new task
    public int taskID;
    public int targetID; // -1 if responding to the server
    public HashMap<String,String> extraDetails;

    public StructuredRequest(Request req){
        /* constructor for client requests over the network */
        // placing the headers to appropriate fields
        authToken = req.headers("authToken");
        taskID = Integer.getInteger(req.headers("taskID"));
        targetID = Integer.getInteger(req.headers("targetID"));
        
        for(String regionName : getRegionNames(req.body())){
            // place the regions into the details to be indexed
            if(regionName.equals("File")){
                // files are stored in a temporary file and what's stored is the file location
                extraDetails.put("file_location","/temporary/"+(fileID++)+".tmp");
                File temp_file = new File(extraDetails.get("file_location"));
                try{
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(req.raw().getInputStream());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(temp_file));

                    byte[] buffer = new byte[1024];
                    int len;
                    boolean started=false,ended=false;
                    while(((len=bufferedInputStream.read(buffer))>0)&&(!ended)){ //process the bytes in chunks so only the file portion get placed into the file
                        int[] regionbounds = getFileBounds(buffer);
                        if(!started){
                            if(regionbounds[0]!=-1){
                                started = true;
                                if(regionbounds[1]==-1){
                                    regionbounds[1] = len;
                                }
                                byte[] tmp = Arrays.copyOfRange(buffer, regionbounds[0], regionbounds[1]);
                                bufferedOutputStream.write(tmp,0,tmp.length);
                            }
                        }else{
                            if(regionbounds[1]==-1){
                                regionbounds[1]=len;
                            }else{
                                ended=true;
                            }
                            byte[] tmp = Arrays.copyOfRange(buffer, 0, regionbounds[1]);
                            bufferedOutputStream.write(tmp,0,tmp.length);
                        }
                    }

                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    bufferedInputStream.close();
                } //the file will not exist but java will yell at me if the errors aren't handled
                catch(Exception e){}
            }else{
                extraDetails.put(regionName, findRegionBody(req.body(),regionName));
            }
        }
    }
    
    public StructuredRequest(int clientID, int taskID, int targetID, String body){
        /* constructor for the MenuMiniClient to make new requests */
        
        this.clientID = clientID;
        this.taskID = taskID;
        this.targetID = targetID;

        for(String regionName : getRegionNames(body)){
            // place the regions into the details to be indexed
            // Since files are zipped and stored locally, there's no need to save the file's content locally with a new name
            extraDetails.put(regionName, findRegionBody(body,regionName));
        }
    }

    //#endregion init

    //#region helpers
    private String[] getRegionNames(String input){
        String[] regionNames = new String[0];
        // placing the body into a usable form based on the regions they're in
        Pattern regionPattern = Pattern.compile("<(.)*>(.)*<\\/(.)*>");
        Matcher matcher = regionPattern.matcher(input);
        
        // find a full region
        while(matcher.find()){
            String region = input.substring(matcher.start(),matcher.end()); // get a string of just that region
            String regionName = region.substring(region.indexOf("<")+1,region.indexOf(">")-1); 
            append(regionNames,regionName);
        }
        return regionNames;
    }

    private String[] append(String[] arr,String str){
        String[] returnArr = new String[arr.length+1];
        System.arraycopy(arr, 0, returnArr, 0, arr.length);
        returnArr[returnArr.length-1] = str;
        return returnArr;
    }

    private String findRegionBody(String input, String regionName){
        Pattern regionPattern = Pattern.compile("<"+regionName+">.*<\\/"+regionName+">");
        Matcher matcher = regionPattern.matcher(input);
        // find a full region
        if(matcher.find()){
            String region = input.substring(matcher.start(),matcher.end()); // get a string of just that region
            Pattern pat = Pattern.compile("<\\/*"+regionName+">"); 
            Matcher mat = pat.matcher(region);
            mat.find();
            int start = mat.end();
            mat.find();
            int end = mat.start();
            return region.substring(start,end);
        }else{
            return "error";
        }
    }

    private int[] getFileBounds(byte[] input){
        byte[] startRegionNameBytes = ("<File>").getBytes();
        byte[] endRegionNameBytes = ("</File>").getBytes();
        int regionNameSize = startRegionNameBytes.length;
        int[] regionBounds = {-1,-1};
        for(int i=0;i<input.length;i++){
            byte[] slice = Arrays.copyOfRange(input, i, (i+regionNameSize));
            byte[] slice2 = Arrays.copyOfRange(input, i, (i+regionNameSize+1));
            if(Arrays.equals(slice, startRegionNameBytes)&&(regionBounds[0]==-1)){
                regionBounds[0] = i+regionNameSize; 
                i+= regionNameSize;
            }else if(Arrays.equals(slice2, endRegionNameBytes)&&(regionBounds[1]==-1)){
                regionBounds[1] = i-1;
                break;
            }
        }
        return regionBounds;
    }
    //#endregion helpers
}
