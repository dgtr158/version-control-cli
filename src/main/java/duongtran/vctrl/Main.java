package duongtran.vctrl;

import duongtran.vctrl.actions.*;
import duongtran.vctrl.cli.CommandLineInterface;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.utils.ActionConstants;
import duongtran.vctrl.utils.DirectoryNames;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

public class Main {

    public static void main(String[] args) {

        CommandLineInterface cli = new CommandLineInterface();
        cli.run();
    }


}