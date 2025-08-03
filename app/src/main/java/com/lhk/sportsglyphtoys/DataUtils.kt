package com.lhk.sportsglyphtoys

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DataUtils {
    fun fetchAndFlattenJson(url: String, onResult: (Map<String, String>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val flattened = flattenJson(json)

                withContext(Dispatchers.Main) {
                    onResult(flattened)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(emptyMap())  // gracefully degrade
                }
            }
        }
    }

    fun flattenJson (
        jsonObject: JSONObject,
        prefix: String = "",
        result: MutableMap<String, String> = mutableMapOf()
    ): Map<String, String> {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            val newKey = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is JSONObject -> flattenJson(value, newKey, result)
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val element = value.get(i)
                        val arrayKey = "$newKey[$i]"
                        when (element) {
                            is JSONObject -> {
                                flattenJson(element, arrayKey, result)
                            }

                            is Number -> {
                                result[arrayKey] = roundNumber(element)
                            }

                            else -> {
                                result[arrayKey] = element.toString()
                            }
                        }
                    }
                }
                is Number -> result[newKey] = roundNumber(value)
                else -> result[newKey] = value.toString()
            }
        }
        return result
    }

    private fun roundNumber(number: Number): String {
        return if (number is Float || number is Double) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }
}