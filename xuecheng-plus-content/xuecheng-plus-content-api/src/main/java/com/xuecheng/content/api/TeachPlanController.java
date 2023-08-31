package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "interface of teach plan",tags = "interface")
public class TeachPlanController {
    @Autowired
    private TeachplanService teachplanService;


    @GetMapping("/teachplan/{courseId}/tree-nodes")
    @ApiOperation("query the treeNodes of teach plan")
    @ApiImplicitParam(value = "coursId",name = "courseId",required = true,dataType = "Long",paramType = "PATH")
    public List<TeachPlanDTO> getTreeNodes(@PathVariable Long courseId){
        List<TeachPlanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        return teachplanTree;
    }

    @ApiOperation("CoursePlan creating or modify")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody @Validated SaveTeachplanDTO saveTeachplanDTO){
         teachplanService.saveTeachplan(saveTeachplanDTO);

    }
}
