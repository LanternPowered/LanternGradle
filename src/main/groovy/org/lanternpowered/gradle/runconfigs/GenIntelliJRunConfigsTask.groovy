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

import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.plugins.ide.idea.model.IdeaModel

import java.nio.file.Paths

class GenIntelliJRunConfigsTask extends GenIDERunConfigsTaskBase {

    /**
     * Whether the task should be executed.
     */
    private boolean shouldExecute;

    GenIntelliJRunConfigsTask() {
        super("Idea")
        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            if (graph.hasTask('idea') || graph.hasTask('ideaWorkspace')) {
                this.shouldExecute = false;

                // We will apply the project files while the idea project generation
                // Generate application setups
                project.tasks.ideaWorkspace.workspace.iws.withXml { provider ->
                    this.applyTo(provider.asNode())
                }
            }
        }
    }

    @Override
    void doTask0() {
        if (!this.shouldExecute) {
            return;
        }

        def ideaModel = project.getExtensions().getByName("idea") as IdeaModel
        def file = ideaModel.project.outputFile

        // Search the workspace file, we cannot retrieve it from the extension,
        // try some options first
        // Try project file name first
        def workspaceFile = new File(file.getParent(), file.getName().replaceAll("\\.ipr\$", "") + ".iws")

        // Then module file name
        if (!workspaceFile.exists()) {
            file = ideaModel.module.outputFile
            workspaceFile = new File(file.getParent(), file.getName().replaceAll("\\.iml\$", "") + ".iws")
        }

        // Search for .iws file
        if (!workspaceFile.exists()) {
            final File[] files = new File().listFiles { it.getName().endsWith(".iws")}
            if (files.length == 0) {
                throw new IllegalStateException("The idea project files must be generated before this task can be run.")
            }
            workspaceFile = files[0]
        }

        def xmlParser = new XmlParser()
        def node = xmlParser.parse(workspaceFile)

        this.applyTo(node)

        def os = new FileOutputStream(workspaceFile)
        try {
            def printer = new XmlNodePrinter(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))))
            printer.print(node)
        } finally {
            os.flush()
            os.close()
        }
    }

    private void applyTo(Node node) {
        // Get the run manager node, this will contain the configuration
        node = node.component.find { it.@name == 'RunManager' }
        // Find the default app node
        def defaultNode = node.find { it.@type == 'Application' && it.@default == 'true' }

        // It should always be present
        if (defaultNode == null) {
            throw new IllegalStateException('Unable to find the default application config node,' +
                    ' please try to regenerate the intellij project files to resolve this issue.')
        }

        configs.collect().each {
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
                                        it.@value = 'file://' + dir;
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
}
