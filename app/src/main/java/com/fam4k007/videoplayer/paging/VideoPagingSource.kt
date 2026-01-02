package com.fam4k007.videoplayer.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fam4k007.videoplayer.VideoFileParcelable
import com.fam4k007.videoplayer.database.VideoCacheDao
import com.fam4k007.videoplayer.database.VideoCacheEntity
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 视频列表分页数据源
 * 从Room数据库分页加载视频列表，防止大量视频导致OOM
 */
class VideoPagingSource(
    private val dao: VideoCacheDao,
    private val folderPath: String
) : PagingSource<Int, VideoFileParcelable>() {

    companion object {
        private const val TAG = "VideoPagingSource"
        const val PAGE_SIZE = 30  // 每页30个视频
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoFileParcelable> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val offset = page * PAGE_SIZE

            Logger.d(TAG, "Loading page $page, pageSize=$pageSize, offset=$offset, folder=$folderPath")

            // 从数据库分页查询（在IO线程执行）
            val entities = withContext(Dispatchers.IO) {
                dao.getVideosByFolderPaged(
                    folderPath = folderPath,
                    limit = pageSize,
                    offset = offset
                )
            }

            // 转换为VideoFileParcelable
            val videos = entities.map { entity ->
                VideoFileParcelable(
                    uri = entity.uri,
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    duration = entity.duration,
                    dateAdded = entity.dateAdded
                )
            }

            Logger.d(TAG, "Loaded ${videos.size} videos for page $page")

            LoadResult.Page(
                data = videos,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (videos.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading page", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VideoFileParcelable>): Int? {
        // 计算最接近当前位置的页码
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
