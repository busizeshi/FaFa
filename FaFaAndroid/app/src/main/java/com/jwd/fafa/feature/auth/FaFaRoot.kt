package com.jwd.fafa.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jwd.fafa.core.data.BootstrapRepository
import com.jwd.fafa.core.data.SessionStore
import com.jwd.fafa.core.data.UserPrefs
import com.jwd.fafa.ui.components.FaFaBrandMark
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 根决策：已登录 → AUTH-05；已看完介绍 → 登录；否则 → 欢迎页。 */
@HiltViewModel
class RootViewModel @Inject constructor(
    sessionStore: SessionStore,
    userPrefs: UserPrefs,
    private val bootstrapRepository: BootstrapRepository,
) : ViewModel() {

    val startDestination: StateFlow<AuthNavKey?> = combine(
        sessionStore.tokenFlow,
        userPrefs.welcomeCompletedFlow,
    ) { token, welcomeDone ->
        when {
            token != null -> OnboardingEntryRoute
            welcomeDone -> LoginRoute
            else -> WelcomeRoute
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { bootstrapRepository.refresh() }
    }
}

/** 品牌启动屏：等待本地会话/偏好读取完成后决定起始页。 */
@Composable
fun FaFaRoot(rootViewModel: RootViewModel = hiltViewModel()) {
    val start by rootViewModel.startDestination.collectAsStateWithLifecycle()
    val current = start
    if (current == null) {
        SplashGate()
    } else {
        FaFaNavGraph(start = current)
    }
}

@Composable
private fun SplashGate() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(24.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(28.dp),
        ) {
            FaFaBrandMark()
        }
        Text(
            text = "FaFa",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "正在准备你的照护伙伴…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
