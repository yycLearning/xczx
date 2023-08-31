package com.xuecheng.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Override
    public List<TeachPlanDTO> findTeachplanTree(Long courseId) {
        List<TeachPlanDTO> result = teachplanMapper.selectTreeNodes(courseId);
        return result;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDTO saveTeachplanDTO) {
        Long teachplanId = saveTeachplanDTO.getId();
        if(teachplanId==null){
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDTO,teachplan);
            //determine order sequence
            Integer count = getInteger(saveTeachplanDTO);
            teachplan.setOrderby(count+1);

            teachplanMapper.insert(teachplan);
        }else{
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(saveTeachplanDTO,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    private Integer getInteger(SaveTeachplanDTO saveTeachplanDTO) {
        Long parentid = saveTeachplanDTO.getParentid();
        Long courseId = saveTeachplanDTO.getCourseId();
        LambdaQueryWrapper<Teachplan> teachplanLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanLambdaQueryWrapper= teachplanLambdaQueryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentid);
        Integer count = teachplanMapper.selectCount(teachplanLambdaQueryWrapper);
        return count;
    }
}
