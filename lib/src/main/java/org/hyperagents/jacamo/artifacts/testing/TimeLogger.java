package org.hyperagents.jacamo.artifacts.testing;

import cartago.Artifact;
import cartago.LINK;
import cartago.OPERATION;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TimeLogger extends Artifact {

  private static final String FILE_NAME = "signifier_log.xlsx";
  private Workbook workbook;
  private Sheet sheet;
  private long startTime;

  public void init() {
    if (Files.exists(Paths.get(FILE_NAME))) {
      try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
        workbook = new XSSFWorkbook(fis);
        sheet = workbook.getSheet("Log");
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      workbook = new XSSFWorkbook();
      sheet = workbook.createSheet("Log");
      Row headerRow = sheet.createRow(0);
      Cell headerCell1 = headerRow.createCell(0);
      headerCell1.setCellValue("Signifiers Num");
      Cell headerCell2 = headerRow.createCell(1);
      headerCell2.setCellValue("Time in Seconds");
    }
  }

  // Start the timer
  @LINK
  @OPERATION
  public void startTimer() {
    startTime = System.currentTimeMillis();
  }

  // Stop the timer and log the time interval
  @LINK
  @OPERATION
  // Stop the timer and log the time interval
  public void stopTimerAndLog(int signifiersNum) {
    long endTime = System.currentTimeMillis();
    long elapsedTimeInMillis = endTime - startTime;

    logTime(signifiersNum, elapsedTimeInMillis);
  }

  // Method to log time in the Excel sheet
  private void logTime(int signifiersNum, long elapsedTimeInMillis) {
    // Find the next empty row
    int rowNum = sheet.getLastRowNum() + 1;
    Row row = sheet.createRow(rowNum);

    // Write data to the row
    Cell cell1 = row.createCell(0);
    cell1.setCellValue(signifiersNum);

    Cell cell2 = row.createCell(1);
    cell2.setCellValue(elapsedTimeInMillis);

    // Write the output to the Excel file
    try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
      workbook.write(fos);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        workbook.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
