package org.example;
import dev.failsafe.internal.util.Durations;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.devtools.v119.network.model.WebSocketFrame;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.devtools.DevTools;

import java.util.Base64;
import java.util.Optional;

public class Main {
    private static void handleWebSocketFrame(Object webSocketFrameObject) {
        if (webSocketFrameObject instanceof WebSocketFrame) {
            WebSocketFrame webSocketFrame = (WebSocketFrame) webSocketFrameObject;

            byte[] payload = Base64.getDecoder().decode(webSocketFrame.getPayloadData());
            String decodedPayload = new String(payload);

            // Process the WebSocket message
            System.out.println("Received WebSocket message: " + decodedPayload);
            // Add your code to display or process the received WebSocket message here
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
                handleWebSocketFrame(webSocketFrameReceived));


        try {
            // Specify the URL you want to visit
            String url = "https://www.rba.hr/korisne-informacije/rba-chatbot";
            // Set up a listener to handle WebSocket frames

            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            WebElement messagingButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ib-button-messaging")));

            messagingButton.click();
            WebElement agree = wait.until((ExpectedConditions.visibilityOfElementLocated((By.id(("chat-overlay-i-agree"))))));
            agree.click();
            WebElement iframe = driver.findElement(By.id("ib-iframe-messaging"));
            driver.switchTo().frame(iframe);
           WebElement messageBox = wait.until((ExpectedConditions.visibilityOfElementLocated(By.className("ib-widget-message-input"))));
           messageBox.click();
           messageBox.sendKeys("testing 123");

            WebElement sendMessage =  driver.findElement(By.cssSelector("button[data-testid='input-submit']"));
            sendMessage.click();

            String cssSelector = ".ib-widget-agent-messages-section";

            int prevElementsCount = 0, currentCount = 0;

            Thread.sleep(10000);
            // Wait for the elements to be present on the page
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(cssSelector)));

            // Get a list of all matching elements
            java.util.List<WebElement> elements = driver.findElements(By.cssSelector(cssSelector));

            for(WebElement elem: elements){
                System.out.println(elem.getText());
            }
        }catch (InterruptedException e) {
            // Handle the exception
            e.printStackTrace();
        }  finally {
            // Close the WebDriver
            driver.quit();
        }
    }
}