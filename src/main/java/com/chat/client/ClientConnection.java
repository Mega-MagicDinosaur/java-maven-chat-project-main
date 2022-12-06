package com.chat.client;

public class ClientConnection {
    private String name;
    private ArrayList<String> messages = new ArrayList<>();

    public ClientConnection(String _name) {
        name = _name;
    }

    public void addMessage(String _message, boolean _writer) { // 0 is target, 1 is me
        if (_writer) {
            messages.add(new ChatMessage());
        }
    }

    public String getName() { return name; }
    public void setName(String _name) { name = _name; }

    public ArrayList getMessages() { return messages; }
}
