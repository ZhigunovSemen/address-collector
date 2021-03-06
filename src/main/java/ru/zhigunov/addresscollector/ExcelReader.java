package ru.zhigunov.addresscollector;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.helper.Validate;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract urls from excel sources
 */
public class ExcelReader {

    private static Logger LOGGER = LogManager.getLogger(ExcelReader.class);


    /**
     * Extract urls from Excel file
     * @param xlsPath
     * @param startLineNumber
     * @return
     * @throws Exception
     */
    public static List<DataRow> extractRowsFromXls(String xlsPath,
                                                   int loadQuantity,
                                                   int startLineNumber) throws Exception {
        Validate.notEmpty(xlsPath);
        List<DataRow> dataRows = new ArrayList<>();

        File file = new File(xlsPath);
        try (FileInputStream fip = new FileInputStream(file)) {
            if (!(file.isFile() && file.exists())) {
                throw new FileNotFoundException();
            }

//            Integer startLineNumber = Integer.valueOf(startPosition.replaceAll("\\D+",""));       // extract line number, ex. D2 -> 2

            XSSFWorkbook workbook = new XSSFWorkbook(fip);
            Sheet sheet = workbook.getSheetAt(0);

            CellReference cellReference;
            Cell cell;
            for (int lineNumber = startLineNumber; lineNumber < startLineNumber + loadQuantity; lineNumber++) {
                Row row = sheet.getRow(lineNumber);
                if (row == null) break;

                cellReference = new CellReference("A" + lineNumber);
                cell = row.getCell(cellReference.getCol());
                if (cell == null) continue;
                String Advertiser = extractCellValue(cell);

                cellReference = new CellReference("B" + lineNumber);
                cell = row.getCell(cellReference.getCol());
                String city = extractCellValue(cell);

                cellReference = new CellReference("C" + lineNumber);
                cell = row.getCell(cellReference.getCol());
                String URL = extractCellValue(cell);

                cellReference = new CellReference("D" + lineNumber);
                cell = row.getCell(cellReference.getCol());
                String landingPage = extractCellValue(cell);

                cellReference = new CellReference("E" + lineNumber);
                cell = row.getCell(cellReference.getCol());
                String domain = extractCellValue(cell);

                DataRow dataRow = new DataRow();
                dataRow.setLineNumber(lineNumber);
                dataRow.setAdvertiser(Advertiser);
                dataRow.setCity(city);
                dataRow.setURL(URL);
                dataRow.setLandingPage(landingPage);
                dataRow.setDomain(domain);

                dataRows.add(dataRow);

                LOGGER.info(String.format(" read from file line number: %s", dataRow.getLineNumber()));
            }
        } catch (FileNotFoundException ex) {
            LOGGER.error("File does not exist or cannot be open: " + xlsPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("Error: ", ex);
        }
        return dataRows;
    }



    private static String extractCellValue(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case Cell.CELL_TYPE_NUMERIC:
                    return String.valueOf(cell.getNumericCellValue());
                case Cell.CELL_TYPE_STRING:
                    return cell.getStringCellValue();
            }
        }
        return null;
    }

}
