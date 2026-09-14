package com.example.smartcustomerservice.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcustomerservice.domain.entity.SkillGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SkillGroupMapper extends BaseMapper<SkillGroup> {

    @Select("""
            SELECT id, group_name, category, status, created_at
            FROM skill_group
            WHERE id = #{skillGroupId}
              AND status = 1
            FOR UPDATE
            """)
    SkillGroup selectActiveByIdForUpdate(@Param("skillGroupId") Long skillGroupId);
}
