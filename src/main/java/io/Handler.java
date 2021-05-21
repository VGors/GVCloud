package io;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable, Closeable {

    private final Socket socket;
    private final String CLOUD_STORAGE_LOCATION = "storage/";

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            while (true) {
                Message msg = (Message) ois.readObject();
                switch (msg.getCommand()) {
                    case SEND:
                        handleFileMessage(msg);
                        break;
                    case RECEIVE:
                        break;
                    case DELETE:
                        break;
                    case LIST_FILES:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Wrong message");
        }
    }

    private void handleFileMessage(Message msg) throws Exception {
        FileObject file = (FileObject) msg;
        Files.write(Paths.get(CLOUD_STORAGE_LOCATION + file.getName()), file.getData());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}