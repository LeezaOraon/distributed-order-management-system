-- create databases for each services
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE order_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO admin;