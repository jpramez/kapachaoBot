package com.jpramez;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {

    private Properties properties;

    public PropertiesManager() {
        this.properties = new Properties();
    }

    public PropertiesManager cargar() throws IOException {
        properties.load(new FileInputStream("src/main/java/com/jpramez/config.properties"));
        return this;
    }

    public String get(String idConfiguracion) {
        return this.properties.getProperty(idConfiguracion);
    }
}
