package psd.group4.handlers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterfaceHandler {

    private Scanner scanner;

    // ANSI escape codes for colors
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";

    private static final String IP_REGEX = 
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);

    private static final Logger infoLogger = Logger.getLogger("InfoLogger");
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");
    private static final Logger internalInfoLogger = Logger.getLogger("InternalInfoLogger");

    private static boolean infoLoggingEnabled = true;
    private static boolean errorLoggingEnabled = true;
    private static boolean internalInfoLoggingEnabled = true;

    /**
     * Constructor
     */
    public InterfaceHandler() {
        this.scanner = new Scanner(System.in);
        setupLogger(infoLogger);
        setupLogger(errorLogger);
        setupLogger(internalInfoLogger);
    }

    /**
     * Setup the logger
     * 
     * @param logger Logger to be setup
     */
    private void setupLogger(Logger logger) {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }

    /**
     * Enable or disable info logging
     * 
     * @param enabled true to enable, false to disable
     */
    public static void setInfoLoggingEnabled(boolean enabled) {
        infoLoggingEnabled = enabled;
        if (infoLoggingEnabled) {
            infoLogger.setLevel(Level.ALL);
        } else {
            infoLogger.setLevel(Level.OFF);
        }

        if (enabled) {
            InterfaceHandler.success("Info enabled");
        } else {
            InterfaceHandler.success("Info disabled");
        }
    }

    /**
     * Enable or disable error logging
     * 
     * @param enabled true to enable, false to disable
     */
    public static void setErrorLoggingEnabled(boolean enabled) {
        errorLoggingEnabled = enabled;
        if (errorLoggingEnabled) {
            errorLogger.setLevel(Level.ALL);
        } else {
            errorLogger.setLevel(Level.OFF);
        }

        if (enabled) {
            InterfaceHandler.success("Error enabled");
        } else {
            InterfaceHandler.success("Error disabled");
        }
    }

    /**
     * Enable or disable internal info logging
     * 
     * @param enabled true to enable, false to disable
     */
    public static void setInternalInfoLoggingEnabled(boolean enabled) {
        internalInfoLoggingEnabled = enabled;
        if (internalInfoLoggingEnabled) {
            internalInfoLogger.setLevel(Level.ALL);
        } else {
            internalInfoLogger.setLevel(Level.OFF);
        }

        if (enabled) {
            InterfaceHandler.success("Internal info enabled");
        } else {
            InterfaceHandler.success("Internal info disabled");
        }
    }

    /**
     * Print the startup message
     */
    public void startUp() {
        System.out.println(ANSI_GREEN + "         ______           _____  _     _        _ _           _     _       \n" +
        "        |___  /          |  __ \\(_)   | |      (_) |         (_)   | |      \n" +
        "           / / __ _ _ __ | |  | |_ ___| |_ _ __ _| |__  _   _ _  __| | ___  \n" +
        "          / / / _` | '_ \\| |  | | / __| __| '__| | '_ \\| | | | |/ _` |/ _ \\ \n" +
        "         / /_| (_| | |_) | |__| | \\__ \\ |_| |  | | |_) | |_| | | (_| | (_) |\n" +
        "        /_____\\__,_| .__/|_____/|_|___/\\__|_|  |_|_.__/ \\__,_|_|\\__,_|\\___/ \n" +
        "                   | |                                                      \n" +
        "                   |_| " + ANSI_RESET);  
        System.out.println();                                             
    }

    /**
     * Get the username from the user
     */
    public String getUserName(){
        do {
            System.out.print("Insira o seu Username: ");
            String ret = getInput();
            if (ret.length() == 0) {
                System.out.println("Username não pode ser vazio");
            } else {
                return ret;
            }
        } while (true);
    }

    /**
     * Get the password from the user
     */
    public String getPassword(){
        do {
            System.out.print("Insira a sua password: ");
            String ret = getInput();
            if (ret.length() < 8) {
                System.out.println("Password não pode ser menor que 8 caracteres");
            } else {
                return ret;
            }
        } while (true);
    }

    /**
     * Get the port from the user
     */
    public String getPort(){
        do {
            System.out.print("Insira port: ");
            String ret = getInput();
            if (ret.length() == 0) {
                System.out.println("Port não pode ser vazio");
            } else {
                return ret;
            }
        } while (true);
    }

    /**
     * Get the IP from the user
     */
    public String getIP() {
        do {
            System.out.print("Insira o seu ip: ");
            String ret = getInput();
            if (!isValidIP(ret)) {
                System.out.println("IP digitado não é válido");
            } else {
                return ret;
            }
        } while (true);
    }

    /**
     * Verify if the IP is valid
     * 
     * @param ip IP to be verified
     */
    private boolean isValidIP(String ip) {
        Matcher matcher = IP_PATTERN.matcher(ip);
        return matcher.matches();
    }

    /**
     * Get the current date and time
     */
    private static String getCurrentDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dtf.format(LocalDateTime.now());
    }

    /**
     * Print a error message to the user
     */
    public static void erro() {
        System.out.println(ANSI_RED + "[e] Error: Ocorreu um erro" + ANSI_RESET);
    }

    /**
     * Print a error message to the user
     * 
     * @param s Error message
     */
    public static void erro(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_RED + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_RED + "[e]Error:" + " " + s + ANSI_RESET);
    }

    /**
     * Print a info message to the user
     */
    public static void info(String s) {
        String dateTime = getCurrentDateTime();
        if (infoLoggingEnabled) {
            System.out.println(ANSI_YELLOW + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_YELLOW + "[i]Info:" + " " + s + ANSI_RESET);
        }
    }

    /**
     * Print a success message to the user
     */
    public static void success(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_GREEN + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_GREEN + "[s]Success:" + " " + s + ANSI_RESET);
    }

    /**
     * Print a message received to the user
     * 
     * @param s Message received
     */
    public static void messageRecived(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_PURPLE + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_PURPLE + "[m]Message:" + " " + s + ANSI_RESET);
    }

    /**
     * Print a internal Info message to the user
     * 
     * @param s Message sent
     */
    public static void internalInfo(String s) {
        String dateTime = getCurrentDateTime();
        if (internalInfoLoggingEnabled) {
            System.out.println(ANSI_CYAN + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_CYAN + "[i]Internal Info:" + " " + s + ANSI_RESET);
        }
    }

    /**
     * Print the help menu
     */
    public void help(){
        System.out.println(ANSI_GREEN + "1 - (ne) " +  ANSI_UNDERLINE + "Print neighbors" + ANSI_RESET + ANSI_PURPLE + " - Print the neighbors of the node"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "2 - (pn) "+  ANSI_UNDERLINE + "Print node" + ANSI_RESET + ANSI_PURPLE + " - Print the node information"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "3 - (s)  "+  ANSI_UNDERLINE + "Send message" + ANSI_RESET + ANSI_PURPLE + " - Send a message to a node"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "4 - (h)  "+  ANSI_UNDERLINE + "Help"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "5 - (e)  "+  ANSI_UNDERLINE + "Exit" + ANSI_RESET + ANSI_PURPLE + " - Exit the program"+ ANSI_RESET);
    
        System.out.println(ANSI_GREEN + "6 - (iinfo 0)  " + ANSI_UNDERLINE + "Disable internal info logging" + ANSI_RESET + ANSI_PURPLE + " - Disable internal info logging on the terminal" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "7 - (iinfo 1)  " + ANSI_UNDERLINE + "Enable internal info logging" + ANSI_RESET + ANSI_PURPLE + " - Enable internal info logging on the terminal" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "8 -  (info 0)  " + ANSI_UNDERLINE + "Disable info logging" + ANSI_RESET + ANSI_PURPLE + " - Disable info logging on the terminal" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "9 -  (info 1)  " + ANSI_UNDERLINE + "Enable info logging" + ANSI_RESET + ANSI_PURPLE + " - Enable info logging on the terminal" + ANSI_RESET);
    
        System.out.println(ANSI_GREEN + "10 - (sg)  "+  ANSI_UNDERLINE + "Send group message" + ANSI_RESET + ANSI_PURPLE + " - Sends a message to all members of a group"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "11 - (ag)  "+  ANSI_UNDERLINE + "Add a new member to a group" + ANSI_RESET + ANSI_PURPLE + " - A new user is added to a group"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "12 - (cg)  "+  ANSI_UNDERLINE + "Create a new group" + ANSI_RESET + ANSI_PURPLE + " - Creates a new group"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "13 - (ls)  "+  ANSI_UNDERLINE + "List all groups" + ANSI_RESET + ANSI_PURPLE + " - Prints a list of all the groups the usar has access to"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "14 - (pm)  "+  ANSI_UNDERLINE + "Prints message history" + ANSI_RESET + ANSI_PURPLE + " - Prints all of the messages from and to the user"+ ANSI_RESET);
    }

    /**
     * Get the user input
     */
    public String getInput() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        } else {
            System.err.println("No input available");
            return "";
        }
    }

    /**
     * Get the user multiple input
     * 
     * @param s number of lines to get
     */
    public String[] getMultipleInput(int n){
        Scanner sc = new Scanner(System.in);
        String s[] = new String[n];
        for(int i = 0; i<n; i++){
            s[i] = sc.nextLine();
        } 
        sc.close();
        return s;
    }
}