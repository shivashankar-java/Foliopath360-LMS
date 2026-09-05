package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_kits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKit extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kit_code", nullable = false, unique = true, length = 50)
    private String kitCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 250)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 30)
    private KitLevel level;

    @Column(name = "price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private KitStatus status = KitStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToMany
    @JoinTable(
            name = "kit_modules",
            joinColumns = @JoinColumn(name = "kit_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id"))
    @OrderColumn(name = "order_index")
    @Builder.Default
    private List<InterviewKitModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "kit")
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<InterviewKitQuestion> questions = new ArrayList<>();
}
