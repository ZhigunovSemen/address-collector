package ru.zhigunov.addresscollector;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.helper.Validate;
import ru.zhigunov.addresscollector.dto.DataRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extract urls from excel sources
 */
public class ExcelWriter {

    private static Logger LOGGER = LogManager.getLogger(ExcelWriter.class);


    /**
     * Save data to excel file
     * @param xlsPath
     * @param dataRows
     * @return
     * @throws Exception
     */
    public boolean saveToExcel(String xlsPath,
                                     Collection<DataRow> dataRows) throws Exception {
        Validate.notEmpty(xlsPath);

        File file = new File(xlsPath);
        if (!(file.isFile() && file.exists())) {
            throw new FileNotFoundException();
        }

        try (FileInputStream fip = new FileInputStream(file)) {
            XSSFWorkbook workbook = new XSSFWorkbook(fip);
            Sheet sheet = workbook.getSheetAt(0);

            CellReference cellReference;
            Cell cell;
            for (DataRow dataRow : dataRows) {
                Integer lineNumber = dataRow.getLineNumber();
                if (lineNumber == null || lineNumber == 0) continue;

                Row row = sheet.getRow(lineNumber);
                if (row == null) continue;

                String city = dataRow.getCity();
                if (StringUtils.isNotBlank(city)) {
                    cellReference = new CellReference("B" + lineNumber);
                    cell = row.getCell(cellReference.getCol());
                    if (cell == null) {
                        cell = row.createCell(cellReference.getCol());
                    }
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    cell.setCellValue(city);
                }

                String source = dataRow.getSource();
                if (StringUtils.isNotBlank(source)) {
                    cellReference = new CellReference("F" + lineNumber);
                    cell = row.getCell(cellReference.getCol());
                    if (cell == null) {
                        cell = row.createCell(cellReference.getCol());
                    }
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    cell.setCellValue(source);
                }
            }

            fip.close();
            //save file
            FileOutputStream fos = new FileOutputStream(xlsPath);
            workbook.write(fos);
            fos.close();

            LOGGER.info(String.format(" Save %d lines to %s", dataRows.size(), xlsPath));

        } catch (FileNotFoundException ex) {
            LOGGER.error("File does not exist or cannot be open: " + xlsPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("Error: ", ex);
        }
        return true;
    }


}
