package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.LearningActivityResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;

public interface LearningActivityService {
    List<LearningActivityResponse> getMyLearningActivity(User user, int limit);
}
