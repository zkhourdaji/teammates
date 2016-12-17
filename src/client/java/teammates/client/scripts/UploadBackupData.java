package teammates.client.scripts;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.common.datatransfer.AccountAttributes;
import teammates.common.datatransfer.CommentAttributes;
import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.StudentProfileAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.JsonUtils;
import teammates.logic.api.Logic;
import teammates.logic.core.FeedbackQuestionsLogic;
import teammates.storage.api.CommentsDb;
import teammates.storage.api.CoursesDb;
import teammates.storage.api.FeedbackQuestionsDb;
import teammates.storage.api.FeedbackResponseCommentsDb;
import teammates.storage.api.FeedbackResponsesDb;
import teammates.storage.api.FeedbackSessionsDb;
import teammates.storage.api.InstructorsDb;
import teammates.storage.api.ProfilesDb;
import teammates.storage.api.StudentsDb;
import teammates.storage.datastore.Datastore;
import teammates.test.util.FileHelper;

/**
 * Usage: This script imports a large data bundle to the appengine. The target of the script is the app with
 * appID in the test.properties file.Can use DataGenerator.java to generate random data.
 * 
 * Notes:
 * -Edit SOURCE_FILE_NAME before use
 * -Should not have any limit on the size of the databundle. However, the number of entities per request
 * should not be set to too large as it may cause Deadline Exception (especially for evaluations)
 * 
 */
public class UploadBackupData extends RemoteApiClient {

    private static final String BACKUP_FOLDER = "BackupFiles/Backup";

    private static DataBundle data;
    private static String jsonString;
    
    private static Set<String> coursesPersisted = new HashSet<String>();
    private static HashMap<String, FeedbackQuestionAttributes> feedbackQuestionsPersisted =
            new HashMap<String, FeedbackQuestionAttributes>();
    private static HashMap<String, String> feedbackQuestionIds = new HashMap<String, String>();
    
    private static Logic logic = new Logic();
    private static final CoursesDb coursesDb = new CoursesDb();
    private static final CommentsDb commentsDb = new CommentsDb();
    private static final StudentsDb studentsDb = new StudentsDb();
    private static final InstructorsDb instructorsDb = new InstructorsDb();
    private static final FeedbackSessionsDb fbDb = new FeedbackSessionsDb();
    private static final FeedbackQuestionsDb fqDb = new FeedbackQuestionsDb();
    private static final FeedbackResponsesDb frDb = new FeedbackResponsesDb();
    private static final FeedbackResponseCommentsDb fcDb = new FeedbackResponseCommentsDb();
    private static final ProfilesDb profilesDb = new ProfilesDb();
    private static final FeedbackQuestionsLogic feedbackQuestionsLogic = new FeedbackQuestionsLogic();
    
    public static void main(String[] args) throws Exception {
        UploadBackupData uploadBackupData = new UploadBackupData();
        uploadBackupData.doOperationRemotely();
    }
    
    @Override
    protected void doOperation() {
        Datastore.initialize();
        
        String[] folders = getFolders();

        for (String folder : folders) {
            String[] backupFiles = getBackupFilesInFolder(folder);
            uploadData(backupFiles, folder);
        }
    }
    
    private static String[] getFolders() {
        File backupFolder = new File(BACKUP_FOLDER);
        String[] folders = backupFolder.list();
        List<String> listOfFolders = Arrays.asList(folders);
        Collections.sort(listOfFolders, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd HH.mm.ss");
                try {
                    Date firstDate = dateFormat.parse(o1);
                    
                    Date secondDate = dateFormat.parse(o2);
                    
                    return secondDate.compareTo(firstDate);
                } catch (ParseException e) {
                    return 0;
                }
            }
        });
        listOfFolders.toArray(folders);
        return folders;
    }
    
    private static String[] getBackupFilesInFolder(String folder) {
        String folderName = BACKUP_FOLDER + "/" + folder;
        File currentFolder = new File(folderName);
        return currentFolder.list();
    }
    
    private static void uploadData(String[] backupFiles, String folder) {
        for (String backupFile : backupFiles) {
            if (coursesPersisted.contains(backupFile)) {
                System.out.println(backupFile + " already persisted.");
                continue;
            }
            try {
                String folderName = BACKUP_FOLDER + "/" + folder;
                
                jsonString = FileHelper.readFile(folderName + "/" + backupFile);
                data = JsonUtils.fromJson(jsonString, DataBundle.class);
                
                feedbackQuestionsPersisted = new HashMap<String, FeedbackQuestionAttributes>();
                feedbackQuestionIds = new HashMap<String, String>();
                
                if (!data.accounts.isEmpty()) {
                    // Accounts
                    persistAccounts(data.accounts);
                }
                if (!data.courses.isEmpty()) {
                    // Courses
                    persistCourses(data.courses);
                }
                if (!data.instructors.isEmpty()) {
                    // Instructors
                    persistInstructors(data.instructors);
                }
                if (!data.students.isEmpty()) {
                    // Students
                    persistStudents(data.students);
                }
                if (!data.feedbackSessions.isEmpty()) {
                    // Feedback sessions
                    persistFeedbackSessions(data.feedbackSessions);
                }
                if (!data.feedbackQuestions.isEmpty()) {
                    // Feedback questions
                    persistFeedbackQuestions(data.feedbackQuestions);
                }
                if (!data.feedbackResponses.isEmpty()) {
                    // Feedback responses
                    persistFeedbackResponses(data.feedbackResponses);
                }
                if (!data.feedbackResponseComments.isEmpty()) {
                    // Feedback response comments
                    persistFeedbackResponseComments(data.feedbackResponseComments);
                }
                if (!data.comments.isEmpty()) {
                    // Comments
                    persistComments(data.comments);
                }
                if (!data.profiles.isEmpty()) {
                    // Profiles
                    persistProfiles(data.profiles);
                }
                coursesPersisted.add(backupFile);
                
            } catch (Exception e) {
                System.out.println("Error in uploading files: " + e.getMessage());
            }
        }
    }
    
    private static void persistAccounts(Map<String, AccountAttributes> accounts) {
        try {
            for (AccountAttributes accountData : accounts.values()) {
                logic.createAccount(accountData.googleId, accountData.name, accountData.isInstructor,
                                    accountData.email, accountData.institute);
            }
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading accounts: " + e.getMessage());
        }
    }
    
    private static void persistCourses(Map<String, CourseAttributes> courses) {
        try {
            coursesDb.createCourses(courses.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading courses: " + e.getMessage());
        }
    }
    
    private static void persistInstructors(Map<String, InstructorAttributes> instructors) {
        try {
            instructorsDb.createInstructors(instructors.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading instructors: " + e.getMessage());
        }
    }
    
    private static void persistStudents(Map<String, StudentAttributes> students) {
        try {
            studentsDb.createStudentsWithoutSearchability(students.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading students: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackSessions(Map<String, FeedbackSessionAttributes> feedbackSessions) {
        try {
            fbDb.createFeedbackSessions(feedbackSessions.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback sessions: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackQuestions(Map<String, FeedbackQuestionAttributes> map) {
        Map<String, FeedbackQuestionAttributes> questions = map;

        try {
            fqDb.createFeedbackQuestions(questions.values());
            
            for (FeedbackQuestionAttributes question : questions.values()) {
                feedbackQuestionsPersisted.put(question.getId(), question);
            }
            
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback questions: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackResponses(Map<String, FeedbackResponseAttributes> map) {
        Map<String, FeedbackResponseAttributes> responses = map;
        
        try {
            for (FeedbackResponseAttributes response : responses.values()) {
                response = adjustFeedbackResponseId(response);
            }
            
            frDb.createFeedbackResponses(responses.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback responses: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackResponseComments(Map<String, FeedbackResponseCommentAttributes> map) {
        Map<String, FeedbackResponseCommentAttributes> responseComments = map;

        try {
            for (FeedbackResponseCommentAttributes responseComment : responseComments.values()) {
                responseComment = adjustFeedbackResponseCommentId(responseComment);
            }
            
            fcDb.createFeedbackResponseComments(responseComments.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback response comments: " + e.getMessage());
        }
    }
    
    private static void persistComments(Map<String, CommentAttributes> map) {
        Map<String, CommentAttributes> comments = map;
        try {
            commentsDb.createComments(comments.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading comments: " + e.getMessage());
        }
    }

    private static void persistProfiles(Map<String, StudentProfileAttributes> studentProfiles) {
        Map<String, StudentProfileAttributes> profiles = studentProfiles;
        try {
            profilesDb.createEntities(profiles.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading profiles: " + e.getMessage());
        }
    }
    
    private static FeedbackResponseAttributes adjustFeedbackResponseId(FeedbackResponseAttributes response) {
        FeedbackQuestionAttributes question = feedbackQuestionsPersisted.get(response.feedbackQuestionId);
        
        if (feedbackQuestionIds.containsKey(question.getId())) {
            response.feedbackQuestionId = feedbackQuestionIds.get(question.getId());
        } else {
            String newId = feedbackQuestionsLogic.getFeedbackQuestion(
                    response.feedbackSessionName, response.courseId,
                    question.questionNumber).getId();
            response.feedbackQuestionId = newId;
            feedbackQuestionIds.put(question.getId(), newId);
        }
        
        return response;
    }
    
    private static FeedbackResponseCommentAttributes
            adjustFeedbackResponseCommentId(FeedbackResponseCommentAttributes response) {
        FeedbackQuestionAttributes question = feedbackQuestionsPersisted.get(response.feedbackQuestionId);
        
        if (feedbackQuestionIds.containsKey(question.getId())) {
            response.feedbackQuestionId = feedbackQuestionIds.get(question.getId());
        } else {
            String newId = feedbackQuestionsLogic.getFeedbackQuestion(
                    response.feedbackSessionName, response.courseId,
                    question.questionNumber).getId();
            response.feedbackQuestionId = newId;
            feedbackQuestionIds.put(question.getId(), newId);
        }
        
        return response;
    }
}
