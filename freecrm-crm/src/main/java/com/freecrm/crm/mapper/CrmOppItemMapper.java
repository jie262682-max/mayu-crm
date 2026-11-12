package com.freecrm.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freecrm.crm.entity.CrmOppItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmOppItemMapper extends BaseMapper<CrmOppItem> {

    @Delete("DELETE FROM crm_opp_item WHERE opportunity_id = #{opportunityId}")
    int physicalDeleteByOpp(Long opportunityId);
}
