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
package org.lanternpowered.gradle.transformer

import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.codehaus.plexus.util.IOUtil
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.util.PatternSet
import org.lanternpowered.gradle.util.asm.ClassDependencyCollector

/**
 * This transformer scans for all the dependencies of the classes specified
 * in the filesToScan {@link PatternSet}. And if those dependencies or the classes
 * themselves are present in the dependencyFiles {@link PatternSet}, then they
 * will be added to the final jar.
 */
class DependencyClassTransformer implements Transformer {

    /**
     * All the files that should be checked for dependencies. If they are also
     * applicable to dependencyPaths, they will also be included in the final
     * jar.
     */
    PatternSet filesToScan = new PatternSet()

    /**
     * The paths of file/packages which may be added as dependencies
     * by the included files.
     */
    PatternSet dependencyFiles = new PatternSet()

    private final Set<String> includableFiles = new HashSet<>()
    private final Set<String> foundFilesToScan = new HashSet<>()
    private final Map<String, File> processedFiles = new HashMap<>()

    @Override
    boolean canTransformResource(FileTreeElement element) {
        boolean includable = this.dependencyFiles.asSpec.isSatisfiedBy(element)
        String path = element.relativePath.pathString
        if (includable) {
            this.includableFiles.add(path)
        }
        if (this.filesToScan.asSpec.isSatisfiedBy(element)) {
            this.foundFilesToScan.add(path)
            return true
        }
        return includable
    }

    @Override
    void transform(String path, InputStream is, List<Relocator> relocators) {
        File tempFile = File.createTempFile(path, '.class')
        OutputStream fos = new FileOutputStream(tempFile)
        try {
            IOUtil.copy(is, fos)
        } finally {
            fos.close()
            is.close()
        }
        this.processedFiles.put(path, tempFile)
    }

    @Override
    boolean hasTransformedResource() {
        return true
    }

    @Override
    void modifyOutputStream(ZipOutputStream os) {
        try {
            Set<String> processedPaths = new HashSet<>()
            for (String path : this.foundFilesToScan) {
                this.modifyOutputStream0(processedPaths, os, path)
            }
        } finally {
            this.processedFiles.each { it.getValue().delete() }
        }
    }

    private void modifyOutputStream0(Set<String> processedPaths, ZipOutputStream os, String path) {
        if (!processedPaths.add(path)) {
            return;
        }
        File file = this.processedFiles.get(path);
        if (file == null) {
            return;
        }
        InputStream is = new FileInputStream(file)
        try {
            if (this.includableFiles.contains(path)) {
                os.putNextEntry(new ZipEntry(path))
                try {
                    IOUtil.copy(is, os)
                } finally {
                    is.close()
                }
                is = new FileInputStream(file)
            }

            ClassDependencyCollector.collect(is).each {
                this.modifyOutputStream0(processedPaths, os, it)
            }
        } finally {
            is.close()
        }
    }
}
