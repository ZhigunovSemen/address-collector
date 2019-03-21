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
import ru.zhigunov.addresscollector.dictionary.HostingProviderDictionary;
import ru.zhigunov.addresscollector.dictionary.PhoneCodeDictionary;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Класс, заполняющий адрес
 */
public class WebCrawler {

    private static Logger LOGGER = LogManager.getLogger(WebCrawler.class);

    private static final String[] SEARCH_CONTACTS_SET = {"контакт", "о компании"};
    private static final String[] SEARCH_ADDRESS_SET = {"г.", "адрес", "офис:"};

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
        }
    }

    /**
     * Записываем город в итоговый набор данных, и в коллекцию известных доменов {@link DomainDictionary}
     * @param dataRow
     */
    public void fillCity(DataRow dataRow) {
        String domain = dataRow.getDomain();
        if (StringUtils.isBlank(domain)) return;
        String stringUrl = domain;
        if (DomainDictionary.getCities().containsKey(domain)) {
            dataRow.setCity(DomainDictionary.getCities().get(domain));
            dataRow.setSource("founded before");
            return;
        }
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getURL();
        if (StringUtils.isBlank(stringUrl)) stringUrl = dataRow.getLandingPage();
        if (stringUrl.equalsIgnoreCase("n/a")) return;

        if (!stringUrl.startsWith("http://")) stringUrl = "http://" + stringUrl;
        stringUrl = removeSpecialChars(stringUrl);

        String city = extractCity(stringUrl, dataRow);

        if (StringUtils.isBlank(city)) {
            LOGGER.error(String.format("Не найден город для домена %s (line %d)", domain, dataRow.getLineNumber()));
            DomainDictionary.getCities().put(domain, "");
            return;
        }

        LOGGER.info(String.format("Найден город %s для домена %s (line %d)", city, domain, dataRow.getLineNumber()));
        DomainDictionary.getCities().put(domain, city);
        dataRow.setCity(city);
    }


    /**
     * Достаем содержимое страницы по {@link WebCrawler#extractCity#baseUrlString} <br>
     *     и записываем комментарий откуа вытащили в {@link WebCrawler#extractCity#dataRow}.
     * @param baseUrlString
     * @param dataRow
     * @return
     */
    public String extractCity(String baseUrlString, DataRow dataRow) {
        URL baseUrl;
        String address;
        try {
            baseUrl = new URL(baseUrlString);
            Document doc = tryToOpenContactUrl(baseUrlString, 10000);
            // проплачен ли хостинг?
            if (checkForHostingRedirect(doc)) {
                dataRow.setSource("redirect to hosting provider");
                LOGGER.debug(dataRow.getDomain() + " can not open page - redirecting to hosting provider");
                return null;
            }

            // если не нашли адрес, ищем вкладку контакты и в ней адрес, затем телефон
            Document contacDocument = findAndOpenContactPage(doc, baseUrlString);
            if (contacDocument != null) {
                address = serchByAddress(contacDocument, dataRow);
                if (address != null) return address;

                address = serchByPhone(contacDocument, dataRow);
                if (address != null) return address;
            }
            address = serchByAddress(doc, dataRow);
            if (address != null) return address;

            // если не нашли в контактах, ещё раз ищем по телефону на основной странице
            address = serchByPhone(doc, dataRow);
            if (address != null) return address;
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
//        new String("Z0LcW");
        return null;
    }

    /**
     * Ищем всех кандидатов на вкладку "контакты" и осуществляем переход на страницу контактов
     * @param doc
     */
    private Document findAndOpenContactPage(Document doc, String baseUrlString) {
        String contactUrlString = null;
        Document contactsDoc = null;
        for (Element contactsElementLink : doc.body().select("a")) {
            for (String contactSearch : SEARCH_CONTACTS_SET) {
                if (contactsElementLink.text().trim().contains(contactSearch)) {
                    contactUrlString = contactsElementLink.attr("href");
                    if (StringUtils.isBlank(contactUrlString)) {
                        continue;
                    }
                    contactsDoc = tryToOpenContactUrl(contactUrlString, 5000);
                    if (contactsDoc == null) {
                        contactsDoc = tryToOpenContactUrl(removeSpecialChars(doc.location()) + contactUrlString, 5000);
                    }
                    if (contactsDoc == null) {
                        contactsDoc = tryToOpenContactUrl(removeSpecialChars(baseUrlString) + contactUrlString, 5000);
                    }
                    if (null != contactsDoc) return contactsDoc;
                }
            }
        }
        LOGGER.error("Не найдено вкладки \"Контакты\".");
        return null;
    }

    private static Document tryToOpenContactUrl(String urlString, int timeout) {
        Document contactsDoc = null;
        try {
            URL contactsUrl = new URL(urlString);
            return Jsoup.connect(urlString).timeout(timeout).ignoreHttpErrors(true).validateTLSCertificates(false).get();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Выбираем все элементы на странице, которые содержат 'адрес', и пытаемся найти город
     * @param doc
     * @return
     */
    private static String serchByAddress(Document doc, DataRow dataRow) {
        // Выбираем все элементы на странице, которые содержат 'адрес'
        for (String addressSearch : SEARCH_ADDRESS_SET) {

            String delimeterText = addressSearch
                    .replace("+", "\\+")
                    .replace(".", "\\.")
                        .replace("(", "\\(");
            String[] adressDivs = doc.body().text().toLowerCase().split(delimeterText);

            int i=0;
            for (String el : adressDivs) {
                i++;
                if (i==1 || el.length() < 3) {    // пропускаем все, что ДО разделителя
                    continue;
                }
                // разбиваем все на разделители
                String elementWithAddress = el;
                for (String afterAddress : elementWithAddress.toLowerCase().split(delimeterText)) {

                    if (afterAddress.startsWith(":")) {
                        afterAddress = afterAddress.substring(1);
                    }
                    // разбиваем адрес на ',' и пытаемся найти слова, содержащиеся в справочнике городов
                    for (String elementWord : afterAddress.split(",", 10)) {
                        if (elementWord.length() > 3 && CityDictionary.getCities().contains(elementWord.trim())) {
                            dataRow.setSource(afterAddress.substring(0, Math.min(afterAddress.length(), 40)));
                            return capitalizeCity(elementWord.trim());
                        }
                    }
                    // Если не нашли через запятую, пытаемся через пробел (совсем крайний случай)
                    for (String elementWord : afterAddress.split(" ", 10)) {
                        if (elementWord.length() > 3 && CityDictionary.getCities().contains(elementWord.trim())) {
                            dataRow.setSource(afterAddress.substring(0, Math.min(afterAddress.length(), 40)));
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
    private String serchByPhone(Document doc, DataRow dataRow) {
        if (doc == null || doc.body() == null || StringUtils.isBlank(doc.body().text())) return null;

        for (String searchBy : SEARCH_PHONE_SET) {
            Elements phoneDivs = doc.body().getElementsContainingText(searchBy);
            if (phoneDivs != null && !phoneDivs.isEmpty()) {
                String delimeterText = searchBy
                        .replace("+", "\\+")
                        .replace(".", "\\.")
                        .replace("(", "\\(");

                for (Element el : phoneDivs) {
                    // разбиваем все на разделители
                    String elementWithPhone = el.text();
                    int i=0;
                    for (String phoneSubString : elementWithPhone.toLowerCase().split(delimeterText)) {
                        i++;
                        if (i==1) {    // пропускаем все, что ДО раззделителя
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
                            dataRow.setSource(phoneSubString.substring(0, Math.min(phoneSubString.length(), 40)));
                            return PhoneCodeDictionary.getPhoneMap().get(phoneText);
                        }
                        phoneText = phoneSubString.substring(0, 4);
                        if (NumberUtils.isDigits(phoneText) && PhoneCodeDictionary.getPhoneMap().containsKey(phoneText)) {
                            dataRow.setSource(phoneSubString.substring(0, Math.min(phoneSubString.length(), 40)));
                            return PhoneCodeDictionary.getPhoneMap().get(phoneText);
                        }
                    }
                }
            }
        }

        return null;
    }


    /**
     * Проверка, не переброшены ли мы на страницу хостинг-провайдера
     * @param doc
     * @return
     */
    private static boolean checkForHostingRedirect(Document doc) {
        String location = doc.location();
        try {
            URL url = new URL(location);
            LOGGER.info(url.getHost());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        if (StringUtils.isBlank(location)) return false;
        if (location.startsWith("http://")) location = location.substring(7);
        if (location.startsWith("https://")) location = location.substring(8);
        if (location.indexOf("/") > 0) {
            location = location.substring(0, location.indexOf("/"));
        }
        String[] urlParts = location.split("\\.");
        if (urlParts.length < 2) return false;
        String mainUrlAddress = urlParts[urlParts.length-2] + "." + urlParts[urlParts.length-1];
        return HostingProviderDictionary.getProviders().contains(mainUrlAddress);
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
