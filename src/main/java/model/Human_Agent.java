package model;

import java.util.Scanner;

public class Human_Agent extends Agent {
    
    private final boolean is_impostor = true;

    
    public Human_Agent(int id) {
        super(id);
    }

    //placeholder
    @Override
    public Message getMessage(String question){

        System.out.println(question);
        Scanner scanner = new Scanner(System.in);
        Message msg = new Message(this.getId(), scanner.next());
        scanner.close();
        return msg;
    }
    
}
