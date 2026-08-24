package com.fafa.domain.model.collaboration;

/** 宠物空间稳定权限码，页面只消费服务端返回的权限集合。 */
public enum PetPermission {
    READ_PROFILE,
    READ_RECORD,
    CREATE_RECORD,
    EDIT_OWN_RECORD,
    COMPLETE_REMINDER,
    READ_MEDICAL,
    MANAGE_MEDICAL,
    READ_EXPENSE,
    MANAGE_EXPENSE,
    READ_FOOD,
    MANAGE_FOOD,
    READ_REPORT,
    GENERATE_REPORT,
    MANAGE_MEMBER,
    READ_AUDIT,
    READ_EMERGENCY,
    EDIT_EMERGENCY
}
