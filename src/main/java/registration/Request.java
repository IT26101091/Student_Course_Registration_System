package registration;

public class Request {
    private String studentId;
    private String courseId;
    private long timestamp;

    public Request(String studentId, String courseId, long timestamp) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.timestamp = timestamp;
    }

    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public long getTimestamp() { return timestamp; }
}