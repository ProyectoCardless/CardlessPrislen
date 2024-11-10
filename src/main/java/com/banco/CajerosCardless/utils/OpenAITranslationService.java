package com.banco.CajerosCardless.utils;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import okhttp3.*;
import org.json.JSONObject;

@Service
public class OpenAITranslationService {

    private static final String API_KEY = "sk-proj-XZBDWnBSFXBPjIIw4QozT3BlbkFJ6Qs1vyn95kwpcXtHwwW5";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // Método para traducir texto usando la API de OpenAI
    public String translate(String text, String targetLanguage) {
        OkHttpClient client = new OkHttpClient();

        // Formato de entrada del prompt de traducción
        String prompt = "Translate the following text to " + targetLanguage + ": " + text;

        // Cuerpo de la solicitud
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "gpt-3.5-turbo");
        jsonObject.put("messages", new JSONObject[] {
                new JSONObject().put("role", "user").put("content", prompt)
        });
        jsonObject.put("temperature", 0.3);

        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));

        // Crear solicitud HTTP
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // Ejecutar solicitud
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Extraer traducción del JSON de respuesta
                String responseBody = response.body().string();
                JSONObject responseJson = new JSONObject(responseBody);
                return responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();
            } else {
                System.out.println("Error en la respuesta: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
