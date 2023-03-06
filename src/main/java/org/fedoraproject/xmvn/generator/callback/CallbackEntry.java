package org.fedoraproject.xmvn.generator.callback;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallbackEntry {
    public static void main(String[] args) throws Exception {
        Path socketPath = Paths.get(args[0]);
        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(socketAddress);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            channel.read(buffer);
        }
    }
}
