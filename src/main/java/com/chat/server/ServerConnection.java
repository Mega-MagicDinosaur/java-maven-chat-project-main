package com.chat.server;

import com.chat.utils.Utils;
import com.chat.utils.message.ErrorType;
import com.chat.utils.message.Message;
import com.chat.utils.message.MessageType;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class ServerConnection {
    private final int id;
    private String name;

    private final Socket socket;

    private boolean open = false;
    private boolean muted = false;

    private ConcurrentSkipListSet<ServerConnection> clients;

    private BufferedReader inputStream;
    private DataOutputStream outputStream;

    public ServerConnection(Socket _socket, int _id) {
        id = _id;
        socket = _socket;
        try {
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public Message read() {
        try { return Utils.serializeJson(inputStream.readLine()); }
        catch (SocketException e) { Utils.println("connection with " + name + " was closed."); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        return null;
    }
    public void write(Message _message) {
        try { outputStream.writeBytes(Utils.deserializeJson(_message) + '\n'); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public void open(ConcurrentSkipListSet<ServerConnection> _clients) {
        open = true;
        clients = _clients;

        while (open) {
            Message message = this.read();

            if (name==null && message!=null) { this.submitName(message); }
            else { this.handle(message); }
        }
    }
    public void handle(Message _message) {
        switch ((_message!=null)? _message.type() : MessageType.NULL_MESSAGE) {
            case CLIENT_CLOSE:
                this.close(null);
                break;
            case CLIENT_SEND_PRIVATE:
                if (this.allowed()) {
                    clients.stream()
                        .filter(client -> client.name != null)
                        .filter(client -> client.name.equals(_message.receiver()) )
                        .findAny()
                        .ifPresentOrElse(
                            client -> client.write( new Message(MessageType.SERVER_SEND_PRIVATE, _message.payload(), _message.sender() ) ),
                            () -> this.write( new Message(MessageType.SERVER_SEND_ERROR, ErrorType.RECEIVER_NOT_FOUND.name() ) ) );
                } else { this.write( new Message(MessageType.SERVER_SEND_ERROR, ErrorType.CLIENT_MUTED.name() ) ); }
                break;
            case CLIENT_SEND_PUBLIC:
                if (this.allowed()) {
                    clients.forEach( client -> client.write( new Message(
                        MessageType.SERVER_SEND_PUBLIC,
                        _message.payload(),
                        _message.sender() ) ) );
                } else { this.write( new Message(MessageType.SERVER_SEND_ERROR, ErrorType.CLIENT_MUTED.name()) ); }
                break;
            case CLIENT_SET_NAME:
                this.submitName(_message);
                break; 
        }
    }

    public void close(String _warn) {
        try {
            open = false;
            if (_warn != null) this.write(new Message(MessageType.SERVER_CLOSE, _warn));

            clients.remove(this);
            this.updateClients();

            socket.close();
        } catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public void updateClients() {
        List<String> names = clients.stream()
                .map(ServerConnection::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        clients.stream()
            .filter(client -> client.name != null)
            .forEach(client -> client.write(new Message(MessageType.SERVER_SEND_CLIENTS, String.join(",", names)) ) );
    }

    public String getName() { return name; }

    public void submitName(@NotNull Message _message) {
        if (_message.type() == MessageType.CLIENT_SET_NAME) {
            final boolean exists = clients.stream()
                    .map(ServerConnection::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> name.equals(_message.payload()) );
            if (exists) {
                this.write(new Message(MessageType.SERVER_SEND_ERROR, ErrorType.NAME_ALREADY_SET.name()));
            } else {
                name = _message.payload();
                this.write( new Message(MessageType.SERVER_APPROVE_NAME, name) );
                this.updateClients();
            }
        } else { this.write( new Message(MessageType.SERVER_SEND_ERROR, ErrorType.NAME_NOT_SET.name()) ); }
    }

    public int getId() { return id; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }

    public boolean allowed() { return name!=null && !muted; }
}
