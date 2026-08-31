package com.looker.kenko.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PlanTransferCodec {

    private const val CURRENT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(plans: List<PlanTransfer>): String =
        json.encodeToString(PlanTransferFile(version = CURRENT_VERSION, plans = plans))

    fun decode(jsonString: String): List<PlanTransfer> {
        val file = json.decodeFromString<PlanTransferFile>(jsonString)
        require(file.version == CURRENT_VERSION) { "Unsupported plan file version: ${file.version}" }
        return file.plans
    }
}
