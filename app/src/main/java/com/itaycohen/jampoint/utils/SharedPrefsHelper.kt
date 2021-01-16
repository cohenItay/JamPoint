package com.itaycohen.jampoint.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPrefsHelper private constructor(
    private val context: Context,
    val gson: Gson,
    private val fileName: String
) {

    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    /**
     * Get the value from Shared preferences file.
     * @param key to find the value
     * @param defaultValue to return if no such key is found
     * @param typeToken if the required output is encapsulating a generic, for example Collection<String>
     * @return The value from file, otherwise the default value
     */
    inline fun <reified T> getValue(key: String, defaultValue: T, typeToken: TypeToken<*>? = null) : T =
        getValueNullable(key, defaultValue, typeToken)!!

    /**
     * Get the value from Shared preferences file.
     * @param key to find the value
     * @param defaultValue to return if no such key is found
     * @param typeToken if the required output is encapsulating a generic, for example Collection<String>
     * @return The value from file, otherwise the default value which might be nullable
     */
    inline fun <reified T> getValueNullable(key: String, defaultValue: T? = null, typeToken: TypeToken<*>? = null) : T?{
        if (key.isEmpty())
            return defaultValue

        return try {
            when (defaultValue) {
                is String -> sharedPreferences.getString(key, defaultValue) as T
                is Int -> sharedPreferences.getInt(key, defaultValue) as T
                is Long -> sharedPreferences.getLong(key, defaultValue) as T
                is Float -> sharedPreferences.getFloat(key, defaultValue) as T
                is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
                else -> {
                    val asStr = sharedPreferences.getString(key, "-1ic*pp")
                    if (asStr == "-1ic*pp") {
                        defaultValue
                    } else {
                        try {
                            gson.fromJson<T>(asStr, typeToken?.type ?: T::class.java)
                        } catch (e: Exception) {
                            defaultValue
                        }
                    }
                }
            }
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    @SuppressLint("ApplySharedPref")
    fun <T> saveValue(key: String, value:T? , commitSync: Boolean = false) {
        val editor = sharedPreferences.edit()
        when (value) {
            is String -> editor.putString(key, value as String)
            is Int -> editor.putInt(key, value as Int)
            is Long -> editor.putLong(key, value as Long)
            is Float -> editor.putFloat(key, value as Float)
            is Boolean -> editor.putBoolean(key, value as Boolean)
            else -> editor.putString(key, gson.toJson(value) as String)
        }
        if (commitSync) editor.commit() else editor.apply()
    }

    fun contains(key: String) = sharedPreferences.contains(key)

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }



    class Factory constructor(
        private val context: Context,
        val gson: Gson
    ){
        fun create(fileName: String) : SharedPrefsHelper {
            return SharedPrefsHelper(context = context, gson = gson, fileName = fileName)
        }
    }
}