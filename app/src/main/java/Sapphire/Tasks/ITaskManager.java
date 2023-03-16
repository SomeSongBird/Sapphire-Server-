package Sapphire.Tasks;

import Sapphire.Networking.*;
import Sapphire.Tasks.Enums.TaskType;
import spark.Response;

public interface ITaskManager{
    public int startNewTask(TaskType type, StructuredRequest r);

    public void timeoutTask(long currentTime);
    public boolean updateTasks(StructuredRequest r);
    public Object updateClient(StructuredRequest req, Response res);  
    public void shutdownSequence();

}