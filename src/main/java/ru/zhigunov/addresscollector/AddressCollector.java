package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AddressCollector implements Callable {

    private static Logger LOGGER = LogManager.getLogger(AddressCollector.class);

    private static DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);

    static {
        try {
            Class.forName("ru.zhigunov.addresscollector.dictionary.CityDictionary");
            Class.forName("ru.zhigunov.addresscollector.dictionary.DomainDictionary");
            Class.forName("ru.zhigunov.addresscollector.dictionary.PhoneCodeDictionary");
            Class.forName("ru.zhigunov.addresscollector.dictionary.HostingProviderDictionary");
        } catch (ClassNotFoundException ex) {
            LOGGER.error(ex);
        }
    }

    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Lock readLock = readWriteLock.readLock();
    private static final Lock writeLock = readWriteLock.writeLock();

    private String[] args;
    private String filePath = "example.xlsx";
    private String batchSize = "1000";
    private String startCell = "D2";
    private long startTime = 0L;
    private long endTime = 0L;

    private List<DataRow> dataRows;

    public AddressCollector(String[] args) {
        this.args = args;
        if (args.length == 0 || StringUtils.isBlank(args[0])) {
            filePath = "example.xlsx";
        }
        if (args.length > 1) {
            batchSize = args[1];
        }
        if (args.length > 2) {
            startCell = args[2];
        }
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);
    }


    @Override
    public Object call() {
        try {
            startTime = System.currentTimeMillis();
            LOGGER.info("Start...");

            long usageMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            readLock.tryLock(60, TimeUnit.SECONDS);
            dataRows = ExcelReader.extractRowsFromXls(filePath, batchSize, startCell);
            readLock.unlock();

            new WebCrawler(dataRows).fillDataRows();

            writeLock.tryLock(60, TimeUnit.SECONDS);
            new ExcelWriter().saveToExcel(filePath, dataRows);
            writeLock.unlock();

            long usageMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            endTime = System.currentTimeMillis();
            LOGGER.info("End address collect process.");
            LOGGER.info(String.format("\t\t\tSpend time: %s ms", formatter.format(endTime - startTime)));
            LOGGER.info(String.format("\t\t\tMemory usage: %s bytes", formatter.format(usageMemoryAfter - usageMemory)));
//        dataRows.stream().forEach(row -> System.out.println(row.getCity()));
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        return new Object();
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
