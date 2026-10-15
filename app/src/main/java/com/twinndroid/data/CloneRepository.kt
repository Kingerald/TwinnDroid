package com.twinndroid.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/** Clone definitions, persisted as a small JSON file in app-private storage. */
class CloneRepository private constructor(context: Context) {

    private val file = File(context.filesDir, "clones.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val _clones = MutableStateFlow(load())
    val clones: StateFlow<List<CloneConfig>> = _clones.asStateFlow()

    private fun load(): List<CloneConfig> = runCatching {
        if (file.exists()) json.decodeFromString<List<CloneConfig>>(file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    @Synchronized
    fun upsert(config: CloneConfig) {
        val list = _clones.value.toMutableList()
        val i = list.indexOfFirst { it.id == config.id }
        if (i >= 0) list[i] = config else list.add(config)
        persist(list)
    }

    @Synchronized
    fun remove(id: String) = persist(_clones.value.filterNot { it.id == id })

    fun get(id: String): CloneConfig? = _clones.value.firstOrNull { it.id == id }

    private fun persist(list: List<CloneConfig>) {
        val tmp = File(file.parentFile, "clones.json.tmp")
        tmp.writeText(json.encodeToString(list))
        tmp.renameTo(file)
        _clones.value = list
    }

    companion object {
        @Volatile private var instance: CloneRepository? = null
        fun get(context: Context): CloneRepository =
            instance ?: synchronized(this) {
                instance ?: CloneRepository(context.applicationContext).also { instance = it }
            }
    }
}
