package com.jwd.fafa.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jwd.fafa.core.common.Validators
import com.jwd.fafa.core.data.AuthRepository
import com.jwd.fafa.core.data.BootState
import com.jwd.fafa.core.data.BootstrapRepository
import com.jwd.fafa.core.data.UserPrefs
import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.ErrorCode
import com.jwd.fafa.ui.components.BannerKind
import com.jwd.fafa.ui.components.ConsentRow
import com.jwd.fafa.ui.components.FaFaBanner
import com.jwd.fafa.ui.components.FaFaBannerWithAction
import com.jwd.fafa.ui.components.FaFaPrimaryButton
import com.jwd.fafa.ui.components.FaFaSecondaryButton
import com.jwd.fafa.ui.components.FaFaTextField
import com.jwd.fafa.ui.components.GlassCard
import com.jwd.fafa.ui.components.FaFaPage
import com.jwd.fafa.ui.components.formatMmss
import com.jwd.fafa.ui.components.rememberNowMs
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val consentChecked: Boolean = false,
    val submitting: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val bannerKind: BannerKind? = null,
    val bannerText: String? = null,
    val bannerActionGoLogin: Boolean = false,
    val rateLimitedUntilMs: Long = 0L,
    val mailFailedRetryable: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bootstrapRepository: BootstrapRepository,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    val bootState: StateFlow<BootState> get() = bootstrapRepository.state

    /**
     * 幂等键：一次注册尝试（含重试/重发邮件 503-01-01）复用同一 key，
     * 服务端按 (scope, key) 去重防止重复建号（手册 §6.2 / 契约接口 2）。
     */
    private val idempotencyKey: String = UUID.randomUUID().toString()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, mailFailedRetryable = false) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }
    fun onConsentChange(v: Boolean) = _state.update { it.copy(consentChecked = v) }
    fun clearBanner() = _state.update { it.copy(bannerKind = null, bannerText = null, bannerActionGoLogin = false) }
    fun retryBootstrap() = viewModelScope.launch { bootstrapRepository.refresh() }

    fun submit(onChallengeCreated: (ChallengeUi) -> Unit) {
        val s = _state.value
        if (s.submitting) return
        val email = s.email.trim()
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = "请输入有效的邮箱地址") }
            return
        }
        Validators.passwordIssue(s.password)?.let {
            _state.update { st -> st.copy(passwordError = it) }
            return
        }
        if (s.confirmPassword != s.password) {
            _state.update { it.copy(confirmError = "两次输入的密码不一致") }
            return
        }
        if (!s.consentChecked) {
            _state.update {
                it.copy(bannerKind = BannerKind.WARN, bannerText = "请先阅读并同意《用户协议》和《隐私政策》")
            }
            return
        }
        val legalVersions = bootstrapRepository.cached?.legalVersions
        if (legalVersions.isNullOrEmpty()) {
            _state.update {
                it.copy(bannerKind = BannerKind.ERROR, bannerText = "协议版本尚未加载，请点击重试")
            }
            return
        }
        val accepted = legalVersions.map { (type, version) -> type to version }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    submitting = true,
                    emailError = null,
                    passwordError = null,
                    confirmError = null,
                    mailFailedRetryable = false,
                )
            }
            try {
                val challenge = authRepository.register(
                    idempotencyKey = idempotencyKey,
                    email = email,
                    password = s.password,
                    acceptedLegal = accepted,
                )
                userPrefs.saveConsent(legalVersions)
                onChallengeCreated(challenge.toUi(idempotencyKey))
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.EMAIL_ALREADY_REGISTERED -> _state.update {
                        it.copy(
                            bannerKind = BannerKind.WARN,
                            bannerText = e.displayMessage.ifBlank { "该邮箱已注册，请直接登录" },
                            bannerActionGoLogin = true,
                        )
                    }
                    ErrorCode.WEAK_PASSWORD -> _state.update {
                        it.copy(
                            passwordError = "密码需为 8–64 位且同时包含字母和数字",
                            bannerKind = BannerKind.ERROR,
                            bannerText = e.displayMessage,
                        )
                    }
                    ErrorCode.LEGAL_VERSION_MISMATCH -> _state.update {
                        it.copy(
                            consentChecked = false,
                            bannerKind = BannerKind.WARN,
                            bannerText = "协议版本已更新，请重新阅读并勾选同意后再提交",
                        )
                    }
                    ErrorCode.RATE_LIMITED -> {
                        val seconds = e.retryAfterSeconds ?: 60L
                        _state.update {
                            it.copy(
                                rateLimitedUntilMs = System.currentTimeMillis() + seconds * 1000,
                                bannerKind = BannerKind.WARN,
                                bannerText = "操作过于频繁，请稍后再试",
                            )
                        }
                    }
                    ErrorCode.MAIL_SEND_FAILED -> _state.update {
                        it.copy(
                            mailFailedRetryable = true,
                            bannerKind = BannerKind.ERROR,
                            bannerText = "验证邮件发送失败，请点击重试（不会重复创建账号）",
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
}

/** AUTH-02 注册页：校验 → 发送验证邮件 → AUTH-03。 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onChallengeCreated: (ChallengeUi) -> Unit,
    onOpenDocument: (String) -> Unit,
    onGoLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val now = rememberNowMs()
    val rateLimited = state.rateLimitedUntilMs > now
    val remainingSeconds = ((state.rateLimitedUntilMs - now) / 1000).coerceAtLeast(0)

    FaFaPage(
        title = "注册 FaFa",
        subtitle = "只需邮箱和密码，稍后验证邮箱即可开始。",
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (boot) {
                is BootState.Loading -> FaFaBanner(text = "正在连接服务…", kind = BannerKind.INFO)
                is BootState.Failed -> FaFaBannerWithAction(
                    text = (boot as BootState.Failed).message,
                    kind = BannerKind.ERROR,
                    actionText = "重试",
                    onAction = viewModel::retryBootstrap,
                )
                else -> Unit
            }
            if (rateLimited) {
                FaFaBanner(
                    text = "操作过于频繁，约 ${formatMmss(remainingSeconds)} 后可重试",
                    kind = BannerKind.WARN,
                )
            }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FaFaTextField(
                        label = "邮箱",
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        hint = "请输入邮箱地址",
                        errorText = state.emailError,
                        keyboardType = KeyboardType.Email,
                    )
                    FaFaTextField(
                        label = "密码",
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        hint = "8–64 位，含字母和数字",
                        errorText = state.passwordError,
                        isPassword = true,
                        supportingText = "需同时包含字母和数字",
                    )
                    FaFaTextField(
                        label = "确认密码",
                        value = state.confirmPassword,
                        onValueChange = viewModel::onConfirmChange,
                        hint = "再次输入密码",
                        errorText = state.confirmError,
                        isPassword = true,
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit(onChallengeCreated) }),
                    )
                }
            }

            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                ConsentRow(
                    checked = state.consentChecked,
                    onCheckedChange = viewModel::onConsentChange,
                    onOpenDocument = onOpenDocument,
                )
            }

            if (state.bannerText != null && state.bannerKind != null) {
                if (state.bannerActionGoLogin) {
                    FaFaBannerWithAction(
                        text = state.bannerText!!,
                        kind = state.bannerKind!!,
                        actionText = "去登录",
                        onAction = onGoLogin,
                    )
                } else {
                    FaFaBanner(text = state.bannerText!!, kind = state.bannerKind!!)
                }
            }

            FaFaPrimaryButton(
                text = "发送验证邮件",
                onClick = { viewModel.submit(onChallengeCreated) },
                submitting = state.submitting,
                submittingText = "提交中",
                enabled = boot is BootState.Ready && !rateLimited,
            )
            FaFaSecondaryButton(
                text = "已有账号？去登录",
                onClick = onGoLogin,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提交携带 Idempotency-Key：网络超时或验证邮件发送失败时重试，不会重复创建账号。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
