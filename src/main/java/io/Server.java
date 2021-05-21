package io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
class Server {
    private static ServerSocket serverSocket;

    static {
        try {
            serverSocket = new ServerSocket(8088);
            log.debug("Server has started.");
        } catch (IOException e) {
            log.error("Start error: ", e);
        }
    }

    public static void main(String[] args) {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                log.debug("Client accepted.");
                Handler handler = new Handler(socket);
                new Thread(handler).start();
            } catch (IOException e) {
                log.error("Connection have done!");
            }
        }
    }
}
