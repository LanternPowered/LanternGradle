/*
 * This file is part of LanternGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://github.com/LanternPowered>
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
import org.gradle.api.Plugin
import org.gradle.api.Project

class RunConfigurationPlugin implements Plugin<Project> {

    static final String EXTENSION_BASE_NAME = 'runConfigurations'
    static final String EXTENSION_IDEA_NAME = 'ideaRunConfigurations'
    static final String EXTENSION_ECLIPSE_NAME = 'eclipseRunConfigurations'

    @Override
    void apply(Project project) {
        project.with {
            def factory = { name -> return new RunConfiguration(name) }
            extensions.add(EXTENSION_BASE_NAME, project.container(RunConfiguration, factory))
            extensions.add(EXTENSION_IDEA_NAME, project.container(RunConfiguration, factory))
            extensions.add(EXTENSION_ECLIPSE_NAME, project.container(RunConfiguration, factory))

            def mainTask = project.tasks.create('genRunConfigurations', DefaultTask.class) {
                group = 'Lantern'
                description = 'Generates run configurations for IntelliJ and Eclipse.'
            }

            if (project.plugins.hasPlugin('idea')) {
                def ideaTask = project.tasks.create('genIdeaRunConfigurations', GenIntelliJRunConfigsTask.class) {
                    group = 'Lantern'
                    description = 'Generates run configurations for IntelliJ.'
                }
                mainTask.dependsOn ideaTask
            }
            if (project.plugins.hasPlugin('eclipse')) {
                def eclipseTask = project.tasks.create('genEclipseRunConfigurations', GenEclipseRunConfigsTask.class) {
                    group = 'Lantern'
                    description = 'Generates run configurations for Eclipse.'
                }
                mainTask.dependsOn eclipseTask
            }
        }
    }
}
