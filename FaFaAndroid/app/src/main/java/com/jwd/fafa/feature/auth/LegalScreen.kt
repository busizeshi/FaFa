package com.jwd.fafa.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jwd.fafa.core.config.LegalDoc
import com.jwd.fafa.ui.components.BannerKind
import com.jwd.fafa.ui.components.FaFaBanner
import com.jwd.fafa.ui.components.FaFaPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 协议只读查看页：客户端内置文本（assets/legal），版本号与 bootstrap legalVersions 对齐。
 * 无法加载时不得默认视为同意（AUTH-01 状态要求）。
 */
@Composable
fun LegalScreen(
    documentType: String,
    onBack: () -> Unit,
) {
    val doc = LegalDoc.of(documentType)
    val context = LocalContext.current
    val content by produceState<Result<String>?>(initialValue = null, key1 = documentType) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (doc == null) error("未知文档")
                context.assets.open(doc.assetPath).bufferedReader().use { it.readText() }
            }
        }
    }

    FaFaPage(
        title = doc?.title ?: "协议",
        subtitle = doc?.let { "内置文本 v${com.jwd.fafa.core.config.FaFaConfig.BUNDLED_LEGAL_VERSION}（以最新同意版本为准）" } ?: null,
        onBack = onBack,
    ) {
        when {
            content == null -> {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    FaFaBanner(text = "正在加载协议文本…", kind = BannerKind.INFO)
                }
            }
            content!!.isFailure -> {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    FaFaBanner(
                        text = "协议文本加载失败。未显示完整文本前，不能视为已同意。",
                        kind = BannerKind.ERROR,
                    )
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = content!!.getOrThrow(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
