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

import org.gradle.api.GradleException
import org.gradle.api.Project

open class ChannelConfigExtension(project: Project) : ConfigExtension(project) {
    companion object {
        // 默认文件名模板
        const val DEFAULT_APK_NAME_FORMAT =
            "${'$'}{appName}-${'$'}{versionName}-${'$'}{versionCode}-${'$'}{flavorName}-${'$'}{buildType}-${'$'}{buildTime}"

        // 默认时间格式
        const val DEFAULT_DATE_FORMAT = "yyyyMMdd-HHmmss"
    }

    /**
     * 渠道包的命名格式
     */
    var apkNameFormat = DEFAULT_APK_NAME_FORMAT

    /**
     * buildTime 的时间格式
     */
    var buildTimeDateFormat = DEFAULT_DATE_FORMAT

    /**
     * 为生成渠道包做准备工作
     */
    fun prepare() {
        if (!outputDir.exists())
            outputDir.mkdirs()

        if (!outputDir.isDirectory)
            throw GradleException("channel config outputDir: ${outputDir.absolutePath} isn't directory")

        // 清理旧的 apk 文件
        outputDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk"))
                file.delete()
        }
    }
}
