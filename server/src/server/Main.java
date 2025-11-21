package server;

public class Main {
    public static void main(String[] args) {
        try {
            // Attempt to read the first argument, which is expected to be a port number.
            // Default to 8000 if arg not present.
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;

            // Initialize a server object that will listen on that port.
            var server = new Server(port);

            // Starts the server.
            server.start();
        } catch (Exception e) {
            System.err.println("Error in main(): " + e.getMessage());
        }
    }
}