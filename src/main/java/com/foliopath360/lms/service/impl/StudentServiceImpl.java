package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.StudentProfileUpdateRequest;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.dto.response.StudentProfileResponse;
import com.foliopath360.lms.dto.response.UserResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.repository.StudentProfileRepository;
import com.foliopath360.lms.service.StudentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentProfileRepository profileRepository;

    public StudentServiceImpl(
            StudentProfileRepository profileRepository
    ) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public StudentDashboardResponse getDashboard(User user) {

        StudentProfile profile = getOrCreateStudentProfile(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(
                        user.getRoles().stream()
                                .map(Role::getRoleName)
                                .collect(Collectors.toSet())
                )
                .build();

        return StudentDashboardResponse.builder()
                .studentInfo(userResponse)
                .studentCode(profile.getStudentCode())
                .build();
    }

    @Override
    @Transactional
    public StudentProfileResponse getProfile(User user) {

        StudentProfile profile = getOrCreateStudentProfile(user);

        return StudentProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobileNumber(user.getMobileNumber())
                .studentCode(profile.getStudentCode())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .qualification(profile.getQualification())
                .occupation(profile.getOccupation())
                .bio(profile.getBio())
                .address(profile.getAddress())
                .city(profile.getCity())
                .state(profile.getState())
                .country(profile.getCountry())
                .joinedAt(profile.getJoinedAt())
                .build();
    }

    @Override
    public StudentProfileResponse updateProfile(User user, StudentProfileUpdateRequest request) {

        StudentProfile profile = getOrCreateStudentProfile(user);

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(Gender.valueOf(request.getGender()));
        }
        if (request.getQualification() != null) {
            profile.setQualification(request.getQualification());
        }
        if (request.getOccupation() != null) {
            profile.setOccupation(request.getOccupation());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            profile.setCity(request.getCity());
        }
        if (request.getState() != null) {
            profile.setState(request.getState());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry());
        }

        return getProfile(user);
    }

    private StudentProfile getOrCreateStudentProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String studentCode = "STU-" + System.currentTimeMillis() % 100000;
                    StudentProfile newProfile = StudentProfile.builder()
                            .user(user)
                            .studentCode(studentCode)
                            .joinedAt(LocalDateTime.now())
                            .build();
                    return profileRepository.save(newProfile);
                });
    }
}
