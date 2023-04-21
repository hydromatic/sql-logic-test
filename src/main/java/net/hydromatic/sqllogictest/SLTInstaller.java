/*
 * Copyright 2022 VMware, Inc.
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */

package net.hydromatic.sqllogictest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class downloads and unzips the SQL Logic Test files.
 */
public class SLTInstaller {
    /**
     * This URL is of a git repository that mirrors the SLT code.
     * The actual code is in a Fossil repository and has a captcha for anonymous downloading.
     */
    static final String SLT_GIT = "https://github.com/gregrahn/sqllogictest/archive/refs/heads/master.zip";

    private final File destination;

    SLTInstaller(File destination) {
        this.destination = destination;
    }

    @Nullable
    static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        name = name.replace("sqllogictest-master/", "");
        if (name.isEmpty())
            return null;
        File destFile = new File(destinationDir, name);
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + name);
        }
        return destFile;
    }

    public void install() throws IOException {
        File zip = File.createTempFile("out", ".zip", new File("."));
        System.out.println("Downloading SLT from " + SLT_GIT + " into " + zip.getAbsolutePath());
        zip.deleteOnExit();
        InputStream in = new URL(SLT_GIT).openStream();
        Files.copy(in, zip.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Unzipping data");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(this.destination, zipEntry);
                if (newFile != null) {
                    System.out.println("Creating " + newFile.getPath());
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            byte[] buffer = new byte[1024];
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }
}
