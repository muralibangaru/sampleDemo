package com.ecm.alfresco.migration.util;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ecm.alfresco.migration.bean.document.DocumentItem;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Miguel Sanchez
 * @version 1.0
 *          2016-04-04
 */
public class ExcelUtil {

    private static final Logger log = Logger.getLogger(ExcelUtil.class);
    private static final String SUMMARY = "Summary";
    private static final String LIST = "List";

    /**
     * Extracts metadata form an excel file and generates a metadata list
     *
     * @param file         excel file
     * @param contentModel content model
     * @param firstRow     row where starts document list
     * @param dateFormat   date format
     * @return metadata list
     * @throws InvalidFormatException
     * @throws IOException
     */
    public static List<Map<String, String>> getMetadataList(File file, int sheetIndex, Map<String, String> contentModel, int firstRow, String dateFormat, String metadataFileNameKey, String sheetNameKey) throws InvalidFormatException, IOException {
        List<Map<String, String>> listOfMetadata = new ArrayList<Map<String, String>>();
        log.debug("Metadata Extraction started");

        InputStream in = new FileInputStream(file);
        Workbook wb = WorkbookFactory.create(in);

        // All Excel indexes start from 0.
        Sheet sheet = wb.getSheetAt(0); //get sheet 1
        int totalRows = sheet.getLastRowNum() + 1;
        log.debug("Document for metadata extraction: " + totalRows);

        int column;
        boolean addDocumentToTheList;
        // Ignore row with index = 0 viz. first row, start processing from second row.
        for (int rowNum = firstRow; rowNum < totalRows; rowNum++) {
            Row row = sheet.getRow(rowNum);

            if (row != null) {
                Map<String, String> metadataNameAndValue = new HashMap<String, String>();
                metadataNameAndValue.put(metadataFileNameKey, file.getName()); // add metadata file name
                metadataNameAndValue.put(sheetNameKey, getSheetName(file, sheetIndex)); // add sheet name (metadata file name) to add later the report information to the right workbook sheet
                addDocumentToTheList = false;
                column = 0;

                while (true) {
                    String propertyName = contentModel.get("col" + (column + 1));
                    if (propertyName == null)
                        break;

                    if (!propertyName.equalsIgnoreCase("ignore")) {
                        String propertyValue = getCellValue(row, column, propertyName, dateFormat);
                        if (propertyValue != null && !propertyValue.isEmpty())
                            addDocumentToTheList = true;

                        metadataNameAndValue.put(propertyName, propertyValue);

                    } else {
                        log.trace("Row: " + rowNum + ", Column: " + column + ", IGNORED ");
                    }

                    column++;
                }

                if (addDocumentToTheList) {
                    listOfMetadata.add(metadataNameAndValue);
                    log.trace("Row " + rowNum + " ADDED");

                } else {
                    log.trace("Row " + rowNum + " is empty. SKIPPED");
                }

            } else {
                log.trace("Row " + rowNum + " is null");
            }
        }

        log.debug("Extraction complete. " + listOfMetadata.size() + " documents added to the DocumentList");
        return listOfMetadata;
    }

    /**
     * Gets a sheet's name by the index
     * @param file
     * @param sheetIndex
     * @return
     */
    private static String getSheetName(File file, int sheetIndex) {
        String metadataFileName = sheetIndex + "-" + file.getName();

        if (metadataFileName.length() > 31) { // sheet names in Excel can be 31 characters max length
            metadataFileName = metadataFileName.substring(0, 30);
        }

        return metadataFileName;
    }

    /**
     * Gets cell value
     *
     * @param row          row
     * @param column       column
     * @param propertyName property name
     * @param dateFormat   date format
     * @return cell value
     */
    private static String getCellValue(Row row, int column, String propertyName, String dateFormat) {
        String value = null;
        String type = null;

        if (row != null) {
            Cell cell = row.getCell(column);

            if (cell != null) {
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        type = "date";
                        Date date = cell.getDateCellValue();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                        value = simpleDateFormat.format(date);

                    } else {
                        type = "numeric";
                        double numericValue = (double) cell.getNumericCellValue();
                        value = numericValue + "";
                    }
                } else {
                    type = "text";
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    value = cell.getStringCellValue().trim();
                }
            }
        }

        log.trace("Row: " + row.getRowNum() + ", Column: " + column + ", Type: " + type + ", Property: " + propertyName + ", Value: " + value);
        return value;
    }

    /**
     * Saves a workbook in a specific path
     * @param wb
     * @param path
     * @throws IOException
     */
    public static void saveWorkbook(Workbook wb, String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        wb.write(fileOut);
        fileOut.close();
    }

    /**
     * Creates a workbook
     * @return
     */
    public static Workbook createWorkBook() {
        Workbook wb = new XSSFWorkbook();
        wb.createSheet(LIST);
        wb.createSheet(SUMMARY);
        return wb;
    }

    /**
     * Adds migration information in a row
     * @param wb
     * @param dateTime
     * @param sourceStatus
     * @param sourceDestinationFolder
     * @param documentItem
     */
    public static synchronized void addMigrationRow(Workbook wb, String dateTime, String sourceStatus, String sourceDestinationFolder, DocumentItem documentItem) {
        Sheet sheet = wb.getSheet(LIST);
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        row.createCell(0).setCellValue(dateTime);
        row.createCell(1).setCellValue(documentItem.getFilename());
        row.createCell(2).setCellValue(sourceStatus);
        row.createCell(3).setCellValue(sourceDestinationFolder);
        row.createCell(4).setCellValue(documentItem.getSourceNodeRef());
        row.createCell(5).setCellValue(documentItem.getTargetNodeRef());
        row.createCell(6).setCellValue(documentItem.getTargetDestinationFolder());
        row.createCell(7).setCellValue(documentItem.getStatus());
        row.createCell(8).setCellValue(documentItem.getMessage());
    }

    /**
     * Adds migration information error in a row
     * @param wb
     * @param dateTime
     * @param sourceStatus
     * @param sourceDestinationFolder
     * @param documentItem
     */
    public static synchronized void addErrorRow(Workbook wb, String dateTime, String sourceStatus, String sourceDestinationFolder, DocumentItem documentItem) {
        Sheet sheet = wb.getSheet(LIST);
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        row.createCell(0).setCellValue(dateTime);
        row.createCell(1).setCellValue(documentItem.getFilename());
        row.createCell(2).setCellValue(sourceStatus);
        row.createCell(3).setCellValue(sourceDestinationFolder);
        row.createCell(4).setCellValue(documentItem.getSourceNodeRef());
        row.createCell(5).setCellValue(documentItem.getTargetDestinationFolder());
        row.createCell(6).setCellValue(documentItem.getMessage());
    }

    /**
     * Adds the migration summary to the report
     * @param wb
     * @param value
     */
    public static void addSummaryRow(Workbook wb, String value) {
        Sheet sheet = wb.getSheet(SUMMARY);
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        row.createCell(0).setCellValue(value);
    }

}
