package nio;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class NIOServer {
    private final ByteBuffer COMMANDS_BUFFER;
    private final ServerSocketChannel SOCKET_CHANNEL;
    private final int SERVER_PORT;
    private Selector selector;
    private String servDir;
    private SocketChannel channel;
    private StringBuilder commandBuilder;

    @SneakyThrows
    public NIOServer() {
        servDir = "servdir";
        SERVER_PORT = 8189;
        COMMANDS_BUFFER = ByteBuffer.allocate(64);
        SOCKET_CHANNEL = ServerSocketChannel.open();
        SOCKET_CHANNEL.bind(new InetSocketAddress(SERVER_PORT));
        SOCKET_CHANNEL.configureBlocking(false);
        selector = Selector.open();
        SOCKET_CHANNEL.register(selector, SelectionKey.OP_ACCEPT);
        while (SOCKET_CHANNEL.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                keyIterator.remove();
            }
        }
    }

    @SneakyThrows
    private void handleRead(SelectionKey key) {
        channel = (SocketChannel) key.channel();
        commandBuilder = new StringBuilder();
        int r;
        while (true) {
            r = channel.read(COMMANDS_BUFFER);
            if (r == -1) {
                channel.close();
                return;
            }
            if (r == 0) {
                break;
            }
            COMMANDS_BUFFER.flip();
            while (COMMANDS_BUFFER.hasRemaining()) {
                commandBuilder.append((char) COMMANDS_BUFFER.get());
            }
            COMMANDS_BUFFER.clear();
        }
        commandsHandler(commandBuilder);
    }

    private void commandsHandler(StringBuilder commandBuilder) throws IOException {
        String command = commandBuilder.toString().trim().toLowerCase(Locale.ROOT);
        String[] parseString = command.split("\\s+", 3);
        try {
            switch (Commands.valueOf(parseString[0])) {
                case ls:
                    lsHandler();
                    break;
                case cat:
                    if (parseString.length == 2) {
                        catHandler(channel, parseString[1]);
                    } else {
                        channel.write(ByteBuffer.wrap("Wrong file name\n\r".getBytes(StandardCharsets.UTF_8)));
                    }
                    break;
                case mkdir:
                    if (parseString.length == 2) {
                        mkdirHandler(parseString[1]);
                    } else {
                        channel.write(ByteBuffer.wrap("Wrong parameter\n\r".getBytes(StandardCharsets.UTF_8)));
                    }
                    break;
                case touch:
                    if (parseString.length == 2) {
                        touchHandler(parseString[1]);
                    } else {
                        channel.write(ByteBuffer.wrap("Wrong parameter\n\r".getBytes(StandardCharsets.UTF_8)));
                    }
                    break;
                case write:
                    if (parseString.length >= 3) {
                        writeHandler(parseString[1], parseString[2]);
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            channel.write(ByteBuffer.wrap(("\r").getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void writeHandler(String fileName, String message) throws IOException {
        Path path = Paths.get(servDir, fileName);
        try (RandomAccessFile accessFile = new RandomAccessFile(String.valueOf(path), "rw");
             FileChannel fileChannel = accessFile.getChannel()) {
            fileChannel.position(fileChannel.size());
            fileChannel.write(ByteBuffer.wrap(("\n" + message).getBytes(StandardCharsets.UTF_8)));
        } catch (FileNotFoundException e) {
            channel.write(ByteBuffer.wrap("File doesn't exists...\n\r".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void touchHandler(String s) throws IOException {
        Path path = Paths.get(servDir, s);
        File file = new File(String.valueOf(path));
        if (!file.exists() && !file.isDirectory()) {
            Files.createFile(path);
            channel.write(ByteBuffer.wrap("Successful...\n\r".getBytes(StandardCharsets.UTF_8)));

        } else {
            channel.write(ByteBuffer.wrap("File already exists...\n\r".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void mkdirHandler(String s) throws IOException {
        Path path = Paths.get(servDir, s);
        Files.createDirectory(path);
        channel.write(ByteBuffer.wrap("Successful...\n\r".getBytes(StandardCharsets.UTF_8)));
    }

    private void lsHandler() {
        try (Stream<Path> paths = Files.walk(Paths.get(servDir))) {
            paths
//                    .filter(Files::isDirectory)
                    .forEach(fileName -> {
                        try {
                            channel.write(ByteBuffer.wrap(fileName.toString().getBytes(StandardCharsets.UTF_8)));
                            channel.write(ByteBuffer.wrap("\n\r".getBytes(StandardCharsets.UTF_8)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void catHandler(SocketChannel channel, String s) throws IOException {
        try {
            Path path = Paths.get(servDir, s);
            byte[] bytes = Files.readAllBytes(path);
            channel.write(ByteBuffer.wrap(bytes));
            channel.write(ByteBuffer.wrap("\n\r".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            channel.write(ByteBuffer.wrap("Wrong file name\n\r".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @SneakyThrows
    private void handleAccept(SelectionKey key) {
        channel = SOCKET_CHANNEL.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap("Hello there!\n\r".getBytes(StandardCharsets.UTF_8)));
    }
}
