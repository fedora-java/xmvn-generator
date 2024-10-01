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
package org.fedoraproject.xmvn.generator.callback;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path javaHome = Path.of(System.getProperty("java.home"));
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
        return new Callback(Arrays.asList(
                javaCmd.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                CallbackEntry.class.getCanonicalName(),
                socketPath.toString()));
    }
}
