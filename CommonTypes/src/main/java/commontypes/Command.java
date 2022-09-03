package commontypes;

import java.io.Serializable;

public enum Command implements Serializable {
    OPEN("open"),
    CHECK("check"),
    AUDIT("audit"),
    SEND("send"),
    RECEIVE("receive"),
    ERROR("error");


    private String text;

    Command(String text) {
        this.text = text;
    }


    public String getText() {
        return this.text;
    }

    public static Command fromStringToCommand(String text) {
        for (Command command : Command.values()) {
            if (command.text.equalsIgnoreCase(text)) {
                return command;
            }
        }
        return null;
    }







}





