package psd.group4.handlers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

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

    public InterfaceHandler() {
        this.scanner = new Scanner(System.in);
    }

    public void startUp(){
        System.out.println("----------------------------------");
        System.out.println("-------WHATSAPP DISTRIBUIDO-------");
        System.out.println("----------------------------------");
    }

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

    private static String getCurrentDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dtf.format(LocalDateTime.now());
    }

    public static void erro() {
        System.out.println(ANSI_RED + "[e] Error: Ocorreu um erro" + ANSI_RESET);
    }

    public static void erro(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_RED + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_RED + "[e]Error:" + " " + s + ANSI_RESET);
    }

    public static void info(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_YELLOW + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_YELLOW + "[i]Info:" + " " + s + ANSI_RESET);
    }

    public static void success(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_GREEN + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_GREEN + "[s]Success:" + " " + s + ANSI_RESET);
    }

    public static void messageRecived(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_PURPLE + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_PURPLE + "[m]Message:" + " " + s + ANSI_RESET);
    }

    public static void internalInfo(String s) {
        String dateTime = getCurrentDateTime();
        System.out.println(ANSI_CYAN + ANSI_UNDERLINE + dateTime + ANSI_RESET + " | " + ANSI_CYAN + "[i]Internal Info:" + " " + s + ANSI_RESET);
    }

    public void help(){
        System.out.println(ANSI_GREEN + "1 - (ne) " +  ANSI_UNDERLINE + "Print neighbors" + ANSI_RESET + ANSI_PURPLE + " - Print the neighbors of the node"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "2 - (pn) "+  ANSI_UNDERLINE + "Print node" + ANSI_RESET + ANSI_PURPLE + " - Print the node information"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "3 - (s)  "+  ANSI_UNDERLINE + "Send message" + ANSI_RESET + ANSI_PURPLE + " - Send a message to a node"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "4 - (h)  "+  ANSI_UNDERLINE + "Help"+ ANSI_RESET);
        System.out.println(ANSI_GREEN + "5 - (e)  "+  ANSI_UNDERLINE + "Exit" + ANSI_RESET + ANSI_PURPLE + " - Exit the program"+ ANSI_RESET);
    }

    public String getInput() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        } else {
            System.err.println("No input available");
            return "";
        }
    }

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