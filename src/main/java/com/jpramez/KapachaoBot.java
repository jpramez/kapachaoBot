package com.jpramez;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class KapachaoBot extends TelegramLongPollingBot {
    private PropertiesManager propertiesManager;
    private ChatGPTApiManager chatGPT;
    private String chatId;
    private String ultimoComando;
    private String chatsPath;
    private List<String> comandosValidos;
    private List<String> informacion;
    private String botToken;
    private int poolMensajes;

    public KapachaoBot(String botToken) throws IOException {
        super(botToken);
        this.propertiesManager = PropertiesManager.obtenerInstancia();
        this.botToken = botToken;
        this.comandosValidos = List.of("/kapachao", "/reload");
        this.informacion = new ArrayList<>();
        this.chatsPath = propertiesManager.get("app.chatsPath");
        this.poolMensajes = Integer.parseInt(propertiesManager.get("bot.poolMensajes"));
    }

    private SendMessage gestionarComando(String comando) {
        switch (comando) {
            case "/kapachao":
                String resumen = generarResumenDeLosUltimosMensajes();
                return new SendMessage(this.chatId, resumen);
            case "/reload":
                return new SendMessage(this.chatId, recargarConfiguracion() ? "Se ha actualizado la configuracion"
                        : "Hubo un error al actualizar la configuracion");
            default:
                return new SendMessage(this.chatId, "No se reconoce el comando");
        }
    }

    private boolean recargarConfiguracion() {
        try {
            this.propertiesManager = PropertiesManager.obtenerInstancia().cargar();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generarResumenDeLosUltimosMensajes() {
        this.informacion = cargarListaConLosUltimosMensajes(this.poolMensajes);
        
        try {
            return new ChatGPTApiManager().consultar(this.informacion);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private List<String> cargarListaConLosUltimosMensajes(int numeroDeMensajes) {
        String nombreArchivo = this.chatsPath + "/" + chatId + ".txt";
        int cantidadLineas = numeroDeMensajes; // Cambiar por el número deseado de últimas líneas a leer

        try (BufferedReader bufferLector = new BufferedReader(new FileReader(nombreArchivo))) {
            List<String> ultimasLineas = new ArrayList<>();
            String linea;

            while ((linea = bufferLector.readLine()) != null) {
                if (ultimasLineas.size() >= cantidadLineas) {
                    ultimasLineas.remove(0);
                }
                ultimasLineas.add(linea);
            }
            for (String ultimaLinea : ultimasLineas) {
                this.informacion.add(ultimaLinea);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.informacion;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            this.chatId = update.getMessage().getChatId().toString();
            if (comandosValidos.contains(update.getMessage().getText())) {
                this.ultimoComando = update.getMessage().getText();
                try {
                    execute(gestionarComando(ultimoComando));
                    borrarInformacion();
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                String info = update.getMessage().getFrom().getUserName() + ": " + update.getMessage().getText();
                actualizarInformacionFicheroConChatId(this.chatId, info);
            }
        }
    }

    private void actualizarInformacionFicheroConChatId(String chatId, String info) {
        String nombreArchivo = this.chatsPath + "/" + chatId + ".txt";

        try {
            FileWriter escritor = new FileWriter(nombreArchivo, existeFicheroConChatID(this.chatId));
            escritor.write(info + "\n"); // Escribir contenido en el archivo
            escritor.close(); // Cerrar el escritor
            System.out.println("Se ha escrito en el archivo " + nombreArchivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean existeFicheroConChatID(String chatId) {
        String nombreArchivo = this.chatsPath + "/" + chatId + ".txt";
        File archivo = new File(nombreArchivo);
        return archivo.exists();
    }

    private void borrarInformacion() {
        this.informacion.clear();
    }

    @Override
    public String getBotUsername() {
        return "kapachaoBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}