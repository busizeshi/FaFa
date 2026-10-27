package com.jwd.fafa.core.config

import android.content.Context
import com.jwd.fafa.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 客户端唯一配置入口。
 *
 * 优先级：设备覆盖文件 > 构建期配置文件（app/fafa.properties → BuildConfig）。
 * 覆盖文件路径：/sdcard/Android/data/<applicationId>/files/fafa.properties
 * 用法说明见 app/fafa.properties 头注释。
 */
@Singleton
class FaFaConfig @Inject constructor(@ApplicationContext context: Context) {

    val baseUrl: String
    val connectTimeoutSeconds: Int
    val readTimeoutSeconds: Int
    val enableHttpLog: Boolean

    init {
        val override = readOverride(context)
        baseUrl = (override.getProperty("baseUrl", BuildConfig.BASE_URL)).trimEnd('/')
        connectTimeoutSeconds = override.getProperty("connectTimeoutSeconds", BuildConfig.CONNECT_TIMEOUT_SECONDS.toString()).toIntOrNull() ?: 10
        readTimeoutSeconds = override.getProperty("readTimeoutSeconds", BuildConfig.READ_TIMEOUT_SECONDS.toString()).toIntOrNull() ?: 30
        enableHttpLog = override.getProperty("enableHttpLogging", BuildConfig.ENABLE_HTTP_LOG.toString()).toBoolean()
    }

    private fun readOverride(context: Context): Properties = try {
        val file = File(context.getExternalFilesDir(null), OVERRIDE_FILE_NAME)
        Properties().apply {
            // Properties.load(InputStream) 规范上是 ISO-8859-1，中文会乱码；显式按 UTF-8 解析。
            if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
        }
    } catch (_: Exception) {
        Properties()
    }

    companion object {
        const val OVERRIDE_FILE_NAME = "fafa.properties"

        /** 客户端内置协议文本版本，必须与服务端 bootstrap legalVersions 对齐。 */
        const val BUNDLED_LEGAL_VERSION = "1.0"
    }
}

/** 内置协议文档清单（文本见 assets/legal/）。 */
enum class LegalDoc(val documentType: String, val title: String, val assetPath: String) {
    USER_AGREEMENT("USER_AGREEMENT", "用户协议", "legal/USER_AGREEMENT_1.0.md"),
    PRIVACY_POLICY("PRIVACY_POLICY", "隐私政策", "legal/PRIVACY_POLICY_1.0.md");

    companion object {
        fun of(documentType: String): LegalDoc? = entries.firstOrNull { it.documentType == documentType }
    }
}
