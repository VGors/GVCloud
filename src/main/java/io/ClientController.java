package io;

import javafx.fxml.Initializable;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
public class ClientController implements Initializable {
    public TreeView<String> serverList;
    public AnchorPane mainWindow;
    private static final String SERVER_ADDRESS;
    private static final int SERVER_PORT;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Socket socket;

    static {
        SERVER_ADDRESS = "localhost";
        SERVER_PORT = 8088;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            log.error("Connection error!");
        }
    }

    public void dragDropped(DragEvent dragEvent) throws IOException {
        List<File> files = dragEvent.getDragboard().getFiles();
        File file = files.get(0);
        if (file.isFile()) {
            FileObject sendingFile = new FileObject(Paths.get(file.getAbsolutePath()));
            oos.writeObject(sendingFile);
            oos.flush();
        } else {
            System.out.println("It's not a file");
        }
    }

    public void dragOver(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.ANY);
        }
    }
}
