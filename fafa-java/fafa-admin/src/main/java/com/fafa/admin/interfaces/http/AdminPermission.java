package com.fafa.admin.interfaces.http;

import java.lang.annotation.*;

/** 业务管理接口必须声明固定权限；未声明的管理入口默认拒绝。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminPermission { String value(); }
