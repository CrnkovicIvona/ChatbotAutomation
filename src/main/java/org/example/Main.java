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

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

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

            String promptText = new String(Files.readAllBytes(Paths.get("C:\\Users\\ivona\\qa-rba-master\\src\\main\\java\\org\\example\\prompts.txt")));

            WebElement messageBox = wait.until((ExpectedConditions.visibilityOfElementLocated(By.className("ib-widget-message-input"))));
            messageBox.click();
            messageBox.sendKeys(promptText);

            lastReceivedMessage = promptText;

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

            String expectedResponse = new String(Files.readAllBytes(Paths.get("C:\\Users\\ivona\\qa-rba-master\\src\\main\\java\\org\\example\\expected_results.txt")));

            System.out.println("Last received message: " + lastReceivedMessage);
            System.out.println("Expected response: " + expectedResponse);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Test Report");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Prompt");
            headerRow.createCell(1).setCellValue("Last received message");
            headerRow.createCell(2).setCellValue("Result");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(promptText);
            dataRow.createCell(1).setCellValue(lastReceivedMessage);

            if(!lastReceivedMessage.trim().equals(expectedResponse.trim())) {
                System.out.println("The received message does not match the expected result.");
                dataRow.createCell(2).setCellValue("FAIL");

                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File screenshotLocation = new File("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot" + System.currentTimeMillis() + ".png");
                Files.copy(screenshot.toPath(), screenshotLocation.toPath());
            } else {
                System.out.println("The received message matches the expected result.");
                dataRow.createCell(2).setCellValue("PASS");
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
