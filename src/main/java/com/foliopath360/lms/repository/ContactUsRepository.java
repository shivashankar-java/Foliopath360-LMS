package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.ContactUs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactUsRepository extends JpaRepository<ContactUs, UUID> {

    List<ContactUs> findAllByOrderByCreatedDtDesc();
}
