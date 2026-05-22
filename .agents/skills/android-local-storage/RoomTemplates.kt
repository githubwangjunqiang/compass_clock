import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date

// ==================== Entity 定义 ====================

/**
 * 用户实体表
 */
@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val userId: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "avatar_url") val avatarUrl: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_vip") val isVip: Boolean = false
)

/**
 * 文章实体表（带外键关联）
 */
@Entity(
    tableName = "post_table",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("userId"),
            childColumns = arrayOf("author_id"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["author_id"])]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "author_id") val authorId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ==================== DAO 定义 ====================

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user_table WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM user_table WHERE is_vip = 1 ORDER BY created_at DESC LIMIT :count")
    suspend fun getVipUsers(count: Int): List<UserEntity>

    @Query("SELECT * FROM user_table WHERE username LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    fun searchUsers(keyword: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // 如果用户已存在，则忽略插入
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM user_table WHERE is_vip = 0")
    suspend fun deleteCommonUsers()

    @Query("UPDATE user_table SET is_vip = :isVip WHERE userId = :userId")
    suspend fun setVip(userId: String, isVip: Boolean)
}

/**
 * 文章数据访问对象
 */
@Dao
interface PostDao {
    @Query("SELECT * FROM post_table WHERE author_id = :userId ORDER BY created_at DESC")
    fun getUserPosts(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM post_table ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedPosts(limit: Int, offset: Int): List<PostEntity>

    @Query("SELECT COUNT(*) FROM post_table WHERE author_id = :userId")
    suspend fun getUserPostCount(userId: String): Int

    @Insert
    suspend fun insertPost(post: PostEntity): Long  // 返回插入的行 ID

    @Query("UPDATE post_table SET title = :title, content = :content, updated_at = :updatedAt WHERE id = :postId")
    suspend fun updatePost(postId: Int, title: String, content: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("DELETE FROM post_table WHERE author_id = :userId AND created_at < :before")
    suspend fun deleteOldPostsOfUser(userId: String, before: Long)
}

// ==================== Database 定义 ====================

/**
 * 类型转换器，将复杂类型转为数据库支持的简单类型存储
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
}

/**
 * 应用数据库
 */
@TypeConverters(Converters::class)
@Database(
    entities = [UserEntity::class, PostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // 版本升级时重建数据库
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// ==================== Repository 定义 ====================

/**
 * 数据仓库层
 * 封装数据库操作，统一管理数据访问
 */
class AppRepository(
    private val database: AppDatabase
) {
    // 用户相关操作
    suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        database.userDao().getUserById(userId)
    }

    fun searchUsers(keyword: String): Flow<List<UserEntity>> {
        return database.userDao().searchUsers(keyword).flowOn(Dispatchers.IO)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        database.userDao().insertUser(user)
    }

    suspend fun toggleUserVipStatus(userId: String, isVip: Boolean) = withContext(Dispatchers.IO) {
        database.userDao().setVip(userId, isVip)
    }

    // 文章相关操作
    fun getUserPosts(userId: String): Flow<List<PostEntity>> {
        return database.postDao().getUserPosts(userId).flowOn(Dispatchers.IO)
    }

    suspend fun getUserPostCount(userId: String): Int = withContext(Dispatchers.IO) {
        database.postDao().getUserPostCount(userId)
    }

    suspend fun insertPost(post: PostEntity) = withContext(Dispatchers.IO) {
        database.postDao().insertPost(post)
    }

    suspend fun updatePost(post: PostEntity) = withContext(Dispatchers.IO) {
        database.postDao().updatePost(post.id, post.title, post.content)
    }
}