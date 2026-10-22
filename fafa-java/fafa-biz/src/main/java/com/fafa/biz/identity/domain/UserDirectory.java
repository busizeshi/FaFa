package com.fafa.biz.identity.domain;

import com.fafa.common.rpc.AdminBusinessRpc.UserSummary;
import java.util.List;

/** 后台可使用的脱敏账号目录端口；修改状态前必须取得账号写锁。 */
public interface UserDirectory {
    List<UserSummary> list(String cursor, int limit);
    UserSummary find(String id);
    void status(String id, String state);
}
