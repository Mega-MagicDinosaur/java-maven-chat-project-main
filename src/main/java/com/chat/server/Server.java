package com.chat.server;

import com.chat.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private final int port = 5050;

    private boolean open = false;
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private ServerSocket server;
    private final ConcurrentSkipListSet<ServerConnection> clients =
            new ConcurrentSkipListSet<>(Comparator.comparingInt(ServerConnection::getId));

    private final BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

    public void launch() {
        if ( this.init() ) {
            new Thread(this::open).start();
            new Thread(this::read).start();
        } else { Utils.println("server failed to boot up"); }
    }

    public boolean init() {
        try {
            open = true;
            server = new ServerSocket(port);
            return true;
        } catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        return false;
    }

    public void open() {
        if (open) {
            try {
                Utils.println("server is listening on port " + port);
                while (open) {
                    Socket socket = server.accept();
                    ServerConnection client = new ServerConnection(socket, idCounter.incrementAndGet());
                    clients.add(client);
                    new Thread(() -> client.open(clients)).start();
                }
            } catch (SocketException e) { Utils.println("server was closed"); }
            catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        } else { Utils.println("server can operate only when it is initialized"); }
    }

    public void close() {
        try {
            open = false;
            clients.forEach(client -> client.close("server is closing"));
            server.close();
        } catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public void read() {
        if (open) {
            try {
                Utils.println("server cli is open: ");
                Utils.println("type '@?' to display commands");
                while (open) {
                    Utils.print("# ");
                    String[] message = keyboard.readLine().split(" ", 2);
                    this.handle(message);
                }
            } catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        } else { Utils.println("cli can operate only when server has been initialized"); }
    }
    public void handle(String[] _message) {
        switch ((_message!=null)? _message[0] : null) {
            case null -> Utils.println("error has occurred: message is null");
            case "" -> {}
            case "@?" -> Utils.println("""
                                commands:
                                @clients
                                @close
                                @kick [name]
                                @mute [name]
                                @unmute [name]""");
            case "@clients" -> Utils.println("clients: " +
                    clients.stream()
                            .map(ServerConnection::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", ")) );
            case "@close" -> this.close();
            case "@kick" -> clients.stream()
                    .filter(client -> client.getName()!=null) // can technically remove this
                    .filter(client -> client.getName().equals(_message[1]))
                    .findAny()
                    .ifPresentOrElse(
                            client -> client.close("you have been kicked out"),
                            () -> Utils.println("client was not found") );
            case "@mute" -> clients.stream()
                    .filter(client -> client.getName()!=null)
                    .filter(client -> client.getName().equals(_message[1]))
                    .findAny()
                    .ifPresentOrElse(
                            client -> client.setMuted(true),
                            () -> Utils.println("client was not found") );
            case "@unmute" -> clients.stream()
                    .filter(client -> client.getName()!=null)
                    .filter(client -> client.getName().equals(_message[1])) // maybe add filter muted check
                    .findAny()
                    .ifPresentOrElse(
                            client -> client.setMuted(false),
                            () -> Utils.println("client was not found"));
            default -> Utils.println("unknown command, type @? for help");
        }
    }
}
