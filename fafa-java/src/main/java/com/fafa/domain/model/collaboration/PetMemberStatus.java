package com.fafa.domain.model.collaboration;

public enum PetMemberStatus {
    ACTIVE("active"), REVOKED("revoked"), EXPIRED("expired");

    private final String code;

    PetMemberStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static PetMemberStatus fromCode(String code) {
        for (PetMemberStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("成员状态不支持: " + code);
    }
}
