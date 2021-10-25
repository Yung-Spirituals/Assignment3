package no.ntnu.datakomm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * A Simple TCP server, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer {
    private static final int PORT =1234;

    public static void main(String[] args) {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    /**
     * Runs the server
     */
    public void run() {
        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            boolean mustRun = true;

            while(mustRun) {
                Socket clientSocket = welcomeSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e){
            System.out.println("Error: " + e.getMessage());
        }
    }


    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        System.out.println(message);
    }
}