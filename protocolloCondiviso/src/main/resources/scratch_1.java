import it.unito.protocol.Protocol;

class Scratch {
    public static void main(String[] args) {
        String msg = Protocol.build(Protocol.CMD_CHECK_USER, "alice@mia.mail.com");
        System.out.println(msg);

        String[] parsed = Protocol.parse(msg);
        for (String p : parsed) {
            System.out.println("> " + p);
        }
    }
}