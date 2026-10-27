package com.jwd.fafa.core.data

import com.jwd.fafa.core.network.AuthApi
import com.jwd.fafa.core.network.BootstrapDto
import com.jwd.fafa.core.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** bootstrap 聚合状态：登录/注册页在 Ready 前禁用提交（协议没加载不得默认视为同意）。 */
sealed interface BootState {
    data object Loading : BootState
    data class Ready(val info: BootstrapInfo) : BootState
    data class Failed(val message: String, val retryable: Boolean) : BootState
}

data class BootstrapInfo(
    val legalVersions: Map<String, String>,
    val featureFlags: BootstrapDto,
    val serverTime: String?,
)

@Singleton
class BootstrapRepository @Inject constructor(
    private val api: AuthApi,
    private val json: Json,
) {
    private val _state = MutableStateFlow<BootState>(BootState.Loading)
    val state: StateFlow<BootState> = _state.asStateFlow()

    /** 最近一次成功结果缓存（供各页面读取协议版本）。 */
    @Volatile
    var cached: BootstrapInfo? = null
        private set

    suspend fun refresh() {
        _state.value = BootState.Loading
        _state.value = try {
            val data = apiCall(json) { api.bootstrap() }.data
                ?: throw com.jwd.fafa.core.network.ApiError.Unexpected("启动数据为空")
            val info = BootstrapInfo(
                legalVersions = data.legalVersions,
                featureFlags = data,
                serverTime = data.serverTime,
            )
            cached = info
            BootState.Ready(info)
        } catch (e: com.jwd.fafa.core.network.ApiError) {
            BootState.Failed(e.message ?: "服务暂时不可用", e.retryable)
        }
    }
}
