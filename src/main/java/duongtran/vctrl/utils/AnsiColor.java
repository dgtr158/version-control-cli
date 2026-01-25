package duongtran.vctrl.utils;

public class AnsiColor {

    public static final String RESET  = "\u001B[0m";

    public static final String BOLD = "\u001B[1m";

    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String CYAN   = "\u001B[36m";
    public static final String GRAY   = "\u001B[90m";

    public static String green(String s) {
        return GREEN + s + RESET;
    }

    public static String red(String s) {
        return RED + s + RESET;
    }

    public static String yellow(String s) {
        return YELLOW + s + RESET;
    }

    public static String cyan(String s) {
        return CYAN + s + RESET;
    }

    public static String bold(String s) {
        return BOLD + s + RESET;
    }

    public static String boldYellow(String s) {
        return BOLD + YELLOW + s + RESET;
    }

}
