import java.io.*;
import java.net.*;

// the main bullet board client (BBClient)
public final class BBClient {

    private Socket clientSocket = null;
    private BufferedReader serverReader = null;
    private PrintWriter serverWriter = null;
    private boolean connected = false;

    //constructor
    public static void main(String[] argv) {
        BBClient client = new BBClient();
        client.startClient();
    }

    // function to start the client, get the commands from the user and interact with the server
    public void startClient() {
        try {
            //this is a reader that will read what the user will input from the terminal
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            // just list the commands in the terminal so that the user can see what commands are valid and how to format them
            printCommands();

            //storing the user inputs here
            String input;

            // loop to handle any user inputs, when a valid user input is recieved, it will call the proper function
            while (true) {
                System.out.print("> ");
                input = userIn.readLine();
                if (input == null) continue;

                if (input.startsWith("%connect")) {
                    handleConnect(input);
                    continue;
                }

                if (input.equals("%exit")) {
                    if (connected) send("QUIT");
                    disconnect();
                    break;
                }

                if (!connected) {
                    System.out.println("ERROR: Not connected. Use %connect first.");
                    continue;
                }

                if (input.equals("%join")) {
                    handleLogin(userIn);
                }
                else if (input.startsWith("%post")) {
                    handlePost(input);
                }
                else if (input.startsWith("%message")) {
                    handleMessageRetrieve(input);
                }
                else if (input.equals("%leave")) {
                    send("LEAVE 1"); // Leaving default group #1 (assignment part 1)
                }

                
                //all of the commands for the groups part
                else if (input.equals("%groups")) {
                    send("GROUPS");
                }
                else if (input.startsWith("%groupjoin")) {
                    handleGroupJoin(input);
                }
                else if (input.startsWith("%grouppost")) {
                    handleGroupPost(input);
                }
                else if (input.startsWith("%groupusers")) {
                    handleGroupUsers(input);
                }
                else if (input.startsWith("%groupleave")) {
                    handleGroupLeave(input);
                }
                else if (input.startsWith("%groupmessage")) {
                    handleGroupMessage(input);
                }
                else {
                    System.out.println("Unknown command.");
                }
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

   
    // this is the printed bulletin board menu for the users to see the command options
    private void printCommands() {
        System.out.println("Bulletin Board Client");
        System.out.println("Commands:");
        System.out.println("  %connect <ip> <port>");
        System.out.println("  %join");
        System.out.println("  %post <subject> | <body>");
        System.out.println("  %message <id>");
        System.out.println("  %leave");
        System.out.println();
        System.out.println("Part 2:");
        System.out.println("  %groups");
        System.out.println("  %groupjoin <group_id>");
        System.out.println("  %groupusers <group_id>");
        System.out.println("  %grouppost <group_id> <subject> | <body>");
        System.out.println("  %groupleave <group_id>");
        System.out.println("  %groupmessage <id>");
        System.out.println();
        System.out.println("  %exit");
    }

    
    //function to connect to the client. this will be called when the user does the %connect <ip> <port> command.
    private void handleConnect(String line) {
        try {
            //spliting the input to get the ip and the port number
            String[] p = line.split("\\s+");

            //if the format from the user input is not correct, it will tell te correct usage
            if (p.length != 3) {
                System.out.println("Usage: %connect <ip> <port>");
                return;
            }

            //getting the IP address from the input and turning it into an integer
            String ip = p[1];
            int port = Integer.parseInt(p[2]);

            // creating the new client TCP socket using the ip address and port number that were inputted by the user
            clientSocket = new Socket(ip, port);

            //creating a server reader to read the data coming in from the server using clientSocket
            serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // creating a server writer to send data tot the server using clienSocjet.getOutputStream
            serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // once it is connected the connected status will become true
            connected = true;

            System.out.println("Connected to " + ip + ":" + port);

            //creating a thread to listen for any messages that come from the server and starting
            Thread t = new Thread(new ServerListener(serverReader, clientSocket));
            t.start();

        } catch (Exception e) {
            System.out.println("ERROR connecting: " + e.getMessage());
        }
    }

    //function to close the connection with the server, so it closes the server writer, server reader and client socket
    private void disconnect() {
        try {
            connected = false;
            if (serverWriter != null) serverWriter.close();
            if (serverReader != null) serverReader.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Disconnected.");
        } catch (IOException ignored) {}
    }

    //function to send the client text message string 
    private void send(String msg) {
        if (serverWriter != null) serverWriter.println(msg);
    }

    //part 1

    //function for the join command and ot asks the the user for the username
    private void handleLogin(BufferedReader userIn) {
        //making sure the input is in the correct format and how to extract it
        try {
            System.out.print("Enter username: ");
            String username = userIn.readLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return;
            }
            //sending the username informatio to the server
            send("LOGIN " + username);
        } catch (IOException e) {
            System.out.println("Error reading username.");
        }
    }

    //functions to handle posting a message to the default group
    private void handlePost(String cmd) {
        //making sure the input is in the correct format and how to extract it
        if (!cmd.contains("|")) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }
        String args = cmd.substring(5).trim();
        String[] parts = args.split("\\|", 2);
        if (parts.length != 2) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }
        String subject = parts[0].trim();
        String body = parts[1].trim();

        // posts the message to goup 1
        send("MESSAGE 1 " + subject + "|" + body);
    }

    private void handleMessageRetrieve(String cmd) {
        //making sure the input is in the correct format and how to extract it
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %message <id>");
            return;
        }
        //sending the request for the specific message id
        send("GET_MESSAGE " + p[1]);
    }

    //part 3

    //function to handle the user trying to join s epcficif group using the group id
    private void handleGroupJoin(String cmd) {
        //making sure the input is in the correct format and how to extract it
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupjoin <group_id>");
            return;
        }
        //seinding the join command and the group id to the server
        send("JOIN " + p[1]);
    }

    //function to handle when the user requests the list of users in a specfific group
    private void handleGroupUsers(String cmd) {
        //making sure the input is in the correct format and extracting it
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupusers <group_id>");
            return;
        }
        //sending the users group id to the server
        send("USERS " + p[1]);
    }

    //function to handle when the user wants to leave a sepcific roup
    private void handleGroupLeave(String cmd) {
        //making sure the input is in the correct format and extracting it
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupleave <group_id>");
            return;
        }
        //sending the leave group id to the server
        send("LEAVE " + p[1]);
    }

    //function to handle when the user posts a message to a specific group
    private void handleGroupPost(String cmd) {
        //making sure the input is in the correct format and extracting all of the infromation
        // like tbe group id, subject, and body message content
        if (!cmd.contains("|")) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        //remove the "%grouppost" part
        String remainder = cmd.substring(11).trim();

        //extracting the remaining info
        String[] p = remainder.split("\\s+", 2);
        if (p.length < 2) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        String groupId = p[0];
        String rest = p[1];

        //splitting the message into 2 parts
        String[] parts = rest.split("\\|", 2);
        if (parts.length != 2) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        //extracting the subject and the message body
        String subject = parts[0].trim();
        String body = parts[1].trim();

        //sending the all of the needed information for the post to the server
        send("MESSAGE " + groupId + " " + subject + "|" + body);
    }

    //function to handle when the user asks to see a specific mesage using the message id
    private void handleGroupMessage(String cmd) {
        //making sure the input is in the correct format and extracting the infromation
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupmessage <id>");
            return;
        }
        //sending the get message command and message id to the server
        send("GET_MESSAGE " + p[1]);
    }
}

