package com.jpramez;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {

    private static PropertiesManager instancia;

    private Properties properties;

    public PropertiesManager() {
        this.properties = new Properties();
    }

    public static synchronized PropertiesManager obtenerInstancia() throws IOException {
        if (instancia == null) {
            instancia = new PropertiesManager();
            instancia.cargar();
        }
        return instancia;
    }

    public PropertiesManager cargar() throws IOException {
        instancia.getProperties().load(new FileInputStream("src/main/java/com/jpramez/config.properties"));
        return instancia;
    }

    public String get(String idConfiguracion) {
        return instancia.getProperties().getProperty(idConfiguracion);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
