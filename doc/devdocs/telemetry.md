<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Telemetry System
The telemetry system is designed to collect and send various types of telemetry data from different parts of the application. Here's a high-level overview of how it works:  

## Telemetry Collectors
These are classes that implement the [`TelemetryCollector`](../../insight-brain-service/src/main/java/com/sonatype/insight/brain/telemetry/TelemetryCollector.java) interface. Each collector is responsible for gathering specific types of telemetry data. Examples include:  
* PropertiesTelemetryCollector: Collects configuration properties.
* RealmTelemetryCollector: Collects data related to realm configurations.
* TelemetryContainerRequestFilter: Collects data on REST endpoint usage.

## Telemetry Data
The data collected by each `TelemetryCollector` is encapsulated in instances of the `TelemetryData` class. This class includes attributes that describe the purpose of the telemetry and the actual data collected.  

### Telemetry Purpose
The purpose of the telemetry data is defined by the `purpose` attribute in the `TelemetryData` class. This attribute is used to categorize the data and provide context for its collection. See [`TelemetryPurpose`](https://github.com/sonatype/hosted-data-services/blob/main/insight-telemetry-model/src/main/java/com/sonatype/insight/telemetry/model/TelemetryPurpose.java) enum for the details. If a new purpose is needed, it should be added to the enum.

### Telemetry Data Attributes
[`TelemetryData`](https://github.com/sonatype/hosted-data-services/blob/main/insight-telemetry-model/src/main/java/com/sonatype/insight/telemetry/model/TelemetryData.java) defines the purpose of the telemetry data and the actual data collected. The attributes are generic name value pairs. Some attributes are obfuscated to protect sensitive information. Example values that are obfuscated include: `application_id`, `application.name`, `scan_id` and `owner_id`. To obfuscate a value, use the `TelemetryUtil#obfuscateIfAdvancedReportingDisabled()` or `TelemetryUtil#obfuscate()` methods. `HdsClientAnalytics#obfuscate()` is also widely used to obfuscate values.

For Integrated Enterprise Reporting it is required that the real owner id and real application id is included in telemetry. See `TelemetryUtil` for details.

## Cluster Telemetry
Some telemetry collectors, like [`RealmTelemetryCollector`](../../insight-brain-service/src/main/java/com/sonatype/insight/brain/telemetry/RealmTelemetryCollector.java), are marked for cluster telemetry by returning `true` in the `isClusterTelemetry` method. This indicates that the data they collect is relevant to the entire cluster.  

## Task Scheduling 
The [`ClusterTelemetryTask`](../../insight-brain-service/src/main/java/com/sonatype/insight/brain/telemetry/ClusterTelemetryTask.java) class is responsible for scheduling and executing the task of sending telemetry data. It collects data from all registered TelemetryCollector instances and sends it using a TelemetrySender.  

## Data Collection and Sending  
Each `TelemetryCollector` gathers its specific data and returns it as a `TelemetryData` object.
The `ClusterTelemetryTask` aggregates this data and ensures it is sent from one node in the cluster.
The [`TelemetrySender`](../../insight-brain-service/src/main/java/com/sonatype/insight/brain/telemetry/TelemetrySender.java) class is responsible for sending the data to the telemetry server.

## Initialization
The TelemetryManager class is responsible for initializing the telemetry system. It reads the configuration file, creates instances of the TelemetryCollectors, and registers them with the ClusterTelemetryTask.

The TelemetryManager also starts the ClusterTelemetryTask, which is responsible for periodically collecting and sending telemetry data.

## Usage
To use the telemetry system, you need to:
1. Create a new [TelemetryCollector](../../insight-brain-service/src/main/java/com/sonatype/insight/brain/telemetry/TelemetryCollector.java) class that implements the `TelemetryCollector` interface.
2. Ensure the new collector is annotated with `@Named` and `@Singleton`.
3. Write tests for the new collector.

You can test the new collector by running your server and triggering the `ClusterTelemetryTask` to run and verify it sends the new telemetry data.

## Conclusion
The telemetry system in the codebase provides a flexible and extensible way to collect and send telemetry data from different parts of the application. By creating custom TelemetryCollector classes and registering them with the TelemetryManager, you can gather specific types of data and send it to a telemetry server for analysis.