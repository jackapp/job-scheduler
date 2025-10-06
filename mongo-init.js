// Switch to admin to create the new user and DB
db = db.getSiblingDB('admin');

// Create the 'job-scheduler' database
const dbName = 'job-scheduler';
const appUser = 'app_user';
const appPass = 'Uu7gYX8TiuaO0XDL';

// Create the user with readWrite role on job-scheduler
db.createUser({
  user: appUser,
  pwd: appPass,
  roles: [
    { role: 'readWrite', db: dbName }
  ]
});

print(`✅ User '${appUser}' created with readWrite on '${dbName}'`);

// Switch to job-scheduler DB
db = db.getSiblingDB(dbName);

// Create collections
const collections = ['producer_config', 'jobs', 'job_executions'];
collections.forEach(name => {
  db.createCollection(name);
  print(`📁 Created collection: ${name}`);
});

// Insert initial config into producer_config
db.producer_config.insertOne({
  _id: "1",
  configId: "1",
  lastProducedTimestamp: 1728058611000
});

print("🚀 Inserted initial document into producer_config");

// Confirmation
print("🎯 Database initialization complete!");

