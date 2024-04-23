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

    private SendMessage gestionarComando(String comando, String[] params) {
        switch (comando) {
            case "/kapachao":
                if (params.length <= 1) {
                    if (params.length == 1) {
                        try {
                            int parametro = Integer.parseInt(params[0]);
                            if (parametro > 0) {
                                this.poolMensajes = parametro;
                            }
                        } catch (NumberFormatException e) {
                            return new SendMessage(this.chatId, "Se esperaba como parametro un numero");
                        }
                    }
                    String resumen = generarResumenDeLosUltimosMensajes();
                    return new SendMessage(this.chatId, resumen);
                } else {
                    return new SendMessage(this.chatId,
                            "Numero de parametros incorrectos para el comando. Se esperaba maximo 1, pero se recibieron "
                                    + params.length);
                }
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

        try (BufferedReader bufferLector = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = bufferLector.readLine()) != null) {
                this.informacion.add(linea);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.informacion.subList(
                this.informacion.size() - numeroDeMensajes >= 0 ? this.informacion.size() - numeroDeMensajes : 0,
                this.informacion.size());
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            this.chatId = update.getMessage().getChatId().toString();
            final String mensajeRecibido = update.getMessage().getText();

            if (update.getMessage().isCommand()) {
                this.ultimoComando = extraerComandoDe(mensajeRecibido);
                final String[] params = extraerParametrosDe(mensajeRecibido);
                try {
                    execute(gestionarComando(ultimoComando, params));
                    borrarInformacion();
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else {
                String info = update.getMessage().getFrom().getUserName() + ": " + mensajeRecibido;
                actualizarInformacionFicheroConChatId(this.chatId, info);
            }
        }
    }

    private String[] extraerParametrosDe(String mensajeRecibido) {
        String[] salida = new String[mensajeRecibido.split(" ").length - 1];
        System.arraycopy(mensajeRecibido.split(" "), 1, salida, 0, mensajeRecibido.split(" ").length - 1);

        return salida;
    }

    private String extraerComandoDe(String mensajeRecibido) {
        return mensajeRecibido.split(" ")[0];
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