CREATE DATABASE IF NOT EXISTS exam_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_portal;
CREATE TABLE IF NOT EXISTS users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, full_name VARCHAR(100) NOT NULL,
 email VARCHAR(150) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 role ENUM('ADMIN','FACULTY','STUDENT') NOT NULL DEFAULT 'STUDENT', active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS subjects (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, subject_code VARCHAR(20) NOT NULL UNIQUE,
 subject_name VARCHAR(120) NOT NULL, created_by BIGINT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS exams (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, subject_id BIGINT NOT NULL, title VARCHAR(150) NOT NULL,
 instructions TEXT, duration_minutes INT NOT NULL, total_marks DECIMAL(7,2) NOT NULL DEFAULT 0,
 status ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT', created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (subject_id) REFERENCES subjects(id), FOREIGN KEY (created_by) REFERENCES users(id),
 CHECK (duration_minutes BETWEEN 1 AND 300), INDEX idx_exam_status(status)
);
CREATE TABLE IF NOT EXISTS questions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, exam_id BIGINT NOT NULL, question_text TEXT NOT NULL,
 option_a VARCHAR(500) NOT NULL, option_b VARCHAR(500) NOT NULL, option_c VARCHAR(500) NOT NULL,
 option_d VARCHAR(500) NOT NULL, correct_option ENUM('A','B','C','D') NOT NULL,
 marks DECIMAL(6,2) NOT NULL DEFAULT 1, sort_order INT NOT NULL DEFAULT 0,
 FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE, CHECK(marks > 0), INDEX idx_questions(exam_id,sort_order,id)
);
CREATE TABLE IF NOT EXISTS attempts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, exam_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, submitted_at DATETIME NULL,
 status ENUM('IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED') NOT NULL DEFAULT 'IN_PROGRESS',
 score DECIMAL(7,2) NULL, max_score DECIMAL(7,2) NOT NULL DEFAULT 0,
 FOREIGN KEY (exam_id) REFERENCES exams(id), FOREIGN KEY (student_id) REFERENCES users(id),
 UNIQUE KEY uq_attempt(exam_id,student_id), INDEX idx_attempt_student(student_id,submitted_at)
);
CREATE TABLE IF NOT EXISTS answers (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, attempt_id BIGINT NOT NULL, question_id BIGINT NOT NULL,
 selected_option ENUM('A','B','C','D') NULL, is_correct BOOLEAN NOT NULL DEFAULT FALSE,
 awarded_marks DECIMAL(6,2) NOT NULL DEFAULT 0,
 FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
 FOREIGN KEY (question_id) REFERENCES questions(id), UNIQUE KEY uq_answer(attempt_id,question_id)
);
-- Register through the application, then promote your own account manually:
-- UPDATE users SET role='ADMIN' WHERE email='your-email@example.com';
