---
name: android-local-storage
description: 本地存储规范。MMKV 键值存储、Room 数据库。持久化数据时使用。
---

# Android 本地存储规范

本文件包含存储方案的依赖配置、使用示例和选型建议。**完整实现代码见附属文件**。

---

## 一、MMKV（键值存储）

腾讯开源的高性能键值存储，基于 mmap 实现，适合轻量级数据。

### 1.1 依赖配置

在 `gradle/libs.versions.toml` 中定义：
```toml
[versions]
mmkv = "1.3.4"

[libraries]
mmkv = { group = "com.tencent", name = "mmkv", version.ref = "mmkv" }
```

在 `app/build.gradle.kts` 中添加：
```kotlin
dependencies {
    implementation(libs.mmkv)
}
```

### 1.2 初始化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKVUtil.initContext(this)  // 使用封装的初始化函数
    }
}
```

### 1.3 使用示例

```kotlin
// 基础类型存储
MMKVUtil.put("token", "your_auth_token")
MMKVUtil.put("user_id", 123L)
MMKVUtil.put("is_first_launch", true)

// 读取数据
val token = MMKVUtil.getString("token")
val userId = MMKVUtil.getLong("user_id")
val isFirstLaunch = MMKVUtil.getBoolean("is_first_launch", defaultValue = true)

// 对象存储（自动 JSON 序列化）
val user = UserData(id = "1001", name = "小强", age = 25)
MMKVUtil.put("current_user", user)
val savedUser = MMKVUtil.getObject("current_user", UserData::class.java)

// 业务隔离存储
val userMMKV = MMKVUtil.mmkvWithID("user_data")
MMKVUtil.put("last_login_time", System.currentTimeMillis(), userMMKV)
```

### 1.4 适用场景

- 登录凭证、Token 存储
- 用户偏好设置
- 临时缓存数据
- 简单对象持久化

> **完整实现代码见 [MMKVUtil.kt](MMKVUtil.kt)**

---

## 二、Room 数据库

Google 官方推荐的 SQLite ORM 库，适合结构化数据。

### 2.1 依赖配置

在 `gradle/libs.versions.toml` 中定义：
```toml
[versions]
room = "2.6.1"
ksp = "2.0.21-1.0.28"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

在 `app/build.gradle.kts` 中添加：
```kotlin
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)  // 协程支持
    ksp(libs.room.compiler)       // 注解处理器
}
```

### 2.2 Entity 定义示例

```kotlin
@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val userId: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "is_vip") val isVip: Boolean = false
)
```

### 2.3 DAO 定义示例

```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM user_table WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
```

### 2.4 适用场景

- 结构化数据存储
- 复杂查询（JOIN、聚合）
- 数据一致性要求高
- 需要事务支持
- 离线数据同步

> **完整实现（Entity/DAO/Database/Repository）见 [RoomTemplates.kt](RoomTemplates.kt)**

---

## 三、存储选型对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| MMKV | 快速高效、内存少 | 不支持复杂查询 | 键值存储、对象快速存取 |
| Room | SQL 完整、类型安全 | 学习成本较高 | 结构化数据、复杂查询 |

**选择建议**：
- 轻量级数据（Token、配置） → MMKV
- 结构化数据（用户、文章） → Room
- 复杂查询需求 → Room