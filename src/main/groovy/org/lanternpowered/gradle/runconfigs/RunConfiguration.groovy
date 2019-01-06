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

import groovy.transform.ToString
import org.gradle.api.Nullable
import org.gradle.api.tasks.SourceSet
import org.lanternpowered.gradle.LanternGradle

import java.nio.file.Path
import java.nio.file.Paths

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class RunConfiguration {

    String name

    Object mainClass

    @Nullable
    Object vmOptions

    @Nullable
    Object programArguments

    @Nullable
    Object workingDirectory

    @Nullable
    Object targetSourceSet

    Map<String, String> environmentVariables = new HashMap<>()

    RunConfiguration(String name) {
        this.name = name
    }

    Map<String, String> getEnvironmentVariables() {
        return this.environmentVariables
    }

    String getName() {
        return this.name
    }

    String getMainClass() {
        return LanternGradle.resolve(this.mainClass, String.class)
    }

    @Nullable
    String getVmOptions() {
        return LanternGradle.resolve(this.vmOptions, String.class)
    }

    @Nullable
    String getProgramArguments() {
        return LanternGradle.resolve(this.programArguments, String.class)
    }

    @Nullable
    Path getWorkingDirectory() {
        def workDir = LanternGradle.resolve(this.workingDirectory, Object.class)
        if (workDir instanceof Path) {
            return workDir
        } else if (workDir instanceof File) {
            return workDir.toPath()
        } else {
            workDir = workDir as String
            if (workDir == null) {
                workDir = ""
            }
            return Paths.get(workDir)
        }
    }

    @Nullable
    SourceSet getTargetSourceSet() {
        return LanternGradle.resolve(this.targetSourceSet, SourceSet.class)
    }
}
