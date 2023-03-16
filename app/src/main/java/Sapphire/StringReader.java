package Sapphire;

import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

public class StringReader {
    String inputfile = "secret.input";      // change secret.input to strings.input
    static HashMap<String,String> strings;
    public StringReader() throws FileNotFoundException{
        strings = new HashMap<String,String>();
        InputStream is = getClass().getClassLoader().getResourceAsStream(inputfile);  
        Scanner inputReader = new Scanner(is);
        while(inputReader.hasNext()){
            String nextLine = inputReader.nextLine();
            if(nextLine.length()>0){
                if(nextLine.charAt(0)!='#'){ //basically a comment
                    String[] seperatedLine = nextLine.split("::");
                    strings.put(seperatedLine[0],seperatedLine[1]);
                }
            }
        }
        inputReader.close();
    }
    public String getString(String stringName){
        return strings.get(stringName);
    }
}
