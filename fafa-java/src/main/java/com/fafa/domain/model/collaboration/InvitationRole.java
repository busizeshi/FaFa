package com.fafa.domain.model.collaboration;

/** 邀请授予的角色，与成员角色保持同构但不复用传输字符串。 */
public enum InvitationRole {
    MEMBER("member"), CAREGIVER("caregiver");

    private final String code;

    InvitationRole(String code) { this.code = code; }

    public String getCode() { return code; }

    public PetMemberRole toMemberRole() { return PetMemberRole.fromCode(code); }

    public static InvitationRole fromCode(String code) {
        for (InvitationRole value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("邀请角色不支持: " + code);
    }
}
