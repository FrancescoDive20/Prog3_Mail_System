package it.unito.mailserver;

import it.unito.protocol.Protocol;

public class MailServerMain {
    public static void main(String[] args) {
        String msg = Protocol.build(Protocol.CMD_SEND_EMAIL, "123");
        System.out.println("Server sees: " + msg);
    }
}