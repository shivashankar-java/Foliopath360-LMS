package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.CourseProgressResponse;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponse enroll(User student, UUID courseId);

    /**
     * Called by the payment flow after a successful payment.
     * Creates or re-activates the enrollment without further checks -
     * the course has already been validated at cart and checkout time.
     */
    EnrollmentResponse enrollAfterPayment(User student, Course course);

    EnrollmentResponse dropEnrollment(User student, UUID courseId);

    List<StudentEnrolledCourseResponse> getMyEnrollments(User student);

    EnrollmentResponse getEnrollment(User student, UUID courseId);

    CourseProgressResponse markLessonCompleted(User student, UUID lessonId);

    CourseProgressResponse markLessonIncomplete(User student, UUID lessonId);

    CourseProgressResponse getCourseProgress(User student, UUID courseId);
}
