package com.chat;

import com.chat.client.ui.ClientInterface;
import com.chat.server.Server;

public class Main {

    private static class MainServer {
        public static void main(String[] args) {
            new Server().launch();
        }
    }

    private static class MainClient {
        public static void main(String[] args) {}
    }
}
