package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JarTransformerTest {
    @TempDir
    private Path workDir;
    private Path testResource;
    private Path testJar;
    private Path backupPath;
    private JarTransformer jarTransformer = new JarTransformer(
            mf -> mf.getMainAttributes().putValue("X-Key", "X-Value"));

    @BeforeEach
    public void setUp() throws Exception {
        prepare("example.jar");
    }

    private void prepare(String testResourceName) throws Exception {
        testResource = Paths.get("src/test/resources").resolve(testResourceName);
        assertTrue(Files.isRegularFile(testResource));
        testJar = workDir.resolve("test.jar");
        Files.copy(testResource, testJar, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        backupPath = Paths.get(testJar + "-backup");
    }

    private void performTest() throws Exception {
        jarTransformer.transformJar(testJar);
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(testJar))) {
            Manifest mf = jis.getManifest();
            assertNotNull(mf);
            Attributes attr = mf.getMainAttributes();
            assertNotNull(attr);
            assertEquals("X-Value", attr.getValue("X-Key"));
        }
    }

    /**
     * Test JAR if manifest injection works as expected.
     * 
     * @throws Exception
     */
    @Test
    public void testManifestInjection() throws Exception {
        performTest();
    }

    /**
     * Test JAR if manifest injection works when MANIFEST.MF file appears later in
     * the file (for example produced by adding manifest to existing jar with plain
     * zip)
     *
     * @throws Exception
     */
    @Test
    public void testManifestInjectionLateManifest() throws Exception {
        prepare("late-manifest.jar");
        performTest();
    }

    /**
     * Regression test for a jar which contains an entry that can recompress with a
     * different size, which caused a mismatch in sizes.
     * 
     * @throws Exception
     */
    @Test
    public void testManifestInjectionRecompressionCausesSizeMismatch() throws Exception {
        prepare("recompression-size.jar");
        performTest();
    }

    /**
     * Test JAR if manifest injection works when MANIFEST.MF entry is duplicated
     *
     * @throws Exception
     */
    @Test
    public void testManifestInjectionDuplicateManifest() throws Exception {
        prepare("duplicate-manifest.jar");
        performTest();
    }

    /**
     * Test JAR if manifest injection preserves sane file perms.
     *
     * @throws Exception
     */
    @Test
    public void testManifestInjectionSanePermissions() throws Exception {
        assumeTrue(Files.getPosixFilePermissions(testJar).contains(PosixFilePermission.OTHERS_READ), "sane umask");
        performTest();
        assertTrue(Files.getPosixFilePermissions(testJar).contains(PosixFilePermission.OTHERS_READ));
    }

    /**
     * Test if any of utility functions throws exception when trying to access
     * invalid JAR file.
     * 
     * @throws Exception
     */
    @Test
    public void testInvalidJar() throws Exception {
        prepare("invalid.jar");
        jarTransformer.transformJar(testJar);
        byte[] testJarContent = Files.readAllBytes(testJar);
        byte[] testResourceContent = Files.readAllBytes(testResource);
        assertTrue(Arrays.equals(testJarContent, testResourceContent));
    }

    /**
     * Test that the manifest file retains the same i-node after being injected into
     * 
     * @throws Exception
     */
    @Test
    public void testSameINode() throws Exception {
        long oldInode = (Long) Files.getAttribute(testJar, "unix:ino");
        performTest();
        long newInode = (Long) Files.getAttribute(testJar, "unix:ino");
        assertEquals(oldInode, newInode, "Different manifest I-node after injection");
    }

    /**
     * Test that the backup file created during injectManifest was deleted after a
     * successful operation
     * 
     * @throws Exception
     */
    @Test
    public void testBackupDeletion() throws Exception {
        performTest();
        assertFalse(Files.exists(backupPath));
    }

    /**
     * Test that the backup file created during injectManifest remains after an
     * unsuccessful operation and its content is identical to the original file
     * 
     * @throws Exception
     */
    @Test
    public void testBackupOnFailure() throws Exception {
        byte[] content = Files.readAllBytes(testJar);
        jarTransformer = new JarTransformer(mf -> {
            throw new RuntimeException("boom");
        });
        Exception ex = assertThrows(Exception.class, () -> performTest());
        assertTrue(ex.getMessage().contains(backupPath.toString()),
                "An exception thrown when injecting manifest does not mention stored backup file");
        assertTrue(Files.exists(backupPath));
        byte[] backupContent = Files.readAllBytes(backupPath);
        assertArrayEquals(content, backupContent,
                "Content of the backup file is different from the content of the original file");
        Files.copy(testResource, testJar, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        try (FileOutputStream os = new FileOutputStream(testJar.toFile(), true)) {
            /// Append garbage to the original file to check if the content of the backup
            /// will be retained
            os.write(0);
        }
        assertThrows(Exception.class, () -> performTest());
        assertArrayEquals(backupContent, Files.readAllBytes(backupPath),
                "Backup file content was overwritten after an unsuccessful injection");
        Files.delete(backupPath);
    }

    /**
     * Test that injectManifest fails if the backup file already exists
     * 
     * @throws Exception
     */
    @Test
    public void testFailWhenBachupPresent() throws Exception {
        Files.writeString(backupPath, "something");
        assertThrows(Exception.class, () -> performTest(),
                "Expected failure because the the backup file already exists");
        assertTrue(Files.exists(backupPath));
    }
}
