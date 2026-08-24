package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.ContactUsRequest;
import com.foliopath360.lms.dto.response.ContactUsResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.entity.ContactPosition;
import com.foliopath360.lms.entity.ContactUs;
import com.foliopath360.lms.repository.ContactUsRepository;
import com.foliopath360.lms.service.ContactUsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactUsServiceImpl implements ContactUsService {

    private final ContactUsRepository contactUsRepository;

    @Override
    @Transactional
    public MessageResponse createContactUs(ContactUsRequest request) {

        ContactUs contactUs = ContactUs.builder()
                .fullName(request.getFullName().trim())
                .mobileNumber(request.getMobileNumber().trim())
                .email(request.getEmail().trim().toLowerCase())
                .currentPosition(ContactPosition.valueOf(request.getCurrentPosition()))
                .location(request.getLocation() == null || request.getLocation().isBlank()
                        ? "Not Provided"
                        : request.getLocation().trim())
                .build();

        contactUsRepository.save(contactUs);
        log.info("Contact request saved successfully for email: {}", contactUs.getEmail());

        return MessageResponse.builder()
                .message("Contact request submitted successfully. Our team will reach out to you shortly.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactUsResponse> getAllContactUs() {

        return contactUsRepository.findAllByOrderByCreatedDtDesc().stream()
                .map(contact -> ContactUsResponse.builder()
                        .id(contact.getId())
                        .fullName(contact.getFullName())
                        .mobileNumber(contact.getMobileNumber())
                        .email(contact.getEmail())
                        .currentPosition(contact.getCurrentPosition().name())
                        .location(contact.getLocation())
                        .createdDt(contact.getCreatedDt())
                        .build())
                .toList();
    }
}
