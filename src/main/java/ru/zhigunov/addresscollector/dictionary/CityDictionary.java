package ru.zhigunov.addresscollector.dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class CityDictionary {

    private final Set<String> cities = new HashSet<>();

    public CityDictionary(String filePath) throws IOException {
        try (Scanner fileInputScanner = new Scanner(new File(filePath))) {
            while(fileInputScanner.hasNextLine()) {
                cities.add(fileInputScanner.nextLine());
            }
        }
    }

    public Set<String> getCities() {
        return cities;
    }
}
