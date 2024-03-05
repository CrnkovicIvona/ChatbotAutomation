package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.devtools.v119.network.model.WebSocketFrame;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.devtools.DevTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class Main {
    private static String lastReceivedMessage = "";

    private static void handleWebSocketFrame(WebDriver driver, Object webSocketFrameObject) {
        if (webSocketFrameObject instanceof WebSocketFrame) {
            WebSocketFrame webSocketFrame = (WebSocketFrame) webSocketFrameObject;

            byte[] payload = Base64.getDecoder().decode(webSocketFrame.getPayloadData());
            String decodedPayload = new String(payload);

            System.out.println("Received WebSocket message: " + decodedPayload);
            lastReceivedMessage = decodedPayload;

            System.out.println("Updated lastReceivedMessage: " + lastReceivedMessage);
        }
    }

    public static List<Map<String, String>> readExcelData(String filePath) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String cellValue = cell != null ? cell.toString() : "";
                    rowData.put(headerRow.getCell(j).toString(), cellValue);
                }
                data.add(rowData);
            }
        }

        return data;
    }

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        DevTools devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));

        devTools.addListener(Network.dataReceived(), webSocketFrameReceived ->
                handleWebSocketFrame(driver, webSocketFrameReceived));

        try {
            String url = "https://www.rba.hr/korisne-informacije/rba-chatbot";

            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            WebElement messagingButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ib-button-messaging")));

            messagingButton.click();
            WebElement agree = wait.until((ExpectedConditions.visibilityOfElementLocated((By.id(("chat-overlay-i-agree"))))));
            agree.click();
            WebElement iframe = driver.findElement(By.id("ib-iframe-messaging"));
            driver.switchTo().frame(iframe);

            List<Map<String, String>> data = readExcelData("C:\\Users\\ivona\\qa-rba-master\\resources\\TestData.xlsx");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Test Report");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Prompt");
            headerRow.createCell(2).setCellValue("Last received message");
            headerRow.createCell(3).setCellValue("Result");

            int currentRow = 1;

            for (Map<String, String> row : data) {
                String promptText = row.get("PROMPT");

                WebElement messageBox = wait.until((ExpectedConditions.visibilityOfElementLocated(By.className("lc-footer-message-input"))));
                messageBox.click();
                messageBox.sendKeys(promptText);

                WebElement sendMessage =  driver.findElement(By.cssSelector("button[data-testid='input-submit']"));
                sendMessage.click();

                String cssSelector = ".ib-widget-message-bubble.agent";

                Thread.sleep(10000);
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(cssSelector)));

                java.util.List<WebElement> elements = driver.findElements(By.cssSelector(cssSelector));

                for(WebElement elem: elements){
                    System.out.println(elem.getText());
                }

                lastReceivedMessage = elements.get(elements.size() - 1).getText().replace("R\nRBHR\n", "");

                String expectedResponse = row.get("EXPECTED_RESULT");

                System.out.println("Expected response: " + expectedResponse);

                Row dataRow = sheet.createRow(currentRow++);

                String id = row.get("ID");

                dataRow.createCell(0).setCellValue(id);
                dataRow.createCell(1).setCellValue(promptText);
                dataRow.createCell(2).setCellValue(lastReceivedMessage);

                if(!lastReceivedMessage.trim().equals(expectedResponse.trim())) {
                    System.out.println("The received message does not match the expected result.");
                    dataRow.createCell(3).setCellValue("FAIL");

                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    File screenshotLocation = new File("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot" + System.currentTimeMillis() + ".png");
                    Files.copy(screenshot.toPath(), screenshotLocation.toPath());
                } else {
                    System.out.println("The received message matches the expected result.");
                    dataRow.createCell(3).setCellValue("PASS");
                }
            }

            FileOutputStream fileOut = new FileOutputStream("C:\\Users\\ivona\\qa-rba-master\\reports\\test_report_" + System.currentTimeMillis() + ".xlsx");
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                Files.copy(screenshot.toPath(), Paths.get("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot" + System.currentTimeMillis() + ".png"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }  finally {
            driver.quit();
        }
    }
}