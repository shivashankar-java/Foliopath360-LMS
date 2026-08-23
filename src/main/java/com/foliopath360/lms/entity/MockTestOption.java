package com.foliopath360.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mock_test_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private MockTestQuestion question;

    @Column(name = "label", nullable = false, length = 1)
    private String label;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String text;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
