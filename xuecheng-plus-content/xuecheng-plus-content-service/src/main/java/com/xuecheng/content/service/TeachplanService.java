package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachPlanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.model.po.TeachplanMedia;

import java.util.List;

public interface TeachplanService {
    public List<TeachPlanDTO> findTeachplanTree(Long courseId);

    public void saveTeachplan(SaveTeachplanDTO saveTeachplanDTO);

    public void deleteTeachPlan(Long courseId);

    public void movedown(Long id);
    public void moveup(Long id);
    public TeachplanMedia associationMedia(BindTeachPlanMediaDto bindTeachPlanMediaDto);
}
