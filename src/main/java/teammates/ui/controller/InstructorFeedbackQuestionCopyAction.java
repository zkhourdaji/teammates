package teammates.ui.controller;

import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.Const.StatusMessageColor;
import teammates.common.util.StatusMessage;
import teammates.logic.api.GateKeeper;

public class InstructorFeedbackQuestionCopyAction extends Action {

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        InstructorAttributes instructorDetailForCourse = logic.getInstructorForGoogleId(courseId, account.googleId);

        FeedbackSessionAttributes fsa = logic.getFeedbackSession(feedbackSessionName, courseId);
        new GateKeeper().verifyAccessible(instructorDetailForCourse,
                                          fsa,
                                          false, Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION);

        String instructorEmail = instructorDetailForCourse.email;

        try {
            int index = 0;
            String feedbackQuestionId = getRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID + "-" + index);
            String oldCourseId = getRequestParamValue(Const.ParamsNames.COURSE_ID + "-" + index);
            String oldFeedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME + "-" + index);
            statusToAdmin = "";

            while (feedbackQuestionId != null) {
                FeedbackQuestionAttributes feedbackQuestion =
                        logic.copyFeedbackQuestion(oldCourseId, oldFeedbackSessionName, feedbackQuestionId, fsa, instructorEmail);

                index++;

                feedbackQuestionId = getRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID + "-" + index);
                oldCourseId = getRequestParamValue(Const.ParamsNames.COURSE_ID + "-" + index);
                oldFeedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME + "-" + index);

                statusToAdmin += "Created Feedback Question for Feedback Session:<span class=\"bold\">("
                                 + feedbackQuestion.feedbackSessionName + ")</span> for Course <span class=\"bold\">["
                                 + feedbackQuestion.courseId + "]</span> created.<br>"
                                 + "<span class=\"bold\">"
                                 + feedbackQuestion.getQuestionDetails().getQuestionTypeDisplayName()
                                 + ":</span> " + feedbackQuestion.getQuestionDetails().getQuestionText();
            }

            if (index > 0) {
                statusToUser.add(new StatusMessage(Const.StatusMessages.FEEDBACK_QUESTION_ADDED,
                                                   StatusMessageColor.SUCCESS));
            } else {
                statusToUser.add(new StatusMessage("No questions are indicated to be copied", StatusMessageColor.DANGER));
                isError = true;
            }
        } catch (InvalidParametersException | EntityAlreadyExistsException e) {
            statusToUser.add(new StatusMessage(e.getMessage(), StatusMessageColor.DANGER));
            statusToAdmin = e.getMessage();
            isError = true;
        }

        return createRedirectResult(new PageData(account)
                                            .getInstructorFeedbackEditLink(courseId, feedbackSessionName));
    }
}
