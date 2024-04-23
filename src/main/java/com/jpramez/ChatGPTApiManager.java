package com.jpramez;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ChatGPTApiManager {

    private PropertiesManager propertiesManager;

    private URL url;
    private HttpURLConnection connection;

    private String stringConnection;
    private String apiKey;
    private String model;
    private String basePromt;

    public ChatGPTApiManager() throws IOException {
        this.propertiesManager = PropertiesManager.obtenerInstancia();
        this.stringConnection = propertiesManager.get("chatgpt.apiString");
        this.apiKey = propertiesManager.get("chatgpt.apiKey");
        this.model = propertiesManager.get("chatgpt.model");
        this.basePromt = propertiesManager.get("chatgpt.basePromt");
    }

    public String consultar(List<String> historico) {
        try {
            this.url = URL.of(URI.create(stringConnection), null);
            this.connection = (HttpURLConnection) this.url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            String promt = this.basePromt + ": ";
            for (String info : historico) {
                promt += info + "\\n";
            }
            promt = promt.replace("\"", "").replace("\\", "");
            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + promt
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
}