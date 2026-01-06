package duongtran.vctrl.cli;

import duongtran.vctrl.cli.parser.ASTCommand;
import duongtran.vctrl.cli.parser.VctrlParser;
import duongtran.vctrl.cli.visitor.CommandVisitor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CommandLineInterface {

    private final Scanner scanner;

    public CommandLineInterface() {
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printWelcome();

        while (true) {
            try {
                String input = readCommand();

                if (input == null) {
                    exit();
                    return;
                }

                if (isExitCommand(input)) {
                    exit();
                    return;
                }

                parseAndExecute(input);

            } catch (NoSuchElementException e) {
                exit();
                return;
            }
        }
    }

    /* =========================
       Input handling
       ========================= */

    private String readCommand() {
        System.out.print("vctrl> ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "" : input;
    }

    private boolean isExitCommand(String input) {
        return input.equalsIgnoreCase("exit");
    }

    /* =========================
       Core logic
       ========================= */

    private void parseAndExecute(String input) {
        try {
            InputStream in = new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            VctrlParser parser = new VctrlParser(in);
            ASTCommand command = parser.command();

            CommandVisitor visitor = new CommandVisitor();
            command.jjtAccept(visitor, null);

        } catch (Exception e) {
            handleError(e);
        }
    }

    /* =========================
       UI helpers
       ========================= */

    private void printWelcome() {
        System.out.println("vctrl interactive shell");
        System.out.println("Type 'exit' to quit");
    }

    private void exit() {
        System.out.println("Bye!");
    }

    private void handleError(Exception e) {
        System.out.println("Error: " + e.getMessage());
    }

}
