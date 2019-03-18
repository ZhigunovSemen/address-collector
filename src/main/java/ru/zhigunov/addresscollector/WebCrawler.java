package ru.zhigunov.addresscollector;

import ru.zhigunov.addresscollector.dto.DataRow;

import java.util.List;

public class WebCrawler {

    public List<DataRow> rowsList;

    public WebCrawler(List<DataRow> rowsList) {
        this.rowsList = rowsList;
    }

    public String extractCity(String url) {
        return "";
    }

    public void fillCity(DataRow dataRow) {
        "Z0LcW"
    }

    public void fillDataRows() {
        for (DataRow dataRow : rowsList) {
            fillCity(dataRow);
        }
    }

}
