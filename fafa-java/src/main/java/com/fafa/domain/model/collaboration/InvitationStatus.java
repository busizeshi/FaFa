package com.fafa.domain.model.collaboration;

public enum InvitationStatus {
    PENDING("pending"), ACCEPTED("accepted"), EXPIRED("expired"), REVOKED("revoked");

    private final String code;

    InvitationStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static InvitationStatus fromCode(String code) {
        for (InvitationStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("邀请状态不支持: " + code);
    }
}
