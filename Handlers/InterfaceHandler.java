package handlers;

import java.util.Scanner;

public class InterfaceHandler {

    public InterfaceHandler(){}

    //Funções de output

    public String startUp(){
        System.out.println("----------------------------------");
        System.out.println("-------WHATSAPP DISTRIBUIDO-------");
        System.out.println("----------------------------------");
        System.out.print("Insira o seu Username: ");
        System.out.println("");
        return getInput();
    }

    public String getPassword(){
        System.out.print("Insira a sua password: ");
        System.out.println("");
        return getInput();
    }

    public void erro(){
        System.out.println("Ocorreu um erro");
    }

    public void erro(String s){
        System.out.println(s);
    }

    //TODO: REFAZER
    public void printMenu() {
        System.out.println("1 - Enviar mensagem");
        System.out.println("2 - Ver mensagens");
        System.out.println("3 - Ver mensagens de um utilizador");
        System.out.println("4 - Ver utilizadores");
        System.out.println("5 - Ver utilizadores online");
        System.out.println("6 - Ver utilizadores offline");
        System.out.println("7 - Ver utilizadores bloqueados");
        System.out.println("8 - Bloquear utilizador");
        System.out.println("9 - Desbloquear utilizador");
        System.out.println("10 - Sair");
    }


    //Funções de input

    public String getInput(){
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        sc.close();
        return s;
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