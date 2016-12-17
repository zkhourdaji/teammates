package teammates.test.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teammates.common.datatransfer.AccountAttributes;
import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.StudentProfileAttributes;
import teammates.common.exception.NotImplementedException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;
import teammates.common.util.ThreadHelper;
import teammates.logic.backdoor.BackDoorServlet;

import com.google.gson.reflect.TypeToken;

/**
 * Used to access the datastore without going through the UI. The main use of
 * this class is for the test suite to prepare test data. <br>
 * It works only if the test.backdoor.key in test.properties matches the
 * app.backdoor.key in build.properties of the deployed app. Using this
 * mechanism we can limit back door access to only the person who deployed the
 * application.
 * 
 */
public final class BackDoor {
    private static final int RETRY_DELAY_IN_MILLISECONDS = 5000;
    
    private BackDoor() {
        //utility class
    }

    public static String putDocumentsForStudents(String dataBundleJson) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_PUT_DOCUMENTS_FOR_STUDENTS);
        params.put(BackDoorServlet.PARAMETER_DATABUNDLE_JSON, dataBundleJson);
        return makePostRequest(params);
    }

    /**
     * This persists the given data if no such data already exists in the
     * datastore.
     * 
     * @param dataBundleJson
     * @return
     */
    public static String persistNewDataBundle(String dataBundleJson) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_PERSIST_DATABUNDLE);
        params.put(BackDoorServlet.PARAMETER_DATABUNDLE_JSON, dataBundleJson);
        return makePostRequest(params);
    }
    
    /**
     * This create documents for entities through back door
     * @param dataBundleJson
     * @return
     */
    public static String putDocumentsInBackDoor(String dataBundleJson) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_PUT_DOCUMENTS);
        params.put(BackDoorServlet.PARAMETER_DATABUNDLE_JSON, dataBundleJson);
        return makePostRequest(params);
    }
    
    /**
     * Removes given data. If given entities have already been deleted,
     * they are ignored
     * 
     * @param dataBundleJson
     * @return
     */
    private static String removeDataBundle(String dataBundleJson) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_REMOVE_DATABUNDLE);
        params.put(BackDoorServlet.PARAMETER_DATABUNDLE_JSON, dataBundleJson);
        return makePostRequest(params);
    }
    
    /**
     * Removes and restores given data.
     * 
     * @param dataBundleJson
     * @return
     */
    private static String removeAndRestoreDataBundle(String dataBundleJson) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_REMOVE_AND_RESTORE_DATABUNDLE);
        params.put(BackDoorServlet.PARAMETER_DATABUNDLE_JSON, dataBundleJson);
        return makePostRequest(params);
    }

    /**
     * Persists given data. If given entities already exist in the data store,
     * they will be overwritten.
     * 
     * @param dataBundleJson
     * @return
     */
    public static String restoreDataBundle(String dataBundleJson) {
        return persistNewDataBundle(dataBundleJson);
    }
    
    /**
     * Removes given data. If given entities have already been deleted,
     * it fails silently
     * 
     * @param dataBundleJson
     * @return
     */
    public static String removeDataBundleFromDb(DataBundle dataBundle) {
        String json = JsonUtils.toJson(dataBundle);
        return removeDataBundle(json);
    }
    
    public static String removeAndRestoreDataBundleFromDb(DataBundle dataBundle) {
        String json = JsonUtils.toJson(dataBundle);
        return removeAndRestoreDataBundle(json);
    }

    /**
     * Persists given data. If given entities already exist in the data store,
     * they will be overwritten.
     */
    public static String restoreDataBundle(DataBundle dataBundle) {
        String json = JsonUtils.toJson(dataBundle);
        return persistNewDataBundle(json);
    }
    
    public static String putDocuments(DataBundle dataBundle) {
        String json = JsonUtils.toJson(dataBundle);
        return putDocumentsInBackDoor(json);
    }

    /**
     * Deletes instructors contained in the jsonString
     * 
     * @param jsonString
     */
    public static void deleteInstructors(String jsonString) {
        DataBundle data = JsonUtils.fromJson(jsonString, DataBundle.class);
        deleteInstructors(data);
    }

    private static void deleteInstructors(DataBundle data) {
        Map<String, InstructorAttributes> instructors = data.instructors;
        for (InstructorAttributes instructor : instructors.values()) {
            deleteInstructor(instructor.email, instructor.courseId);
        }
    }
    
    /**
     * Deletes COURSES contained in the jsonString
     * 
     * This should recursively delete all INSTRUCTORS, EVALUATIONS, SUBMISSIONS and STUDENTS related
     * 
     * @param jsonString
     */
    public static void deleteCourses(String jsonString) {
        DataBundle data = JsonUtils.fromJson(jsonString, DataBundle.class);
        Map<String, CourseAttributes> courses = data.courses;
        for (CourseAttributes course : courses.values()) {
            deleteCourse(course.getId());
        }
    }
    
    /**
     * Deletes FEEDBACK SESSIONS contained in the jsonString
     * 
     * This should recursively delete all FEEDBACK QUESIONS AND RESPONSES related to the session.
     * 
     * @param jsonString
     */
    public static void deleteFeedbackSessions(DataBundle data) {
        Map<String, FeedbackSessionAttributes> feedbackSessions = data.feedbackSessions;
        for (FeedbackSessionAttributes feedbackSession : feedbackSessions.values()) {
            deleteFeedbackSession(
                    feedbackSession.getFeedbackSessionName(),
                    feedbackSession.getCourseId());
        }
    }
    
    //====================================================================================

    public static String createAccount(AccountAttributes account) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.accounts.put(account.googleId, account);
        return persistNewDataBundle(JsonUtils.toJson(dataBundle));
    }
    
    public static AccountAttributes getAccount(String googleId) {
        return JsonUtils.fromJson(getAccountAsJson(googleId), AccountAttributes.class);
    }
    
    public static StudentProfileAttributes getStudentProfile(String googleId) {
        return JsonUtils.fromJson(getStudentProfileAsJson(googleId), StudentProfileAttributes.class);
    }
    
    /**
     * If object not found in the first try, it will retry once more after a delay.
     */
    public static AccountAttributes getAccountWithRetry(String googleId) {
        AccountAttributes a = getAccount(googleId);
        if (a == null) {
            ThreadHelper.waitFor(RETRY_DELAY_IN_MILLISECONDS);
            a = getAccount(googleId);
        }
        return a;
    }
    
    public static String getAccountAsJson(String googleId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_ACCOUNT_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_GOOGLE_ID, googleId);
        return makePostRequest(params);
    }
    
    public static String getStudentProfileAsJson(String googleId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_STUDENTPROFILE_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_GOOGLE_ID, googleId);
        return makePostRequest(params);
    }
    
    public static boolean isPicturePresentInGcs(String pictureKey) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_IS_PICTURE_PRESENT_IN_GCS);
        params.put(BackDoorServlet.PARAMETER_PICTURE_KEY, pictureKey);
        return Boolean.parseBoolean(makePostRequest(params));
    }

    public static String editAccount(AccountAttributes account) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_EDIT_ACCOUNT);
        params.put(BackDoorServlet.PARAMETER_JSON_STRING, JsonUtils.toJson(account));
        return makePostRequest(params);
    }
    
    public static String uploadAndUpdateStudentProfilePicture(String googleId, String pictureKey) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_EDIT_STUDENT_PROFILE_PICTURE);
        params.put(BackDoorServlet.PARAMETER_GOOGLE_ID, googleId);
        params.put(BackDoorServlet.PARAMETER_PICTURE_DATA, pictureKey);
        return makePostRequest(params);
    }

    public static String deleteAccount(String googleId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_ACCOUNT);
        params.put(BackDoorServlet.PARAMETER_GOOGLE_ID, googleId);
        return makePostRequest(params);
    }
    
    public static String createInstructor(InstructorAttributes instructor) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.instructors.put(instructor.googleId, instructor);
        return persistNewDataBundle(JsonUtils.toJson(dataBundle));
    }

    public static String getInstructorAsJsonByGoogleId(String instructorId, String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_INSTRUCTOR_AS_JSON_BY_ID);
        params.put(BackDoorServlet.PARAMETER_INSTRUCTOR_ID, instructorId);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }
    
    public static String getInstructorAsJsonByEmail(String instructorEmail, String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_INSTRUCTOR_AS_JSON_BY_EMAIL);
        params.put(BackDoorServlet.PARAMETER_INSTRUCTOR_EMAIL, instructorEmail);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }
    
    public static InstructorAttributes getInstructorByGoogleId(String instructorId, String courseId) {
        String json = getInstructorAsJsonByGoogleId(instructorId, courseId);
        return JsonUtils.fromJson(json, InstructorAttributes.class);
    }
    
    public static InstructorAttributes getInstructorByEmail(String instructorEmail, String courseId) {
        String json = getInstructorAsJsonByEmail(instructorEmail, courseId);
        return JsonUtils.fromJson(json, InstructorAttributes.class);
    }
    
    public static String getEncryptedKeyForInstructor(String courseId, String instructorEmail) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_ENCRYPTED_KEY_FOR_INSTRUCTOR);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_INSTRUCTOR_EMAIL, instructorEmail);
        return makePostRequest(params);

    }

    public static String editInstructor(InstructorAttributes instructor)
            throws NotImplementedException {
        throw new NotImplementedException(
                "Not implemented because editing instructors is not currently allowed");
    }

    public static String deleteInstructor(String courseId, String instructorEmail) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_INSTRUCTOR);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_INSTRUCTOR_EMAIL, instructorEmail);
        return makePostRequest(params);
    }

    public static String createCourse(CourseAttributes course) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.courses.put("dummy-key", course);
        return persistNewDataBundle(JsonUtils.toJson(dataBundle));
    }

    public static String getCourseAsJson(String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_COURSE_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }
    
    public static CourseAttributes getCourse(String courseId) {
        return JsonUtils.fromJson(getCourseAsJson(courseId), CourseAttributes.class);
    }
    
    /**
     * Checks existence with a bias for non existence. If object found in the
     * first try, it will retry once more after a delay.
     */
    public static boolean isCourseNonExistent(String courseId) {
        CourseAttributes c = getCourse(courseId);
        if (c != null) {
            ThreadHelper.waitFor(RETRY_DELAY_IN_MILLISECONDS);
            c = getCourse(courseId);
        }
        return c == null;
    }

    public static String editCourse(CourseAttributes course)
            throws NotImplementedException {
        throw new NotImplementedException(
                "Not implemented because editing courses is not currently allowed");
    }

    public static String deleteCourse(String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_COURSE);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }

    public static String createStudent(StudentAttributes student) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.students.put("dummy-key", student);
        return persistNewDataBundle(JsonUtils.toJson(dataBundle));
    }

    public static String getStudentAsJson(String courseId, String studentEmail) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_STUDENT_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_STUDENT_EMAIL, studentEmail);
        return makePostRequest(params);
    }
    
    public static StudentAttributes getStudent(String courseId, String studentEmail) {
        String studentJson = getStudentAsJson(courseId, studentEmail);
        return JsonUtils.fromJson(studentJson, StudentAttributes.class);
    }

    public static List<StudentAttributes> getAllStudentsForCourse(String courseId) {
        HashMap<String, Object> params = createParamMap(
                BackDoorServlet.OPERATION_GET_ALL_STUDENTS_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        String studentJson = makePostRequest(params);
        
        List<StudentAttributes> studentList = JsonUtils
                .fromJson(studentJson, new TypeToken<List<StudentAttributes>>() { }
                .getType());
        return studentList;
    }
    
    public static String getEncryptedKeyForStudent(String courseId, String studentEmail) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_ENCRYPTED_KEY_FOR_STUDENT);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_STUDENT_EMAIL, studentEmail);
        return makePostRequest(params);
    }

    public static String editStudent(String originalEmail, StudentAttributes student) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_EDIT_STUDENT);
        params.put(BackDoorServlet.PARAMETER_STUDENT_EMAIL, originalEmail);
        params.put(BackDoorServlet.PARAMETER_JSON_STRING, JsonUtils.toJson(student));
        return makePostRequest(params);
    }

    public static String deleteStudent(String courseId, String studentEmail) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_STUDENT);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_STUDENT_EMAIL, studentEmail);
        return makePostRequest(params);
    }

    public static FeedbackSessionAttributes getFeedbackSession(String courseId,
            String feedbackSessionName) {
        String jsonString = getFeedbackSessionAsJson(feedbackSessionName, courseId);
        return JsonUtils.fromJson(jsonString, FeedbackSessionAttributes.class);
    }
    
    public static String getFeedbackSessionAsJson(String feedbackSessionName,
            String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_FEEDBACK_SESSION_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_SESSION_NAME, feedbackSessionName);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }
    
    public static String editFeedbackSession(FeedbackSessionAttributes updatedFeedbackSession) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_EDIT_FEEDBACK_SESSION);
        params.put(BackDoorServlet.PARAMETER_JSON_STRING, JsonUtils.toJson(updatedFeedbackSession));
        return makePostRequest(params);
    }
    
    public static String deleteFeedbackSession(String feedbackSessionName,
            String courseId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_FEEDBACK_SESSION);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_SESSION_NAME, feedbackSessionName);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        return makePostRequest(params);
    }

    public static FeedbackQuestionAttributes getFeedbackQuestion(String courseId,
            String feedbackSessionName, int qnNumber) {
        String jsonString = getFeedbackQuestionAsJson(feedbackSessionName, courseId, qnNumber);
        return JsonUtils.fromJson(jsonString, FeedbackQuestionAttributes.class);
    }
    
    public static FeedbackQuestionAttributes getFeedbackQuestion(String questionId) {
        String jsonString = getFeedbackQuestionForIdAsJson(questionId);
        return JsonUtils.fromJson(jsonString, FeedbackQuestionAttributes.class);
    }
    
    public static String getFeedbackQuestionForIdAsJson(String questionId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_FEEDBACK_QUESTION_FOR_ID_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_QUESTION_ID, questionId);
        return makePostRequest(params);
    }
    
    public static String getFeedbackQuestionAsJson(String feedbackSessionName,
            String courseId, int qnNumber) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_FEEDBACK_QUESTION_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_SESSION_NAME, feedbackSessionName);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_QUESTION_NUMBER, qnNumber);
        return makePostRequest(params);
    }
    
    public static String editFeedbackQuestion(FeedbackQuestionAttributes updatedFeedbackQuestion) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_EDIT_FEEDBACK_QUESTION);
        params.put(BackDoorServlet.PARAMETER_JSON_STRING, JsonUtils.toJson(updatedFeedbackQuestion));
        return makePostRequest(params);
    }

    public static String deleteFeedbackQuestion(String questionId) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_FEEDBACK_QUESTION);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_QUESTION_ID, questionId);
        return makePostRequest(params);
    }

    public static String createFeedbackResponse(FeedbackResponseAttributes feedbackResponse) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.feedbackResponses.put("dummy-key", feedbackResponse);
        return persistNewDataBundle(JsonUtils.toJson(dataBundle));
    }
    
    public static FeedbackResponseAttributes getFeedbackResponse(String feedbackQuestionId,
            String giverEmail, String recipient) {
        String jsonString = getFeedbackResponseAsJson(feedbackQuestionId, giverEmail, recipient);
        return JsonUtils.fromJson(jsonString, FeedbackResponseAttributes.class);
    }
    
    public static List<FeedbackResponseAttributes> getFeedbackResponsesForReceiverForCourse(
            String courseId, String recipientEmail) {
        HashMap<String, Object> params = createParamMap(
                BackDoorServlet.OPERATION_GET_FEEDBACK_RESPONSES_FOR_RECEIVER_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_RECIPIENT, recipientEmail);
        
        String feedbackResponsesJson = makePostRequest(params);
        
        List<FeedbackResponseAttributes> responseList = JsonUtils
                .fromJson(feedbackResponsesJson, new TypeToken<List<FeedbackResponseAttributes>>() {}
                .getType());
        return responseList;
        
    }
    
    public static List<FeedbackResponseAttributes> getFeedbackResponsesFromGiverForCourse(
            String courseId, String giverEmail) {
        HashMap<String, Object> params = createParamMap(
                BackDoorServlet.OPERATION_GET_FEEDBACK_RESPONSES_FOR_GIVER_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_COURSE_ID, courseId);
        params.put(BackDoorServlet.PARAMETER_GIVER_EMAIL, giverEmail);
        
        String feedbackResponsesJson = makePostRequest(params);
        
        List<FeedbackResponseAttributes> responseList = JsonUtils
                .fromJson(feedbackResponsesJson, new TypeToken<List<FeedbackResponseAttributes>>() {}
                .getType());
        return responseList;
    }

    public static String getFeedbackResponseAsJson(String feedbackQuestionId,
            String giverEmail, String recipient) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_GET_FEEDBACK_RESPONSE_AS_JSON);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_QUESTION_ID, feedbackQuestionId);
        params.put(BackDoorServlet.PARAMETER_GIVER_EMAIL, giverEmail);
        params.put(BackDoorServlet.PARAMETER_RECIPIENT, recipient);
        return makePostRequest(params);
    }
    
    public static String deleteFeedbackResponse(String feedbackQuestionId,
                                              String giverEmail,
                                              String recipient) {
        HashMap<String, Object> params = createParamMap(BackDoorServlet.OPERATION_DELETE_FEEDBACK_RESPONSE);
        params.put(BackDoorServlet.PARAMETER_FEEDBACK_QUESTION_ID, feedbackQuestionId);
        params.put(BackDoorServlet.PARAMETER_GIVER_EMAIL, giverEmail);
        params.put(BackDoorServlet.PARAMETER_RECIPIENT, recipient);
        return makePostRequest(params);
    }

    private static HashMap<String, Object> createParamMap(String operation) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(BackDoorServlet.PARAMETER_BACKDOOR_OPERATION, operation);

        // For Authentication
        map.put(BackDoorServlet.PARAMETER_BACKDOOR_KEY,
                TestProperties.BACKDOOR_KEY);

        return map;
    }

    private static String makePostRequest(HashMap<String, Object> map) {
        try {
            String paramString = encodeParameters(map);
            String urlString = TestProperties.TEAMMATES_URL + Const.ActionURIs.BACKDOOR;
            URLConnection conn = getConnectionToUrl(urlString);
            sendRequest(paramString, conn);
            return readResponse(conn);
        } catch (Exception e) {
            return TeammatesException.toStringWithStackTrace(e);
        }
    }

    private static String readResponse(URLConnection conn) throws IOException {
        conn.setReadTimeout(10000);
        BufferedReader rd = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), "UTF-8"));
        
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        return sb.toString();
    }

    private static void sendRequest(String paramString, URLConnection conn)
            throws IOException {
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        wr.write(paramString);
        wr.flush();
        wr.close();
    }

    private static URLConnection getConnectionToUrl(String urlString)
            throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        return conn;
    }

    private static String encodeParameters(HashMap<String, Object> map)
            throws UnsupportedEncodingException {
        StringBuilder dataStringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            dataStringBuilder.append(URLEncoder.encode(e.getKey(), "UTF-8")
                    + "=" + URLEncoder.encode(e.getValue().toString(), "UTF-8")
                    + "&");
        }
        return dataStringBuilder.toString();
    }
}
