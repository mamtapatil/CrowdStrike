# CrowdStrike

<h2>Take home project</h2>

This is a web service that displays the open ports for a given ipaddress/hostname.
<br>

<h3>HTTP Endpoint</h3>
  UI  - http://localhost:8080
<br>
  To get the history in JSON format - http://localhost:8080/getPortHistory.json?address=<input>

<h3>Prerequisites</h3>
<hr>

* Eclipse IDE
* Java 8
* JUnit5
* Gradle
* MySQL

<h3>Eclipse Instructions</h3>
<hr>

Import the project as a gradle project using the Githib repo link.<br> Right click on the project and click Java Build Path <br> Select the Libraries tab and click on Add Library button present on the left <br> Select JUnit and on the next box select JUnit5 and click finish.

The project can be run by follwoing two ways
* Right click on the EngineeringApplication.java present in com.crowdstrike.engineering package and selct Run As -> Java application. The console tab should display all the logs.
* Run this command in the terminal./gradlew bootRun

To run the JUnit test cases, right click on the AppControllerTest.java and select Run As -> JUnit test. The result would be displayed in the JUnit tab.

<h3>Database schema</h3>
<hr>
  
 <h4> Address </h4> <br>
   CREATE TABLE `address` (
  `address_id` int NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(40) NOT NULL,
  `added_date` date DEFAULT NULL,
  PRIMARY KEY (`address_id`))
 
  <h4> Port </h4> <br>
    CREATE TABLE `port` (
  `port_id` int NOT NULL AUTO_INCREMENT,
  `port_number` int NOT NULL,
  `protocol` varchar(40) NOT NULL,
  `service` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`port_id`))
  
   <h4> Scans </h4> <br>
  CREATE TABLE `scans` (
  `scan_history_id` int NOT NULL AUTO_INCREMENT,
  `address_id` int NOT NULL,
  `port_id` int DEFAULT NULL,
  `scan_id` int DEFAULT NULL,
  `added_date` datetime DEFAULT NULL,
  PRIMARY KEY (`scan_history_id`),
  KEY `FK_User` (`address_id`),
  KEY `FK_Port` (`port_id`),
  CONSTRAINT `FK_Port` FOREIGN KEY (`port_id`) REFERENCES `port` (`port_id`),
  CONSTRAINT `FK_User` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`))
  
<hr>
  
<h3>Docker image</h3>
mamtapatil/crowdstrike
  
<h3>Packaging Layout</h3>
<hr>
<pre>
├── HELP.md
├── README.md
├── bin
├── build.gradle
|── Dockerfile
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── resources
│   ├── application.properties
│   ├── static
│   └── templates
|       └──app.html       
├── settings.gradle
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── crowdStrike
│   │   │           └── engineering
│   │   │               ├── EngineeringApplication.java
│   │   │               ├── controller
│   │   │               │   └── AppController.java
|   |   |               ├── dao
│   │   │               │   └── AddressDao.java
│   │   │               │   └── OpenPortsDao.java
│   │   │               │   └── ScanHistoryDao.java
│   │   │               ├── exceptions
│   │   │               │   └── InvalidInputException.java
│   │   │               │   └── NMapException.java
|   |   |               ├── model
│   │   │               │   └── Address.java
│   │   │               │   └── NMaprun.java
│   │   │               │   └── Port.java
│   │   │               │   └── PortDisplayDTO.java
│   │   │               │   └── Scans.java
│   │   │               └── service
│   │   │                   └── AppService.java
│   │   └── resources
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       ├── static
|   |       |   └── css
|   |       |   └── js
│   │       └── templates
│   └── test
│       └── java
│           └── com
│               └── adobe
│                   └── aem
│                       ├── EngineeringApplicationTests.java
│                       └── AppControllerTest.java
</pre>


