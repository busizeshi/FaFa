package com.jwd.fafa.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jwd.fafa.core.common.Validators
import com.jwd.fafa.core.data.AuthRepository
import com.jwd.fafa.core.data.SessionStore
import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.SessionInfoDto
import com.jwd.fafa.ui.components.BannerKind
import com.jwd.fafa.ui.components.FaFaBanner
import com.jwd.fafa.ui.components.FaFaPage
import com.jwd.fafa.ui.components.FaFaPrimaryButton
import com.jwd.fafa.ui.components.FaFaSecondaryButton
import com.jwd.fafa.ui.components.FaFaSegmented
import com.jwd.fafa.ui.components.FaFaTextButton
import com.jwd.fafa.ui.components.FaFaTextField
import com.jwd.fafa.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- ViewModel ----------------

data class OnboardingUiState(
    val segment: Int = 0, // 0 创建宠物 / 1 加入家庭
    val nickname: String = "",
    val species: Int = 0, // 0 猫 / 1 狗
    val optionalExpanded: Boolean = false,
    val inviteCode: String = "",
    val nicknameError: String? = null,
    val inviteError: String? = null,
    val bannerKind: BannerKind? = null,
    val bannerText: String? = null,
)

data class SessionsUiState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val items: List<SessionInfoDto> = emptyList(),
    val errorText: String? = null,
    val loadedOnce: Boolean = false,
    val revokingId: String? = null,
    val loggingOut: Boolean = false,
    val bannerText: String? = null,
)

@HiltViewModel
class OnboardingEntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _sessions = MutableStateFlow(SessionsUiState())
    val sessions: StateFlow<SessionsUiState> = _sessions.asStateFlow()

    val userEmail: String get() = sessionStore.userEmailNow() ?: "-"

    fun onSegment(i: Int) = _state.update { it.copy(segment = i, bannerKind = null, bannerText = null) }
    fun onNickname(v: String) = _state.update { it.copy(nickname = v, nicknameError = null) }
    fun onSpecies(i: Int) = _state.update { it.copy(species = i) }
    fun toggleOptional() = _state.update { it.copy(optionalExpanded = !it.optionalExpanded) }
    fun onInviteCode(v: String) = _state.update { it.copy(inviteCode = v, inviteError = null) }
    fun clearBanner() = _state.update { it.copy(bannerKind = null, bannerText = null) }

    /**
     * M00 范围内 ONBOARD 接口未实现（服务端计划在 M01 提供）。
     * 这里明确提示“即将开放”，不伪造建档成功（手册：不得假成功掩盖后端未实现）。
     */
    fun submitCreatePet(onShowNotice: () -> Unit) {
        val s = _state.value
        if (!Validators.isValidNickname(s.nickname.trim())) {
            _state.update { it.copy(nicknameError = "昵称需 1–20 个字符") }
            return
        }
        onShowNotice()
    }

    fun submitJoinFamily(onShowNotice: () -> Unit) {
        val s = _state.value
        if (s.inviteCode.isBlank()) {
            _state.update { it.copy(inviteError = "请输入邀请码") }
            return
        }
        onShowNotice()
    }

    // ---------- 设备与登录 ----------

    fun showSessions() {
        _sessions.update { it.copy(visible = true) }
        loadSessions()
    }

    fun dismissSessions() = _sessions.update { it.copy(visible = false, bannerText = null) }

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.update { it.copy(loading = true, errorText = null) }
            try {
                val items = authRepository.sessions()
                _sessions.update { it.copy(loading = false, items = items, loadedOnce = true) }
            } catch (e: ApiError) {
                _sessions.update { it.copy(loading = false, errorText = e.message) }
            }
        }
    }

    fun revoke(sessionId: String) {
        viewModelScope.launch {
            _sessions.update { it.copy(revokingId = sessionId) }
            try {
                authRepository.revokeSession(sessionId)
                _sessions.update { current ->
                    current.copy(
                        revokingId = null,
                        items = current.items.filterNot { it.sessionId == sessionId },
                    )
                }
            } catch (e: ApiError) {
                _sessions.update { it.copy(revokingId = null, bannerText = e.message) }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _sessions.update { it.copy(loggingOut = true) }
            try {
                authRepository.logout()
            } finally {
                _sessions.update { it.copy(loggingOut = false, visible = false) }
                onLoggedOut()
            }
        }
    }
}

// ---------------- 页面 ----------------

/** AUTH-05 首次建档/选择路径。M00 仅提供入口与设备管理；建档与邀请属 M01。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingEntryScreen(
    onExitToLogin: () -> Unit,
    viewModel: OnboardingEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var showFeatureNotice by remember { mutableStateOf<String?>(null) }

    FaFaPage(
        title = "先认识你的伙伴",
        subtitle = "创建宠物档案，或加入已有的照护家庭。",
        topBarRight = {
            IconButton(onClick = viewModel::showSessions, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Rounded.Devices,
                    contentDescription = "设备与登录管理",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FaFaSegmented(
                options = listOf("创建宠物", "加入家庭"),
                selectedIndex = state.segment,
                onSelect = viewModel::onSegment,
            )

            if (state.bannerText != null && state.bannerKind != null) {
                FaFaBanner(text = state.bannerText!!, kind = state.bannerKind!!)
            }

            if (state.segment == 0) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FaFaTextField(
                            label = "宠物昵称",
                            value = state.nickname,
                            onValueChange = viewModel::onNickname,
                            hint = "1–20 个字符",
                            errorText = state.nicknameError,
                            maxLength = 20,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpeciesChip("猫", Icons.Rounded.Pets, state.species == 0, Modifier.weight(1f)) {
                                viewModel.onSpecies(0)
                            }
                            SpeciesChip("狗", Icons.Rounded.Pets, state.species == 1, Modifier.weight(1f)) {
                                viewModel.onSpecies(1)
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleOptional() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "补充宠物资料（选填）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (state.optionalExpanded) "收起" else "展开",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.optionalExpanded) {
                            FaFaBanner(
                                text = "头像、生日、品种、体重等选填资料将在 M01 模块联调后支持保存。",
                                kind = BannerKind.INFO,
                            )
                        }
                        FaFaPrimaryButton(
                            text = "创建宠物的家",
                            onClick = {
                                viewModel.submitCreatePet {
                                    showFeatureNotice = "创建宠物档案与默认家庭由 M01 模块（ONBOARD 接口）提供，将在服务端联调后开放。你的登录与会话已全部就绪。"
                                }
                            },
                        )
                    }
                }
            } else {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FaFaTextField(
                            label = "家庭邀请码",
                            value = state.inviteCode,
                            onValueChange = viewModel::onInviteCode,
                            hint = "请向家庭成员获取邀请码",
                            errorText = state.inviteError,
                        )
                        FaFaPrimaryButton(
                            text = "查看邀请并加入",
                            onClick = {
                                viewModel.submitJoinFamily {
                                    showFeatureNotice = "接受家庭邀请需要 M01 模块（INVITES 接口）提供预览与授权确认，将在服务端联调后开放。"
                                }
                            },
                        )
                    }
                }
            }

            FaFaSecondaryButton(
                text = "稍后再说，先去逛逛",
                onClick = {
                    showFeatureNotice = "浏览与发现能力属于 M09 社区模块，当前版本暂未开放。"
                },
            )
        }
    }

    showFeatureNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { showFeatureNotice = null },
            title = { Text("即将开放") },
            text = { Text(notice) },
            confirmButton = {
                TextButton(onClick = { showFeatureNotice = null }) { Text("知道了") }
            },
        )
    }

    if (sessions.visible) {
        SessionsSheet(
            state = sessions,
            userEmail = viewModel.userEmail,
            onLoad = viewModel::loadSessions,
            onDismiss = viewModel::dismissSessions,
            onRevoke = viewModel::revoke,
            onLogout = { viewModel.logout(onExitToLogin) },
        )
    }
}

@Composable
private fun SpeciesChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                },
                shape,
            )
            .border(
                1.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape,
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsSheet(
    state: SessionsUiState,
    userEmail: String,
    onLoad: () -> Unit,
    onDismiss: () -> Unit,
    onRevoke: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var confirmRevoke by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("设备与登录", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "当前账号：$userEmail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                state.loading && state.items.isEmpty() -> FaFaBanner(text = "正在加载设备列表…", kind = BannerKind.INFO)
                state.errorText != null -> FaFaBannerWithActionCompat(
                    text = state.errorText!!,
                    onLoad = onLoad,
                )
                state.items.isEmpty() && state.loadedOnce -> FaFaBanner(text = "暂无其他有效会话。", kind = BannerKind.INFO)
                else -> state.items.forEach { session ->
                    GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = session.device?.ifBlank { "未知设备" } ?: "未知设备",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "最近活跃：${displayTime(session.lastActiveAt)}" +
                                        if (session.current) " · 本机" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!session.current) {
                                TextButton(onClick = { confirmRevoke = session.sessionId }) {
                                    Text("下线")
                                }
                            }
                        }
                    }
                }
            }

            if (state.bannerText != null) {
                FaFaBanner(text = state.bannerText!!, kind = BannerKind.ERROR)
            }

            FaFaSecondaryButton(
                text = "退出登录",
                onClick = { confirmLogout = true },
                enabled = !state.loggingOut,
            )
            Text(
                text = "退出登录会撤销本机会话并清理本地令牌与缓存；其他设备不受影响。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    confirmRevoke?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmRevoke = null },
            title = { Text("下线该设备？") },
            text = { Text("下线后该设备需要重新登录才能访问你的家庭数据。") },
            confirmButton = {
                TextButton(onClick = { onRevoke(id); confirmRevoke = null }) { Text("确认下线") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = null }) { Text("取消") }
            },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录？") },
            text = { Text("退出后将撤销本机会话，本地令牌与私有缓存会被清理。") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("确认退出") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FaFaBannerWithActionCompat(text: String, onLoad: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FaFaBanner(text = text, kind = BannerKind.ERROR, modifier = Modifier.weight(1f))
        TextButton(onClick = onLoad) { Text("重试") }
    }
}
