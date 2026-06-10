package app.luzzy.helpers

import android.content.Context
import com.goodwy.commons.helpers.ensureBackgroundThread
import app.luzzy.databases.MessagesDatabase
import app.luzzy.models.ContactSendMode
import app.luzzy.models.SendMode
import app.luzzy.utils.SharedPrefsManager
import java.util.concurrent.ConcurrentHashMap

class ContactSendModeRepository(private val context: Context) {

    private val dao = MessagesDatabase.getInstance(context).ContactSendModeDao()
    private val cache = ConcurrentHashMap<Long, SendMode>()

    init {
        loadCache()
    }

    private fun loadCache() {
        ensureBackgroundThread {
            val allModes = dao.getAll()
            cache.clear()
            allModes.forEach { cache[it.threadId] = it.sendMode }
        }
    }

    fun getSendMode(threadId: Long): SendMode {
        return cache[threadId] ?: SendMode.AUTO
    }

    fun getResolvedSendMode(threadId: Long, context: Context): SendMode {
        val mode = getSendMode(threadId)
        return if (mode == SendMode.AUTO) {
            if (SharedPrefsManager.isGlobalDraftModeEnabled(context)) SendMode.DRAFT else SendMode.SEND
        } else {
            mode
        }
    }

    fun setSendMode(threadId: Long, sendMode: SendMode) {
        cache[threadId] = sendMode
        ensureBackgroundThread {
            dao.insertOrUpdate(ContactSendMode(threadId, sendMode))
        }
    }

    fun toggleSendMode(threadId: Long): SendMode {
        val current = getSendMode(threadId)
        val newMode = if (current == SendMode.SEND) SendMode.DRAFT else SendMode.SEND
        setSendMode(threadId, newMode)
        return newMode
    }

    fun resetToSend(threadId: Long) {
        cache.remove(threadId)
        ensureBackgroundThread {
            dao.delete(threadId)
        }
    }

    fun refreshCache() {
        loadCache()
    }
}
