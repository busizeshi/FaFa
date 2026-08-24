package com.fafa.domain.model.collaboration;

import java.util.Locale;

/** 宠物协作者角色。主人由 pet.user_id 派生，不写入成员表。 */
public enum PetMemberRole {
    MEMBER("member"),
    CAREGIVER("caregiver");

    private final String code;

    PetMemberRole(String code) { this.code = code; }

    public String getCode() { return code; }

    public static PetMemberRole fromCode(String code) {
        if (code == null) throw new IllegalArgumentException("成员角色不能为空");
        for (PetMemberRole role : values()) {
            if (role.code.equals(code.trim().toLowerCase(Locale.ROOT))) return role;
        }
        throw new IllegalArgumentException("成员角色不支持: " + code);
    }
}
