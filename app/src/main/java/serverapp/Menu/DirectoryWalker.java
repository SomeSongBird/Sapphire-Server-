package serverapp.Menu;

import java.io.File;

class DirectoryWalker{
    public String[] fileStructure;
    public DirectoryWalker(String dir_path){
        File[] dir_name = {new File(dir_path)};
        fileStructure = this.walk(dir_name[0].getName(),dir_name);
    }

    private String[] walk(String root_path,File[] dirs){
        String[] file_names = new String[dirs.length];
        int index = 0;
        for(File fi : dirs){
            file_names[index++] = root_path+"/"+fi.getName();
            if(fi.isDirectory()){
                file_names = concat(file_names,walk(root_path+"/"+fi.getName(),fi.listFiles()));
            }
        }
        return file_names;
    
    }

    private String[] concat(String[] s1,String[] s2){
        String[] forRet = new String[s1.length+s2.length];
        int i=0;
        for(;i<s1.length;i++){
            forRet[i] = s1[i];
        }
        for(;i<s1.length+s2.length;i++){
            forRet[i] = s2[i-s1.length];
        }
        return forRet;
    }

    public static void main(String[] args) {
        String dir = "/home/usr";
        DirectoryWalker dw = new DirectoryWalker(dir);

        for(String file :dw.fileStructure){
            System.out.println(file);
        }

    }
}