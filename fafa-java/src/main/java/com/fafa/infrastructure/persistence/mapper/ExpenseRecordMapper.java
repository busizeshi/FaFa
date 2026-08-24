package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ExpenseRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 开支记录 Mapper，包含医疗来源恢复所需的逻辑删除旁路查询。 */
@Mapper
public interface ExpenseRecordMapper extends BaseMapper<ExpenseRecordPO> {

    /**
     * 这里是医疗费用恢复场景的有意旁路：必须读到 is_deleted=1 的旧行，
     * 否则重新填写就医费用会因 medical_visit_id 唯一键无法恢复原账单。
     */
    @Select("SELECT id,user_id,pet_id,category,amount,currency,expense_date,title,notes,"
            + "receipt_media_id,food_card_id,food_name_snapshot,medical_visit_id,source_type,"
            + "system_generated,is_deleted,created_at,updated_at FROM expense_record "
            + "WHERE user_id=#{userId} AND medical_visit_id=#{medicalVisitId} LIMIT 1")
    ExpenseRecordPO selectMedicalIncludingDeleted(@Param("userId") Long userId,
                                                  @Param("medicalVisitId") Long medicalVisitId);

    @Update("UPDATE expense_record SET category=#{category},amount=#{amount},currency=#{currency},"
            + "expense_date=#{expenseDate},title=#{title},notes=#{notes},receipt_media_id=#{receiptMediaId},"
            + "food_card_id=#{foodCardId},food_name_snapshot=#{foodNameSnapshot},medical_visit_id=#{medicalVisitId},"
            + "source_type=#{sourceType},system_generated=#{systemGenerated},is_deleted=0,updated_at=CURRENT_TIMESTAMP "
            + "WHERE id=#{id} AND user_id=#{userId}")
    int restoreMedical(ExpenseRecordPO record);

    @Update("UPDATE expense_record SET is_deleted=1,updated_at=CURRENT_TIMESTAMP "
            + "WHERE user_id=#{userId} AND medical_visit_id=#{medicalVisitId} AND is_deleted=0")
    int deleteMedicalSource(@Param("userId") Long userId, @Param("medicalVisitId") Long medicalVisitId);
}
