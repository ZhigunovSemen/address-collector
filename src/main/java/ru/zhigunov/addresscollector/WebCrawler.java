package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.zhigunov.addresscollector.dictionary.CityDictionary;
import ru.zhigunov.addresscollector.dto.DataRow;
import sun.swing.StringUIClientPropertyKey;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class WebCrawler {

    private static Logger LOGGER = LogManager.getLogger(UrlExctractor.class);

    private List<DataRow> rowsList;
    private CityDictionary cityDictionary;

    public WebCrawler(List<DataRow> rowsList) {
        this.rowsList = rowsList;
        try {
            cityDictionary = new CityDictionary("city.txt");
        } catch (IOException ex) {
            LOGGER.error("Error when loading city dictionary", ex);
        }
    }

    public String extractCity(String url) {
        return "";
    }

    public void fillCity(DataRow dataRow) {
        String stringUrl = dataRow.getDomain();
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getURL();
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getLandingPage();

        URL url;
        try {
            url = new URL(stringUrl);
            Document doc = Jsoup.parse(url, 10000);
            doc.body().select(".Z0LcW").text();


        } catch (Exception ex) {
            LOGGER.error(ex);
        }


        new String("Z0LcW");
    }

    public void fillDataRows() {
        for (DataRow dataRow : rowsList) {
            fillCity(dataRow);
            break;
        }
    }

}
