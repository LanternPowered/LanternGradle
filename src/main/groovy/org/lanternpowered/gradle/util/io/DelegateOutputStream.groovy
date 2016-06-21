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
package org.lanternpowered.gradle.util.io

import static java.util.Objects.requireNonNull
/**
 * A output stream that delegates all it's method
 * to the output stream passed in the constructor.
 */
public class DelegateOutputStream extends OutputStream {

    private final OutputStream outputStream;

    public DelegateOutputStream(OutputStream outputStream) {
        this.outputStream = requireNonNull(outputStream, "outputStream");
    }

    @Override
    public void write(int b) throws IOException {
        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }
}
