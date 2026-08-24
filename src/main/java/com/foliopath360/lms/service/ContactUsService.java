package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.ContactUsRequest;
import com.foliopath360.lms.dto.response.ContactUsResponse;
import com.foliopath360.lms.dto.response.MessageResponse;

import java.util.List;

public interface ContactUsService {

    MessageResponse createContactUs(ContactUsRequest request);

    List<ContactUsResponse> getAllContactUs();
}
