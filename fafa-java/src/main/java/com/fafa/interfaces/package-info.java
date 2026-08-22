/**
 * 接口层（Interfaces Layer）
 * 
 * 职责：
 * - REST 控制器：处理 HTTP 请求
 * - DTO 定义：请求和响应数据传输对象
 * - 参数校验：使用 Jakarta Validation
 * - 协议转换：DTO <-> 领域对象
 * 
 * 原则：
 * - 只做参数校验和协议转换，不包含业务逻辑
 * - 依赖应用层服务
 */
package com.fafa.interfaces;
