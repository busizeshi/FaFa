package com.fafa.interfaces.assembler;

import com.fafa.application.dto.user.UserInfoDTO;
import com.fafa.application.dto.user.VipInfoDTO;
import com.fafa.interfaces.dto.user.UserInfoResponse;
import com.fafa.interfaces.dto.user.VipInfoResponse;

/**
 * 用户对象转换器
 *
 * @author FaFa Team
 * @since 1.0
 */
public class UserAssembler {

    /**
     * 转换为用户信息响应
     */
    public static UserInfoResponse toUserInfoResponse(UserInfoDTO dto) {
        return new UserInfoResponse(
            dto.id(),
            dto.openId(),
            dto.nickname(),
            dto.phone(),
            dto.gender(),
            dto.age(),
            dto.avatarUrl(),
            dto.city(),
            dto.vip(),
            dto.vipExpireTime(),
            dto.displayName()
        );
    }

    /**
     * 转换为会员信息响应
     */
    public static VipInfoResponse toVipInfoResponse(VipInfoDTO dto) {
        return new VipInfoResponse(
            dto.isVip(),
            dto.isVipValid(),
            dto.vipExpireTime()
        );
    }
}
