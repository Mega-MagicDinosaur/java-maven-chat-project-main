package com.chat.utils;

import com.chat.utils.message.Message;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class Utils {
    private static final File log;

    private static final JsonMapper jsonMapper;
    private static final BufferedWriter consoleWriter;
    private static BufferedWriter logWriter;

    static {
        log = new File("log.txt");
        try {
            if ( !log.exists() ) log.createNewFile();
            else new FileWriter(log).close();

            logWriter = new BufferedWriter(new FileWriter(log, true));
        } catch (IOException e) {
            Utils.logln("exception: IO exception occurred; " +  e.getMessage());
        } finally {
            jsonMapper = new JsonMapper();
            consoleWriter = new BufferedWriter(new OutputStreamWriter(System.out));
        }
    }

    public static @Nullable String deserializeJson(Message _message) {
        try { return jsonMapper.writeValueAsString(_message); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        return null;
    }

    public static @Nullable Message serializeJson(String _string) {
        try { return jsonMapper.readValue(_string, Message.class); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
        return null;
    }

    public static void print(String _string) {
        try {
            consoleWriter.write(_string);
            consoleWriter.flush();
        } catch (NullPointerException e) { Utils.logln("exception: tried to print null value. message: " + e.getMessage()); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public static void println(String _string) {
        try {
            consoleWriter.write(_string + '\n');
            consoleWriter.flush();
        } catch (NullPointerException e) { Utils.logln("exception: tried to print null value. message: " + e.getMessage()); }
        catch (IOException e) { Utils.logln("exception: IO exception occurred; " +  e.getMessage()); }
    }

    public static void log(String _string) {
        try {
            logWriter.write(_string);
            logWriter.flush();
        } catch (NullPointerException | IOException e) { e.printStackTrace(); }
    }

    public static void logln(String _string) {
        try {
            logWriter.write(_string + '\n');
            logWriter.flush();
        } catch (NullPointerException | IOException e) { e.printStackTrace(); }
    }
}