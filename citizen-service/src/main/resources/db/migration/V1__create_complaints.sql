CREATE TABLE complaints (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    citizen_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
