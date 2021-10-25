package no.ntnu.datakomm;

/**
 * A class used to test whether the server can handle multiple TCP client simultaneously (a multi-threaded server)
 */
public class MultiClientTest {

    /**
     * Run multiple parallel clients that all connect to the same server
     *
     * @param args Command-line arguments. Not used.
     */
    public static void main(String args[]) {
        log("Starting several clients to test servers multi-threading capability");
        startNewClient();
        startNewClient();
        startNewClient();
        log("Multi client app main thread done");
    }

    /**
     * Start a new client in a new thread.
     */
    private static void startNewClient() {
        final SimpleTcpClient client = new SimpleTcpClient();
        Runnable taskToBeExecutedOnAnotherThread = new Runnable() {
            public void run() {
                try {
                    long threadId = Thread.currentThread().getId();
                    log("Starting a client on thread #" + threadId);
                    client.run();
                    log("Done processing client on thread #" + threadId);
                } catch (InterruptedException e) {
                    log("Client 1 interrupted");
                    Thread.currentThread().interrupt();
                }
            }
        };
        Thread t = new Thread(taskToBeExecutedOnAnotherThread);
        t.start();
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        String threadId = "THREAD #" + Thread.currentThread().getId() + ": ";
        System.out.println(threadId + message);
    }
}
