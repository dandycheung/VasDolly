package com.tencent.vasdolly.plugin.task

import com.tencent.vasdolly.plugin.extension.RebuildChannelApkConfig
import com.tencent.vasdolly.reader.ChannelReader
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

/***
 * 根据已有基础包重新生成多渠道包
 */
open class RebuildChannelApkTask : BaseTask() {
    @get:Input
    var config: RebuildChannelApkConfig? = null

    @TaskAction
    fun taskAction() {
        if (config == null) {
            println("Task $name: config is missing, please check it")
            return
        }

        // 如果用户把 baseApk 和 baseMap 都配置了，就都处理；平滑兼容原有功能
        processBaseApk()
        processBaseApks()
    }

    private fun processBaseApk() {
        processChannelList()

        // 1. check channel List
        if (channelList.isEmpty()) {
            println("Task $name: channel list is empty, please check it")
            return
        }

        println("Task $name: channelList: $channelList")

        // 2. generate channel apk
        generateChannelApkSingle(config?.baseApk, config?.outputDir)
    }

    private fun processBaseApks() {
        if (config?.baseMap == null)
            return

        val baseMap: Map<File, File> = config?.baseMap!!
        baseMap.forEach { (apk, channels) ->
            val channelList = config?.getChannelList(channels) ?: listOf()
            generateChannelApk(apk, config?.outputDir, channelList)
        }
    }

    /***
     * 生成渠道包
     */
    private fun generateChannelApkSingle(baseApk: File?, outputDir: File?) {
        generateChannelApk(baseApk, outputDir, channelList)
    }

    private fun generateChannelApk(baseApk: File?, outputDir: File?, channelList: List<String>) {
        println("generateChannelApk baseApk: ${baseApk?.absolutePath}, outputDir: ${outputDir?.path}")

        val lowMemory = config?.lowMemory ?: false
        val isFastMode = config?.fastMode ?: false

        // 校验 baseApk
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile) {
            println("baseApk ($baseApk) is not a valid file, so can not rebuild channel apk")
            return
        }

        // 检查是否有输出目录
        if (outputDir == null) {
            println("rebuild apk channel outputDir is empty")
            return
        }

        prepare(outputDir)

        // 开始生成渠道包
        if (ChannelReader.containV2Signature(baseApk))
            generateV2ChannelApk(baseApk, outputDir, lowMemory, isFastMode, channelList)
        else if (ChannelReader.containV1Signature(baseApk))
            generateV1ChannelApk(baseApk, outputDir, isFastMode, channelList)
    }

    /**
     * 获取 Apk 文件名
     */
    override fun getChannelApkName(baseApkName: String, channel: String): String {
        return if (baseApkName.contains("base"))
            baseApkName.replace("base", channel)
        else
            "$channel-$baseApkName"
    }

    /***
     * 获取渠道列表
     */
    override fun getExtensionChannelList(): List<String> {
        return config?.getChannelList() ?: listOf()
    }
}
