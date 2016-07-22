package teammates.logic.automated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.log.AppLogLine;
import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogService;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.log.LogService.LogLevel;

import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.logic.core.EmailGenerator;
import teammates.logic.core.EmailSender;

@SuppressWarnings("serial")
public class CompileLogsServlet extends AutomatedRemindersServlet {
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        servletName = Const.AutomatedActionNames.AUTOMATED_LOG_COMPILATION;
        action = "send severe log notifications";

        String message = "Compiling logs for email notification";
        logMessage(req, message);
        
        List<AppLogLine> errorLogs = getErrorLogs();
        sendEmail(errorLogs);
    }
    
    private List<AppLogLine> getErrorLogs() {
        LogService logService = LogServiceFactory.getLogService();

        long endTime = new java.util.Date().getTime();
        // Sets the range to 6 minutes to slightly overlap the 5 minute email timer
        long queryRange = 1000 * 60 * 6;
        long startTime = endTime - queryRange;

        LogQuery q = LogQuery.Builder.withDefaults()
                                     .includeAppLogs(true)
                                     .startTimeMillis(startTime)
                                     .endTimeMillis(endTime)
                                     .minLogLevel(LogLevel.ERROR);
        
        Iterator<RequestLogs> logIterator = logService.fetch(q).iterator();
        List<AppLogLine> errorLogs = new ArrayList<AppLogLine>();

        while (logIterator.hasNext()) {
            RequestLogs requestLogs = logIterator.next();
            List<AppLogLine> logList = requestLogs.getAppLogLines();

            for (AppLogLine currentLog : logList) {
                LogLevel logLevel = currentLog.getLogLevel();
                
                if (LogLevel.FATAL == logLevel || LogLevel.ERROR == logLevel) {
                    errorLogs.add(currentLog);
                }
            }
        }

        return errorLogs;
    }

    private void sendEmail(List<AppLogLine> logs) {
        // Do not send any emails if there are no severe logs; prevents spamming
        if (!logs.isEmpty()) {
            EmailWrapper message = new EmailGenerator().generateCompiledLogsEmail(logs);
            new EmailSender().sendLogReport(message);
        }
    }
    
}
