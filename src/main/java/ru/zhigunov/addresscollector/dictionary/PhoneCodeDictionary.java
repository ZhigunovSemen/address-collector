package ru.zhigunov.addresscollector.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PhoneCodeDictionary {

    private static Logger LOGGER = LogManager.getLogger(PhoneCodeDictionary.class);

    private static final Map<String, String> phoneCodes = new HashMap<>();

    static {
        String filePath = "phones.txt";
            try (Scanner fileInputScanner = new Scanner(new File(filePath))) {
                while (fileInputScanner.hasNextLine()) {
                    String line = fileInputScanner.nextLine();
                    String[] s = line.split("\t");
                    phoneCodes.put(s[1], s[0]);
                }
            } catch (FileNotFoundException ex) {
                LOGGER.error("File not found '" + filePath + "'. Can not load phones dictionary", ex);
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        LOGGER.info("Loaded " + phoneCodes.size() + " records on phones dictionary");
    }

    public static Map<String, String> getPhoneMap() {
        return Collections.unmodifiableMap(phoneCodes);
    }
}
