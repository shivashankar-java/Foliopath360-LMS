package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.InterviewKitQuestionRequest;
import com.foliopath360.lms.dto.request.InterviewKitRequest;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitQuestionResponse;
import com.foliopath360.lms.dto.response.InterviewKitResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledKitResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.InterviewKitMapper;
import com.foliopath360.lms.repository.InterviewKitEnrollmentRepository;
import com.foliopath360.lms.repository.InterviewKitQuestionRepository;
import com.foliopath360.lms.repository.InterviewKitRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.InterviewKitService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewKitServiceImpl implements InterviewKitService {

    private final InterviewKitRepository kitRepository;
    private final InterviewKitQuestionRepository questionRepository;
    private final InterviewKitEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final InterviewKitMapper mapper;

    public InterviewKitServiceImpl(
            InterviewKitRepository kitRepository,
            InterviewKitQuestionRepository questionRepository,
            InterviewKitEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            InterviewKitMapper mapper
    ) {
        this.kitRepository = kitRepository;
        this.questionRepository = questionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    // ── Admin CRUD ──────────────────────────────────────────────────

    @Override
    public InterviewKitResponse createKit(InterviewKitRequest request) {
        String slug = request.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (kitRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        InterviewKit kit = InterviewKit.builder()
                .kitCode("KIT-" + System.currentTimeMillis() % 100000)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .level(KitLevel.valueOf(request.getLevel().toUpperCase()))
                .price(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO)
                .status(KitStatus.DRAFT)
                .build();

        return mapper.toResponse(kitRepository.save(kit));
    }

    @Override
    public InterviewKitResponse updateKit(UUID kitId, InterviewKitRequest request) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        String slug = request.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        kit.setName(request.getName());
        kit.setSlug(slug);
        kit.setDescription(request.getDescription());
        kit.setThumbnailUrl(request.getThumbnailUrl());
        kit.setLevel(KitLevel.valueOf(request.getLevel().toUpperCase()));
        kit.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);

        return mapper.toResponse(kitRepository.save(kit));
    }

    @Override
    public void deleteKit(UUID kitId) {
        if (!kitRepository.existsById(kitId)) {
            throw new ResourceNotFoundException("InterviewKit", "id", kitId);
        }
        kitRepository.deleteById(kitId);
    }

    @Override
    public InterviewKitResponse publishKit(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        kit.setStatus(KitStatus.PUBLISHED);
        kit.setPublishedAt(LocalDateTime.now());
        return mapper.toResponse(kitRepository.save(kit));
    }

    @Override
    public InterviewKitResponse unpublishKit(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        kit.setStatus(KitStatus.DRAFT);
        return mapper.toResponse(kitRepository.save(kit));
    }

    @Override
    public List<InterviewKitResponse> getAllKits() {
        return kitRepository.findAll().stream()
                .map(kit -> {
                    InterviewKitResponse resp = mapper.toResponse(kit);
                    resp.setEnrollmentCount(enrollmentRepository.countByKitIdAndStatusNot(
                            kit.getId(), KitEnrollmentStatus.DROPPED));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public InterviewKitResponse getKitById(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        InterviewKitResponse resp = mapper.toResponse(kit);
        resp.setEnrollmentCount(enrollmentRepository.countByKitIdAndStatusNot(
                kit.getId(), KitEnrollmentStatus.DROPPED));
        resp.setQuestions(
                questionRepository.findByKitIdOrderByDisplayOrderAsc(kitId).stream()
                        .map(mapper::toQuestionResponse)
                        .collect(Collectors.toList())
        );
        return resp;
    }

    // ── Admin Question Management ───────────────────────────────────

    @Override
    public InterviewKitQuestionResponse addQuestion(UUID kitId, InterviewKitQuestionRequest request) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitQuestion question = InterviewKitQuestion.builder()
                .kit(kit)
                .questionType(KitQuestionType.valueOf(request.getQuestionType().toUpperCase()))
                .codeLanguage(request.getCodeLanguage())
                .question(request.getQuestion())
                .codeSnippet(request.getCodeSnippet())
                .answer(request.getAnswer())
                .displayOrder(request.getDisplayOrder())
                .build();

        return mapper.toQuestionResponse(questionRepository.save(question));
    }

    @Override
    public InterviewKitQuestionResponse updateQuestion(UUID kitId, UUID questionId, InterviewKitQuestionRequest request) {
        kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKitQuestion", "id", questionId));

        question.setQuestionType(KitQuestionType.valueOf(request.getQuestionType().toUpperCase()));
        question.setCodeLanguage(request.getCodeLanguage());
        question.setQuestion(request.getQuestion());
        question.setCodeSnippet(request.getCodeSnippet());
        question.setAnswer(request.getAnswer());
        question.setDisplayOrder(request.getDisplayOrder());

        return mapper.toQuestionResponse(questionRepository.save(question));
    }

    @Override
    public void deleteQuestion(UUID kitId, UUID questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("InterviewKitQuestion", "id", questionId);
        }
        questionRepository.deleteById(questionId);
    }

    @Override
    public List<InterviewKitQuestionResponse> getQuestions(UUID kitId) {
        return questionRepository.findByKitIdOrderByDisplayOrderAsc(kitId).stream()
                .map(mapper::toQuestionResponse)
                .collect(Collectors.toList());
    }

    // ── Admin Enrollment ────────────────────────────────────────────

    @Override
    public InterviewKitEnrollmentResponse adminEnrollStudent(UUID studentId, UUID kitId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        if (kit.getStatus() != KitStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only enroll in published kits");
        }

        return activateEnrollment(student, kit, true);
    }

    @Override
    public List<InterviewKitEnrollmentResponse> getStudentKitEnrollments(UUID studentId) {
        return enrollmentRepository.findByUserId(studentId).stream()
                .map(mapper::toEnrollmentResponse)
                .collect(Collectors.toList());
    }

    // ── Student ─────────────────────────────────────────────────────

    @Override
    public List<StudentEnrolledKitResponse> getMyEnrolledKits(User student) {
        return enrollmentRepository.findByUserId(student.getId()).stream()
                .filter(e -> e.getStatus() != KitEnrollmentStatus.DROPPED)
                .map(mapper::toStudentEnrolledKitResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InterviewKitEnrollmentResponse getMyKitEnrollment(User student, UUID kitId) {
        InterviewKitEnrollment enrollment = enrollmentRepository
                .findByUserIdAndKitId(student.getId(), kitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "InterviewKitEnrollment", "kitId", kitId));
        return mapper.toEnrollmentResponse(enrollment);
    }

    @Override
    public InterviewKitEnrollmentResponse enrollInKit(User student, UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        if (kit.getStatus() != KitStatus.PUBLISHED) {
            throw new IllegalArgumentException("You can only enroll in published kits");
        }

        if (kit.getPrice() != null && kit.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "This kit requires payment. Please add it to your cart and complete the checkout.");
        }

        return activateEnrollment(student, kit, true);
    }

    @Override
    public InterviewKitEnrollmentResponse dropKitEnrollment(User student, UUID kitId) {
        InterviewKitEnrollment enrollment = enrollmentRepository
                .findByUserIdAndKitId(student.getId(), kitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "InterviewKitEnrollment", "kitId", kitId));

        enrollment.setStatus(KitEnrollmentStatus.DROPPED);
        return mapper.toEnrollmentResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public List<InterviewKitResponse> getPublishedKits() {
        return kitRepository.findByStatus(KitStatus.PUBLISHED).stream()
                .map(kit -> {
                    InterviewKitResponse resp = mapper.toResponse(kit);
                    resp.setEnrollmentCount(enrollmentRepository.countByKitIdAndStatusNot(
                            kit.getId(), KitEnrollmentStatus.DROPPED));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse enrollStudentInKit(User student, UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        if (kit.getStatus() != KitStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only enroll in published kits");
        }

        activateEnrollment(student, kit, true);
        return MessageResponse.builder().message("Enrolled in kit successfully").build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private InterviewKitEnrollmentResponse activateEnrollment(
            User student, InterviewKit kit, boolean throwIfAlreadyActive) {

        var existing = enrollmentRepository
                .findByUserIdAndKitId(student.getId(), kit.getId());

        if (existing.isPresent()) {
            InterviewKitEnrollment enrollment = existing.get();
            if (enrollment.getStatus() != KitEnrollmentStatus.DROPPED) {
                if (throwIfAlreadyActive) {
                    throw new IllegalArgumentException("You are already enrolled in this kit");
                }
                return mapper.toEnrollmentResponse(enrollment);
            }
            enrollment.setStatus(KitEnrollmentStatus.ENROLLED);
            return mapper.toEnrollmentResponse(enrollmentRepository.save(enrollment));
        }

        InterviewKitEnrollment enrollment = InterviewKitEnrollment.builder()
                .user(student)
                .kit(kit)
                .status(KitEnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build();

        return mapper.toEnrollmentResponse(enrollmentRepository.save(enrollment));
    }
}
