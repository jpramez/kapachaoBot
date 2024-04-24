package com.jpramez;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.InputFile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.audio.AudioSpeechRequest;
import io.github.sashirestela.openai.domain.audio.SpeechRespFmt;
import io.github.sashirestela.openai.domain.audio.Voice;

public class ChatGPTApiManager {

    private PropertiesManager propertiesManager;

    private URL url;
    private HttpURLConnection connection;

    private String completionsAPI;
    private String speechAPI;
    private String apiKey;
    private String modelCompletions;
    private String basePromtCompletions;
    private String modelSpeech;
    private String voiceSpeech;

    public ChatGPTApiManager() throws IOException {
        this.propertiesManager = PropertiesManager.obtenerInstancia();
        this.completionsAPI = propertiesManager.get("chatgpt.completionsAPI");
        this.apiKey = propertiesManager.get("chatgpt.apiKey");
        this.modelCompletions = propertiesManager.get("chatgpt.completionsAPI.model");
        this.basePromtCompletions = propertiesManager.get("chatgpt.completionsAPI.basePromt");
        this.speechAPI = propertiesManager.get("chatgpt.speechAPI");
        this.modelSpeech = propertiesManager.get("chatgpt.speechAPI.model");
        this.voiceSpeech = propertiesManager.get("chatgpt.speechAPI.voice");

    }

    public String consultar(List<String> historico) {
        try {
            this.url = URL.of(URI.create(completionsAPI), null);
            this.connection = (HttpURLConnection) this.url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            String promt = this.basePromtCompletions + ": ";
            for (String info : historico) {
                promt += info + "\\n";
            }
            promt = promt.replace("\"", "").replace("\\", "");
            String body = "{\"model\": \"" + modelCompletions
                    + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + promt
                    + "\"}]}";
            connection.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractMessageFromJSONResponse(String response) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        String content = jsonObject.getAsJsonArray("choices")
                .get(0) // El primer elemento del arreglo
                .getAsJsonObject() // Convertirlo a JsonObject
                .getAsJsonObject("message") // Obtener el objeto "message"
                .get("content") // Obtener el contenido
                .getAsString(); // Convertirlo a String

        return content;

    }

    public InputFile speech(String resumen) {
        var openai = SimpleOpenAI.builder()
                .apiKey(this.apiKey)
                .build();

        var speechRequest = AudioSpeechRequest.builder()
                .model("tts-1")
                .input(resumen)
                .voice(Voice.ECHO)
                .responseFormat(SpeechRespFmt.MP3)
                .speed(1.0)
                .build();
        var futureSpeech = openai.audios().speak(speechRequest);
        var speechResponse = futureSpeech.join();
        try {
            FileOutputStream audioFile = new FileOutputStream("src/main/java/com/jpramez/audio/resumen.mp3");
            audioFile.write(speechResponse.readAllBytes());
            System.out.println(audioFile.getChannel().size() + " bytes");
            audioFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new InputFile(new File("src/main/java/com/jpramez/audio/resumen.mp3"));
    }
}