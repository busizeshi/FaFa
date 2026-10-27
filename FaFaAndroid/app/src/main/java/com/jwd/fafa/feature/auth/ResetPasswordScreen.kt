package com.jwd.fafa.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.ErrorCode
import com.jwd.fafa.ui.components.BannerKind
import com.jwd.fafa.ui.components.FaFaBanner
import com.jwd.fafa.ui.components.FaFaOtpInput
import com.jwd.fafa.ui.components.FaFaPage
import com.jwd.fafa.ui.components.FaFaPrimaryButton
import com.jwd.fafa.ui.components.FaFaTextButton
import com.jwd.fafa.ui.components.FaFaTextField
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

enum class ResetStep { EMAIL, CODE, NEW_PASSWORD }

data class ResetUiState(
    val step: ResetStep = ResetStep.EMAIL,
    val email: String = "",
    val maskedEmail: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val submitting: Boolean = false,
    val emailError: String? = null,
    val codeError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val bannerKind: BannerKind? = null,
    val bannerText: String? = null,
    val sendCooldownUntilMs: Long = 0L,
    val dailyLimited: Boolean = false,
    val expiresAtEpochMs: Long = 0L,
    val showSuccessDialog: Boolean = false,
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResetUiState())
    val state: StateFlow<ResetUiState> = _state.asStateFlow()

    private var challengeId: Long = 0
    private var resetCredential: String? = null

    fun bind(prefillEmail: String?) {
        if (prefillEmail != null && _state.value.email.isEmpty()) {
            _state.update { it.copy(email = prefillEmail) }
        }
    }

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun onCodeChange(v: String) = _state.update { it.copy(code = v, codeError = null, bannerKind = null, bannerText = null) }
    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }
    fun clearBanner() = _state.update { it.copy(bannerKind = null, bannerText = null) }

    /** 系统返回：向导步骤内先回退步骤。 */
    fun onBack(): Boolean {
        val s = _state.value
        return when (s.step) {
            ResetStep.CODE -> {
                _state.update { it.copy(step = ResetStep.EMAIL, code = "", bannerKind = null, bannerText = null) }
                true
            }
            ResetStep.NEW_PASSWORD -> {
                _state.update { it.copy(step = ResetStep.CODE, newPassword = "", confirmPassword = "") }
                true
            }
            ResetStep.EMAIL -> false
        }
    }

    fun sendCode() {
        val s = _state.value
        if (s.submitting || s.dailyLimited) return
        val email = s.email.trim()
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = "请输入有效的邮箱地址") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, emailError = null) }
            try {
                val challenge = authRepository.createPasswordResetChallenge(email)
                challengeId = challenge.challengeId
                val expiryMs = challenge.expiresAt
                    ?.let { text -> com.jwd.fafa.core.common.parseServerTime(text).toInstant().toEpochMilli() }
                    ?: (System.currentTimeMillis() + DEFAULT_CODE_TTL_MS)
                _state.update {
                    it.copy(
                        step = ResetStep.CODE,
                        maskedEmail = challenge.email,
                        sendCooldownUntilMs = System.currentTimeMillis() + DEFAULT_RESEND_COOLDOWN_MS,
                        expiresAtEpochMs = expiryMs,
                        bannerKind = BannerKind.INFO,
                        bannerText = "若该邮箱已注册，验证码已发送；若未收到请检查垃圾邮件或稍后重发。",
                        code = "",
                    )
                }
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.SEND_TOO_FREQUENT -> _state.update {
                        it.copy(
                            sendCooldownUntilMs = System.currentTimeMillis() + (e.retryAfterSeconds ?: 60) * 1000,
                            bannerKind = BannerKind.WARN,
                            bannerText = "验证码发送太频繁，请稍后再试",
                        )
                    }
                    ErrorCode.DAILY_SEND_LIMIT -> _state.update {
                        it.copy(
                            dailyLimited = true,
                            bannerKind = BannerKind.WARN,
                            bannerText = "今日发送已达上限，请明日再试",
                        )
                    }
                    ErrorCode.MAIL_SEND_FAILED -> _state.update {
                        it.copy(
                            bannerKind = BannerKind.ERROR,
                            bannerText = "验证邮件发送失败，请点击重新发送",
                        )
                    }
                    else -> _state.update {
                        it.copy(bannerKind = BannerKind.ERROR, bannerText = e.authMessage())
                    }
                }
            } catch (e: ApiError) {
                _state.update { it.copy(bannerKind = BannerKind.ERROR, bannerText = e.message) }
            } finally {
                _state.update { it.copy(submitting = false) }
            }
        }
    }

    fun verifyCode() {
        val s = _state.value
        if (s.submitting) return
        if (!Validators.isValidVerificationCode(s.code)) {
            _state.update { it.copy(codeError = "请输入 6 位数字验证码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, codeError = null) }
            try {
                resetCredential = authRepository.verifyPasswordResetChallenge(challengeId, s.code)
                _state.update { it.copy(step = ResetStep.NEW_PASSWORD, bannerKind = null, bannerText = null) }
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.CODE_EXPIRED, ErrorCode.CHALLENGE_NOT_FOUND, ErrorCode.TOO_MANY_ATTEMPTS -> _state.update {
                        it.copy(
                            bannerKind = BannerKind.WARN,
                            bannerText = when (e.code) {
                                ErrorCode.CODE_EXPIRED -> "验证码已过期，请重新发送"
                                ErrorCode.CHALLENGE_NOT_FOUND -> "验证请求不存在或已失效，请重新发送验证码"
                                else -> "尝试次数过多，请重新发送验证码"
                            },
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

    fun resendCode() {
        val s = _state.value
        if (s.sendCooldownUntilMs > System.currentTimeMillis() || s.dailyLimited) return
        sendCode()
    }

    fun submitNewPassword(onDone: () -> Unit) {
        val s = _state.value
        val credential = resetCredential
        if (s.submitting || credential == null) return
        Validators.passwordIssue(s.newPassword)?.let {
            _state.update { st -> st.copy(passwordError = it) }
            return
        }
        if (s.confirmPassword != s.newPassword) {
            _state.update { it.copy(confirmError = "两次输入的密码不一致") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, passwordError = null, confirmError = null) }
            try {
                authRepository.resetPassword(credential, s.newPassword)
                _state.update { it.copy(showSuccessDialog = true) }
            } catch (e: ApiError.Biz) {
                when (e.code) {
                    ErrorCode.RESET_CREDENTIAL_INVALID -> {
                        resetCredential = null
                        _state.update {
                            it.copy(
                                step = ResetStep.EMAIL,
                                code = "",
                                newPassword = "",
                                confirmPassword = "",
                                bannerKind = BannerKind.WARN,
                                bannerText = "重置凭据无效或已过期，请从第一步重新开始",
                            )
                        }
                    }
                    ErrorCode.WEAK_PASSWORD -> _state.update {
                        it.copy(passwordError = "密码需为 8–64 位且同时包含字母和数字")
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

    fun dismissSuccess(onDone: () -> Unit) {
        _state.update { it.copy(showSuccessDialog = false) }
        onDone()
    }
}

/** AUTH-04 找回密码：同一全屏向导两屏（①邮箱+验证码发送/验证 ②新密码）。 */
@Composable
fun ResetPasswordScreen(
    prefillEmail: String?,
    onDone: () -> Unit,
    onExit: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(prefillEmail) { viewModel.bind(prefillEmail) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNowMs()
    val cooldownLeft = ((state.sendCooldownUntilMs - now) / 1000).coerceAtLeast(0)

    BackHandler(enabled = state.step != ResetStep.EMAIL) {
        if (!viewModel.onBack()) onExit()
    }

    val progress = if (state.step == ResetStep.NEW_PASSWORD) "2/2" else "1/2"
    val title = when (state.step) {
        ResetStep.EMAIL -> "找回密码"
        ResetStep.CODE -> "验证邮箱"
        ResetStep.NEW_PASSWORD -> "设置新密码"
    }

    FaFaPage(title = title, subtitle = "第 $progress 步 · 重置成功后需要重新登录", onBack = { if (!viewModel.onBack()) onExit() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.bannerText != null && state.bannerKind != null) {
                FaFaBanner(text = state.bannerText!!, kind = state.bannerKind!!)
            }

            when (state.step) {
                ResetStep.EMAIL -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            FaFaTextField(
                                label = "注册邮箱",
                                value = state.email,
                                onValueChange = viewModel::onEmailChange,
                                hint = "请输入注册时使用的邮箱",
                                errorText = state.emailError,
                                keyboardType = KeyboardType.Email,
                                supportingText = "发送结果不会透露该邮箱是否已注册",
                            )
                            FaFaPrimaryButton(
                                text = "发送验证邮件",
                                onClick = viewModel::sendCode,
                                submitting = state.submitting,
                                submittingText = "发送中",
                                enabled = !state.dailyLimited && cooldownLeft <= 0,
                            )
                        }
                    }
                    if (cooldownLeft > 0) {
                        FaFaBanner(
                            text = "发送冷却中，约 ${formatMmss(cooldownLeft)} 后可重发",
                            kind = BannerKind.INFO,
                        )
                    }
                }

                ResetStep.CODE -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "验证码已发送至 ${state.maskedEmail}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FaFaOtpInput(
                                value = state.code,
                                onValueChange = viewModel::onCodeChange,
                                enabled = !state.submitting,
                            )
                            if (state.codeError != null) {
                                Text(
                                    text = state.codeError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            FaFaPrimaryButton(
                                text = "验证",
                                onClick = viewModel::verifyCode,
                                submitting = state.submitting,
                                submittingText = "校验中",
                                enabled = state.code.length == 6,
                            )
                            FaFaTextButton(
                                text = if (cooldownLeft > 0) "重新发送 (${formatMmss(cooldownLeft)})" else "重新发送验证码",
                                onClick = viewModel::resendCode,
                                enabled = cooldownLeft <= 0 && !state.dailyLimited,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                ResetStep.NEW_PASSWORD -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            FaFaTextField(
                                label = "新密码",
                                value = state.newPassword,
                                onValueChange = viewModel::onNewPasswordChange,
                                hint = "8–64 位，含字母和数字",
                                errorText = state.passwordError,
                                isPassword = true,
                            )
                            FaFaTextField(
                                label = "确认新密码",
                                value = state.confirmPassword,
                                onValueChange = viewModel::onConfirmChange,
                                hint = "再次输入新密码",
                                errorText = state.confirmError,
                                isPassword = true,
                                keyboardActions = KeyboardActions(onDone = { viewModel.submitNewPassword(onDone) }),
                            )
                            FaFaPrimaryButton(
                                text = "重设密码",
                                onClick = { viewModel.submitNewPassword(onDone) },
                                submitting = state.submitting,
                                submittingText = "提交中",
                            )
                            Text(
                                text = "重设成功后，该账号在其他设备的登录会话将全部失效。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccess(onDone) },
            title = { Text("密码已重置") },
            text = { Text("请使用新密码重新登录。该账号在其他设备的会话已全部下线。") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSuccess(onDone) }) { Text("去登录") }
            },
        )
    }
}
