package su.android.core.repository

import su.android.core.Const
import su.android.core.Info
import su.android.core.data.SuLogDao
import su.android.core.ktx.await
import su.android.core.model.su.SuLog
import com.topjohnwu.superuser.Shell


class LogRepository(
    private val logDao: SuLogDao
) {

    suspend fun fetchSuLogs() = logDao.fetchAll()

    suspend fun fetchSuperSULogs(): String {
        val list = object : AbstractMutableList<String>() {
            val buf = StringBuilder()
            override val size get() = 0
            override fun get(index: Int): String = ""
            override fun removeAt(index: Int): String = ""
            override fun set(index: Int, element: String): String = ""
            override fun add(index: Int, element: String) {
                if (element.isNotEmpty()) {
                    buf.append(element)
                    buf.append('\n')
                }
            }
        }
        if (Info.env.isActive) {
            Shell.cmd("cat ${Const.SUPERSU_LOG} || logcat -d -s SuperSU").to(list).await()
        } else {
            Shell.cmd("logcat -d").to(list).await()
        }
        return list.buf.toString()
    }

    suspend fun clearLogs() = logDao.deleteAll()

    fun clearSuperSULogs(cb: (Shell.Result) -> Unit) =
        Shell.cmd("echo -n > ${Const.SUPERSU_LOG}").submit(cb)

    suspend fun insert(log: SuLog) = logDao.insert(log)

}
