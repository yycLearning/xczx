package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class CoursePreviewDTO {
    private CourseBaseInfoDto CourseBase;
    private List<TeachPlanDTO> teachPlans;


}
