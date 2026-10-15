CREATE TABLE activity_seed_runs (
    seed_key VARCHAR(60) PRIMARY KEY,
    completed_at TIMESTAMP NULL
);
INSERT INTO activity_seed_runs(seed_key) VALUES ('store-history-v1');
