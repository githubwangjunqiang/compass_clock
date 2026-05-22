/**
 * Repository 数据仓库模板
 * 负责统一管理数据的获取、缓存、持久化
 * ViewModel 不直接调用网络层，通过 Repository 获取数据
 *
 * 依赖的基础设施（需从其他 skill 引入）：
 * - HttpCallPool → 项目自定义网络层封装（参见 android-project-overview）
 * - MMKVUtil → MMKV 存储封装（参见 android-local-storage）
 * - BaseViewModel → ViewModel 基类（参见 android-mvi-compose）
 * - launchTryViewModelScope → ViewModel 协程安全启动（参见 android-mvi-compose）
 * - showLoadingView/showContent/showErrorLayout → UI 状态管理（参见 android-mvi-compose）
 */

package com.xxx.app.feature.user

import com.xxx.app.base.http.HttpCallPool
import com.xxx.app.base.storage.mmkv.MMKVUtil
import com.xxx.app.model.response.UserInfoResponse
import com.xxx.app.model.response.UserListResponse
import com.xxx.app.model.entity.UserInfoData
import org.json.JSONObject

/**
 * 用户信息数据仓库
 * 负责统一管理用户数据的获取、缓存、持久化
 * ViewModel 不直接调用网络层，通过 Repository 获取数据
 */
class UserRepository {

    /**
     * 获取用户信息
     * 策略：优先网络，失败降级 MMKV 缓存
     * @param userId 用户ID
     * @return 用户信息，失败返回 null
     */
    suspend fun getUserInfo(userId: String): Result<UserInfoData> {
        return try {
            val response = HttpCallPool.user_info_url.getCall(
                UserInfoResponse::class.java,
                json = JSONObject().apply { put("user_id", userId) }
            )
            if (response.success && response.resultData?.data != null) {
                val data = response.resultData!!.data!!
                // 成功后缓存到 MMKV
                MMKVUtil.put("user_$userId", data)
                Result.success(data)
            } else {
                // 络络失败，尝试读取本地缓存
                val cached = MMKVUtil.getObject("user_$userId", UserInfoData::class.java)
                if (cached != null) Result.success(cached)
                else Result.failure(Exception(response.getErrorMsg()))
            }
        } catch (e: Exception) {
            val cached = MMKVUtil.getObject("user_$userId", UserInfoData::class.java)
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    /**
     * 获取用户列表（分页）
     */
    suspend fun getUserList(pageNo: Int, pageSize: Int): Result<List<UserInfoData>> {
        return try {
            val response = HttpCallPool.user_list_url.getCall(
                UserListResponse::class.java,
                json = JSONObject().apply {
                    put("pageNo", pageNo)
                    put("pageSize", pageSize)
                }
            )
            if (response.success) {
                Result.success(response.resultData?.data?.list.orEmpty())
            } else {
                Result.failure(Exception(response.getErrorMsg()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ==================== ViewModel 中使用示例 ====================

/**
 * 使用 Repository 的 ViewModel 示例
 */
class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun loadUserInfo(userId: String) {
        launchTryViewModelScope {
            showLoadingView()
            repository.getUserInfo(userId)
                .onSuccess { data ->
                    showContent()
                    _uiState.update { it.copy(userInfo = data) }
                }
                .onFailure { e ->
                    e.message?.show()
                    showErrorLayout(e.message)
                }
        }
    }
}