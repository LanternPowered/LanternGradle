/*
 * This file is part of LanternGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, andor sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.gradle.runconfigs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path

abstract class GenRunConfigsTaskBase extends DefaultTask {

    @TaskAction
    void doTask() {
        def configs = this.configs
        configs.each {
            if (it.workingDirectory != null) {
                def path = getWorkDirPath(it.workingDirectory)
                if (!Files.exists(path)) {
                    Files.createDirectories(path)
                }
            }
        }
        generateRunConfig(configs)
    }

    protected abstract void generateRunConfig(List<RunConfiguration> configs)

    protected abstract List<RunConfiguration> getConfigs()

    protected Path getWorkDirPath(Path workDirPath) {
        if (workDirPath.absolute) {
            return workDirPath
        }
        // Resolve the path relative to the project one
        return project.projectDir.toPath().resolve(workDirPath)
    }
}
