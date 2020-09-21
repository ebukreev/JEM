package org.jetbrains.research.jem.interaction

import com.google.gson.Gson
import java.io.FileReader

object InfoReader {
    fun read(pathToJson: String): Library {
        FileReader(pathToJson).use {
            return Gson().fromJson(it.readText(), Library::class.java)
        }
    }
}