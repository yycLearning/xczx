package com.xuecheng.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachPlanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

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

    @Override
    public void deleteTeachPlan(Long courseId) {
        Teachplan teachplan = teachplanMapper.selectById(courseId);
        if(teachplan==null){
            XueChengPlusException.cast("course not exists");
        }
        Integer grade = teachplan.getGrade();
        if(grade==2){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper();
            queryWrapper = queryWrapper.eq(Teachplan::getParentid,teachplan.getParentid()).eq(Teachplan::getCourseId,courseId);
            Integer count = teachplanMapper.selectCount(queryWrapper);
            if(count>0){
                XueChengPlusException.cast("failed to delete course:Sub-courses detected");
            }else{
                int result = teachplanMapper.deleteById(courseId);
                if(result<1){
                    XueChengPlusException.cast("deletion failed");
                }else{
                    return;
                }
            }

        }else{
            LambdaQueryWrapper<TeachplanMedia> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper = lambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId,courseId);
            teachplanMediaMapper.delete(lambdaQueryWrapper);
            teachplanMapper.deleteById(courseId);
        }
    }

    @Override
    public void movedown(Long id) {
        Teachplan teachplan1 = teachplanMapper.selectById(id);
        Integer order = teachplan1.getOrderby();
        Integer target = order+1;
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getOrderby,target).eq(Teachplan::getCourseId,teachplan1.getCourseId());
        Teachplan teachplan2 = teachplanMapper.selectOne(queryWrapper);
        if(teachplan2!=null){
            teachplan1.setOrderby(order+1);
            teachplan2.setOrderby(target-1);
            int i = teachplanMapper.updateById(teachplan1);
            int i1 = teachplanMapper.updateById(teachplan2);
            if(i<1||i1<1){
                XueChengPlusException.cast("Please try again");
            }
        }
    }

    @Override
    public void moveup(Long id) {
        Teachplan teachplan1 = teachplanMapper.selectById(id);
        Integer order = teachplan1.getOrderby();
        if(order==1){
            return;
        }
        Integer target = order-1;
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getOrderby,target).eq(Teachplan::getCourseId,teachplan1.getCourseId());
        Teachplan teachplan2 = teachplanMapper.selectOne(queryWrapper);
        if(teachplan2!=null){
            teachplan1.setOrderby(order-1);
            teachplan2.setOrderby(target+1);
            int i = teachplanMapper.updateById(teachplan1);
            int i1 = teachplanMapper.updateById(teachplan2);
            if(i<1||i1<1){
                XueChengPlusException.cast("Please try again");
            }
        }
    }

    @Override
    @Transactional
    public TeachplanMedia associationMedia(BindTeachPlanMediaDto bindTeachPlanMediaDto) {
        Long teachplanId =  bindTeachPlanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("teachplan no exists");
        }
        Long courseId = teachplan.getCourseId();

        int delete = teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, bindTeachPlanMediaDto.getTeachplanId()));
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachPlanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setMediaFilename(bindTeachPlanMediaDto.getFilename());
        teachplanMediaMapper.insert(teachplanMedia);
        return null;
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
