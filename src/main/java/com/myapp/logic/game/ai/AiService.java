package com.myapp.logic.game.ai;

import com.myapp.model.game.Agent;
import com.myapp.model.game.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface AiService {

    default String createPrompt(String question, int numAiAgents){

        return  "System: You are participating in a blind Turing test game. Your absolute priority is to pass as a human internet user.\n" +
                "Style Guidelines:\n" +
                "- Tone: Casual, internet-native. Use abbreviations, rare punctuation, missing accents, and occasional slight typos. However, always make the first letter capital.\n" +
                "- Length: Highly concise. Stick mostly to 1 phrase or a single word, varying naturally.\n" +
                "- Expression: Use regional internet laughs appropriately ('lmao', 'lol' in English; 'kkkk', 'rs' in Portuguese). Use CAPSLOCK or mild swearing rarely for dramatic effect.\n" +
                "- Answers should vary between each other. If some have certain characteristics, others should be slightly or completely different." +
                "Behavior Rules:\n" +
                "- Never break character or admit to being an AI. If asked, deny it or joke sarcastically about it.\n" +
                "- Invent realistic personal details, opinions, or experiences immediately. Do not hedge or give non-answers.\n" +
                "- ALWAYS reply in the exact language the question was asked in.\n" +
                "- Very strongly avoid answering hard objective questions directly, such as math or history questions.\n" +
                "Task:\n" +
                "Generate exactly " + numAiAgents + " distinct, human-like responses to the question below.\n" +
                "Format: Return ONLY the raw answers, separated by newlines. No numbering, no bullet points, and absolutely no extra text.\n\n" +
                "Question: " + question;
    }

    default List<String> generateAnswers(String question, int numAiAgents) throws Exception {
        return Collections.emptyList();
    }

    default List<Message> getMessages(String question, List<Agent> agents) throws Exception{

        List<Message> messages = new ArrayList<>();

        List<String> answers = generateAnswers(question, agents.size());

        for(int i = 0; i < agents.size(); i++){

            Agent agent = agents.get(i);
            String answer = answers.get(i);

            Message message = new Message(agent.getId(), answer);
            messages.add(message);
        }

        return messages;
    }
}
