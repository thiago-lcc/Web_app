package ai;
import model.Agent;
import model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONObject;


public class AiService {

    private final static String apiKey = Dotenv.load().get("OPENAI_API_KEY");
    private final static OkHttpClient client = new OkHttpClient();


    private static String createPrompt(String question, int numAiAgents){

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

    private static List<String> generateAnswers(String question, int numAiAgents) throws IOException {

        String jsonBody = new JSONObject()
                .put("model", "gpt-4.1-mini")
                .put("input", createPrompt(question, numAiAgents))
                .toString();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/responses")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()){

        if(!response.isSuccessful()) throw new RuntimeException("API call failed: " + response.code());
        if(response.body() == null) throw new NullPointerException("Empty response body");


        String responseBody = response.body().string();

        JSONObject json = new JSONObject(responseBody);

        String answersString = json
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text");

        return answersString.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        }
    }


    public static List<Message> getMessages(String question, List<Agent> agents) throws IOException{

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
