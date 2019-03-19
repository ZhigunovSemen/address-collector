package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.zhigunov.addresscollector.dictionary.CityDictionary;
import ru.zhigunov.addresscollector.dictionary.DomainDictionary;
import ru.zhigunov.addresscollector.dictionary.PhoneCodeDictionary;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Класс, заполняющий адрес
 */
public class WebCrawler {

    private static Logger LOGGER = LogManager.getLogger(WebCrawler.class);

    private static final String[] SEARCH_ADDRESS_SET = {"адрес", "офис:"};

    private static final String[] SEARCH_PHONE_SET = {"+7", "тел.", "телефон:", "телефон", "8("};

    private List<DataRow> rowsList;

    public WebCrawler(List<DataRow> rowsList) {
        this.rowsList = rowsList;
    }

    /**
     * Заполнение строки
     *  - сначала проверяем, заполнен ли уже город в данной строке,
     *  - затем смотрим, был ли уже анализирован данный домен ранее
     */
    public void fillDataRows() {
        for (DataRow dataRow : rowsList) {
            fillCity(dataRow);
//            break;  // todo remove after test
        }
    }

    /**
     * Записываем город в итоговый набор данных, и в коллекцию известных доменов {@link DomainDictionary}
     * @param dataRow
     */
    public void fillCity(DataRow dataRow) {
        String domain = dataRow.getDomain();
        if (StringUtils.isNotBlank(domain) && DomainDictionary.getCities().containsKey(domain)) {
            dataRow.setCity(DomainDictionary.getCities().get(domain));
        }
        String stringUrl = dataRow.getDomain();
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getURL();
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getLandingPage();
//        stringUrl = "e-loco.ru";  // TODO remove after test
        if (!stringUrl.startsWith("http://")) stringUrl = "http://" + stringUrl;
        stringUrl = removeSpecialChars(stringUrl);

        String city = extractCity(stringUrl);
        if (city == null) {
            LOGGER.error("Не найден город для домена " + stringUrl);
            return;
        }
        DomainDictionary.getCities().put(domain, city);
        LOGGER.info("Найден город " + city + " для домена " + domain);
        dataRow.setCity(city);
    }

    public String extractCity(String baseUrlString) {
        URL baseUrl;
        String address;
        try {
            baseUrl = new URL(baseUrlString);
            Document doc = Jsoup.parse(baseUrl, 10000);

            address = serchByAddress(doc);
            if (address != null) return address;

            // если не нашли адрес, ищем вкладку контакты и в ней адрес, затем телефон
            Document contacDocument = findAndOpenContactPage(doc);
            if (contacDocument != null) {
                address = serchByAddress(contacDocument);
                if (address != null) return address;

                address = serchByPhone(contacDocument);
                if (address != null) return address;
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        new String("Z0LcW");
        return null;
    }

    /**
     * Ищем всех кандидатов на вкладку "контакты" и осуществляем переход на страницу контактов
     * @param doc
     */
    private Document findAndOpenContactPage(Document doc) {
        URL contactsUrl;
        String contactUrlString = null;
        Document contactsDoc;
        for (Element contactsElementLink : doc.body().select("a")) {
            if (contactsElementLink.text().trim().contains("Контакты")) {
                contactUrlString = contactsElementLink.attr("href");
                if (StringUtils.isBlank(contactUrlString)) {
                    continue;
                }

                try {
                    contactsUrl = new URL(contactUrlString);
                    contactsDoc = Jsoup.parse(contactsUrl, 5000);
                    return contactsDoc;
                } catch (MalformedURLException ex) {
                    try {
                        contactsUrl = new URL(doc.location() + contactUrlString);
                        contactsDoc = Jsoup.parse(contactsUrl, 5000);
                        return contactsDoc;
                    } catch (Exception e) {
                        LOGGER.error("Unable to open contacts link: " + contactUrlString);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unable to open contacts link: " + contactUrlString);
                }
            }
        }

        LOGGER.error("Не найдено вкладки \"Контакты\".");
        return null;
    }


    /**
     * Выбираем все элементы на странице, которые содержат 'адрес', и пытаемся найти город
     * @param doc
     * @return
     */
    private static String serchByAddress(Document doc) {
        // Выбираем все элементы на странице, которые содержат 'адрес'
        for (String addressSearch : SEARCH_ADDRESS_SET) {
            Elements adressDiv = doc.body().getElementsContainingText(addressSearch);

            for (Element el : adressDiv) {
                // разбиваем все на разделители
                String elementWithAddress = el.text();
                for (String afterAddress : elementWithAddress.toLowerCase().split(addressSearch)) {

                    if (afterAddress.startsWith(":")) {
                        afterAddress = afterAddress.substring(1);
                    }
                    // разбиваем адрес на ',' и пытаемся найти слова, содержащиеся в справочнике городов
                    for (String elementWord : afterAddress.split(",", 7)) {
                        if (elementWord.length() > 3 && CityDictionary.getCities().contains(elementWord.trim())) {
                            return capitalizeCity(elementWord.trim());
                        }
                    }
                    // Если не нашли через запятую, пытаемся через пробел (совсем крайний случай)
                    for (String elementWord : afterAddress.split(" ", 7)) {
                        if (elementWord.length() > 3 && CityDictionary.getCities().contains(elementWord.trim())) {
                            return capitalizeCity(elementWord.trim());
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * Пытаемся найти телефон, и по телефону определить по справочнику город
     * @param doc
     * @return
     */
    private String serchByPhone(Document doc) {
        for (String searchBy : SEARCH_PHONE_SET) {
            Elements phoneDivs = doc.body().getElementsContainingText(searchBy);
            if (phoneDivs != null && !phoneDivs.isEmpty()) {
                String delimeterText = searchBy
                        .replace("+", "\\+")
                        .replace(".", "\\.");

                for (Element el : phoneDivs) {
                    // разбиваем все на разделители
                    String elementWithPhone = el.text();
                    int i=0;
                    for (String phoneSubString : elementWithPhone.toLowerCase().split(delimeterText)) {
                        i++;
                        if (i==1) {    // пропускаем все, что ДО "телефон"а
                            continue;
                        }
                        phoneSubString = phoneSubString.replaceAll(" ", "");
                        if (phoneSubString.startsWith(":")) {
                            phoneSubString = phoneSubString.substring(1);
                        }
                        if (phoneSubString.startsWith("+7")) {
                            phoneSubString = phoneSubString.substring(2);
                        } else if (phoneSubString.startsWith("8")) {
                            phoneSubString = phoneSubString.substring(1);
                        }
                        if (phoneSubString.startsWith("(")) {
                            phoneSubString = phoneSubString.substring(1);
                        }

                        // После всех преобразований анализируем первые 3 или 4 цыфры - они должны совпадать с кодом города
                        // Если совпадает, значит город найден
                        String phoneText = phoneSubString.substring(0, 3);
                        if (NumberUtils.isDigits(phoneText) && PhoneCodeDictionary.getPhoneMap().containsKey(phoneText)) {
                            return PhoneCodeDictionary.getPhoneMap().get(phoneText);
                        }
                        phoneText = phoneSubString.substring(0, 4);
                        if (NumberUtils.isDigits(phoneText) && PhoneCodeDictionary.getPhoneMap().containsKey(phoneText)) {
                            return PhoneCodeDictionary.getPhoneMap().get(phoneText);
                        }
                    }
                }
            }
        }

        return null;
    }


    /**
     * Приводим город в достойный вид санкт-петербург -> Санкт-Петербург
     * @param cityName
     * @return
     */
    private static String capitalizeCity(String cityName) {
        StringBuilder returnValue = new StringBuilder();
        String[] parts = cityName.split("\\-");
        for (String part : parts) {
            if (returnValue.length() > 0) returnValue.append("-");
            returnValue.append(StringUtils.capitalize(part));
        }
        return returnValue.toString();
    }

    /**
     * очистка url от специальных символов в конце
     */
    private static char[] charsToRemove = "/\\.,? ".toCharArray();
    private String removeSpecialChars(String inputString) {
        char endChar = inputString.charAt(inputString.length()-1);
        for (char c : charsToRemove) {
            if (endChar == c) {
                return inputString.substring(0, inputString.length()-1);
            }
        }
        return inputString;
    }

}
