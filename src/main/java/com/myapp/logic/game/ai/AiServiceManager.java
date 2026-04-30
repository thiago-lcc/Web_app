package com.myapp.logic.game.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class AiServiceManager implements AiService{

    private final OpenAiService openAiService;
    private final GeminiService geminiService;

    public AiServiceManager(OpenAiService openAiService, GeminiService geminiService){

        this.openAiService = openAiService;
        this.geminiService = geminiService;
    }

    public List<String> generateAnswers(String question, int numAiAgents) throws Exception {

        for(int i = 0; i < 2; i++)

            try {return openAiService.generateAnswers(question, numAiAgents);}
            catch (Exception e) {

                try {return geminiService.generateAnswers(question, numAiAgents);}
                catch (Exception ignored) {}
            }

        throw new Exception("Failed to get AI answers");
    }
}
