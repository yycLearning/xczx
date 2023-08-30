package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

public interface CoureseCategoryService {
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
