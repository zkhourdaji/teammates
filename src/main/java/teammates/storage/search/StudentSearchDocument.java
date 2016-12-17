package teammates.storage.search;

import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;

public class StudentSearchDocument extends SearchDocument {

    private StudentAttributes student;
    private CourseAttributes course;
    
    public StudentSearchDocument(StudentAttributes student) {
        this.student = student;
    }
    
    @Override
    protected void prepareData() {
        if (student == null) {
            return;
        }
        
        course = logic.getCourse(student.course);
    }

    @Override
    public Document toDocument() {
        
        String delim = ",";
        
        //produce searchableText for this student document:
        //it contains
        //courseId, courseName, studentEmail, studentName
        //studentTeam and studentSection
        StringBuilder searchableTextBuilder = new StringBuilder("");
        searchableTextBuilder.append(student.course).append(delim)
                             .append(course == null ? "" : course.getName()).append(delim)
                             .append(student.email).append(delim)
                             .append(student.name).append(delim)
                             .append(student.team).append(delim)
                             .append(student.section);
        
        Document doc = Document.newBuilder()
                // this is used to filter documents visible to certain instructor
                .addField(Field.newBuilder().setName(Const.SearchDocumentField.COURSE_ID)
                                            .setText(student.course))
                // searchableText and createdDate are used to match the query string
                .addField(Field.newBuilder().setName(Const.SearchDocumentField.SEARCHABLE_TEXT)
                                            .setText(searchableTextBuilder.toString()))
                // attribute field is used to convert a doc back to attribute
                .addField(Field.newBuilder().setName(Const.SearchDocumentField.STUDENT_ATTRIBUTE)
                                            .setText(JsonUtils.toJson(student)))
                .setId(student.key)
                .build();
        
        return doc;
    }
}
