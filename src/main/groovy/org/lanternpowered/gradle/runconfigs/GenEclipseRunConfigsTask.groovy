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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.plugins.ide.eclipse.model.EclipseModel

class GenEclipseRunConfigsTask extends GenRunConfigsTaskBase {

    @Override
    protected List<RunConfiguration> getConfigs() {
        List<RunConfiguration> configurations = []
        configurations.addAll((project.extensions.getByName(RunConfigurationPlugin.EXTENSION_BASE_NAME)
                as NamedDomainObjectContainer).asMap.values())
        configurations.addAll((project.extensions.getByName(RunConfigurationPlugin.EXTENSION_ECLIPSE_NAME)
                as NamedDomainObjectContainer).asMap.values())
        return configurations
    }

    @Override
    protected void generateRunConfig(List<RunConfiguration> configs) {
        def eclipseModel = project.rootProject.getExtensions().getByName("eclipse") as EclipseModel

        configs.each {
            def node = new Node(null, 'launchConfiguration',
                    [type: 'org.eclipse.jdt.launching.localJavaApplication'])
            node.append(new Node(null, 'stringAttribute',
                    [key: 'org.eclipse.jdt.launching.MAIN_TYPE', value: it.mainClass]))
            node.append(new Node(null, 'stringAttribute',
                    [key: 'org.eclipse.jdt.launching.PROJECT_ATTR', value: eclipseModel.project.name]))
            def mappedResourcePathsNode = new Node(null, 'listAttribute',
                    [key: 'org.eclipse.debug.core.MAPPED_RESOURCE_PATHS'])
            mappedResourcePathsNode.append(new Node(null, 'listEntry',
                    [value: '/' + eclipseModel.project.name]))
            node.append(mappedResourcePathsNode)
            def workDirPath = getWorkDirPath(it.workingDirectory)
            // Try to relativize against the root project dir, if possible, otherwise use the absolute path
            def workDir
            try {
                workDir = workDirPath.relativize(project.rootProject.projectDir.toPath()).toFile().getPath()
                workDir = '${workspace_loc:' + eclipseModel.project.name + '}' + (workDir.isEmpty() ? '' : '/' + workDir)
            } catch (IllegalArgumentException ignored) {
                workDir = workDirPath.toFile().getPath()
            }
            node.append(new Node(null, 'stringAttribute',
                    [key: 'org.eclipse.jdt.launching.WORKING_DIRECTORY', value: workDir]))
            def arguments = it.programArguments
            if (arguments != null && !arguments.isEmpty()) {
                node.append(new Node(null, 'stringAttribute',
                        [key: 'org.eclipse.jdt.launching.PROGRAM_ARGUMENTS', value: arguments]))
            }
            def vmOptions = it.vmOptions
            if (vmOptions != null && !vmOptions.isEmpty()) {
                node.append(new Node(null, 'stringAttribute',
                        [key: 'org.eclipse.jdt.launching.VM_ARGUMENTS', value: vmOptions]))
            }
            def envVars = it.environmentVariables
            if (envVars != null && !envVars.isEmpty()) {
                def envVarsNode = new Node(null, 'mapAttribute',
                        [key: 'org.eclipse.debug.core.environmentVariables'])
                envVars.each { key, value ->
                    envVarsNode.append(new Node(null, 'mapEntry', [key: key, value: value]))
                }
                node.append(envVarsNode)
            }

            def os = new FileOutputStream(new File("${it.name.replace(' ', '_')}.launch"))
            try {
                def printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)))
                printWriter.append('<?xml version="1.0" encoding="UTF-8" standalone="no"?>')
                def printer = new XmlNodePrinter(printWriter)
                printer.print(node)
            } finally {
                os.flush()
                os.close()
            }
        }
    }
}
