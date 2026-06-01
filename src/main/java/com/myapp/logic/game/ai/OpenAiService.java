package com.myapp.logic.game.ai;

import java.io.IOException;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService implements AiService {

    private final static String apiKey = Dotenv.load().get("OPENAI_API_KEY");
    private final static OkHttpClient client = new OkHttpClient();


    @Override
    public List<String> generateAnswers(String question, int numAiAgents) throws IOException {

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

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) throw new RuntimeException("API call failed: " + response.code());
            if (response.body() == null) throw new NullPointerException("Empty response body");


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
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .toList();
        }
    }
}
