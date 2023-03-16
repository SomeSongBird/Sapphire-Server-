package serverapp.Networking;

//#region imports
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.*;
import java.io.*;
import java.net.HttpURLConnection;
//#endregion imports

public class StructuredResponse {
    public static long fileID = 1;
    public HashMap<String,String> regions;
    public int taskID;

    public StructuredResponse(HttpURLConnection res){
        Map<String,List<String>> headers = res.getRequestProperties();
        taskID = Integer.getInteger(headers.get("TaskID").get(0));
        Scanner s = null;
        try{
            s = new Scanner(res.getInputStream());
        }catch(Exception e){}
            String sResponseBody ="";
        while(s.hasNext()){
            sResponseBody += s.next();
        }
        s.close();
        
        for(String regionName : getRegionNames(sResponseBody)){
            // place the regions into the details to be indexed
            if(regionName.equals("File")){
                // files are stored in a temporary file and what's stored is the file location
                regions.put("file_location","/temporary_files/"+(fileID++)+".tmp");
                File temp_file = new File(regions.get("file_location"));
                try{
                    res.getInputStream().reset();
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(res.getInputStream());
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
                regions.put(regionName, findRegionBody(sResponseBody,regionName));
            }
        }
    }

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
