package teammates.client.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.storage.datastore.Datastore;
import teammates.storage.entity.FeedbackQuestion;
import teammates.storage.entity.Question;

/**
 * Script to create a Question copy of old FeedbackQuestions.
 * 
 * Uses low level DB calls for efficiency.
 */
public class DataMigrationForFeedbackQuestions extends RemoteApiClient {
    
    private final int numDays = Integer.MAX_VALUE;
    
    public static void main(String[] args) throws IOException {
        new DataMigrationForFeedbackQuestions().doOperationRemotely();
    }

    @SuppressWarnings("unused")
    @Override
    protected void doOperation() {
        
        Datastore.initialize();
        List<FeedbackQuestion> feedbackQuestions;
        if (numDays == Integer.MAX_VALUE) {
            feedbackQuestions = getAllOldQuestions();
        } else {
            Calendar startCal = Calendar.getInstance();
            startCal.add(Calendar.DAY_OF_YEAR, -1 * numDays);
            
            feedbackQuestions = getOldQuestionsSince(startCal.getTime());
        }
        
        for (FeedbackQuestion old : feedbackQuestions) {
            Datastore.getPersistenceManager().makePersistent(new Question(old));
        }
        Datastore.getPersistenceManager().close();
    }

    private List<FeedbackQuestion> getAllOldQuestions() {
        String query = "SELECT FROM " + FeedbackQuestion.class.getName();
        @SuppressWarnings("unchecked")
        List<FeedbackQuestion> feedbackQuestions = 
            (List<FeedbackQuestion>) Datastore.getPersistenceManager().newQuery(query).execute();
        return feedbackQuestions;
    }
    
    private List<FeedbackQuestion> getOldQuestionsSince(Date date) {
        String query = "SELECT FROM " + FeedbackQuestion.class.getName()
                        + "WHERE this.updatedAt >= startDate"
                        + "PARAMETERS java.util.Date startDate";
        @SuppressWarnings("unchecked")
        List<FeedbackQuestion> feedbackQuestions = 
            (List<FeedbackQuestion>) Datastore.getPersistenceManager().newQuery(query).execute(date);
        return feedbackQuestions;
    }
}
