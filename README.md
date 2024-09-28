# Chatbot Automated Tester
This project provides a highly scalable script for automated testing of chatbots.

**Key Features:**
* **Dialogue Triggering**: Triggers dialogues with the chatbot based on test cases.
* **Test Reporting**: Generates reports with PASS, FAIL, and ERROR statuses.
* **Scalability:** Efficiently handles large volumes of test data.

**Functionality:**
* Reads test cases from an Excel file.
* Initiates separate chat sessions for each test case.
* Collects chatbot responses to each input message.
* Compares received responses with expected responses.
* Handles exceptions during test execution.
* Records results (ID, prompt text, received message, expected response, comparison outcome) in a new Excel file.
* Captures screenshots of the web interface in case of exceptions.
* Parallel Execution can reduce script execution time.

**Prerequisites:**
* Java Development Kit (JDK)
* Maven
* Selenium WebDriver
* ChromeDriver
* Apache POI
* Git
* IntelliJ IDEA

**Benefits:**
* Efficiently tests large volumes of chatbot interactions (demonstrated by handling over 1600 test cases).
* Provides clear documentation of chatbot performance through test results.
* Identifies potential chatbot issues for further analysis.

**Potential Use Cases:**
* Regression testing of chatbots after updates or changes.
* Identifying inconsistencies in chatbot responses.
* Validating chatbot behavior against predefined scenarios.

# Script Demo Video

This repository includes a demo video that showcases the execution and capabilities of the script. The video demonstrates:

- How to run the script
- Key features and functionalities

Watch the demo video to get a better understanding of how the script works and what it can do.

## License
This repository is licensed under the MIT License.
