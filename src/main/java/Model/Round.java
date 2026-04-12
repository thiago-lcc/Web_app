package Model;

import java.util.List;

public record Round(String question, List<Message> answers, int num){}