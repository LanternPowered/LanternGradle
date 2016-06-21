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

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Paths

class RunConfigurationPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "runConfigurations"

    @Override
    void apply(Project project) {
        project.with {
            plugins.apply('java')
            plugins.apply('eclipse')
            plugins.apply('idea')

            def extension = extensions.create(EXTENSION_NAME, RunConfigurationExtension, project, project.name)
            def createRunDirTask = task('createRunDirTask') {
                extension.configs.collect().each {
                    def config = it as RunConfiguration;
                    if (config.workingDirectory != null) {
                        def path = Paths.get(config.workingDirectory as String);
                        if (!Files.exists(path)) {
                            Files.createDirectories(path);
                        }
                    }
                }
            }

            tasks.eclipse.dependsOn createRunDirTask
            tasks.idea.dependsOn createRunDirTask

            // Generate application setups
            tasks.ideaWorkspace.workspace.iws.withXml { provider ->
                // Get the run manager node, this will contain the configuration
                def node = provider.asNode().component.find { it.@name == 'RunManager' }
                // Find the default app node
                def defaultNode = node.find { it.@type == 'Application' && it.@default == 'true' }

                // It should always be present
                if (defaultNode == null) {
                    throw new IllegalStateException('Unable to find the default application config node,' +
                            ' please try to regenerate the intellij project files to resolve this issue.')
                }

                extension.configs.collect().each {
                    def config = it as RunConfiguration;
                    // Find the custom app node
                    def customNode = node.find { it.@type == 'Application' && it.@name == config.name }

                    // Only create a new configuration if the old one is missing
                    if (customNode == null) {
                        customNode = defaultNode.clone()
                        customNode.@default = 'false'
                        customNode.@name = config.name
                        customNode.each {
                            if (it.name() == 'option' && it.@name != null) {
                                switch (it.@name) {
                                    case 'MAIN_CLASS_NAME':
                                        it.@value = config.mainClass
                                        break
                                    case 'VM_PARAMETERS':
                                        if (config.vmOptions != null) {
                                            it.@value = config.vmOptions
                                        }
                                        break
                                    case 'PROGRAM_PARAMETERS':
                                        if (config.programArguments != null) {
                                            it.@value = config.programArguments
                                        }
                                        break
                                    case 'WORKING_DIRECTORY':
                                        if (config.workingDirectory != null) {
                                            String dir = config.workingDirectory as String;
                                            if (Paths.get(dir).absolute) {
                                                it.@value = dir;
                                            } else {
                                                it.@value = 'file://$PROJECT_DIR$/' + dir
                                            }
                                        }
                                        break
                                }
                            } else if (it.name() == 'module') {
                                it.@name = project.name
                            } else if (it.name() == 'envs') {
                                def envsNode = it as Node
                                config.environmentVariables.each { key, value ->
                                    envsNode.append(new Node(envsNode, 'env', [name: key, value: value]))
                                }
                            }
                        }
                        node.append customNode
                    }
                }
            }

            // TODO: Eclipse
        }
    }
}
