-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'STUDENT', 'VERIFIER')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Institutions Table (One-to-One with Users for Admin roles)
CREATE TABLE IF NOT EXISTS institutions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER UNIQUE NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Students Table (One-to-One or One-to-Many linked to User if registered)
CREATE TABLE IF NOT EXISTS students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER UNIQUE,
    register_number TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id TEXT UNIQUE NOT NULL, -- e.g. CT-2026-000001
    document_type TEXT NOT NULL CHECK(document_type IN (
        '10th Marksheet', '12th Marksheet', 'Degree Certificate',
        'Diploma Certificate', 'Internship Certificate', 'Course Completion Certificate'
    )),
    student_id INTEGER NOT NULL,
    institution_id INTEGER NOT NULL,
    issue_date TEXT NOT NULL,
    academic_year TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('VERIFIED', 'SUSPICIOUS', 'HIGH RISK', 'NOT FOUND', 'REVOKED', 'UNABLE TO VERIFY')) DEFAULT 'VERIFIED',
    document_hash TEXT NOT NULL, -- SHA-256 hash of document data/file
    revocation_reason TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE RESTRICT,
    FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE RESTRICT
);

-- 5. Document Marks Table (Arbitrary subjects support)
CREATE TABLE IF NOT EXISTS document_marks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL,
    subject_name TEXT NOT NULL,
    marks_obtained INTEGER NOT NULL,
    max_marks INTEGER NOT NULL DEFAULT 100,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- 6. Document Hashes (Tracks file mapping)
CREATE TABLE IF NOT EXISTS document_hashes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER UNIQUE NOT NULL,
    sha256_hash TEXT NOT NULL,
    file_path TEXT NOT NULL,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- 7. Verification History (Audits and checks reuse)
CREATE TABLE IF NOT EXISTS verification_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER NOT NULL,
    verifier_user_id INTEGER,
    verification_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    status_returned TEXT NOT NULL,
    trust_score INTEGER NOT NULL,
    evidence_log TEXT NOT NULL, -- JSON string detailing the checks results
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY(verifier_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 8. QR Codes (References for verification)
CREATE TABLE IF NOT EXISTS qr_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id INTEGER UNIQUE NOT NULL,
    qr_data TEXT NOT NULL, -- e.g., CT-2026-000001
    qr_image_path TEXT NOT NULL,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- 9. Audit Logs (Log authentication, document issuing, revoking, scans)
CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    document_id INTEGER,
    details TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_documents_doc_id ON documents(document_id);
CREATE INDEX IF NOT EXISTS idx_students_reg_no ON students(register_number);
CREATE INDEX IF NOT EXISTS idx_verification_doc_id ON verification_history(document_id);
