package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.CourseProgressResponse;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.EnrollmentMapper;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.repository.LessonProgressRepository;
import com.foliopath360.lms.repository.LessonRepository;
import com.foliopath360.lms.service.EnrollmentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentServiceImpl(
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            EnrollmentMapper enrollmentMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    @Override
    public EnrollmentResponse enroll(User student, UUID courseId) {

        Course course = findCourse(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "You can only enroll in published courses"
            );
        }

        var existing = enrollmentRepository
                .findByUserIdAndCourseId(student.getId(), courseId);

        if (existing.isPresent()) {

            Enrollment enrollment = existing.get();

            if (enrollment.getStatus() != EnrollmentStatus.DROPPED) {
                throw new IllegalArgumentException(
                        "You are already enrolled in this course"
                );
            }

            // Re-activate a dropped enrollment
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setCompletedAt(null);

            return enrollmentMapper.toResponse(
                    enrollmentRepository.save(enrollment)
            );
        }

        Enrollment enrollment = Enrollment.builder()
                .user(student)
                .course(course)
                .status(EnrollmentStatus.ENROLLED)
                .progressPercentage(0)
                .enrolledAt(LocalDateTime.now())
                .build();

        return enrollmentMapper.toResponse(
                enrollmentRepository.save(enrollment)
        );
    }

    @Override
    public EnrollmentResponse dropEnrollment(User student, UUID courseId) {

        Enrollment enrollment = getActiveEnrollment(student.getId(), courseId);

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException(
                    "You have already dropped this course"
            );
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);

        return enrollmentMapper.toResponse(
                enrollmentRepository.save(enrollment)
        );
    }

    @Override
    public List<StudentEnrolledCourseResponse> getMyEnrollments(User student) {

        return enrollmentRepository.findByUserId(student.getId())
                .stream()
                .map(enrollmentMapper::toEnrolledCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EnrollmentResponse getEnrollment(User student, UUID courseId) {

        return enrollmentMapper.toResponse(
                getActiveEnrollment(student.getId(), courseId)
        );
    }

    @Override
    public CourseProgressResponse markLessonCompleted(User student, UUID lessonId) {

        Lesson lesson = findLesson(lessonId);

        Enrollment enrollment = requireEnrolled(
                student.getId(), lesson.getModule().getCourse().getId()
        );

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(student.getId(), lessonId)
                .orElse(null);

        if (progress == null) {

            progress = LessonProgress.builder()
                    .user(student)
                    .lesson(lesson)
                    .build();
        }

        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
            lessonProgressRepository.save(progress);
        }

        recomputeProgress(enrollment);

        return buildProgressResponse(enrollment, student.getId());
    }

    @Override
    public CourseProgressResponse markLessonIncomplete(User student, UUID lessonId) {

        Lesson lesson = findLesson(lessonId);

        Enrollment enrollment = requireEnrolled(
                student.getId(), lesson.getModule().getCourse().getId()
        );

        lessonProgressRepository
                .findByUserIdAndLessonId(student.getId(), lessonId)
                .ifPresent(lessonProgressRepository::delete);

        recomputeProgress(enrollment);

        return buildProgressResponse(enrollment, student.getId());
    }

    @Override
    public CourseProgressResponse getCourseProgress(User student, UUID courseId) {

        Enrollment enrollment = getActiveEnrollment(student.getId(), courseId);

        return buildProgressResponse(enrollment, student.getId());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Course findCourse(UUID courseId) {

        return courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));
    }

    private Lesson findLesson(UUID lessonId) {

        return lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private Enrollment getActiveEnrollment(UUID userId, UUID courseId) {

        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Enrollment", "courseId", courseId
                        )
                );
    }

    private Enrollment requireEnrolled(UUID userId, UUID courseId) {

        Enrollment enrollment = getActiveEnrollment(userId, courseId);

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException(
                    "You are not enrolled in this course"
            );
        }

        return enrollment;
    }

    private void recomputeProgress(Enrollment enrollment) {

        UUID userId = enrollment.getUser().getId();
        UUID courseId = enrollment.getCourse().getId();

        long total = lessonRepository.countByCourseId(courseId);
        long done = lessonProgressRepository
                .countByUserIdAndLessonModuleCourseId(userId, courseId);

        int percentage = total == 0 ? 0 : (int) ((done * 100) / total);

        enrollment.setProgressPercentage(percentage);

        if (enrollment.getStatus() != EnrollmentStatus.DROPPED) {

            if (percentage >= 100) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                if (enrollment.getCompletedAt() == null) {
                    enrollment.setCompletedAt(LocalDateTime.now());
                }
            } else {
                if (done > 0) {
                    enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
                } else {
                    enrollment.setStatus(EnrollmentStatus.ENROLLED);
                }
                enrollment.setCompletedAt(null);
            }
        }

        enrollmentRepository.save(enrollment);
    }

    private CourseProgressResponse buildProgressResponse(
            Enrollment enrollment, UUID userId
    ) {

        UUID courseId = enrollment.getCourse().getId();

        Set<UUID> completedIds = lessonProgressRepository
                .findByUserIdAndLessonModuleCourseId(userId, courseId)
                .stream()
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        long total = lessonRepository.countByCourseId(courseId);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .enrollmentStatus(enrollment.getStatus().name())
                .totalLessons(total)
                .completedLessons(completedIds.size())
                .progressPercentage(enrollment.getProgressPercentage())
                .completedLessonIds(completedIds)
                .build();
    }
}
