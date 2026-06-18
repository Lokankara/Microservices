# Module 1: Microservice architecture overview

## Table of contents

- [What to do](#what-to-do)
- [Sub-task 1: Resource Service](#sub-task-1-resource-service)
- [Sub-task 2: Resource Processor](#sub-task-2-resource-processor)
- [FAQ](#faq)

## What to do

This task involves enhancing an existing microservices architecture by modifying the current **Resource Service** and adding a new microservice called **Resource Processor**. The starting point for this work is the solution you implemented in the [Introduction to Microservices](https://git.epam.com/epm-cdp/global-java-foundation-program/java-courses/-/tree/main/introduction-to-microservices) program.

The main objectives are:

1. Make structural changes to the existing **Resource Service**.
2. Develop a new microservice called **Resource Processor**.

## Sub-task 1: Resource Service

For the **Resource Service**, you need to implement the following modifications:

1. **Use Cloud Storage**: Replace the current database storage for resource files with a cloud storage solution, such as an emulator (e.g., [S3 emulator](https://github.com/localstack/localstack)). The resource files were previously saved in the service database.

2. **Resource Tracking**: Update the underlying database to track the resource by storing its location in the cloud storage.

3. **Upload Process**: When a user uploads an MP3 file, the **Resource Service** should:
    - Store the original MP3 file in the cloud storage (or its emulation).
    - Save the file's location (i.e., the link in cloud storage) in the database.
    - Note: In the current module, the **Resource Service** should not call any other services during this process.

## Sub-task 2: Resource Processor

The **Resource Processor** microservice will be responsible for processing MP3 files in the upcoming modules. It will not have a web interface and, in the current module, should be implemented as a basic Spring Boot application with minimal configuration.

- **Initial Functionality**: The service should be able to extract metadata from an MP3 file for future use with the **Song Service** API. You can use an external library like [Apache Tika](https://www.tutorialspoint.com/tika/tika_extracting_mp3_files.htm) to handle the metadata extraction.

- **Basic Spring Boot Setup**: Implement the initial version of the service as a standard Spring Boot project.

The diagram below illustrates the overall microservice architecture:

<img src="images/microservice_architecture_overview.png" width="501" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

## FAQ

> **Q:** Are we going to reuse our own implementation of the Resource and Song services from the "Introduction to Microservices" course, or will there be common artifacts provided for everyone?

**A:** Yes, you must **reuse your own implementation**. The starting point for this work is the artifact (codebase) you created during the [Introduction to Microservices](https://git.epam.com/epm-cdp/global-java-foundation-program/java-courses/-/tree/main/introduction-to-microservices) program. You will continue to enhance that existing project by adding new features.

---

> **Q:** The task describes uploading new resources to **Cloud Storage**, but what about removing them? Is this out of scope?

**A:** No, you must still support resource deletion. In the **Introduction to Microservices** program, there was a clear business rule: each resource has a 1:1 relation with song metadata, so when you call `DELETE /resources`, the related metadata in Song Service must also be deleted.

In this module, this logic serves as a cleanup mechanism and must be **adapted** for the new storage architecture:

1. Remove the file from the **Cloud Storage** (S3 bucket).
2. Delete the record from the **Resource DB**.
3. Call **Song Service** to cascade-delete the related song metadata.