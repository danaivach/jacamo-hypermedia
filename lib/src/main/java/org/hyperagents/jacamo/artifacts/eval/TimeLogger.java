package eval;

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

  private Workbook workbook;
  private Sheet sheet;
  private long startTime;
  private int signifiersNum;
  private String fileName;

  public void init(int signifiersNum, String fileName) {
    this.signifiersNum = signifiersNum;
    this.fileName = fileName + ".xlsx";

    if (Files.exists(Paths.get(fileName))) {
      try (FileInputStream fis = new FileInputStream(fileName)) {
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
      headerCell1.setCellValue("signifiers_num");
      Cell headerCell2 = headerRow.createCell(1);
      headerCell2.setCellValue("time_ms");
    }
  }

  // Start the timer
  @LINK
  @OPERATION
  public void setSignifiersNum(int signifiersNum) {
    this.signifiersNum = signifiersNum;
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
  public void stopTimerAndLog() {
    long endTime = System.currentTimeMillis();
    long elapsedTimeInMillis = endTime - startTime;

    logTime(elapsedTimeInMillis);
  }

  // Method to log time in the Excel sheet
  private void logTime(long elapsedTimeInMillis) {

    if (this.signifiersNum > 0) {
      // Find the next empty row
      int rowNum = sheet.getLastRowNum() + 1;
      Row row = sheet.createRow(rowNum);

      // Write data to the row
      Cell cell1 = row.createCell(0);
      cell1.setCellValue(signifiersNum);

      Cell cell2 = row.createCell(1);
      cell2.setCellValue(elapsedTimeInMillis);

      // Write the output to the Excel file
      try (FileOutputStream fos = new FileOutputStream(fileName)) {
        workbook.write(fos);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
