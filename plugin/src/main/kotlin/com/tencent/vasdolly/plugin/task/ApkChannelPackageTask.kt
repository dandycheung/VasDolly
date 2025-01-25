package com.tencent.vasdolly.plugin.task

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.tencent.vasdolly.plugin.extension.ChannelConfigExtension
import com.tencent.vasdolly.plugin.util.SimpleAGPVersion
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class ApkChannelPackageTask : ChannelPackageTask() {
    @Internal
    var baseApk: File? = null // 当前基础 apk

    @get:Input
    var variant: ApplicationVariant? = null

    @get:Input
    var channelExtension: ChannelConfigExtension? = null

    @TaskAction
    fun taskAction() {
        // 1. check all params
        if (!sanityCheck())
            return

        // 2. generate channel apk
        generateChannelApk()
    }

    /***
     * check channel plugin parameters
     */
    private fun sanityCheck(): Boolean {
        if (mergeExtChannelList)
            mergeChannelList()

        // 1. check channel List
        if (channelList.isEmpty()) {
            println("Task $name: channel list is empty, please check it")
            return false
        }

        println("Task $name: channelList: $channelList")

        // 2. check base apk
        if (variant == null) {
            println("Task $name: variant is null")
            return false
        }

        baseApk = getVariantBaseApk()
        if (baseApk == null) {
            println("Task $name: can't find base apk")
            return false
        }

        println("Task $name: baseApk: ${baseApk?.absolutePath}")

        // 3. check ChannelExtension
        if (channelExtension == null) {
            println("Task $name: channel is null")
            return false
        }

        channelExtension?.prepare()
        println("Task $name: channel files outputDir: ${channelExtension?.outputDir?.absolutePath}")
        return true
    }

    @Suppress("PrivateApi")
    private fun getVariantBaseApk(): File? {
        return variant?.let { variant ->
            val currentAGPVersion = SimpleAGPVersion.ANDROID_GRADLE_PLUGIN_VERSION
            val agpVersion7 = SimpleAGPVersion(7, 0)
            val apkFolder = if (currentAGPVersion < agpVersion7) {
                // AGP4.2
                val artifactCls = Class.forName("com.android.build.api.artifact.ArtifactType")
                val apkClass = Class.forName("com.android.build.api.artifact.ArtifactType${'$'}APK").kotlin
                @Suppress("UNCHECKED_CAST")
                val provider = variant.artifacts.javaClass.getMethod("get", artifactCls)
                    .invoke(variant.artifacts, apkClass.objectInstance) as Provider<Directory>
                provider.get()
            } else {
                // AGP7.0
                variant.artifacts.get(SingleArtifact.APK).get()
            }
            variant.artifacts.getBuiltArtifactsLoader().load(apkFolder)?.let {
                File(it.elements.first().outputFile)
            }
        }
    }

    /***
     * 根据签名类型生成不同的渠道包
     */
    private fun generateChannelApk() {
        val outputDir = channelExtension?.outputDir
        println("generateChannelApk baseApk: ${baseApk?.absolutePath}, outputDir: ${outputDir?.path}")

        val signingConfig = variant?.signingConfig!!
        val lowMemory = channelExtension?.lowMemory ?: false
        val isFastMode = channelExtension?.fastMode ?: false

        when {
            signingConfig.enableV2Signing.get() -> {
                generateV2ChannelApk(baseApk!!, outputDir!!, lowMemory, isFastMode)
            }
            signingConfig.enableV1Signing.get() -> {
                generateV1ChannelApk(baseApk!!, outputDir!!, isFastMode)
            }
            else -> {
                println("not have precise channel package mode")
            }
        }
    }

    /***
     * 获取渠道文件名
     */
    override fun getChannelApkName(baseApkName: String, channel: String): String {
        var timeFormat = ChannelConfigExtension.DEFAULT_DATE_FORMAT
        if (channelExtension?.buildTimeDateFormat!!.isNotEmpty())
            timeFormat = channelExtension?.buildTimeDateFormat!!

        val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timeFormat))
        val outInfo = variant?.outputs?.first()

        val values: MutableMap<String, String> = mutableMapOf()
        values["appName"] = project.name
        values["flavorName"] = channel
        values["buildType"] = variant?.buildType ?: ""
        values["versionName"] = outInfo?.versionName?.get() ?: ""
        values["versionCode"] = outInfo?.versionCode?.get().toString()
        values["appId"] = variant?.applicationId?.get() ?: ""
        values["buildTime"] = buildTime

        // 默认文件名
        var apkName = ChannelConfigExtension.DEFAULT_APK_NAME_FORMAT
        if (channelExtension?.apkNameFormat!!.isNotEmpty())
            apkName = channelExtension?.apkNameFormat!!

        values.forEach { (k, v) ->
            apkName = apkName.replace("${'$'}{" + k + "}", v)
        }

        return "$apkName.apk"
    }

    /***
     * 获取渠道列表
     */
    override fun getExtensionChannelList(): List<String> {
        return channelExtension?.getExtensionChannelList() ?: listOf()
    }
}
