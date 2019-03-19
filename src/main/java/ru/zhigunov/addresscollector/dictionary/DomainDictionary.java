package ru.zhigunov.addresscollector.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Список доменов, которые уже были анализированы: <br>
 *  Храним название домена и город
 */
public class DomainDictionary {

    private static Logger LOGGER = LogManager.getLogger(DomainDictionary.class);

    private static final Map<String, String> domains = new ConcurrentHashMap<>();

    public static Map<String, String> getCities() {
        return domains;
    }
}
