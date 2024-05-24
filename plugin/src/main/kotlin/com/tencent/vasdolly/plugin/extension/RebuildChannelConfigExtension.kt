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

open class RebuildChannelConfigExtension(project: Project) : ConfigExtension(project) {
    /**
     * base APK path
     */
    var baseApk: File? = null

    // 这是新增的另外一种形式的任务信息源，map 中的每一项，key 是 apk 文件的文件对象，value 是相应的渠道
    // 列表文件的文件对象。原则上，对于用户，直接指定路径会更简单，但是由于原始实现的 baseApk 属性就是文件
    // 对象，为了保持风格一致，顺便减少一些文件存在与否的校验判断，故遵循现有风格
    var baseMap: Map<File, File>? = null

    init {
        outputDir = File(project.buildDir, "rebuildChannel")
    }
}
