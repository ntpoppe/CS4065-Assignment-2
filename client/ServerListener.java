//importing the buffered reader to read tet from server and socket for the tcp connection to the server
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

//class for the server listener and it will be able to run on a separate thread
public class ServerListener implements Runnable {
    //buffered reader for getting the text that is sent by the server
    private final BufferedReader in;

    //socket for the tcp connection to the server
    private final Socket socket;

    //constructor to initialize the reader and seocket  
    public ServerListener(BufferedReader in, Socket socket) {
        this.in = in;
        this.socket = socket;
    }

    //run function runs when the server listener thread will start
    @Override
    public void run() {
        try {
            //this will store all of the lines that are sent by the server
            String line;

            //keep reading the lines until the server closes the stream
            while ((line = in.readLine()) != null) {
                //printing the message from the server so it can be seen
                System.out.println("\n[SERVER] " + line);
                System.out.print("> ");
            }
        } catch (IOException e) {
            //closing the socket
            System.out.println("Server connection closed.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

