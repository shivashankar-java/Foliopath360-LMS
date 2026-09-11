package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.InterviewKitQuestionRequest;
import com.foliopath360.lms.dto.request.InterviewKitModuleRequest;
import com.foliopath360.lms.dto.request.InterviewKitRequest;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitModuleResponse;
import com.foliopath360.lms.dto.response.InterviewKitQuestionResponse;
import com.foliopath360.lms.dto.response.InterviewKitResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledKitResponse;
import com.foliopath360.lms.dto.response.StudentKitPaymentInfoResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.InterviewKitMapper;
import com.foliopath360.lms.repository.InterviewKitEnrollmentRepository;
import com.foliopath360.lms.repository.InterviewKitModuleRepository;
import com.foliopath360.lms.repository.InterviewKitQuestionRepository;
import com.foliopath360.lms.repository.InterviewKitRepository;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.InterviewKitService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewKitServiceImpl implements InterviewKitService {

    private final InterviewKitRepository kitRepository;
    private final InterviewKitQuestionRepository questionRepository;
    private final InterviewKitModuleRepository moduleRepository;
    private final InterviewKitEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InterviewKitMapper mapper;

    public InterviewKitServiceImpl(
            InterviewKitRepository kitRepository,
            InterviewKitQuestionRepository questionRepository,
            InterviewKitModuleRepository moduleRepository,
            InterviewKitEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            InterviewKitMapper mapper
    ) {
        this.kitRepository = kitRepository;
        this.questionRepository = questionRepository;
        this.moduleRepository = moduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
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

        return toResponseWithQuestions(kitRepository.save(kit));
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

        return toResponseWithQuestions(kitRepository.save(kit));
    }

    @Override
    public void deleteKit(UUID kitId) {
        if (!kitRepository.existsById(kitId)) {
            throw new ResourceNotFoundException("InterviewKit", "id", kitId);
        }
        questionRepository.clearKitIdForModuleQuestions(kitId);
        questionRepository.deleteByKitIdAndModuleIdIsNull(kitId);
        kitRepository.deleteById(kitId);
    }

    @Override
    public InterviewKitResponse publishKit(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        kit.setStatus(KitStatus.PUBLISHED);
        kit.setPublishedAt(LocalDateTime.now());
        InterviewKitResponse resp = mapper.toResponse(kitRepository.save(kit));
        resp.setQuestionCount(countKitQuestions(kit));
        return resp;
    }

    @Override
    public InterviewKitResponse unpublishKit(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        kit.setStatus(KitStatus.DRAFT);
        InterviewKitResponse resp = mapper.toResponse(kitRepository.save(kit));
        resp.setQuestionCount(countKitQuestions(kit));
        return resp;
    }

    @Override
    public List<InterviewKitResponse> getAllKits() {
        return kitRepository.findAll().stream()
                .map(kit -> {
                    InterviewKitResponse resp = mapper.toResponse(kit);
                    resp.setEnrollmentCount(enrollmentRepository.countByKitIdAndStatusNot(
                            kit.getId(), KitEnrollmentStatus.DROPPED));
                    resp.setQuestionCount(countKitQuestions(kit));
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
        resp.setQuestionCount(countKitQuestions(kit));
        resp.setModules(
                kit.getModules().stream()
                        .map(this::buildModuleResponse)
                        .collect(Collectors.toList())
        );
        resp.setQuestions(buildFlatQuestions(kit));
        return resp;
    }

    // ── Admin Question Management ───────────────────────────────────

    @Override
    public InterviewKitQuestionResponse addQuestion(UUID kitId, InterviewKitQuestionRequest request) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitQuestion.InterviewKitQuestionBuilder builder = InterviewKitQuestion.builder()
                .kit(kit)
                .questionType(KitQuestionType.valueOf(request.getQuestionType().toUpperCase()))
                .codeLanguage(request.getCodeLanguage())
                .question(request.getQuestion())
                .codeSnippet(request.getCodeSnippet())
                .answer(request.getAnswer())
                .displayOrder(request.getDisplayOrder());

        if (request.getModuleId() != null && !request.getModuleId().isEmpty()) {
            UUID moduleId = UUID.fromString(request.getModuleId());
            InterviewKitModule module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("InterviewKitModule", "id", moduleId));
            builder.module(module);
        }

        return mapper.toQuestionResponse(questionRepository.save(builder.build()));
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

        if (request.getModuleId() != null && !request.getModuleId().isEmpty()) {
            UUID moduleId = UUID.fromString(request.getModuleId());
            InterviewKitModule module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("InterviewKitModule", "id", moduleId));
            question.setModule(module);
        } else {
            question.setModule(null);
        }

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
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        return buildFlatQuestions(kit);
    }

    // -- Admin Module Management ---------------------------------------------

    @Override
    public InterviewKitModuleResponse addModule(UUID kitId, InterviewKitModuleRequest request) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitModule module = InterviewKitModule.builder()
                .name(request.getName())
                .build();

        module = moduleRepository.save(module);
        kit.getModules().add(module);
        kitRepository.save(kit);

        return buildModuleResponse(module);
    }

    @Override
    public InterviewKitModuleResponse attachModule(UUID kitId, UUID moduleId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKitModule", "id", moduleId));

        boolean attached = kit.getModules().stream()
                .anyMatch(m -> m.getId().equals(moduleId));
        if (attached) {
            throw new IllegalArgumentException("Module is already linked to this kit");
        }

        kit.getModules().add(module);
        kitRepository.save(kit);

        return buildModuleResponse(module);
    }

    @Override
    public InterviewKitModuleResponse updateModule(UUID kitId, UUID moduleId, InterviewKitModuleRequest request) {
        kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKitModule", "id", moduleId));

        module.setName(request.getName());

        return buildModuleResponse(moduleRepository.save(module));
    }

    @Override
    public void deleteModule(UUID kitId, UUID moduleId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        List<InterviewKitModule> remaining = new ArrayList<>(kit.getModules());
        boolean removed = remaining.removeIf(m -> m.getId().equals(moduleId));
        if (!removed) {
            throw new ResourceNotFoundException("InterviewKitModule", "id", moduleId);
        }

        // Unlink: remove the join row only. Order indices are rewritten by Hibernate.
        kit.getModules().clear();
        kit.getModules().addAll(remaining);
        kitRepository.save(kit);

        // If no kit references the module anymore, clean up the orphan (and its questions).
        if (moduleRepository.countKitsByModuleId(moduleId) == 0) {
            moduleRepository.deleteById(moduleId);
        }
    }

    @Override
    public List<InterviewKitModuleResponse> getModules(UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));
        return kit.getModules().stream()
                .map(this::buildModuleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewKitModuleResponse> getExistingModules() {
        return moduleRepository.findAll().stream()
                .sorted(Comparator.comparing(InterviewKitModule::getName, String.CASE_INSENSITIVE_ORDER))
                .map(module -> InterviewKitModuleResponse.builder()
                        .id(module.getId())
                        .name(module.getName())
                        .questionCount(questionRepository.countByModuleId(module.getId()))
                        .kitsCount(moduleRepository.countKitsByModuleId(module.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void reorderModules(UUID kitId, List<UUID> moduleIds) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        List<InterviewKitModule> current = new ArrayList<>(kit.getModules());
        List<InterviewKitModule> ordered = new ArrayList<>();
        for (UUID id : moduleIds) {
            current.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .ifPresent(m -> {
                        if (!ordered.contains(m)) {
                            ordered.add(m);
                        }
                    });
        }
        current.stream().filter(m -> !ordered.contains(m)).forEach(ordered::add);

        kit.getModules().clear();
        kit.getModules().addAll(ordered);
        kitRepository.save(kit);
    }

    @Override
    public void reorderQuestions(UUID kitId, List<UUID> questionIds) {
        kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        for (int i = 0; i < questionIds.size(); i++) {
            int order = i + 1;
            UUID questionId = questionIds.get(i);
            questionRepository.findById(questionId).ifPresent(q -> {
                q.setDisplayOrder(order);
                questionRepository.save(q);
            });
        }
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
    public InterviewKitEnrollmentResponse adminEnrollStudentWithPayment(
            UUID studentId, UUID kitId,
            BigDecimal discountAmount, BigDecimal amountPaid, String paymentMethod) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        if (kit.getStatus() != KitStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only enroll in published kits");
        }

        BigDecimal kitPrice = kit.getPrice() == null ? BigDecimal.ZERO : kit.getPrice();
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;

        if (paid.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (paid.add(discount).compareTo(kitPrice) > 0) {
            throw new IllegalArgumentException(
                    "Amount paid + discount cannot exceed the kit fee"
            );
        }

        // Build a PAID order + an order item snapshot for the kit.
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(student)
                .totalAmount(kitPrice)
                .discountAmount(discount)
                .finalAmount(paid)
                .currency("INR")
                .status(OrderStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .kit(kit)
                .kitName(kit.getName())
                .price(kitPrice)
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
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return activateEnrollment(student, kit, true);
    }

    @Override
    public InterviewKitEnrollmentResponse adminUnenrollStudentWithRefund(
            UUID studentId, UUID kitId, BigDecimal refundAmount) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        InterviewKitEnrollment enrollment = enrollmentRepository
                .findByUserIdAndKitId(studentId, kitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "InterviewKitEnrollment", "kitId", kitId));

        if (enrollment.getStatus() == KitEnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException(
                    "Student is not currently enrolled in this kit"
            );
        }

        BigDecimal refund = refundAmount == null ? BigDecimal.ZERO : refundAmount;
        if (refund.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative");
        }

        List<Order> orders = orderRepository.findOrdersByUserIdAndKitId(studentId, kitId);
        Order order = orders.isEmpty() ? null : orders.get(0);

        if (order != null && order.getStatus() == OrderStatus.PAID) {
            BigDecimal amountPaid = order.getFinalAmount() == null
                    ? BigDecimal.ZERO : order.getFinalAmount();

            BigDecimal previouslyRefunded = paymentRepository.sumRefundedByOrderId(order.getId());
            if (previouslyRefunded == null) {
                previouslyRefunded = BigDecimal.ZERO;
            }
            BigDecimal remaining = amountPaid.subtract(previouslyRefunded.abs());

            if (refund.compareTo(remaining) > 0
                    || remaining.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Refund amount cannot be more than the remaining paid "
                                + "amount (\u20B9" + remaining.max(BigDecimal.ZERO) + ")"
                );
            }

            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                String originalMethod = paymentRepository
                        .findFirstByOrderIdAndStatusOrderByCreatedDtDesc(
                                order.getId(), PaymentStatus.SUCCESS)
                        .map(Payment::getPaymentMethod)
                        .orElse(null);

                Payment refundPayment = Payment.builder()
                        .order(order)
                        .user(student)
                        .amount(refund.negate())
                        .currency(order.getCurrency() == null ? "INR" : order.getCurrency())
                        .status(PaymentStatus.REFUNDED)
                        .paymentMethod(originalMethod)
                        .paidAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(refundPayment);
            }
        }

        enrollment.setStatus(KitEnrollmentStatus.DROPPED);
        return mapper.toEnrollmentResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public List<InterviewKitEnrollmentResponse> getStudentKitEnrollments(UUID studentId) {
        return enrollmentRepository.findByUserId(studentId).stream()
                .map(mapper::toEnrollmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentKitPaymentInfoResponse> getStudentKitPaymentInfo(UUID studentId) {
        return orderRepository.findByUserIdOrderByCreatedDtDesc(studentId)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.REFUNDED)
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> item.getKit() != null)
                        .map(item -> StudentKitPaymentInfoResponse.builder()
                                .kitId(item.getKit().getId())
                                .kitName(item.getKitName())
                                .kitFee(item.getPrice())
                                .discountAmount(order.getDiscountAmount())
                                .amountPaid(order.getFinalAmount())
                                .status(order.getStatus().name())
                                .build()))
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
    public InterviewKitEnrollmentResponse activatePaidKitEnrollment(User student, UUID kitId) {
        InterviewKit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewKit", "id", kitId));

        if (kit.getStatus() != KitStatus.PUBLISHED) {
            throw new IllegalArgumentException("This kit is no longer available");
        }

        // Idempotent by design: an already-active enrollment is returned as-is and
        // a dropped one is re-activated (matches how course payments behave).
        return activateEnrollment(student, kit, false);
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
                    resp.setQuestionCount(countKitQuestions(kit));
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

    private InterviewKitResponse toResponseWithQuestions(InterviewKit kit) {
        InterviewKitResponse resp = mapper.toResponse(kit);
        resp.setQuestionCount(countKitQuestions(kit));
        return resp;
    }

    private InterviewKitModuleResponse buildModuleResponse(InterviewKitModule module) {
        InterviewKitModuleResponse resp = mapper.toModuleResponse(module);
        List<InterviewKitQuestionResponse> questions = questionRepository
                .findByModuleIdOrderByDisplayOrderAsc(module.getId()).stream()
                .map(mapper::toQuestionResponse)
                .collect(Collectors.toList());
        resp.setQuestions(questions);
        resp.setQuestionCount((long) questions.size());
        resp.setKitsCount(moduleRepository.countKitsByModuleId(module.getId()));
        return resp;
    }

    private List<InterviewKitQuestionResponse> buildFlatQuestions(InterviewKit kit) {
        List<UUID> moduleIds = kit.getModules().stream()
                .map(InterviewKitModule::getId)
                .collect(Collectors.toList());

        List<InterviewKitQuestion> moduleQuestions = moduleIds.isEmpty()
                ? List.of()
                : questionRepository.findByModuleIdInOrderByDisplayOrderAsc(moduleIds);
        List<InterviewKitQuestion> unassigned =
                questionRepository.findByKitIdAndModuleIsNull(kit.getId());

        Map<UUID, InterviewKitQuestion> merged = new LinkedHashMap<>();
        for (InterviewKitQuestion q : moduleQuestions) {
            merged.put(q.getId(), q);
        }
        for (InterviewKitQuestion q : unassigned) {
            merged.put(q.getId(), q);
        }
        return merged.values().stream()
                .map(mapper::toQuestionResponse)
                .collect(Collectors.toList());
    }

    private long countKitQuestions(InterviewKit kit) {
        List<UUID> moduleIds = kit.getModules().stream()
                .map(InterviewKitModule::getId)
                .collect(Collectors.toList());
        long moduleCount = moduleIds.isEmpty()
                ? 0L
                : questionRepository.countByModuleIdIn(moduleIds);
        return moduleCount + questionRepository.countByKitIdAndModuleIsNull(kit.getId());
    }

    private String generateOrderNumber() {
        String timestampPart = Long.toString(System.currentTimeMillis(), 36)
                .toUpperCase();
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "FP360-" + timestampPart + "-" + randomPart;
    }

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
