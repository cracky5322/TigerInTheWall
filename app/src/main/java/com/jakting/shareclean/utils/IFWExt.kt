package com.jakting.shareclean.utils

import android.content.Context
import android.content.Intent
import com.jakting.shareclean.utils.application.Companion.kv
import com.jakting.shareclean.utils.application.Companion.settingSharedPreferences

const val ifw_send_file_path = "/data/system/ifw/TigerInTheWall_Intent_send.xml"
const val ifw_send_multi_file_path = "/data/system/ifw/TigerInTheWall_Intent_send_multi.xml"
const val ifw_view_file_path = "/data/system/ifw/TigerInTheWall_Intent_view.xml"
const val ifw_text_file_path = "/data/system/ifw/TigerInTheWall_Intent_text.xml"
const val ifw_browser_file_path = "/data/system/ifw/TigerInTheWall_Intent_browser.xml"
const val ifw_direct_share_file_path = "/data/system/ifw/TigerInTheWall_Intent_direct_share.xml"

val intentTypeList = arrayListOf("1_share", "2_share_multi", "3_view", "4_text", "5_browser")
fun getIFWContent(tag: String, intentString: String): String {
    return "   <activity block=\"true\" log=\"true\">\n" +
            "    <intent-filter>\n" +
            "      <action name=\"${getIFWAction(tag)}\" />\n" +
            "      <cat name=\"android.intent.category.DEFAULT\" />\n" +
            isBrowser(tag) +
            "    </intent-filter>\n" +
            "    <component equals=\"$intentString\" />\n" +
            "    <or>\n" +
            "      <sender type=\"system\" />\n" +
            "      <not>\n" +
            "        <sender type=\"userId\" />\n" +
            "      </not>\n" +
            "    </or>\n" +
            "  </activity>\n"
}

fun isBrowser(tag: String): String {
    return if (tag == "browser") {
        "      <cat name=\"android.intent.category.BROWSABLE\" />\n" +
                "      <scheme name=\"http\" />\n" +
                "      <scheme name=\"https\" />\n"
    } else
        "      <type name=\"*/*\" />\n"
}

const val ifw_direct_share =
    "<rules>\n" +
            "  <service block=\"true\" log=\"true\">\n" +
            "    <intent-filter>\n" +
            "      <action name=\"android.service.chooser.ChooserTargetService\" />\n" +
            "    </intent-filter>\n" +
            "  </service>\n" +
            "</rules>\n"

fun getIFWPath(tag: String): String {
    return when (tag) {
        "1_share" -> ifw_send_file_path
        "2_share_multi" -> ifw_send_multi_file_path
        "3_view" -> ifw_view_file_path
        "4_text" -> ifw_text_file_path
        "5_browser" -> ifw_browser_file_path
        else -> ""
    }
}

fun getIFWAction(tag: String): String {
    return when (tag) {
        "1_share" -> Intent.ACTION_SEND
        "2_share_multi" -> Intent.ACTION_SEND_MULTIPLE
        "3_view", "5_browser" -> Intent.ACTION_VIEW
        "4_text" -> Intent.ACTION_PROCESS_TEXT
        else -> ""
    }
}

fun generateIfwFileContent(intentContentList: MutableList<String>): String {
    var ifwContent = "<rules>\n"
    intentContentList.forEach {
        ifwContent += it
    }
    ifwContent += "</rules>"
    return ifwContent
}

fun Context?.writeIfwFiles(): Boolean {
    val intentTypeMap = HashMap<String, MutableList<String>>()
    intentTypeMap["1_share"] = mutableListOf()
    intentTypeMap["2_share_multi"] = mutableListOf()
    intentTypeMap["3_view"] = mutableListOf()
    intentTypeMap["4_text"] = mutableListOf()
    intentTypeMap["5_browser"] = mutableListOf()
    kv.allKeys()?.forEach { itKey ->
        if (kv.decodeBool(itKey) == settingSharedPreferences.getBoolean("pref_blacklist", true)) {
            intentTypeList.forEach { itType ->
                if (itKey.startsWith(itType)) {
                    intentTypeMap[itType]?.add(getIFWContent(itType, itKey.replace("$itType/", "")))
                }
            }
        }
    }
    var result = true
    intentTypeList.forEach { itType ->
        if (runShell("touch ${getIFWPath(itType)}").isSuccess &&
            runShell(
                "echo '${generateIfwFileContent(intentTypeMap[itType]!!)}' > ${
                    getIFWPath(
                        itType
                    )
                }"
            ).isSuccess
        ) {
            logd("写入${getIFWPath(itType)}成功")
        } else {
            result = false
        }
    }
    return result
}


fun deleteIfwFiles(type: String): Boolean {
    return if (type == "all") {
        runShell("find /data/system/ifw/ -name \"TigerInTheWall_Intent*.xml\" -exec rm -rf {} \\; ").isSuccess
    } else {
        runShell("rm -f ${getIFWPath(ifw_send_file_path)}").isSuccess
    }
}