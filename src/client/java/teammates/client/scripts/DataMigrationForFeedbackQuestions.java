package teammates.client.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.logic.api.Logic;
import teammates.logic.core.FeedbackSessionsLogic;
import teammates.storage.api.FeedbackQuestionsDb;
import teammates.storage.api.FeedbackSessionsDb;
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
        
        List<FeedbackQuestionAttributes> feedbackQuestionAttributes =
                getFeedbackQuestionAttributesFromFeedbackQuestions(feedbackQuestions);
        for (FeedbackQuestionAttributes old : feedbackQuestionAttributes) {
            FeedbackSessionAttributes session = new Logic().getFeedbackSession(old.getFeedbackSessionName(), old.getCourseId());
            if (session == null) {
                System.out.println("question: " + old.getIdentificationString());
                System.out.println(String.format("error finding session %s", old.getFeedbackSessionName() + ":" + old.getCourseId()));
                System.out.println("possibly due to orphaned responses");
                continue;
            }
            System.out.println("worked");
            
            try {
                new FeedbackSessionsDb().addQuestionToSession(session, old);
            } catch (EntityDoesNotExistException | EntityAlreadyExistsException | InvalidParametersException e) {
                e.printStackTrace();
                throw new RuntimeException(
                        String.format("Unable to update existing session %s with question %s",
                                      session.getIdentificationString(),
                                      old.getIdentificationString()),
                        e);
            }
        }
        Datastore.getPersistenceManager().close();
    }
    
    public static List<FeedbackQuestionAttributes> getFeedbackQuestionAttributesFromFeedbackQuestions(
            Collection<FeedbackQuestion> questions) {
        List<FeedbackQuestionAttributes> fqList = new ArrayList<FeedbackQuestionAttributes>();
        
        for (FeedbackQuestion question : questions) {
            if (!JDOHelper.isDeleted(question)) {
                fqList.add(new FeedbackQuestionAttributes(new Question(question)));
            }
        }
        
        Collections.sort(fqList);
        return fqList;
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
