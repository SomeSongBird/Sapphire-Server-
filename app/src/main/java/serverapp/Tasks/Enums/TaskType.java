package serverapp.Tasks.Enums;

public enum TaskType{
    fileTransfer,       // transfer file from one client to another (include a partial subversion to only pull from a client)
    directory,          // read a directory from one client and send the structure to another
    remoteStart,        // start an application on a client
    updateConnection    // change details of a connection (not used)
}