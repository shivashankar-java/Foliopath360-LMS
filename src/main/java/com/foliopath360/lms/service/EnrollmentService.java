package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.CourseProgressResponse;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponse enroll(User student, UUID courseId);

    EnrollmentResponse dropEnrollment(User student, UUID courseId);

    List<StudentEnrolledCourseResponse> getMyEnrollments(User student);

    EnrollmentResponse getEnrollment(User student, UUID courseId);

    CourseProgressResponse markLessonCompleted(User student, UUID lessonId);

    CourseProgressResponse markLessonIncomplete(User student, UUID lessonId);

    CourseProgressResponse getCourseProgress(User student, UUID courseId);
}
