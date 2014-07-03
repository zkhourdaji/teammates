package teammates.test.cases;

import static org.testng.AssertJUnit.assertEquals;
import java.util.Map;

import teammates.common.util.ThreadHelper;

import com.google.appengine.api.taskqueue.dev.LocalTaskQueueCallback;

@SuppressWarnings("serial")
public abstract class BaseTaskQueueCallback implements LocalTaskQueueCallback {

    public static int taskCount;
    
    @Override
    public void initialize(Map<String, String> arg0) {
        taskCount = 0;
    }
    
    public static void resetTaskCount() {
        taskCount = 0;
    }
    
    public static boolean verifyTaskCount(int expectedCount) {
        waitForTaskQueueExecution(expectedCount, 0);
        if(expectedCount == BaseTaskQueueCallback.taskCount){
            return true;
        } else {
            return false;
        }
    }

    public static void waitForTaskQueueExecution(int expectedNumberOfTasks) {
        waitForTaskQueueExecution(expectedNumberOfTasks, 5);
    }
    
    public static void waitForTaskQueueExecution(int tasks, int buffer) {
        /*
         *  Current rate of task execution is 1/s
         *  Wait for 1 more second to see if erroneous or unwanted tasks
         *  are added too
         */
        ThreadHelper.waitFor((tasks + buffer) * 1000);
    }
}
