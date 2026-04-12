package ai;
import model.Agent;
import model.Message;

import java.util.ArrayList;
import java.util.List;


public class AiService {

    public static List<Message> generateAnswers(List<Agent> agents){

        List<Message> answers = new ArrayList<>();

        for(Agent agent: agents)
            if(!agent.isImpostor())
                answers.add(new Message(agent.getId(), "This message is from agent " + agent.getId()));


        return answers;
    }
}
