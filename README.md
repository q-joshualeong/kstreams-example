# Kafka Streams Example

## Table of Contents

1. [Introduction to Kafka Streams Processor API](#introduction-to-kafka-streams-processor-api)
2. [Explanation of the Code](#explanation-of-the-code)
    - [Overview](#overview)
    - [Key Functionalities](#key-functionalities)
    - [Topology Diagram](#topology)
    - [Use Case](#use-case)
3. [Concepts Used in This Application](#concepts-used-in-this-application)
4. [Project Setup](#project-setup)
    - [Building the Project](#building-the-project)
    - [Running the Project](#running-the-project)
5. [Missing Components](#missing-components)


## Introduction to Kafka Streams Processor API

Apache Kafka Streams is a client library for building applications and microservices where the input and output data are stored in Kafka clusters. It combines the simplicity of writing and deploying standard Java and Scala applications on the client side with the benefits of Kafka's server-side cluster technology.

Kafka Streams simplifies the development of stream processing applications by providing a straightforward DSL for building robust and scalable stream processing applications in Java or Scala. It supports stateful and stateless processing, windowing, and time-based aggregations.

For more detailed information, refer to the official Kafka Streams documentation:
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)

## Explanation of the Code

This Kafka Streams Example project demonstrates a comprehensive use of the Kafka Streams API to process streaming data. The project is structured into several Scala files, each serving a specific purpose in the stream processing pipeline.

### Overview

- **Applications.scala, ApplicationDetails.scala, ImportedApplications.scala**: These files define case classes representing different types of data entities in our application. They are used to model the data that flows through our Kafka Streams application.
- **Types.scala**: This file defines type aliases and tuples used throughout the application, making the code more readable and maintainable.
- **DataProcessor.scala**: Contains the core processing logic. It defines a custom processor that joins data from different streams and manages state stores.
- **Implicits.scala**: Provides implicit SerDe (Serializer/Deserializer) functions for the data entities, enabling Kafka to serialize and deserialize these objects as they are sent to and received from topics.
- **package.scala**: Defines constants used throughout the application, such as topic names and state store names.
- **KafkaStreamJoinsExample.scala**: The main entry point of the application. It sets up the Kafka Streams topology, defining how different streams and tables are joined and processed.

### Key Functionalities

- **Stream-Table Joins**: The application demonstrates how to perform joins between KStreams and KTables, which is a common operation in stream processing to enrich a real-time data stream with more static data.
- **Stateful Processing**: The use of state stores in `DataProcessor.scala` illustrates how to maintain and update state within a Kafka Streams application.
- **Retries**: The application includes mechanisms to handle failed joins and retry processing, ensuring robustness in the face of data inconsistencies or operational issues.

### Topology
![topologies.png](diagrams%2Ftopologies.png)


### Use Case

The application simulates a scenario where application details are continuously streamed and need to be joined with corresponding application data. The result of this join is then processed and stored, demonstrating a typical use case in real-time data processing and aggregation.

This Kafka Streams Example serves as a comprehensive guide to understanding and implementing a Kafka Streams application using Scala, covering everything from basic setup to advanced stream processing techniques.


## Concepts Used in This Application

- **KStream and KTable**: Fundamental abstractions in Kafka Streams for representing real-time data streams and changelogs of state.
- **State Stores**: Local storage associated with stream processors for maintaining state.
- **Processor API**: Low-level API for stream processing, allowing more control over the processing logic.
- **Serdes**: Serialization and deserialization mechanisms used for converting data between Kafka's binary format and Java/Scala objects.
- **Topology**: The logical representation of the processing graph.

## Project Setup

### Building the Project

1. Ensure Scala and sbt are installed on your system.
2. Navigate to the project directory.
3. Run `./gradlew data-source-applications:shadowJar` to build fat jar. Output will be in `data-source-applications/build/libs/data-source-applications_2.12-0.1.0-all.jar`

### Running the Project

1. Start your Kafka environment.
2. Execute `create-topics.sh` to create the necessary Kafka topics.
3. Run `java -jar data-source-applications/build/libs/data-source-applications_2.12-0.1.0-all.jar` to start the Kafka Streams application.


## Missing components
This example is by no means production ready. It is lacking the following components.
1. Tests 
2. Containerisation (docker & helm charts for deployment on kubernetes/openshift)
3. Error handling (forwarding failures to error topics)
4. Dependency injection (spring boot)
5. Rocksdb as a backend for state stores (Highly recommended for production)