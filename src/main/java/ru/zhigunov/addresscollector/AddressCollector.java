package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AddressCollector {

    private static Logger LOGGER = LogManager.getLogger(AddressCollector.class);

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

    private static DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Lock readLock = readWriteLock.readLock();
    private static final Lock writeLock = readWriteLock.writeLock();

    private String[] args;
    private String filePath = "example.xlsx";
    private int loadQuantity = 2000;
    private int startLineNumber = 1;
    private int endLineNumber = 20000;


    public AddressCollector(String[] args) {
        this.args = args;
        if (args.length > 0 || StringUtils.isNotBlank(args[0])) {
            filePath = args[0];
            if (args.length > 1) {
                loadQuantity = Math.max(Integer.valueOf(args[1]), 10);
                if (args.length > 2) {
                    startLineNumber = Math.max(Integer.valueOf(args[2]), 1);
                    if (args.length > 3) {
                        endLineNumber = Math.max(Integer.valueOf(args[3]), 1);
                    }
                }
            }
        }
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);
    }

    /**
     * разделение основной задачи на множество мелких и отдача воркерам
     */
    public void run() {
        int line = startLineNumber;
        int numThreads = Runtime.getRuntime().availableProcessors();

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        while (line <= endLineNumber) {
            List<Callable<Boolean>> processors = new ArrayList<>();
            for (int i = 1; i <= numThreads; i++) {
                line+=loadQuantity;
                processors.add(
                    new AddressCollectorWorker(line, loadQuantity)
                );
            }
            try {
                List<Future<Boolean>> futures = executorService.invokeAll(processors);
                for (Future<Boolean> future : futures ) {
                    future.get();
                }
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
            processors.clear();
        }
    }


    /**
     * Воркер, обрабатывающий партию строчек в отдельном потоке. <br>
     *     Основная задача: чтение - обработка - запись.
     */
    private class AddressCollectorWorker implements Callable {

        private List<DataRow> dataRows;

        private int startLineNumber;
        private int loadQuantity;
        private long startTime = 0L;
        private long endTime = 0L;

        private AddressCollectorWorker(int startLineNumber, int loadQuantity) {
            this.startLineNumber = startLineNumber;
            this.loadQuantity = loadQuantity;
        }

        @Override
        public Boolean call() {
            try {
                startTime = System.currentTimeMillis();
                LOGGER.info("Start...");

                long usageMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                if (readLock.tryLock(600, TimeUnit.SECONDS)) {
                    LOGGER.debug(" try to read data from excel...");
                    dataRows = ExcelReader.extractRowsFromXls(filePath, loadQuantity, startLineNumber);
                    readLock.unlock();
                } else return false;

                new WebCrawler(dataRows).fillDataRows();

                if (writeLock.tryLock(600, TimeUnit.SECONDS)) {
                    new ExcelWriter().saveToExcel(filePath, dataRows);
                    writeLock.unlock();
                } else return false;

                long usageMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                endTime = System.currentTimeMillis();
                LOGGER.info("End address collect process.");
                LOGGER.info(String.format("\t\t\tSpend time: %s ms", formatter.format(endTime - startTime)));
                LOGGER.info(String.format("\t\t\tMemory usage: %s bytes", formatter.format(usageMemoryAfter - usageMemory)));
                return Boolean.TRUE;
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage() + "\n" + ex.getStackTrace()[0]);
                return Boolean.FALSE;
            }
        }
    }

    private static String Help() {
        return new StringBuilder()
                .append("\n Аргументы запуска:\n")
                .append("\n <filepath>(optional) (путь к файлу) \n")
                .append("\n <batchSize>(optional, default 10) (размер буфера считывания строк из файла)\n")
                .append("\n <startCell>(optional, default 1) (номер линии считывания потока url (для excel-файлов))\n")
                .toString();
    }

}
