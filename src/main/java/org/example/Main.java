package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
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
    private static StringBuilder lastReceivedMessage = new StringBuilder();
    private static long lastPromptTime = 0;

    private static void handleWebSocketFrame(WebDriver driver, Object webSocketFrameObject) {
        if (webSocketFrameObject instanceof WebSocketFrame) {
            WebSocketFrame webSocketFrame = (WebSocketFrame) webSocketFrameObject;

            byte[] payload = Base64.getDecoder().decode(webSocketFrame.getPayloadData());
            String decodedPayload = new String(payload);

            // Check the timestamp of the message
            long messageTime = System.currentTimeMillis(); // This line assumes that the message time is the current time

            // Only append the message if it was sent after the last prompt
            if (messageTime > lastPromptTime) {
                System.out.println("Received WebSocket message: " + decodedPayload);
                lastReceivedMessage.append(decodedPayload).append("\n");
                System.out.println("Updated lastReceivedMessage: " + lastReceivedMessage);
            }
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

    public static void main(String[] args) throws IOException {
        List<Map<String, String>> data = readExcelData("C:\\Users\\ivona\\qa-rba-master\\resources\\TestData.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Report");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Prompt");
        headerRow.createCell(2).setCellValue("Last received message");
        headerRow.createCell(3).setCellValue("Expected Result");
        headerRow.createCell(4).setCellValue("Result");

        int currentRow = 1;

        for (Map<String, String> row : data) {
            ChromeDriver driver = new ChromeDriver();
            DevTools devTools = driver.getDevTools();
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

                // Reset the lastReceivedMessage after the page loads and before entering the prompt
                lastReceivedMessage.setLength(0);

                String promptText = row.get("PROMPT");
                WebElement messageBox = wait.until((ExpectedConditions.visibilityOfElementLocated(By.className("lc-footer-message-input"))));
                messageBox.click();
                messageBox.sendKeys(promptText);
                WebElement sendMessage =  driver.findElement(By.cssSelector("button[data-testid='input-submit']"));
                sendMessage.click();

                // Record the time of the last prompt
                lastPromptTime = System.currentTimeMillis();

                // Wait for all messages to be received
                Thread.sleep(10000);

                // Get all messages
                java.util.List<WebElement> elements = driver.findElements(By.cssSelector(".ib-widget-message-bubble.agent"));
                for (WebElement element : elements) {
                    // Get the main message text
                    WebElement mainMessageText = element.findElement(By.cssSelector(".ib-widget-message-text"));
                    System.out.println("main message text: " + mainMessageText.getText());
                    lastReceivedMessage.append(mainMessageText.getText()).append("\n");
                }

                // Check if there is a bubble with buttons after the prompt
                java.util.List<WebElement> buttonBubbles = driver.findElements(By.cssSelector(".ib-cta-widget-bubble"));
                if (!buttonBubbles.isEmpty()) {
                    for (WebElement bubble : buttonBubbles) {
                        // Get the text above the buttons within the current bubble
                        try {
                            WebElement textAboveButtons = bubble.findElement(By.cssSelector(".ib-cta-widget-bubble__header-text"));
                            System.out.println("text above buttons: " + textAboveButtons.getText());
                            lastReceivedMessage.append(textAboveButtons.getText()).append("\n");
                        } catch (NoSuchElementException e) {
                            System.out.println("Element with CSS selector '.ib-cta-widget-bubble__header-text' is not present.");
                        }

                        // Get all buttons within the current bubble
                        java.util.List<WebElement> buttons = bubble.findElements(By.cssSelector(".ib-cta-widget-button"));
                        for(WebElement button:buttons){
                            System.out.println("button text: " + button.getText());
                            lastReceivedMessage.append(button.getText()).append("\n");
                        }
                    }
                }

                // Remove the specified text from lastReceivedMessage
                String unwantedText = "Dobar dan, moje ime je RBA Chatbot. Mogu Vam pružiti informacije o proizvodima koje nudi RBA.\n\nOdaberite proizvod koji Vas zanima :\nTekući račun\nKreditne kartice\nGotovinski kredit\nStambeni kredit";
                String receivedMessage = lastReceivedMessage.toString().replace(unwantedText, "");

                // Remove extra spaces and newlines
                receivedMessage = receivedMessage.replaceAll("\\s+", " ").trim();

                String expectedResponse = row.get("EXPECTED_RESULT");
                // Remove extra spaces and newlines from the expected response as well
                expectedResponse = expectedResponse.replaceAll("\\s+", " ").trim();

                System.out.println("Expected response: " + expectedResponse);
                Row dataRow = sheet.createRow(currentRow++);
                String id = row.get("ID");
                dataRow.createCell(0).setCellValue(id);
                dataRow.createCell(1).setCellValue(promptText);
                dataRow.createCell(2).setCellValue(receivedMessage);
                dataRow.createCell(3).setCellValue(expectedResponse);
                if(!receivedMessage.trim().equals(expectedResponse.trim())) {
                    System.out.println("The received message does not match the expected result.");
                    dataRow.createCell(4).setCellValue("FAIL");
                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    File screenshotLocation = new File("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot" + System.currentTimeMillis() + ".png");
                    Files.copy(screenshot.toPath(), screenshotLocation.toPath());
                } else {
                    System.out.println("The received message matches the expected result.");
                    dataRow.createCell(4).setCellValue("PASS");
                }
                lastReceivedMessage.setLength(0); // Reset the lastReceivedMessage for the next test case
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

        FileOutputStream fileOut = new FileOutputStream("C:\\Users\\ivona\\qa-rba-master\\reports\\test_report_" + System.currentTimeMillis() + ".xlsx");
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }
}
