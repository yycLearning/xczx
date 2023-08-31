package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
@ApiModel(value = "EditCourseDTO",description = "modify course")
@Data
public class EditCourseDTO extends AddCourseDto {
    @ApiModelProperty(value = "courseId",required = true)
    private Long id;
}
