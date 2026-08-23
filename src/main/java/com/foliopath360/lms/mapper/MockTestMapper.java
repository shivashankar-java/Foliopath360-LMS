package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.request.MockTestOptionRequest;
import com.foliopath360.lms.dto.request.MockTestRequest;
import com.foliopath360.lms.dto.request.MockTestQuestionRequest;
import com.foliopath360.lms.dto.response.MockTestOptionResponse;
import com.foliopath360.lms.dto.response.MockTestQuestionResponse;
import com.foliopath360.lms.dto.response.MockTestResponse;
import com.foliopath360.lms.entity.MockTest;
import com.foliopath360.lms.entity.MockTestOption;
import com.foliopath360.lms.entity.MockTestQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MockTestMapper {

    // Questions are wired manually in the service so the
    // mock_test back-reference is never left null.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "module", ignore = true)
    @Mapping(target = "questions", ignore = true)
    MockTest toEntity(MockTestRequest request);

    // Options are wired manually in the service so the
    // question back-reference is never left null.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mockTest", ignore = true)
    @Mapping(target = "options", ignore = true)
    MockTestQuestion toQuestionEntity(MockTestQuestionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    MockTestOption toOptionEntity(MockTestOptionRequest request);

    // questions auto-mapped via toQuestionResponse
    @Mapping(target = "moduleId", source = "module.id")
    MockTestResponse toResponse(MockTest mockTest);

    // options auto-mapped via toOptionResponse
    MockTestQuestionResponse toQuestionResponse(MockTestQuestion question);

    MockTestOptionResponse toOptionResponse(MockTestOption option);
}
