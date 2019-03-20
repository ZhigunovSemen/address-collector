package ru.zhigunov.addresscollector.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class HostingProviderDictionary {

    private static Logger LOGGER = LogManager.getLogger(HostingProviderDictionary.class);

    private static final Set<String> providers = new HashSet<>();

    static {
        String filePath = "hostings.txt";
            try (Scanner fileInputScanner = new Scanner(new File(filePath))) {
                while (fileInputScanner.hasNextLine()) {
                    providers.add(fileInputScanner.nextLine().toLowerCase());
                }
            } catch (FileNotFoundException ex) {
                LOGGER.error("File not found '" + filePath + "'. Can not load hosting dictionary", ex);
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        LOGGER.info("Loaded " + providers.size() + " records on hosting dictionary");
    }

    public static Set<String> getProviders() {
        return Collections.unmodifiableSet(providers);
    }
}
