package su.android.ui.log

import android.system.Os
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import su.android.BR
import su.android.arch.AsyncLoadViewModel
import su.android.core.BuildConfig
import su.android.core.Info
import su.android.core.R
import su.android.core.ktx.timeFormatStandard
import su.android.core.ktx.toTime
import su.android.core.repository.LogRepository
import su.android.core.utils.MediaStoreUtils
import su.android.core.utils.MediaStoreUtils.outputStream
import su.android.databinding.bindExtra
import su.android.databinding.diffList
import su.android.databinding.set
import su.android.events.SnackbarEvent
import su.android.view.TextItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class LogViewModel(
    private val repo: LogRepository
) : AsyncLoadViewModel() {
    @get:Bindable
    var loading = true
        private set(value) = set(value, field, { field = it }, BR.loading)

    // --- empty view

    val itemEmpty = TextItem(R.string.log_data_none)
    val itemSuperSUEmpty = TextItem(R.string.log_data_supersu_none)

    // --- su log

    val items = diffList<SuLogRvItem>()
    val extraBindings = bindExtra {
        it.put(BR.viewModel, this)
    }

    // --- supersu log
    val logs = diffList<LogRvItem>()
    var supersuLogRaw = " "

    override suspend fun doLoadWork() {
        loading = true

        val (suLogs, suDiff) = withContext(Dispatchers.Default) {
            supersuLogRaw = repo.fetchSuperSULogs()
            val newLogs = supersuLogRaw.split('\n').map { LogRvItem(it) }
            logs.update(newLogs)
            val suLogs = repo.fetchSuLogs().map { SuLogRvItem(it) }
            suLogs to items.calculateDiff(suLogs)
        }

        items.firstOrNull()?.isTop = false
        items.lastOrNull()?.isBottom = false
        items.update(suLogs, suDiff)
        items.firstOrNull()?.isTop = true
        items.lastOrNull()?.isBottom = true
        loading = false
    }

    fun saveSuperSULog() = withExternalRW {
        viewModelScope.launch(Dispatchers.IO) {
            val filename = "supersu_log_%s.log".format(
                System.currentTimeMillis().toTime(timeFormatStandard))
            val logFile = MediaStoreUtils.getFile(filename)
            logFile.uri.outputStream().bufferedWriter().use { file ->
                file.write("---Detected Device Info---\n\n")
                file.write("isAB=${Info.isAB}\n")
                file.write("isSAR=${Info.isSAR}\n")
                file.write("ramdisk=${Info.ramdisk}\n")
                val uname = Os.uname()
                file.write("kernel=${uname.sysname} ${uname.machine} ${uname.release} ${uname.version}\n")

                file.write("\n\n---System Properties---\n\n")
                ProcessBuilder("getprop").start()
                    .inputStream.reader().use { it.copyTo(file) }

                file.write("\n\n---Environment Variables---\n\n")
                System.getenv().forEach { (key, value) -> file.write("${key}=${value}\n") }

                file.write("\n\n---System MountInfo---\n\n")
                FileInputStream("/proc/self/mountinfo").reader().use { it.copyTo(file) }

                file.write("\n---SuperSU Logs---\n")
                file.write("${Info.env.versionString} (${Info.env.versionCode})\n\n")
                if (Info.env.isActive) file.write(supersuLogRaw)

                file.write("\n---Manager Logs---\n")
                file.write("${BuildConfig.APP_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})\n\n")
                ProcessBuilder("logcat", "-d").start()
                    .inputStream.reader().use { it.copyTo(file) }
            }
            SnackbarEvent(logFile.toString()).publish()
        }
    }

    fun clearSuperSULog() = repo.clearSuperSULogs {
        SnackbarEvent(R.string.logs_cleared).publish()
        startLoading()
    }

    fun clearLog() = viewModelScope.launch {
        repo.clearLogs()
        SnackbarEvent(R.string.logs_cleared).publish()
        startLoading()
    }
}
