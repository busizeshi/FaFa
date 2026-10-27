package com.jwd.fafa.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jwd.fafa.core.data.UserPrefs
import com.jwd.fafa.ui.components.FaFaBrandMark
import com.jwd.fafa.ui.components.FaFaPageIndicator
import com.jwd.fafa.ui.components.FaFaPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

private data class WelcomePage(
    val title: String,
    val body: String,
)

private val PAGES = listOf(
    WelcomePage("欢迎来到 FaFa", "为你的猫猫狗狗记下每一餐、每一次陪伴，照护从此有条不紊。"),
    WelcomePage("喂食饮水，按时提醒", "低负担记录喂食、饮水与健康观察，重要的照护节点不再遗漏。"),
    WelcomePage("家人一起照护", "邀请家人共享任务与记录，谁做了、什么时候做的，都清清楚楚。"),
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
) : ViewModel() {
    fun completeWelcome() {
        viewModelScope.launch { userPrefs.setWelcomeCompleted() }
    }
}

/** AUTH-01 欢迎与首次介绍：3 屏横向滑动，协议同意不在介绍页阻断。 */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    fun finish() {
        viewModel.completeWelcome()
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { finish() }) {
                    Text("跳过", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                WelcomePageContent(page)
            }
            FaFaPageIndicator(
                count = PAGES.size,
                current = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
            FaFaPrimaryButton(
                text = if (pagerState.currentPage == PAGES.lastIndex) "开始使用" else "继续",
                onClick = {
                    if (pagerState.currentPage == PAGES.lastIndex) {
                        finish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WelcomePageContent(index: Int) {
    val page = PAGES[index]
    Column(
        Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = when (index) {
            0 -> Icons.Rounded.Pets
            1 -> Icons.Rounded.Alarm
            else -> Icons.Rounded.Groups
        }
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            if (index == 0) {
                FaFaBrandMark(size = 96)
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
