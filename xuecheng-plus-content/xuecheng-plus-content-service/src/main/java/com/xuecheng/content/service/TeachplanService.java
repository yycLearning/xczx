package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;

import java.util.List;

public interface TeachplanService {
    public List<TeachPlanDTO> findTeachplanTree(Long courseId);

    public void saveTeachplan(SaveTeachplanDTO saveTeachplanDTO);
}
