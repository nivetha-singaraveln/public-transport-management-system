-- Application account used by the Java and Python programs
CREATE USER IF NOT EXISTS 'transport'@'localhost' IDENTIFIED BY 'transport123';
CREATE USER IF NOT EXISTS 'transport'@'127.0.0.1' IDENTIFIED BY 'transport123';
GRANT ALL PRIVILEGES ON transport_db.* TO 'transport'@'localhost';
GRANT ALL PRIVILEGES ON transport_db.* TO 'transport'@'127.0.0.1';
FLUSH PRIVILEGES;
