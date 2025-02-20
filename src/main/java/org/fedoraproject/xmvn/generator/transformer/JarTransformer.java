/*-
 * Copyright (c) 2023-2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.xmvn.generator.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.Manifest;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.fedoraproject.xmvn.generator.logging.Logger;

class JarTransformer {
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    private final ManifestTransformer manifestTransformer;

    public JarTransformer(ManifestTransformer manifestTransformer) {
        this.manifestTransformer = manifestTransformer;
    }

    public void transformJar(Path targetJar) throws IOException {
        try (ZipFile jar = ZipFile.builder().setPath(targetJar).get()) {
            if (jar.getEntry(MANIFEST_PATH) == null) {
                Logger.debug("transformation skipped: no pre-existing manifest found to update");
                return;
            }
        } catch (IOException e) {
            return;
        }
        Path backupPath =
                targetJar.getParent().resolve(targetJar.getFileName().toString() + "-backup");
        try {
            Files.copy(targetJar, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to inject manifest: I/O error when creating backup file: " + backupPath,
                    e);
        }
        try (ZipFile jar = ZipFile.builder().setPath(backupPath).get();
                ZipArchiveOutputStream os = new ZipArchiveOutputStream(targetJar.toFile())) {
            try (InputStream mfIs = jar.getInputStream(jar.getEntry(MANIFEST_PATH))) {
                Manifest manifest = new Manifest(mfIs);
                manifestTransformer.transform(manifest);
                // write manifest
                ZipArchiveEntry newManifestEntry = new ZipArchiveEntry(MANIFEST_PATH);
                os.putArchiveEntry(newManifestEntry);
                manifest.write(os);
                os.closeArchiveEntry();
            }
            // copy the rest of content
            jar.copyRawEntries(os, entry -> !entry.equals(jar.getEntry(MANIFEST_PATH)));
        } catch (Exception e) {
            // Re-throw exceptions that occur when processing JAR file after reading header and
            // manifest.
            throw new RuntimeException(
                    "Failed to inject manifest; backup file is available at " + backupPath, e);
        }
        try {
            Files.delete(backupPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete backup file " + backupPath, e);
        }
    }
}
