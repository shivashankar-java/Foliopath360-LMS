package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instructor_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorProfile extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "instructor_code", nullable = false, unique = true, length = 50)
    private String instructorCode;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "qualification", length = 200)
    private String qualification;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "specialization", length = 200)
    private String specialization;

    @Column(name = "designation", length = 150)
    private String designation;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

}
