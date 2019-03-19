package ru.zhigunov.addresscollector.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class DataRow {

    private Integer LineNumber;

    private String Advertiser;

    private String city;

    private String URL;

    private String LandingPage;

    private String Domain;

    /**
     * источник информации о городе
     */
    private String source;

    public Integer getLineNumber() {
        return LineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        LineNumber = lineNumber;
    }

    public String getAdvertiser() {
        return Advertiser;
    }

    public void setAdvertiser(String advertiser) {
        Advertiser = advertiser;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getLandingPage() {
        return LandingPage;
    }

    public void setLandingPage(String landingPage) {
        LandingPage = landingPage;
    }

    public String getDomain() {
        return Domain;
    }

    public void setDomain(String domain) {
        Domain = domain;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Row{");
        sb.append("LineNumber=").append(LineNumber);
        sb.append(", Advertiser='").append(Advertiser).append('\'');
        sb.append(", city='").append(city).append('\'');
        sb.append(", URL='").append(URL).append('\'');
        sb.append(", LandingPage='").append(LandingPage.subSequence(0, Math.min(LandingPage.length(),30))).append('\'');
        sb.append(", Domain='").append(Domain).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
