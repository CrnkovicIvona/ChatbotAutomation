package org.example;
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

public class Main {
    private static String lastReceivedMessage = "";

    private static void handleWebSocketFrame(WebDriver driver, Object webSocketFrameObject) {
        if (webSocketFrameObject instanceof WebSocketFrame) {
            WebSocketFrame webSocketFrame = (WebSocketFrame) webSocketFrameObject;

            byte[] payload = Base64.getDecoder().decode(webSocketFrame.getPayloadData());
            String decodedPayload = new String(payload);

            // Process the WebSocket message
            System.out.println("Received WebSocket message: " + decodedPayload);
            lastReceivedMessage = decodedPayload;

            // Print out the updated lastReceivedMessage
            System.out.println("Updated lastReceivedMessage: " + lastReceivedMessage);
        }
    }

    public static void main(String[] args) {
        // Set the path to the ChromeDriver executable (download it from https://sites.google.com/chromium.org/driver/)
        // Create a new instance of the ChromeDriver
        WebDriver driver = new ChromeDriver();
        DevTools devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));

        // Set up a listener to handle WebSocket frames
        devTools.addListener(Network.dataReceived(), webSocketFrameReceived ->
                handleWebSocketFrame(driver, webSocketFrameReceived));

        try {
            // Specify the URL you want to visit
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
            // Use promptText instead of hardcoded string
            messageBox.click();
            messageBox.sendKeys(promptText);

            WebElement sendMessage =  driver.findElement(By.cssSelector("button[data-testid='input-submit']"));
            sendMessage.click();

            String cssSelector = ".ib-widget-agent-messages-section";

            Thread.sleep(10000);
            // Wait for the elements to be present on the page
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(cssSelector)));

            // Get a list of all matching elements
            java.util.List<WebElement> elements = driver.findElements(By.cssSelector(cssSelector));

            for(WebElement elem: elements){
                System.out.println(elem.getText());
            }

            // Compare the last received message with the expected result
            String expectedResponse = new String(Files.readAllBytes(Paths.get("C:\\Users\\ivona\\qa-rba-master\\src\\main\\java\\org\\example\\expected_results.txt")));

// Print out the messages that are being compared
            System.out.println("Last received message: " + lastReceivedMessage);
            System.out.println("Expected response: " + expectedResponse);

            if(!lastReceivedMessage.equals(expectedResponse)) {
                System.out.println("The received message does not match the expected result.");
                // Take a screenshot
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File screenshotLocation = new File("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot.png");
                if(screenshotLocation.exists()) {
                    // If the file already exists, append a timestamp to the filename
                    screenshotLocation = new File("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot" + System.currentTimeMillis() + ".png");
                }
                Files.copy(screenshot.toPath(), screenshotLocation.toPath());
            }

        } catch (InterruptedException | IOException e) {
            // Handle the exception
            e.printStackTrace();
            // Take a screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                Files.copy(screenshot.toPath(), Paths.get("C:\\Users\\ivona\\qa-rba-master\\screenshots\\screenshot.png"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }  finally {
            // Close the WebDriver
            driver.quit();
        }
    }
}
