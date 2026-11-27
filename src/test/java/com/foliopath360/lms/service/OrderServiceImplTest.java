package com.foliopath360.lms.service;

import com.foliopath360.lms.entity.Cart;
import com.foliopath360.lms.entity.CartItem;
import com.foliopath360.lms.entity.CartStatus;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseStatus;
import com.foliopath360.lms.entity.EnrollmentStatus;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderStatus;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.mapper.OrderMapper;
import com.foliopath360.lms.repository.CartRepository;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.repository.InterviewKitEnrollmentRepository;
import com.foliopath360.lms.repository.InterviewKitRepository;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private InterviewKitRepository kitRepository;
    @Mock private InterviewKitEnrollmentRepository kitEnrollmentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;

    private OrderServiceImpl service;
    private User student;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                cartRepository,
                courseRepository,
                enrollmentRepository,
                kitRepository,
                kitEnrollmentRepository,
                orderRepository,
                orderMapper
        );
        student = User.builder().id(UUID.randomUUID()).email("student@foliopath360.com").build();
    }

    @Test
    void checkoutUsesSelectedCartCourseAndCancelsStaleOrders() {
        UUID selectedCourseId = UUID.randomUUID();
        Course selectedCourse = Course.builder()
                .id(selectedCourseId)
                .title("Selected Course")
                .status(CourseStatus.PUBLISHED)
                .price(new BigDecimal("499.00"))
                .build();

        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .course(selectedCourse)
                .price(new BigDecimal("999.00"))
                .build();
        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .user(student)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();
        cartItem.setCart(cart);

        Order staleOrder = Order.builder()
                .id(UUID.randomUUID())
                .user(student)
                .status(OrderStatus.CREATED)
                .build();

        when(cartRepository.findByUserIdAndStatus(student.getId(), CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(courseRepository.findById(selectedCourseId))
                .thenReturn(Optional.of(selectedCourse));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                student.getId(), selectedCourseId, EnrollmentStatus.DROPPED))
                .thenReturn(false);
        when(orderRepository.findAllByUserIdAndStatusIn(eq(student.getId()), anyList()))
                .thenReturn(List.of(staleOrder));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.checkout(student);

        assertThat(staleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        verify(orderRepository).saveAll(List.of(staleOrder));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order createdOrder = orderCaptor.getValue();
        assertThat(createdOrder.getItems()).hasSize(1);
        assertThat(createdOrder.getItems().get(0).getCourse().getId()).isEqualTo(selectedCourseId);
        assertThat(createdOrder.getItems().get(0).getCourseTitle()).isEqualTo("Selected Course");
        assertThat(createdOrder.getFinalAmount()).isEqualByComparingTo("499.00");
    }
}
