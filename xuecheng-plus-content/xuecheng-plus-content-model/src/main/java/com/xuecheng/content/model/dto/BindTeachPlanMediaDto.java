package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

@Data
@ApiModel(value = "BindTeachPlanMediaDto",description = "")
public class BindTeachPlanMediaDto {
    @ApiModelProperty(value = "",required = true)
    private String mediaId;
    @ApiModelProperty(value = "",required = true)
    private String filename;
    @ApiModelProperty(value = "",required = true)
    private Long teachplanId;
}
