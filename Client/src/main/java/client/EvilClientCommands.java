package client;

import java.io.Serializable;

public enum EvilClientCommands implements Serializable {
    REPLAY("replay"),
    EVILTRANSFER("evilTransfer"),
    EVILBROADCAST("evilBroadcast");


    private String text;

    EvilClientCommands(String text) {
        this.text = text;
    }


    public String getText() {
        return this.text;
    }

    public static EvilClientCommands fromStringToCommand(String text) {
        for (EvilClientCommands command : EvilClientCommands.values()) {
            if (command.text.equalsIgnoreCase(text)) {
                return command;
            }
        }
        return null;
    }







}





