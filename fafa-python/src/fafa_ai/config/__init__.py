"""统一配置模块。

集中加载原则（架构规范 §5.2）：配置只在 :mod:`fafa_ai.config` 读取一次，
业务代码不散读 os.environ；密钥（DashScope Key 等）仅经环境变量/Secret 注入。
"""
