package Sapphire.Tasks;

import Sapphire.Networking.*;
import Sapphire.Tasks.Enums.TaskType;
import spark.Response;

public interface ITaskManager{
    public int startNewTask(TaskType type, StructuredRequest r);

    public void timeoutTask(long currentTime);
    public boolean updateTasks(StructuredRequest req);
    public Object updateClient(StructuredRequest req, Response res); 
    public Object sendClientList(StructuredRequest req,Response res); 
    public void shutdownSequence();

}