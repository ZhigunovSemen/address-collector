package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.util.List;


public class AddressCollector {


    private String filePath;
    private String batchSize;
    private String startCell;

    void run(String[] args) throws Exception {
        filePath = args[0];
        Validate.isTrue(!StringUtils.isBlank(filePath), "Не указан путь к файлу. " + Help());
        if (args.length > 1) {
            batchSize = args[1];
        }
        if (args.length > 2) {
            startCell = args[2];
        }

        List<DataRow> dataRows = UrlExctractor.extractFromXls(filePath, batchSize, startCell);

    }

    private static String Help() {
        return new StringBuilder()
                .append("\n Аргументы запуска:\n")
                .append("\n <filepath> (путь к файлу) \n")
                .append("\n <batchSize> (размер буфера считывания строк из файла)\n")
                .append("\n <startCell> (позиция начала считывания потока url (для excel-файлов))\n")
                .toString();

    }

}
