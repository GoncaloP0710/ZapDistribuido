package Handlers;

import java.util.Scanner;

public class InterfaceHandler {

    private Scanner scanner;

    public InterfaceHandler() {
        this.scanner = new Scanner(System.in);
    }

    //Funções de output

    public String startUp(){
        System.out.println("----------------------------------");
        System.out.println("-------WHATSAPP DISTRIBUIDO-------");
        System.out.println("----------------------------------");
        System.out.print("Insira o seu Username: ");
        String ret = getInput();
        return ret;
    }

    public String getPassword(){
        System.out.print("Insira a sua password: ");
        String ret = getInput();
        return ret;
    }

    public String getPort(){
        System.out.print("Insira port: ");
        String ret = getInput();
        return ret;
    }

    public void erro(){
        System.out.println("Ocorreu um erro");
    }

    public void erro(String s){
        System.out.println(s);
    }

    //TODO: REFAZER - Change to not print all the time and be like a help command
    public void printMenu() {
        System.out.println("1 - Print neighbors");
        System.out.println("2 - Print node");
        System.out.println("3 - Send message");
        System.out.println("4 - Exit");
    }

    //Funções de input

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