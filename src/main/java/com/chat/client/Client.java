package com.chat.client;

public class Client {
    private Socket server;
    private String name;

    private ArrayList<String> clients;

    private boolean open = false;

    private final BufferedReader keyboardStream = new BufferedReader(new InputStreamReader(System.in));

    private BufferedReader inputStream;
    private DataOutputStream outputStream;

    public void connect(String _serverName, int _serverPort) {
        try {
            open = true;
            server = new Socket(_serverName, _serverPort);
            inputStream = new BufferedReader(new InputStreamReader(server.getInputStream()));
            outputStream = new DataOutputStream(server.getOutputStream());

            new Thread(() -> { while (open) listen(); } ).start();

            while (open) {
                if (clients != null) { System.out.println("people: " + clients); }

                System.out.println("commands: \n@setname [name], \n@talk [target] [message], \n@shout [message], \n@exit");
                
                String[] string = keyboardStream.readLine().split(" ", 2);
                String command = string[0];
                String message = (string.length > 1)? string[1] : "";
                String[] arguments = message.split(" ", 2);
                
                if (command.contains("@")) {
                    switch (command) {
                        case "@setname" -> {
                            if (!message.equals("")) {
                                this.write( new Message(MessageType.CLIENT_SET_NAME, message) );
                            } else { System.out.println("command is incomplete"); }
                        } case "@talk" -> {
                            if (arguments.length >= 2) { this.write( new Message(
                                    MessageType.CLIENT_SEND_PRIVATE,
                                    arguments[1], this.name, arguments[0]) );
                            }  else { System.out.println("command is incomplete"); }
                        } case "@shout" -> { // message has to be message[1]+message[2]
                            if (arguments.length >= 1) { this.write(new Message(
                                    MessageType.CLIENT_SEND_PUBLIC,
                                    String.join(" ", arguments), this.name ) );
                            } else { System.out.println("command is incomplete"); }
                        } case "@exit" -> this.disconnect(true);
                        default -> System.out.println("unknown command");
                    }
                } else { System.out.println("command has to start with @"); }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void disconnect(boolean _warn) {
        try {
            open = false;
            if (_warn) this.write( new Message(MessageType.CLIENT_CLOSE) );
            server.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void listen() {
        Message message = this.read();

        switch ((message!=null)? message.getType() : MessageType.NULL_MESSAGE) {
            case SERVER_SEND_PRIVATE -> System.out.println(message.getSender() + " wrote " + message.getPayload());
            case SERVER_SEND_PUBLIC -> System.out.println(message.getSender() + " shouted " + message.getPayload());
            case SERVER_SEND_CLIENTS -> this.setClients(message);
            case SERVER_APPROVE_NAME -> name = message.getPayload();
            case SERVER_SEND_ERROR -> System.out.println("error: " + message.getPayload());
            case SERVER_CLOSE -> {
                System.out.println( message.getPayload() );
                this.disconnect(false);
            }
        }
    }

    public Message read() {
        try { return Utils.serializeJson(inputStream.readLine()); }
        catch (SocketException e) { System.out.println("socket is closed"); }
        catch (IOException e) { e.printStackTrace(); }
        return null;
    }

    public void write(Message _message) {
        try { outputStream.writeBytes(Utils.deserializeJson(_message) + '\n'); }
        catch (SocketException e) { System.out.println("socket was closed, you cannot communicate"); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void setClients(@NotNull Message message) {
        clients = new ArrayList<>(Arrays.asList( message.getPayload().split(",") ) );
    }

}
