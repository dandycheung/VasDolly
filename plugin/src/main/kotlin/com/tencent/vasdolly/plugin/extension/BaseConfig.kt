/*
 * Tencent is pleased to support the open source community by making VasDolly available.
 *
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.vasdolly.plugin.extension

import org.gradle.api.Project
import java.io.File

open class BaseConfig(project: Project) {
    // 低内存模式（仅针对 V2 签名，默认为 false）：只把签名块、中央目录和 EOCD 读
    // 取到内存，不把最大头的内容块读取到内存。 在手机上合成 APK 时，可以使用该模式
    var lowMemory = false

    // 是否为快速模式，即不验证渠道名
    var fastMode = false

    // 渠道列表文件
    var channelFile: File? = null

    // 渠道包保持目录
    var outputDir: File = File(project.buildDir, "channel")

    /**
     * 从扩展属性中获取 channelFile 配置的扩展渠道列表
     */
    fun getChannelList(): List<String> {
        val channelList = getChannelListFromFile(channelFile)
        println("get channels from `channelFile`, channels: $channelList")
        return channelList
    }

    fun getChannelList(channelFile: File?): List<String> {
        val channelList = getChannelListFromFile(channelFile)
        println("get channels from mapped `channelFile`, channels: $channelList")
        return channelList
    }
}

fun getChannelListFromString(channels: String): List<String> {
    val channelList = mutableListOf<String>()

    if (channels.isNotEmpty())
        channelList.addAll(channels.split(","))

    return channelList.distinct() // 去重
}

fun getChannelListFromFile(channelFile: File?): List<String> {
    val channelList = mutableListOf<String>()

    if (channelFile != null && channelFile.exists() && channelFile.isFile) {
        channelFile.forEachLine { channel ->
            if (channel.isNotEmpty())
                channelList.add(channel)
        }
    }

    return channelList.distinct() // 去重
}

fun getChannelListFromFile(filePath: String): List<String> {
    return if (filePath.isEmpty()) listOf() else getChannelListFromFile(File(filePath))
}
