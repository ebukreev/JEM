package org.jetbrains.research.jem.interaction

import kotlinx.serialization.json.Json
import java.io.FileReader
import kotlinx.serialization.*


object InfoReader {
    fun read(pathToJson: String): Package {
        FileReader(pathToJson).use {
            return Json.decodeFromString(it.readText())
        }
    }
}