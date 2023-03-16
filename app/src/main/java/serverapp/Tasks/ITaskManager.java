package serverapp.Tasks;

import serverapp.Networking.*;
import serverapp.Tasks.Enums.TaskType;
import spark.Response;

public interface ITaskManager{
    public int startNewTask(TaskType type, StructuredRequest r);

    public void timeoutTask(long currentTime);
    public boolean updateTasks(StructuredRequest r);
    public Object updateClient(StructuredRequest req, Response res);  
    public void shutdownSequence();

}