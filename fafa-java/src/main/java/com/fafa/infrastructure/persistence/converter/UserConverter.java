package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.user.User;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import cn.hutool.core.bean.BeanUtil;

/**
 * User 领域对象 <-> DO 转换
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        // 字段同名直接拷贝（openid/nickname/memberLevel 等）
        User user = new User();
        BeanUtil.copyProperties(userDO, user);
        return user;
    }

    public static UserDO toDO(User user) {
        if (user == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtil.copyProperties(user, userDO);
        return userDO;
    }
}
