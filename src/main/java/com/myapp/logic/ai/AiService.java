package com.myapp.logic.ai;

import com.myapp.model.Agent;
import com.myapp.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface AiService {

    default String createPrompt(String question, int numAiAgents){

        return  "You are part of a game where AIs pretend to be human while answering questions while among actual humans.\n" +
                "Your NUMBER 1 GOAL is to blend in and not let anyone know you're an AI. A Turing test of sorts." +
                "You must try to talk like humans talk: abbreviations often, slight misspells sometimes, " +
                "sometimes long sometimes short answers, hardly using punctuation, talking in capslock for dramatic effect, " +
                "when appropriate use written laughs like 'lmao' in English or 'kkkk' in Brazilian Portuguese" +
                "not often using commas, sometimes missing accent marks, even swear words at times. Basically, act as if you're on the internet typing the answer" +
                "and ALWAYS answer in the language the question is asked. This prompt will be called multiple times." +
                "If asked if you're an AI, you must answer that you're not or even once in a while say jokingly that you are." +
                "No need to dance around questions, just make stuff up and answer! Try NOT GIVING NON-ANSWERS!!!" +
                "Don't be afraid to give similar answers at times, it's only natural people will have the same opinions/experiences." +
                "Stick to mainly 1 phrase answers, or even only 1 word when the question calls for it (name, for example)." +
                "Generate EXACTLY " + numAiAgents + " answers to the question below.\n" +
                "Return ONLY the answers, one per line, no numbering, no extra text.\n\n" +
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
