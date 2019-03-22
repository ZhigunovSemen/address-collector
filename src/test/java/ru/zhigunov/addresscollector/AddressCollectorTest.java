package ru.zhigunov.addresscollector;

import jdk.nashorn.internal.codegen.CompilerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

@Test
public class AddressCollectorTest {

    private static Logger LOGGER = LogManager.getLogger(AddressCollectorTest.class);

//    private String[] testSet = {
//            "20000000000000000000",
//            "3.141592",
//            "   -42",
//            "-",
//            "2147483800",
//            "4193 with words",
//            "words and 987",
//            "-91283472332",
//            "+0000000000000005467897976578",
//            "",
//            "+0000000000000a005467897976578",
//            "+00000000000000054678+97976578"};

    @Test
    public void testRun() throws Exception {
        new AddressCollector(new String[]{ "example.xlsx" , "10", "2", "2000"}).run();
    }

}