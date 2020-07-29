package org.jetbrains.research.jem

import com.google.gson.Gson
import java.io.FileReader

object InfoReader {
    fun read(pathToJson: String): Library {
        var jsonString = ""
        FileReader(pathToJson).use { jsonString = it.readText() }
        return Gson().fromJson(jsonString, Library::class.java)
    }
}