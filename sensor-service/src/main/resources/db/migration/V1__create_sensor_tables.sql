CREATE TABLE sensors (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL
);

CREATE TABLE sensor_readings (
    id UUID PRIMARY KEY,
    sensor_id UUID NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    value DECIMAL(10, 2) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);
