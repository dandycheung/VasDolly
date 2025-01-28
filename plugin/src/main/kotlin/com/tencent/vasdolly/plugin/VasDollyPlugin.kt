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
package com.tencent.vasdolly.plugin

import com.android.build.api.variant.ApplicationVariant
import com.tencent.vasdolly.plugin.extension.BuildChannelApkConfig
import com.tencent.vasdolly.plugin.extension.RebuildChannelApkConfig
import com.tencent.vasdolly.plugin.extension.getChannelListFromFile
import com.tencent.vasdolly.plugin.extension.getChannelListFromString
import com.tencent.vasdolly.plugin.task.BuildChannelApkTask
import com.tencent.vasdolly.plugin.task.RebuildChannelApkTask
import com.tencent.vasdolly.plugin.util.AndroidComponentsExtensionCompat
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Locale

/***
 * VasDolly 插件
 * https://developer.android.com/studio/build/extend-agp
 */
class VasDollyPlugin : Plugin<Project> {
    companion object {
        const val PROPERTY_CHANNELS = "channels"
        const val PROPERTY_CHANNEL_FILE = "channel_file"
    }

    // 当前 project
    private lateinit var project: Project

    // 渠道配置
    private lateinit var buildConfig: BuildChannelApkConfig
    private lateinit var rebuildConfig: RebuildChannelApkConfig

    // 渠道列表
    private var channelList: List<String> = listOf()

    override fun apply(project: Project) {
        this.project = project

        // 检查是否为 android application。事实上，对于仅有 rebuildChannel 任务的情况，这不是必须的
        if (!project.plugins.hasPlugin("com.android.application"))
            throw GradleException("VasDolly: plugin 'com.android.application' must be applied")

        // 检查扩展配置（channel/rebuildChannel）
        buildConfig = project.extensions.create(
            "channel", BuildChannelApkConfig::class.java, project)
        rebuildConfig = project.extensions.create(
            "rebuildChannel", RebuildChannelApkConfig::class.java, project)

        // 获取全局工程中配置的渠道列表（gradle.properties 文件指定渠道属性）
        channelList = getChannelList()

        // 添加扩展渠道任务
        createTasks()
    }

    /***
     * 新建渠道任务
     */
    private fun createTasks() {
        val androidComponents =
            AndroidComponentsExtensionCompat.getAndroidComponentsExtension(project)
        androidComponents.onAllVariants { variant ->
            if (variant is ApplicationVariant) {
                val variantName = variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                    else it.toString()
                }
                println("VasDolly: build variant found: ${variant.name}")
                project.tasks.register("channel$variantName", BuildChannelApkTask::class.java) {
                    it.variant = variant
                    it.config = buildConfig
                    it.channelList.addAll(channelList)
                    it.mergeExtChannelList = !project.hasProperty(PROPERTY_CHANNELS)
                    it.dependsOn("assemble$variantName")
                }
            }
        }

        // 重新生成渠道包
        project.tasks.register("rebuildChannel", RebuildChannelApkTask::class.java) {
            it.mergeExtChannelList = !project.hasProperty(PROPERTY_CHANNELS)
            it.channelList.addAll(channelList)
            it.config = rebuildConfig
        }
    }

    /**
     * 获取 gradle.properties 中配置的渠道列表
     * 从 v2.0.0 开始支持添加渠道参数：
     *    gradle rebuildChannel -Pchannels=yingyongbao,gamecenter
     * 这里通过属性 channels 指定的渠道列表拥有更高的优先级，且和原始的文件方式 channel_file 是互斥的
     */
    private fun getChannelList(): List<String> {
        var channelList = listOf<String>()

        if (project.hasProperty(PROPERTY_CHANNELS)) { // 检查 channels 属性（优先，一般用于命令行测试用）
            val channels = project.properties[PROPERTY_CHANNELS] as String
            channelList = getChannelListFromString(channels)
            println("VasDolly: channels (from `channels` property): $channelList")
        } else if (project.hasProperty(PROPERTY_CHANNEL_FILE)) { // 检查 channel_file 属性
            val channelFilePath = project.properties[PROPERTY_CHANNEL_FILE] as String
            channelList = getChannelListFromFile(channelFilePath)
            println("VasDolly: channels (from file $channelFilePath that `channel_file` property specified): $channelList")
        }

        if (channelList.isEmpty())
            println("Warning: channel list is empty")

        return channelList
    }
}
