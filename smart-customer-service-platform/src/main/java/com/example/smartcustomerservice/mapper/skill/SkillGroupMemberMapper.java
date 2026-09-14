package com.example.smartcustomerservice.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcustomerservice.domain.entity.SkillGroupMember;
import com.example.smartcustomerservice.domain.vo.SkillGroupAgentLoadVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SkillGroupMemberMapper extends BaseMapper<SkillGroupMember> {

    @Select("""
            SELECT sgm.id AS member_id,
                   sgm.user_id,
                   u.username,
                   u.real_name,
                   sgm.max_active_tickets,
                   COUNT(t.id) AS active_ticket_count
            FROM skill_group_member sgm
            JOIN skill_group sg ON sg.id = sgm.skill_group_id AND sg.status = 1
            JOIN sys_user u ON u.id = sgm.user_id AND u.status = 1 AND u.deleted = 0
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'AGENT' AND r.status = 1
            LEFT JOIN ticket t ON t.assignee_id = u.id
                              AND t.deleted = 0
                              AND t.status IN ('ASSIGNED', 'PROCESSING')
            WHERE sgm.skill_group_id = #{skillGroupId}
              AND sgm.status = 1
            GROUP BY sgm.id, sgm.user_id, u.username, u.real_name, sgm.max_active_tickets
            HAVING COUNT(t.id) < sgm.max_active_tickets
            ORDER BY active_ticket_count ASC, sgm.id ASC
            """)
    List<SkillGroupAgentLoadVO> selectAvailableAgentsWithLoad(@Param("skillGroupId") Long skillGroupId);
}
