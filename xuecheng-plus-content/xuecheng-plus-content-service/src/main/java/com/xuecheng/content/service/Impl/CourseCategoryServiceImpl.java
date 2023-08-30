package com.xuecheng.content.service.Impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CoureseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseCategoryServiceImpl implements CoureseCategoryService {
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
      List<CourseCategoryTreeDto> result = new ArrayList<>();
      List<CourseCategoryTreeDto> temp = courseCategoryMapper.selectTreeNodes(id);
      Map<String, CourseCategoryTreeDto> map = temp.stream().filter(item->!id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
      temp.stream().filter(item->!id.equals(item.getId())).forEach(item->{
          if(item.getParentid().equals(id)){
              result.add(item);
          }
          CourseCategoryTreeDto SecondLevelDTO = map.get(item.getParentid());
          if(SecondLevelDTO!=null) {
              if (SecondLevelDTO.getChildrenTreeNodes() == null) {
                  SecondLevelDTO.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
              }
              SecondLevelDTO.getChildrenTreeNodes().add(item);
          }

      });
        return result;
    }
}
