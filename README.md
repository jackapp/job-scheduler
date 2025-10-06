# 🧭 Job Scheduler — Setup & Usage Guide

This document explains how to set up, configure, and run the **Job Scheduler** project locally using IntelliJ IDEA and Docker.

---

## 📦 1. Import the Project

**Repository Location:**  
[Google Drive Project Folder](https://drive.google.com/drive/folders/1-Y9R0hXFyJfl0cFqlyWWY_xg9xC7RnM_?usp=drive_link)

1. Download or clone the project from the above link.
2. Ensure that you have **IntelliJ IDEA** installed.
3. In IntelliJ → `File` → `Open` → select the downloaded project folder.
4. IntelliJ will automatically detect and import the project configuration.

---

## 🐳 2. Run Required Docker Containers

The project depends on the following services:

- **MongoDB**
- **Redis**
- **ElasticMQ (SQS-compatible message queue)**

These are defined in the `docker-compose.yml` file at the project root.

Run the following command to start them:

```bash
docker compose up -d
```

---

## ⚙️ 3. Initialize MongoDB

Once the containers are up, run the Mongo init script to create the required collections:

```bash
docker exec -i local-mongo mongosh -u root -p password --authenticationDatabase admin < mongo-init.js
```

This will create the following collections:

| Collection | Description |
|-------------|--------------|
| **Jobs** | Stores job metadata such as cron expressions and API details |
| **JobExecutions** | Tracks job execution records |
| **ProducerConfig** | Maintains the `lastProducedTimestamp` for job scheduling continuity |

---

## 📬 4. Create Local SQS Queue (ElasticMQ)

Create a local SQS-compatible queue with a visibility timeout of **120 seconds**:

```bash
aws --endpoint-url=http://localhost:9324 sqs create-queue   --queue-name local-job-queue   --attributes VisibilityTimeout=120
```

---

## 🧾 5. Verify Application Configuration

All configurations (MongoDB, Redis, and SQS) are defined in the project's `application.yaml` file.

> ⚠️ If you change container ports or credentials, ensure those updates are also reflected in `application.yaml`.

---

## 🚀 6. Run the Application

Once all services are up and configured, run the application from IntelliJ.

**IntelliJ Run Configuration:**

| Setting | Value |
|----------|--------|
| **Main Class** | `com.fampay.scheduler.api.JobSchedulerApplication` |
| **Classpath** | `scheduler-api` |
| **Java Version** | `21` |

> 💡 The app automatically picks up the `application.yaml` from the classpath.

---

## 🧪 7. Test via Swagger UI

Once the app is running, you can access the API documentation through Swagger:

🔗 **Swagger URL:**  
[http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

> ⚠️ The application supports **only Quartz-style cron expressions**.  
> Use [Quartz Cron Expression Generator](https://www.freeformatter.com/cron-expression-generator-quartz.html) to create one.

---

### ✅ Example API Calls

#### ➕ Create a Job

```bash
curl -w "Status: %{http_code}\n" -X 'POST'   'http://localhost:8080/job'   -H 'accept: */*'   -H 'Content-Type: application/json'   -d '{
    "schedule": "*/1 * * * * ? *",
    "api": {
      "url": "http://localhost:3000/mock-endpoint",
      "httpMethod": "POST",
      "payload": {
        "message": "job-scheduler"
      },
      "readTimeoutMs": 120000
    },
    "type": "ATLEAST_ONCE"
  }'
```

#### 📜 Get Last 10 Executions

```bash
curl -X 'GET'   'http://localhost:8080/job/job-executions/68e3785b5738d56e6fc3a007?limit=50'   -H 'accept: */*'
```

---

## 🧰 8. Useful Tools & URLs

### 🔹 ElasticMQ Admin UI
Inspect and manage queues:  
👉 [http://localhost:9325/](http://localhost:9325/)

### 🔹 MongoDB Compass
Use Compass or any MongoDB GUI to inspect data.

**Connection String:**
```
mongodb://root:password@localhost:27017/?authSource=admin
```

---

## 🧩 Summary

| Step | Description |
|------|--------------|
| 1 | Import the project in IntelliJ |
| 2 | Start required Docker containers |
| 3 | Initialize MongoDB collections |
| 4 | Create ElasticMQ SQS queue |
| 5 | Verify and update `application.yaml` if needed |
| 6 | Run the app from IntelliJ |
| 7 | Test APIs via Swagger or curl |
| 8 | Use ElasticMQ & MongoDB UIs for monitoring |

---

📘 **Author:** Fampay Scheduler Team  
🕓 **Java Version:** 21  
🧩 **Framework:** Spring Boot  
🧱 **Dependencies:** MongoDB • Redis • ElasticMQ
