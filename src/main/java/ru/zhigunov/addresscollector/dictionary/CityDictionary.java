package ru.zhigunov.addresscollector.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class CityDictionary {

    private static Logger LOGGER = LogManager.getLogger(CityDictionary.class);

    private static final Set<String> cities = new HashSet<>();

    static {
        String filePath = "city.txt";
            try (Scanner fileInputScanner = new Scanner(new File(filePath))) {
                while (fileInputScanner.hasNextLine()) {
                    cities.add(fileInputScanner.nextLine().toLowerCase());
                }
            } catch (FileNotFoundException ex) {
                LOGGER.error("File not found '" + filePath + "'. Can not load city dictionary", ex);
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        LOGGER.info("Loaded " + cities.size() + " records on city dictionary");
    }

    public static Set<String> getCities() {
        return Collections.unmodifiableSet(cities);
    }
}
