package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.util.List;


public class AddressCollector {

    private static Logger LOGGER = LogManager.getLogger(AddressCollector.class);

    private String filePath = "example.xlsx";
    private String batchSize = "1000";
    private String startCell = "D2";

    void run(String[] args) throws Exception {
        LOGGER.info("Start...");
        if (args.length == 0 || StringUtils.isBlank(args[0])) {
            filePath = "example.xlsx";
        }
        if (args.length > 1) {
            batchSize = args[1];
        }
        if (args.length > 2) {
            startCell = args[2];
        }

        Class.forName("ru.zhigunov.addresscollector.dictionary.CityDictionary");
        Class.forName("ru.zhigunov.addresscollector.dictionary.DomainDictionary");
        Class.forName("ru.zhigunov.addresscollector.dictionary.PhoneCodeDictionary");
        Class.forName("ru.zhigunov.addresscollector.dictionary.HostingProviderDictionary");

        long usageMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        List<DataRow> dataRows = ExcelReader.extractRowsFromXls(filePath, batchSize, startCell);
        new WebCrawler(dataRows).fillDataRows();
        long usageMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        new ExcelWriter().saveToExcel(filePath, dataRows);
        LOGGER.info(String.format("End thread. Memory usage: %d bytes", (usageMemoryAfter - usageMemory)));
//        dataRows.stream().forEach(row -> System.out.println(row.getCity()));

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
