package it.unito.mailclient;

import it.unito.protocol.Protocol;

public class MailClientApp {
    public static void main(String[] args) {
        String msg = Protocol.build(Protocol.CMD_SEND_EMAIL, "123");
        System.out.println("Client sees: " + msg);
    }
}