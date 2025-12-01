import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class ServerListener implements Runnable {
    private final BufferedReader in;
    private final Socket socket;

    public ServerListener(BufferedReader in, Socket socket) {
        this.in = in;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("\n[SERVER] " + line);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("Server connection closed.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

