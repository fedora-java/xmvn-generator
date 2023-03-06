package org.fedoraproject.xmvn.generator.callback;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Callback {
    private final List<String> command;

    public Callback(List<String> command) {
        this.command = command;
    }

    public List<String> getCommand() {
        return command;
    }

    public static Callback setUp(Runnable delegate) throws IOException {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path javaCmd = javaHome.resolve("bin").resolve("java");
        Path tempDir = Files.createTempDirectory("xmvngen-");
        Path socketPath = tempDir.resolve("socket");
        Semaphore semaphore = new Semaphore(0);
        Thread thread = new Thread(() -> {
            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);
            try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
                serverChannel.bind(socketAddress);
                semaphore.release();
                try (SocketChannel channel = serverChannel.accept()) {
                    delegate.run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        });
        thread.setDaemon(true);
        thread.start();
        semaphore.acquireUninterruptibly();
        return new Callback(Arrays.asList(javaCmd.toString(), "-cp", System.getProperty("java.class.path"),
                CallbackEntry.class.getCanonicalName(), socketPath.toString()));
    }
}
