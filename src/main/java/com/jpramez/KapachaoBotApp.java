package com.jpramez;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class KapachaoBotApp {

    static Logger logger = Logger.getLogger(KapachaoBotApp.class.getName());

    public static void main(String[] args) {

        try {
            PropertiesManager propertiesManager = PropertiesManager.obtenerInstancia();
            String botToken = propertiesManager.get("bot.token");

            KapachaoBot kapachaoBot = new KapachaoBot(botToken);
            TelegramBotsApi kapachaoBotApp = new TelegramBotsApi(DefaultBotSession.class);
            kapachaoBotApp.registerBot(kapachaoBot);

            logger.log(Level.INFO, "KapachapBot ready");
        } catch (TelegramApiException e) {
            logger.log(Level.CONFIG, "Error al registrar el bot", e);
        } catch (IOException e) {
            logger.log(Level.CONFIG, "Error al abrir el archivo properties", e);
        }
    }
}