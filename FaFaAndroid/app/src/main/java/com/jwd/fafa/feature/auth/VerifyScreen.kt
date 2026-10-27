package com.jwd.fafa.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jwd.fafa.core.common.Validators
import com.jwd.fafa.core.data.AuthRepository
import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.ErrorCode
import com.jwd.fafa.ui.components.BannerKind
import com.jwd.fafa.ui.components.FaFaBanner
import com.jwd.fafa.ui.components.FaFaOtpInput
import com.jwd.fafa.ui.components.FaFaPage
import com.jwd.fafa.ui.components.FaFaPrimaryButton
import com.jwd.fafa.ui.components.FaFaTextButton
import com.jwd.fafa.ui.components.GlassCard
import com.jwd.fafa.ui.components.formatMmss
import com.jwd.fafa.ui.components.rememberNowMs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyUiState(
    val code: String = "",
    val submitting: Boolean = false,
    val codeError: String? = null,
    val bannerKind: BannerKind? = null,
    val bannerText: String? = null,
    val resendCooldownUntilMs: Long = 0L,
    val expired: Boolean = false,
)

@HiltViewModel
class VerifyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    @Volatile
    private var bound: VerifyRoute? = null

    private val _state = MutableStateFlow(
        VerifyUiState(resendCooldownUntilMs = System.currentTimeMillis() + DEFAULT_RESEND_COOLDOWN_MS),
    )
    val state: StateFlow<VerifyUiState> = _state.asStateFlow()

    /** 页面组合时绑定路由参数（Nav3 直接传 Key 对象）。 */
    fun bind(route: VerifyRoute) {
        bound = route
    }

    val maskedEmail: String get() = bound?.maskedEmail.orEmpty()

    fun onCodeChange(v: String) = _state.update {
        it.copy(code = v, codeError = null, bannerKind = null, bannerText = null)
    }

    fun verify(onSuccess: () -> Unit) {
        val route = bound ?: return
        val s = _state.value
        if (s.submitting || s.expired) return
        if (!Validators.isValidVerificationCode(s.code)) {
            _state.update { it.copy(codeError = "请输入 6 位数字验证码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, codeError = null) }
            try {
                authRepository.verifyRegisterChallenge(route.challengeId, s.code)
                onSuccess()
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.CODE_EXPIRED -> _state.update {
                        it.copy(
                            expired = true,
                            bannerKind = BannerKind.WARN,
                            bannerText = "验证码已过期，请重新获取",
                        )
                    }
                    ErrorCode.CODE_INVALID -> _state.update {
                        it.copy(
                            codeError = "验证码错误，请重新输入",
                            bannerKind = BannerKind.ERROR,
                            bannerText = e.displayMessage,
                        )
                    }
                    ErrorCode.TOO_MANY_ATTEMPTS -> _state.update {
                        it.copy(
                            bannerKind = BannerKind.WARN,
                            bannerText = "尝试次数过多。重新发送注册验证码的能力依赖服务端重发接口（当前版本暂未提供，详见联调纪要）。",
                        )
                    }
                    ErrorCode.CHALLENGE_NOT_FOUND -> _state.update {
                        it.copy(
                            bannerKind = BannerKind.ERROR,
                            bannerText = "验证请求不存在或已失效，请返回重新获取验证码",
                        )
                    }
                    else -> _state.update {
                        it.copy(bannerKind = BannerKind.ERROR, bannerText = e.displayMessage)
                    }
                }
            } catch (e: ApiError) {
                _state.update { it.copy(bannerKind = BannerKind.ERROR, bannerText = e.message) }
            } finally {
                _state.update { it.copy(submitting = false) }
            }
        }
    }

    fun resend() {
        val s = _state.value
        if (s.resendCooldownUntilMs > System.currentTimeMillis()) return
        // 契约缺口（联调纪要 #1）：M00 未提供注册验证码重发接口；
        // register 对已存在邮箱返回 409-01-01，不能借它重发。给出诚实提示而非假成功。
        _state.update {
            it.copy(
                bannerKind = BannerKind.INFO,
                bannerText = "当前服务端版本暂未提供注册验证码重发接口（联调纪要 #1）；如长时间未收到邮件，请返回重新提交注册或联系支持。",
            )
        }
    }
}

/** AUTH-03 邮箱验证页：6 位验证码、有效期与重发冷却倒计时。 */
@Composable
fun VerifyScreen(
    route: VerifyRoute,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    viewModel: VerifyViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { viewModel.bind(route) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNowMs()
    val cooldownLeft = ((state.resendCooldownUntilMs - now) / 1000).coerceAtLeast(0)
    val expireLeft = ((route.expiresAtEpochMs - now) / 1000).coerceAtLeast(0)

    FaFaPage(
        title = "验证邮箱",
        subtitle = "验证码已发送至 ${route.maskedEmail}，10 分钟内有效。",
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!state.expired && expireLeft > 0) {
                Text(
                    text = "有效期剩余 ${formatMmss(expireLeft)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            if (state.expired || expireLeft == 0L) {
                FaFaBanner(text = "验证码已过期，请返回重新获取验证码。", kind = BannerKind.WARN)
            }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FaFaOtpInput(
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        enabled = !state.submitting && !state.expired,
                    )
                    if (state.codeError != null) {
                        Text(
                            text = state.codeError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "没有收到？",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FaFaTextButton(
                            text = if (cooldownLeft > 0) "重新发送 (${formatMmss(cooldownLeft)})" else "重新发送",
                            onClick = viewModel::resend,
                            enabled = cooldownLeft <= 0 && !state.submitting,
                        )
                    }
                }
            }

            if (state.bannerText != null && state.bannerKind != null) {
                FaFaBanner(text = state.bannerText!!, kind = state.bannerKind!!)
            }

            FaFaPrimaryButton(
                text = "验证并继续",
                onClick = { viewModel.verify(onVerified) },
                submitting = state.submitting,
                submittingText = "校验中",
                enabled = state.code.length == 6 && !state.expired,
            )
            FaFaTextButton(
                text = "修改邮箱，返回上一步",
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}
