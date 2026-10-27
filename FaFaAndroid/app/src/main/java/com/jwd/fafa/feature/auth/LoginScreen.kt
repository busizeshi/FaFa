package com.jwd.fafa.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jwd.fafa.ui.theme.FaFaRadius
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val consentChecked: Boolean = false,
    val consentRequired: Boolean = true,
    val submitting: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val bannerKind: BannerKind? = null,
    val bannerText: String? = null,
    val rateLimitedUntilMs: Long = 0L,
    val suspendedMessage: String? = null,
    val unverified: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bootstrapRepository: BootstrapRepository,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    val bootState: StateFlow<BootState> get() = bootstrapRepository.state

    init {
        // 协议版本变化 → 需重新同意
        viewModelScope.launch {
            bootstrapRepository.state.collect { boot ->
                if (boot is BootState.Ready) {
                    val agreed = userPrefs.consentVersions()
                    val required = boot.info.legalVersions
                    val mismatch = required.isNotEmpty() && required.any { entry -> agreed[entry.key] != entry.value }
                    _state.update {
                        it.copy(
                            consentRequired = mismatch || required.isEmpty(),
                            consentChecked = if (mismatch || required.isEmpty()) false else it.consentChecked,
                        )
                    }
                }
            }
        }
    }

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, unverified = false) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun onConsentChange(v: Boolean) = _state.update { it.copy(consentChecked = v) }
    fun clearBanner() = _state.update { it.copy(bannerKind = null, bannerText = null) }
    fun clearSuspended() = _state.update { it.copy(suspendedMessage = null) }
    fun retryBootstrap() = viewModelScope.launch { bootstrapRepository.refresh() }

    fun login(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.submitting) return
        val email = s.email.trim()
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = "请输入有效的邮箱地址") }
            return
        }
        if (s.password.isEmpty()) {
            _state.update { it.copy(passwordError = "请输入密码") }
            return
        }
        if (!s.consentChecked) {
            _state.update {
                it.copy(bannerKind = BannerKind.WARN, bannerText = "请先阅读并同意《用户协议》和《隐私政策》")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, emailError = null, passwordError = null) }
            try {
                authRepository.login(email, s.password)
                bootstrapRepository.cached?.legalVersions?.let { userPrefs.saveConsent(it) }
                onSuccess()
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.INVALID_CREDENTIALS -> _state.update {
                        it.copy(passwordError = "邮箱或密码不正确")
                    }
                    ErrorCode.ACCOUNT_SUSPENDED -> _state.update {
                        it.copy(suspendedMessage = e.displayMessage.ifBlank { "账号已停用，如有疑问请联系客服申诉" })
                    }
                    ErrorCode.CHALLENGE_NOT_FOUND -> _state.update {
                        it.copy(
                            unverified = true,
                            bannerKind = BannerKind.WARN,
                            bannerText = "该账号尚未完成邮箱验证，请先完成注册验证后再登录",
                        )
                    }
                    ErrorCode.LOGIN_RATE_LIMITED -> {
                        val seconds = e.retryAfterSeconds ?: 60L
                        _state.update {
                            it.copy(
                                rateLimitedUntilMs = System.currentTimeMillis() + seconds * 1000,
                                bannerKind = BannerKind.WARN,
                                bannerText = e.displayMessage,
                            )
                        }
                    }
                    else -> _state.update {
                        it.copy(bannerKind = BannerKind.ERROR, bannerText = e.displayMessage)
                    }
                }
            } catch (e: ApiError) {
                _state.update {
                    it.copy(bannerKind = BannerKind.ERROR, bannerText = e.message, )
                }
            } finally {
                _state.update { it.copy(submitting = false) }
            }
        }
    }
}

/** AUTH-02 登录页（苹果圆风 + 液态玻璃；rate-limited 状态帧含倒计时）。 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onOpenRegister: () -> Unit,
    onOpenReset: () -> Unit,
    onOpenDocument: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val now = rememberNowMs()
    val rateLimited = state.rateLimitedUntilMs > now
    val remainingSeconds = ((state.rateLimitedUntilMs - now) / 1000).coerceAtLeast(0)

    LaunchedEffect(rateLimited) {
        if (!rateLimited && state.rateLimitedUntilMs > 0) viewModel.clearBanner()
    }

    FaFaPage(
        title = "欢迎回来",
        subtitle = "登录 FaFa，继续记录你家毛孩子的日常。",
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
                    text = "登录尝试过于频繁，约 ${formatMmss(remainingSeconds)} 后可重试",
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
                        hint = "请输入密码",
                        errorText = state.passwordError,
                        isPassword = true,
                        keyboardActions = KeyboardActions(onDone = { viewModel.login(onLoggedIn) }),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onOpenReset) {
                            Text("忘记密码？", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
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
                FaFaBanner(text = state.bannerText!!, kind = state.bannerKind!!)
            }

            FaFaPrimaryButton(
                text = "登录",
                onClick = { viewModel.login(onLoggedIn) },
                submitting = state.submitting,
                submittingText = "登录中",
                enabled = boot is BootState.Ready && !rateLimited,
            )
            FaFaSecondaryButton(
                text = "还没有账号？注册 FaFa",
                onClick = onOpenRegister,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "凭据错误不会透露邮箱是否已注册；会话令牌仅保存在本机安全区。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }

    if (state.suspendedMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSuspended() },
            title = { Text("账号已停用") },
            text = { Text(state.suspendedMessage!! + "\n\n如需申诉，请联系客服邮箱 support@fafa.app") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSuspended() }) { Text("知道了") }
            },
        )
    }
}
