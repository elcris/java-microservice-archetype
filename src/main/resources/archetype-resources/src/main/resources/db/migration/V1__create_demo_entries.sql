-- Technical example only. Replace before the first deployment of a business service.
CREATE TABLE demo_entries (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
