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
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentServiceImpl(
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            EnrollmentMapper enrollmentMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
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

        if (course.getPrice() != null
                && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "This course requires payment. Please add it to your cart "
                            + "and complete the checkout."
            );
        }

        return activateEnrollment(student, course, true);
    }

    @Override
    public EnrollmentResponse enrollAfterPayment(User student, Course course) {
        // The course was already validated at cart and checkout time;
        // payment success is the only gate required here. Idempotent:
        // verify + webhook may both trigger this for the same order.
        return activateEnrollment(student, course, false);
    }

    private EnrollmentResponse activateEnrollment(
            User student, Course course, boolean throwIfAlreadyActive) {

        var existing = enrollmentRepository
                .findByUserIdAndCourseId(student.getId(), course.getId());

        if (existing.isPresent()) {

            Enrollment enrollment = existing.get();

            if (enrollment.getStatus() != EnrollmentStatus.DROPPED) {
                if (throwIfAlreadyActive) {
                    throw new IllegalArgumentException(
                            "You are already enrolled in this course"
                    );
                }
                return enrollmentMapper.toResponse(enrollment);
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

    @Override
    public EnrollmentResponse adminEnrollStudent(UUID studentId, UUID courseId) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", "id", studentId));

        Course course = findCourse(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Can only enroll in published courses"
            );
        }

        return activateEnrollment(student, course, true);
    }

    @Override
    public EnrollmentResponse adminEnrollStudentWithPayment(
            UUID studentId, UUID courseId,
            java.math.BigDecimal discountAmount,
            java.math.BigDecimal amountPaid,
            String paymentMethod) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", "id", studentId));

        Course course = findCourse(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Can only enroll in published courses"
            );
        }

        java.math.BigDecimal coursePrice = course.getPrice() == null
                ? java.math.BigDecimal.ZERO : course.getPrice();
        java.math.BigDecimal discount = discountAmount == null
                ? java.math.BigDecimal.ZERO : discountAmount;
        java.math.BigDecimal paid = amountPaid == null
                ? java.math.BigDecimal.ZERO : amountPaid;

        if (paid.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (paid.add(discount).compareTo(coursePrice) > 0) {
            throw new IllegalArgumentException(
                    "Amount paid + discount cannot exceed the course fee"
            );
        }

        // Build a PAID order + an order item snapshot for the course.
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(student)
                .totalAmount(coursePrice)
                .discountAmount(discount)
                .finalAmount(paid)
                .currency("INR")
                .status(OrderStatus.PAID)
                .paidAt(java.time.LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .course(course)
                .courseTitle(course.getTitle())
                .price(coursePrice)
                .build();

        order.getItems().add(orderItem);
        Order savedOrder = orderRepository.save(order);

        // Record a successful payment for the amount collected.
        Payment payment = Payment.builder()
                .order(savedOrder)
                .user(student)
                .amount(paid)
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(paymentMethod)
                .paidAt(java.time.LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return activateEnrollment(student, course, true);
    }

    @Override
    public EnrollmentResponse adminUnenrollStudentWithRefund(
            UUID studentId, UUID courseId, java.math.BigDecimal refundAmount) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", "id", studentId));

        Course course = findCourse(courseId);

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(studentId, courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Enrollment", "courseId", courseId));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException(
                    "Student is not currently enrolled in this course"
            );
        }

        java.math.BigDecimal refund = refundAmount == null
                ? java.math.BigDecimal.ZERO : refundAmount;

        if (refund.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative");
        }

        // Find the most recent non-cancelled order that includes this course.
        List<Order> orders = orderRepository
                .findOrdersByUserIdAndCourseId(studentId, courseId);
        Order order = orders.isEmpty() ? null : orders.get(0);

        if (order != null && order.getStatus() == OrderStatus.PAID) {
            java.math.BigDecimal amountPaid = order.getFinalAmount() == null
                    ? java.math.BigDecimal.ZERO : order.getFinalAmount();

            // Account for anything already refunded so a second unenroll-refund
            // cannot exceed what the student has actually paid (net of refunds).
            java.math.BigDecimal previouslyRefunded =
                    paymentRepository.sumRefundedByOrderId(order.getId());
            if (previouslyRefunded == null) {
                previouslyRefunded = java.math.BigDecimal.ZERO;
            }
            java.math.BigDecimal remaining =
                    amountPaid.subtract(previouslyRefunded.abs());

            if (refund.compareTo(remaining) > 0
                    || remaining.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Refund amount cannot be more than the remaining paid "
                                + "amount (\u20B9" + remaining.max(java.math.BigDecimal.ZERO) + ")"
                );
            }

            if (refund.compareTo(java.math.BigDecimal.ZERO) > 0) {
                // Record the refund as a separate negative payment row so:
                //  - it shows in the transaction list as a red, negative amount
                //  - it reverses the collected / revenue totals by the refunded
                //    portion only, keeping the remaining amount counted.
                String originalMethod = paymentRepository
                        .findFirstByOrderIdAndStatusOrderByCreatedDtDesc(
                                order.getId(), PaymentStatus.SUCCESS)
                        .map(Payment::getPaymentMethod)
                        .orElse(null);

                Payment refundPayment = Payment.builder()
                        .order(order)
                        .user(student)
                        .amount(refund.negate())
                        .currency(order.getCurrency() == null
                                ? "INR" : order.getCurrency())
                        .status(PaymentStatus.REFUNDED)
                        .paymentMethod(originalMethod)
                        .paidAt(java.time.LocalDateTime.now())
                        .build();

                paymentRepository.save(refundPayment);
            }
        }

        // Drop the enrollment (retain the history row as DROPPED).
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setCompletedAt(null);

        return enrollmentMapper.toResponse(
                enrollmentRepository.save(enrollment)
        );
    }

    @Override
    public List<EnrollmentResponse> getEnrollmentsByUserId(UUID userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(enrollmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.foliopath360.lms.dto.response.StudentPaymentInfoResponse>
            getStudentPaymentInfo(UUID userId) {

        return orderRepository.findByUserIdOrderByCreatedDtDesc(userId)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.REFUNDED)
                .flatMap(order -> order.getItems().stream()
                        .map(item -> com.foliopath360.lms.dto.response
                                .StudentPaymentInfoResponse.builder()
                                .courseId(item.getCourse().getId())
                                .courseTitle(item.getCourseTitle())
                                .courseFee(item.getPrice())
                                .discountAmount(order.getDiscountAmount())
                                .amountPaid(order.getFinalAmount())
                                .status(order.getStatus().name())
                                .build()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String generateOrderNumber() {
        String timestampPart = Long.toString(System.currentTimeMillis(), 36)
                .toUpperCase();
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "FP360-" + timestampPart + "-" + randomPart;
    }

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
