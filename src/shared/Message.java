package shared;

public class Message {
    public static String encode(String command, String... args) {
        return command + " " + String.join(" ", args);
    }

    public static String[] decode(String line) {
        return line.trim().split("\\s+");
    }
}
