package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

class RpmLujavriteStub {
    public static String stubMain(String arg) {
        System.err.println("Hello from Java!");
        System.out.println("foo");
        BuildContext context = new RpmBuildContext();
        return "7 plus " + arg + " equals " + context.eval("%[7+" + arg + "]");
    }
}

public class RpmLujavriteTest {
    @Test
    public void testContextWithoutLibRpmIo() throws Exception {
        Path javaExecutable = Path.of(ProcessHandle.current().info().command().get());
        assertTrue(javaExecutable.endsWith(Path.of("bin/java")));
        Path javaHome = javaExecutable.getParent().getParent();
        Path libjvmPath = javaHome.resolve("lib/server/libjvm.so");
        String classPath = System.getProperty("java.class.path");
        StringBuilder macro = new StringBuilder();
        macro.append("%{lua:\n");
        macro.append("local lujavrite = require \"lujavrite\"\n");
        macro.append("local libjvm = \"").append(libjvmPath).append("\"\n");
        macro.append("local classpath = \"").append(classPath).append("\"\n");
        macro.append(
                "lujavrite.init(libjvm, \"-Djava.class.path=\" .. classpath, \"--enable-native-access=ALL-UNNAMED\")\n");
        macro.append(
                "print(lujavrite.call(\"org/fedoraproject/xmvn/generator/stub/RpmLujavriteStub\", \"stubMain\", \"(Ljava/lang/String;)Ljava/lang/String;\", \"35\"))\n");
        macro.append("}");
        ProcessBuilder pb = new ProcessBuilder("rpm", "-E", macro.toString());
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        assertEquals(0, p.waitFor());
        String out;
        try (InputStream is = p.getInputStream()) {
            out = new String(is.readAllBytes());
        }
        assertEquals("foo\n7 plus 35 equals 42\n", out);
    }
}
