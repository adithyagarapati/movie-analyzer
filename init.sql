CREATE DATABASE IF NOT EXISTS movie_db;
USE movie_db;

-- Ensure this password matches what the backend container uses for SPRING_DATASOURCE_PASSWORD
-- and what you set for MYSQL_PASSWORD when starting the 'db' container.
CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'apppassword';
GRANT ALL PRIVILEGES ON movie_db.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS movie_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_name VARCHAR(255) NOT NULL,
    review_text TEXT NOT NULL,
    predicted_sentiment VARCHAR(50) NOT NULL,
    pseudo_rating INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;