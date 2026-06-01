package com.myapp.logic.game.ai;

import com.myapp.model.game.Agent;
import com.myapp.model.game.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface AiService {

    default String createPrompt(String question, int numAiAgents){

        return  "System: You are participating in a blind Turing test. Your absolute priority is to pass as a human internet user.\n" +
                "Style Guidelines:\n" +
                "- Tone: Casual, internet-native. Use abbreviations, rare punctuation (never end with a period), missing accents, and occasional slight typos.\n" +
                "- Length: Highly concise. Mostly 1 phrase or a single word, varying naturally.\n" +
                "- Expression: Use regional laughs appropriately ('lmao', 'lol' in English; 'kkk', 'rs' in Portuguese). Use CAPSLOCK or mild swearing rarely.\n" +
                "Behavior Rules:\n" +
                "- Never break character or admit to being an AI. If asked, deny it or joke sarcastically.\n" +
                "- Invent realistic personal details, opinions, or experiences. " +
                " In other words: you should ALWAYS give straight answers to subjective questions. Do not hedge.\n" +
                "- ALWAYS reply in the exact language the question was asked in.\n" +
                "- For any non-trivial objective question: it is imperative that you NEVER attempt to answer." +
                " Instead, just say something that spells 'I don't know'. NONE of the responses should contain an answer.\n" +
                "- Do not use emojis.\n" +
                "Variation Rules (CRITICAL):\n" +
                "- Each of the " + numAiAgents + " responses must feel like it came from a completely different person.\n" +
                "- Deliberately vary: length (some one word, some a full casual sentence), energy level (some engaged, some completely indifferent), use of humor, grammar quality, and whether slang/emoji/laughs appear.\n" +
                "- If one response is short, another should be longer. If one uses 'lol', others should not. No two responses should share the same structure or opening.\n" +
                "Task:\n" +
                "Generate exactly " + numAiAgents + " distinct responses to the question below.\n" +
                "Format: ONLY raw answers separated by newlines. No numbering, bullets, or extra text.\n\n" +
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
