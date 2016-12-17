package teammates.logic.core;

import java.util.ArrayList;
import java.util.List;

import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.InstructorSearchResultBundle;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.EmailWrapper;
import teammates.common.util.FieldValidator;
import teammates.common.util.Logger;
import teammates.common.util.StringHelper;
import teammates.storage.api.InstructorsDb;

/**
 * Handles  operations related to instructor roles.
 */
public class InstructorsLogic {
    //The API of this class doesn't have header comments because it sits behind
    //  the API of the logic class. Those who use this class is expected to be
    //  familiar with the its code and Logic's code. Hence, no need for header
    //  comments.
    
    private static final InstructorsDb instructorsDb = new InstructorsDb();
    private static final AccountsLogic accountsLogic = AccountsLogic.inst();
    private static final CoursesLogic coursesLogic = CoursesLogic.inst();
    private static final CommentsLogic commentsLogic = CommentsLogic.inst();
    private static final FeedbackSessionsLogic fsLogic = FeedbackSessionsLogic.inst();
    
    private static final Logger log = Logger.getLogger();
    
    private static InstructorsLogic instance;
    
    public static InstructorsLogic inst() {
        if (instance == null) {
            instance = new InstructorsLogic();
        }
        return instance;
    }
    
    /* ====================================
     * methods related to google search API
     * ====================================
     */
    
    public void putDocument(InstructorAttributes instructor) {
        instructorsDb.putDocument(instructor);
    }
    
    public void deleteDocument(InstructorAttributes instructor) {
        instructorsDb.deleteDocument(instructor);
    }
    
    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search instructors in the whole system.
     * @param queryString
     * @param cursorString
     * @return null if no result found
     */
    public InstructorSearchResultBundle searchInstructorsInWholeSystem(String queryString, String cursorString) {
        return instructorsDb.searchInstructorsInWholeSystem(queryString, cursorString);
    }
    
    /* ====================================
     * ====================================
     */
    
    public InstructorAttributes createInstructor(InstructorAttributes instructorToAdd)
            throws InvalidParametersException, EntityAlreadyExistsException {
        
        Assumption.assertNotNull("Supplied parameter was null", instructorToAdd);
        
        log.info("going to create instructor :\n" + instructorToAdd.toString());
        
        return instructorsDb.createInstructor(instructorToAdd);
    }

    public void setArchiveStatusOfInstructor(String googleId, String courseId, boolean archiveStatus)
           throws InvalidParametersException, EntityDoesNotExistException {
        
        InstructorAttributes instructor = instructorsDb.getInstructorForGoogleId(courseId, googleId);
        instructor.isArchived = archiveStatus;
        instructorsDb.updateInstructorByGoogleId(instructor);
    }
    
    public InstructorAttributes getInstructorForEmail(String courseId, String email) {
        
        return instructorsDb.getInstructorForEmail(courseId, email);
    }

    public InstructorAttributes getInstructorForGoogleId(String courseId, String googleId) {
        
        return instructorsDb.getInstructorForGoogleId(courseId, googleId);
    }
    
    public InstructorAttributes getInstructorForRegistrationKey(String encryptedKey) {
        
        return instructorsDb.getInstructorForRegistrationKey(encryptedKey);
    }

    public List<InstructorAttributes> getInstructorsForCourse(String courseId) {
        
        return instructorsDb.getInstructorsForCourse(courseId);
    }
    
    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId) {
        
        return getInstructorsForGoogleId(googleId, false);
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId, boolean omitArchived) {
        
        return instructorsDb.getInstructorsForGoogleId(googleId, omitArchived);
    }
    
    public String getEncryptedKeyForInstructor(String courseId, String email)
            throws EntityDoesNotExistException {
        
        verifyIsEmailOfInstructorOfCourse(email, courseId);
        
        InstructorAttributes instructor = getInstructorForEmail(courseId, email);
    
        return StringHelper.encrypt(instructor.key);
    }
    
    public List<InstructorAttributes> getInstructorsForEmail(String email) {
        
        return instructorsDb.getInstructorsForEmail(email);
    }

    /**
     * @deprecated Not scalable. Use only for admin features.
     */
    @Deprecated
    public List<InstructorAttributes> getAllInstructors() {
        
        return instructorsDb.getAllInstructors();
    }

    public boolean isGoogleIdOfInstructorOfCourse(String instructorId, String courseId) {
        
        return instructorsDb.getInstructorForGoogleId(courseId, instructorId) != null;
    }
    
    public boolean isEmailOfInstructorOfCourse(String instructorEmail, String courseId) {
       
        return instructorsDb.getInstructorForEmail(courseId, instructorEmail) != null;
    }
    
    public boolean isNewInstructor(String googleId) {
        List<InstructorAttributes> instructorList = getInstructorsForGoogleId(googleId);
        return instructorList.isEmpty()
               || instructorList.size() == 1 && coursesLogic.isSampleCourse(instructorList.get(0).courseId);
    }
    
    public void verifyInstructorExists(String instructorId)
            throws EntityDoesNotExistException {
        
        if (!accountsLogic.isAccountAnInstructor(instructorId)) {
            throw new EntityDoesNotExistException("Instructor does not exist :"
                    + instructorId);
        }
    }
    
    public void verifyIsEmailOfInstructorOfCourse(String instructorEmail, String courseId)
            throws EntityDoesNotExistException {
        
        if (!isEmailOfInstructorOfCourse(instructorEmail, courseId)) {
            throw new EntityDoesNotExistException("Instructor " + instructorEmail
                    + " does not belong to course " + courseId);
        }
    }

    /**
     * Update the name and email address of an instructor with the specific Google ID.
     * @param googleId
     * @param instructor InstructorAttributes object containing the details to be updated
     * @throws InvalidParametersException
     * @throws EntityDoesNotExistException
     */
    public void updateInstructorByGoogleId(String googleId, InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {

        // TODO: either refactor this to constant or just remove it. check not null should be in db
        Assumption.assertNotNull("Supplied parameter was null", instructor);

        coursesLogic.verifyCourseIsPresent(instructor.courseId);
        verifyInstructorInDbAndCascadeEmailChange(googleId, instructor);
        checkForUpdatingRespondents(instructor);
        
        instructorsDb.updateInstructorByGoogleId(instructor);
    }
    
    private void checkForUpdatingRespondents(InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {

        InstructorAttributes currentInstructor = getInstructorForGoogleId(instructor.courseId, instructor.googleId);
        if (!currentInstructor.email.equals(instructor.email)) {
            fsLogic.updateRespondentsForInstructor(currentInstructor.email, instructor.email, instructor.courseId);
        }
    }

    private void verifyInstructorInDbAndCascadeEmailChange(String googleId,
            InstructorAttributes instructor) throws EntityDoesNotExistException {
        InstructorAttributes instructorInDb = instructorsDb.getInstructorForGoogleId(instructor.courseId, googleId);
        if (instructorInDb == null) {
            throw new EntityDoesNotExistException("Instructor " + googleId
                    + " does not belong to course " + instructor.courseId);
        }
        // cascade comments
        if (!instructorInDb.email.equals(instructor.email)) {
            commentsLogic.updateInstructorEmail(instructor.courseId, instructorInDb.email, instructor.email);
            FeedbackResponseCommentsLogic.inst().updateFeedbackResponseCommentsEmails(
                    instructor.courseId, instructorInDb.email, instructor.email);
        }
    }
    
    /**
     * Update the Google ID and name of an instructor with the specific email.
     * @param email
     * @param instructor InstructorAttributes object containing the details to be updated
     * @throws InvalidParametersException
     * @throws EntityDoesNotExistException
     */
    public void updateInstructorByEmail(String email, InstructorAttributes instructor)
            throws InvalidParametersException, EntityDoesNotExistException {
        
        Assumption.assertNotNull("Supplied parameter was null", instructor);

        coursesLogic.verifyCourseIsPresent(instructor.courseId);
        verifyIsEmailOfInstructorOfCourse(email, instructor.courseId);
        
        instructorsDb.updateInstructorByEmail(instructor);
    }
    
    /**
     * Sends a registration email to the instructor
     * Vulnerable to eventual consistency
     */
    public EmailWrapper sendRegistrationInviteToInstructor(String courseId, String instructorEmail)
            throws EntityDoesNotExistException {
        
        CourseAttributes course = coursesLogic.getCourse(courseId);
        if (course == null) {
            throw new EntityDoesNotExistException(
                    "Course does not exist [" + courseId + "], "
                    + "trying to send invite email to instructor [" + instructorEmail + "]");
        }
        
        InstructorAttributes instructorData = getInstructorForEmail(courseId, instructorEmail);
        if (instructorData == null) {
            throw new EntityDoesNotExistException(
                    "Instructor [" + instructorEmail + "] does not exist in course [" + courseId + "]");
        }

        try {
            EmailWrapper email = new EmailGenerator().generateInstructorCourseJoinEmail(course, instructorData);
            new EmailSender().sendEmail(email);
            return email;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while sending email", e);
        }
        
    }
    
    /**
     * Sends a registration email using the instructor attributes provided instead of retrieving the instructor
     * object from the datastore
     * @param courseId
     * @param instructor InstructorAttributes object containing the details of the instructor
     * @throws InvalidParametersException
     * @throws EntityDoesNotExistException
     */
    public EmailWrapper sendRegistrationInviteToInstructor(String courseId, InstructorAttributes instructor)
            throws EntityDoesNotExistException {
        
        CourseAttributes course = coursesLogic.getCourse(courseId);
        if (course == null) {
            throw new EntityDoesNotExistException(
                    "Course does not exist [" + courseId + "], "
                    + "trying to send invite email to student [" + instructor.email + "]");
        }

        try {
            EmailWrapper email = new EmailGenerator().generateInstructorCourseJoinEmail(course, instructor);
            new EmailSender().sendEmail(email);
            return email;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while sending email", e);
        }
        
    }
    
    public String sendJoinLinkToNewInstructor(InstructorAttributes instructor, String shortName, String institute) {
        
        EmailGenerator emailGenerator = new EmailGenerator();

        try {
            EmailWrapper email = emailGenerator.generateNewInstructorAccountJoinEmail(instructor, shortName, institute);
            new EmailSender().sendEmail(email);
            return emailGenerator.generateNewInstructorAccountJoinLink(instructor, institute);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while sending email", e);
        }
    }

    public List<String> getInvalidityInfoForNewInstructorData(String shortName, String name,
                                                              String institute, String email) {
        
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<String>();
        String error;
        
        error = validator.getInvalidityInfoForPersonName(shortName);
        if (!error.isEmpty()) {
            errors.add(error);
        }
        
        error = validator.getInvalidityInfoForPersonName(name);
        if (!error.isEmpty()) {
            errors.add(error);
        }
        
        error = validator.getInvalidityInfoForEmail(email);
        if (!error.isEmpty()) {
            errors.add(error);
        }
        
        error = validator.getInvalidityInfoForInstituteName(institute);
        if (!error.isEmpty()) {
            errors.add(error);
        }
        
        //No validation for isInstructor and createdAt fields.
        return errors;
    }
    
    public void deleteInstructorCascade(String courseId, String email) {
        commentsLogic.deleteCommentsForInstructor(courseId, email);
        fsLogic.deleteInstructorFromRespondentsList(getInstructorForEmail(courseId, email));
        instructorsDb.deleteInstructor(courseId, email);
    }

    public void deleteInstructorsForGoogleIdAndCascade(String googleId) {
        List<InstructorAttributes> instructors = instructorsDb.getInstructorsForGoogleId(googleId, false);
        
        //Cascade delete instructors
        for (InstructorAttributes instructor : instructors) {
            deleteInstructorCascade(instructor.courseId, instructor.email);
        }
    }

    // this method is only being used in course logic. cascade to comments is therefore not necessary
    // as it it taken care of when deleting course
    public void deleteInstructorsForCourse(String courseId) {
        
        instructorsDb.deleteInstructorsForCourse(courseId);
    }

}
