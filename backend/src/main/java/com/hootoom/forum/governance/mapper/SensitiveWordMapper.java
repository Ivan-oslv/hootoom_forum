package com.hootoom.forum.governance.mapper;

import com.hootoom.forum.governance.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {
    List<SensitiveWord> findAllActive();
}
