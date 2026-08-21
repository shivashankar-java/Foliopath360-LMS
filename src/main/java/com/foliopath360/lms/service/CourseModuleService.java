package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.ModuleRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.ModuleResponse;

import java.util.List;
import java.util.UUID;

public interface CourseModuleService {

    ModuleResponse createModule(UUID courseId, ModuleRequest request);

    ModuleResponse updateModule(UUID moduleId, ModuleRequest request);

    ModuleResponse getModuleById(UUID moduleId);

    List<ModuleResponse> getModulesByCourseId(UUID courseId);

    void deleteModule(UUID moduleId);

    List<ModuleResponse> reorderModules(UUID courseId, ReorderRequest request);
}
