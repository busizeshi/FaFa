package com.jwd.fafa.core.di

import javax.inject.Qualifier

/** 会话令牌存储（fafa_session）。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

/** 用户偏好存储（fafa_user_prefs）。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPrefsDataStore
