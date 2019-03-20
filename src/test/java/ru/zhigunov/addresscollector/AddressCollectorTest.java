package ru.zhigunov.addresscollector;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AddressCollectorTest {

    @Test
    public void testRun() throws Exception {
        new AddressCollector().run( new String[]{ "example.xlsx", "1", "D613"} );
    }
}