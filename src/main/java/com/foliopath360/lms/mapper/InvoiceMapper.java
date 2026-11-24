package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.InvoiceItemResponse;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.dto.response.InvoiceTaxResponse;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceItem;
import com.foliopath360.lms.entity.InvoiceTax;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "itemType", source = "itemType")
    InvoiceItemResponse toItemResponse(InvoiceItem item);

    List<InvoiceItemResponse> toItemResponses(List<InvoiceItem> items);

    InvoiceTaxResponse toTaxResponse(InvoiceTax tax);

    List<InvoiceTaxResponse> toTaxResponses(List<InvoiceTax> taxes);

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentCode", source = "studentCode")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "taxes", source = "taxes")
    @Mapping(target = "downloadUrl",
            expression = "java(invoice.getId() == null ? null : "
                    + "\"/api/invoices/\" + invoice.getId() + \"/download\")")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponses(List<Invoice> invoices);
}
