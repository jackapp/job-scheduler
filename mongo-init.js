
// Switch to the desired database
db = db.getSiblingDB('job-scheduler');

// Create application user with read/write permissions
db.createUser({
  user: "app_user",
  pwd: "Uu7gYX8TiuaO0XDL",
  roles: [
    {
      role: "readWrite",
      db: "job-scheduler"
    }
  ]
});

// Create collections
db.createCollection("jobs");
db.createCollection("job_executions");
db.createCollection("producer_config");

// Insert initial data into producer_config
db.producer_config.insertOne({
  _id: "1",
  configId: "1",
  lastProducedTimestamp: 1759740104824
});

