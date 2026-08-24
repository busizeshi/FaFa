package com.fafa.common;

import lombok.Getter;

/**
 * 业务错误码
 *
 * 分段规则：0xxx 通用 / 10xxx 用户 / 20xxx 宠物 / 30xxx 记录 / 40xxx 照片 / 50xxx 提醒
 * / 60xxx 就医 / 70xxx 记账 / 90xxx 基础设施
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),

    // 通用 0xxx
    PARAM_INVALID(1001, "参数校验失败"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "资源不存在"),
    SYSTEM_ERROR(9999, "系统异常，请稍后重试"),

    // 用户模块 10xxx
    USER_LOGIN_FAILED(10001, "登录失败，请稍后重试"),
    WECHAT_CODE_INVALID(10002, "微信登录凭证无效"),
    USER_NOT_FOUND(10003, "用户不存在"),

    // 记录模块 30xxx
    RECORD_NOT_FOUND(30001, "记录不存在"),
    RECORD_DATE_IN_FUTURE(30002, "记录日期不能晚于今天"),
    RECORD_NO_PREVIOUS(30003, "没有可复用的历史记录"),
    DAILY_DIMENSION_MISSING(30004, "必填记录维度缺失"),
    FEEDING_TIME_IN_FUTURE(30005, "喂食时间不能晚于当前时间"),

    // 素材与成长档案 40xxx
    MEDIA_NOT_FOUND(40001, "素材不存在"),
    MEDIA_FILE_TOO_LARGE(40002, "单个文件超过大小限制"),
    MEDIA_FILE_COUNT_EXCEEDED(40003, "单次上传文件数量超过限制"),
    MEDIA_TYPE_INVALID(40004, "不支持的文件类型"),
    STORAGE_QUOTA_EXCEEDED(40005, "存储空间不足，升级会员解锁更大空间"),
    AI_SEARCH_QUOTA_EXCEEDED(40006, "本月 AI 语义检索次数已用完，升级会员解锁不限次"),
    TAG_NAME_INVALID(40007, "标签名不能为空且不超过50个字符"),
    TAG_DUPLICATE(40008, "标签已存在"),
    TAG_NOT_FOUND(40009, "标签不存在"),
    EVENT_RECORD_NOT_FOUND(40010, "事件记录不存在"),
    EVENT_RECORD_NOT_MODIFIABLE(40011, "提醒生成的事件不可编辑或删除"),
    FOOD_NOT_FOUND(40012, "食品卡不存在"),
    FOOD_FIELD_INVALID(40013, "食品卡参数无效"),
    FOOD_IN_ACTIVE_PLAN(40014, "食品卡仍被生效计划使用"),
    FOOD_MEDIA_INVALID(40015, "食品识别图片无效"),
    FOOD_RECOGNITION_QUOTA_EXCEEDED(40016, "本月食品包装识别次数已用完"),
    FOOD_RECOGNITION_NOT_READY(40017, "食品识别结果尚未就绪"),
    FOOD_RECOGNITION_EXPIRED(40018, "食品识别草稿已过期"),
    FOOD_RECOGNITION_CONFIRM_CONFLICT(40019, "食品识别任务已确认"),
    FOOD_TRANSITION_INVALID(40020, "换粮计划参数无效"),
    FEEDING_PLAN_NOT_FOUND(40021, "喂食计划不存在"),
    FEEDING_PLAN_INVALID(40022, "喂食计划参数无效"),
    FEEDING_RECORD_DUPLICATE(40023, "该计划时段已经完成"),
    FEEDING_ANALYSIS_RANGE_INVALID(40024, "喂养分析日期范围无效"),

    // 记账模块 70xxx
    EXPENSE_NOT_FOUND(70001, "开支记录不存在"),
    EXPENSE_FIELD_INVALID(70002, "开支参数无效"),
    EXPENSE_CATEGORY_INVALID(70003, "开支分类无效"),
    EXPENSE_CURRENCY_UNSUPPORTED(70004, "当前仅支持人民币 CNY"),
    EXPENSE_SOURCE_INVALID(70005, "开支来源无效"),
    EXPENSE_AUTO_RECORD_IMMUTABLE(70006, "系统生成的医疗开支不可直接修改"),
    EXPENSE_RANGE_INVALID(70007, "开支统计日期范围无效"),
    EXPENSE_MEDICAL_SYNC_FAILED(70008, "医疗费用同步失败"),

    // 周期报告与内容创作 71xxx
    REPORT_NOT_FOUND(71001, "报告不存在"),
    REPORT_PERIOD_INVALID(71002, "报告周期无效"),
    REPORT_GENERATION_IN_PROGRESS(71003, "报告正在生成中，请稍后查看"),
    REPORT_GENERATION_QUOTA_EXCEEDED(71004, "本月报告与内容生成次数已用完"),
    REPORT_CONTENT_INVALID(71005, "报告内容校验失败"),
    REPORT_DATA_UNAVAILABLE(71006, "报告事实数据暂时不可用"),
    CONTENT_DRAFT_NOT_FOUND(71007, "内容草稿不存在"),
    CONTENT_TYPE_INVALID(71008, "内容类型无效"),
    CONTENT_DRAFT_INVALID(71009, "内容草稿参数无效"),
    CONTENT_SCHEDULE_NOT_FOUND(71010, "内容周期配置不存在"),
    CONTENT_SCHEDULE_INVALID(71011, "内容周期配置无效"),

    // 家庭协同与应急信息卡 72xxx
    COLLABORATION_FEATURE_UNAVAILABLE(72001, "宠物空间协同功能未开放"),
    COLLABORATION_MEMBER_LIMIT_EXCEEDED(72002, "成员数量已达上限"),
    COLLABORATION_MEMBER_NOT_FOUND(72003, "成员不存在"),
    COLLABORATION_MEMBER_MANAGEMENT_FORBIDDEN(72004, "无权管理成员"),
    COLLABORATION_INVITATION_INVALID(72005, "邀请不存在或已失效"),
    COLLABORATION_INVITATION_USED(72006, "邀请已被使用"),
    COLLABORATION_INVITATION_TOKEN_INVALID(72007, "邀请凭证无效"),
    COLLABORATION_INVITATION_RATE_LIMITED(72008, "邀请操作频率超限"),
    COLLABORATION_PERMISSION_DENIED(72009, "成员权限不足"),
    COLLABORATION_CAREGIVER_EXPIRED(72010, "临时照护权限已过期"),
    EMERGENCY_PROFILE_NOT_FOUND(72011, "应急资料不存在"),
    EMERGENCY_PROFILE_VERSION_CONFLICT(72012, "应急资料版本冲突"),
    EMERGENCY_PROFILE_INVALID(72013, "应急资料字段无效"),
    COLLABORATION_AUDIT_FORBIDDEN(72014, "审计查询无权限"),
    COLLABORATION_REMINDER_DELIVERY_FAILED(72015, "共享提醒推送失败"),

    // 提醒模块 50xxx
    REMINDER_NOT_FOUND(50001, "提醒不存在"),
    REMINDER_TIME_IN_PAST(50002, "提醒时间不能早于当前时间"),
    REMINDER_INTERVAL_INVALID(50003, "周期配置无效"),
    REMINDER_STATE_INVALID(50004, "提醒当前状态不支持该操作"),
    REMINDER_COMPLETION_TIME_INVALID(50005, "完成时间不能晚于当前时间"),

    // 就医与健康模块 60xxx
    MEDICAL_RECORD_NOT_FOUND(60001, "就医记录不存在"),
    MEDICAL_RECORD_INVALID(60002, "就医记录参数无效"),
    MEDICAL_ATTACHMENT_INVALID(60003, "就医附件无效"),
    MEDICATION_PLAN_NOT_FOUND(60004, "用药计划不存在"),
    MEDICATION_PLAN_INVALID(60005, "用药计划参数无效"),
    MEDICATION_PLAN_STATE_INVALID(60006, "用药计划当前状态不支持该操作"),
    MEDICATION_RECORD_INVALID(60007, "实际用药记录参数无效"),
    MEDICATION_PLAN_REQUIRED(60008, "用药提醒必须由用药计划创建"),
    HEALTH_ALERT_NOT_FOUND(60009, "健康预警不存在"),
    HEALTH_ALERT_STATE_INVALID(60010, "健康预警当前状态不支持该操作"),
    HEALTH_SUMMARY_INVALID(60011, "就诊摘要参数无效"),

    // 基础设施 90xxx
    AI_SERVICE_UNAVAILABLE(90001, "AI 服务暂时不可用，请稍后重试"),
    STORAGE_ERROR(90002, "文件存储服务异常"),

    // AI 对话 80xxx
    AI_CHAT_SESSION_NOT_FOUND(80001, "AI 会话不存在或已删除"),
    AI_CHAT_MESSAGE_INVALID(80002, "AI 消息为空或超长"),
    AI_CHAT_PET_FORBIDDEN(80003, "无权访问该宠物的 AI 数据"),
    AI_CHAT_TIMEOUT(80004, "AI 对话处理超时，请稍后重试"),
    AI_CHAT_TOOL_FAILED(80005, "AI 数据工具暂时不可用"),
    AI_CHAT_BOUNDARY_BLOCKED(80006, "回答未通过安全校验"),
    AI_CHAT_FEATURE_UNAVAILABLE(80007, "该 AI 功能尚未开放"),
    AI_CHAT_DUPLICATE(80008, "该消息正在处理或已经处理");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
