package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ContactUsRequest;
import com.foliopath360.lms.dto.response.ContactUsResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.service.ContactUsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contact-us")
@RequiredArgsConstructor
public class ContactUsController {

    private final ContactUsService contactUsService;

    @PostMapping
    public ResponseEntity<MessageResponse> createContactUs(
            @Valid @RequestBody ContactUsRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(contactUsService.createContactUs(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<List<ContactUsResponse>> getAllContactUs() {
        return ResponseEntity.ok(contactUsService.getAllContactUs());
    }
}
