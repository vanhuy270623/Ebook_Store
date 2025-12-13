# Utility Tools - Developer Only

This folder contains utility tools for development purposes. These tools are **NOT** used during runtime.

---

## 📋 Available Tools

### 1. PasswordEncoderUtil.java

**Purpose:** Generate BCrypt password hashes for testing or creating user accounts.

**Type:** CLI Tool / Dev Utility

**Status:** ✅ Completed, Dev-only

---

## 🔧 PasswordEncoderUtil

### Description
A command-line utility that generates BCrypt password hashes using Spring Security's BCryptPasswordEncoder. This is useful for:
- Creating passwords for admin/test users
- Testing password encoding/verification
- Generating hashes for database seeding

### Usage

#### Method 1: Run from IDE
1. Open `PasswordEncoderUtil.java` in your IDE
2. Run the `main` method
3. Check console output for generated hashes

#### Method 2: Maven Command
```bash
# From project root directory
mvn compile exec:java -Dexec.mainClass="stu.datn.ebook_store.util.PasswordEncoderUtil"
```

#### Method 3: Custom Password
Edit the file and change the `customPassword` variable:
```java
String customPassword = "your_password_here";
```

Then run using Method 1 or 2.

---

### Sample Output

```
=== BCrypt Password Generator ===

Plain text: admin
BCrypt hash: $2a$10$xK5Z8r3FqGh7YcM2Nv4wD.L5yR9pQ3xW7bT6jK8mH2nS4vC1lE9fO
Verify: true
---
Plain text: user123
BCrypt hash: $2a$10$mP9kT2xL5nQ4wR8yJ6vH3.S7dF1gK9bN4cM8pT2xL5nQ4wR8yJ6vH
Verify: true
---

=== Custom Password ===
Password: mypassword
BCrypt hash: $2a$10$oT4kL9mP3xQ7wN2yH5vK8.D6fG2jM9bP4cL8tX3mK7nQ4wR8yJ6vH
```

---

### Code Example

**Basic Usage:**
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

// Encode password
String plainPassword = "admin123";
String hashedPassword = encoder.encode(plainPassword);
System.out.println("Hash: " + hashedPassword);

// Verify password
boolean matches = encoder.matches(plainPassword, hashedPassword);
System.out.println("Valid: " + matches); // true
```

**For Database Insert:**
```sql
-- Use the generated hash in your SQL
INSERT INTO users (username, password, email, role) 
VALUES ('admin', '$2a$10$xK5Z8r3FqGh7YcM2Nv4wD...', 'admin@ebook.com', 'ADMIN');
```

---

### Important Notes

⚠️ **BCrypt Characteristics:**
- Each time you encode the same password, you get a **different hash** (due to random salt)
- This is **normal** and **secure** behavior
- The `matches()` method will still return `true` for the correct password
- Hash length: ~60 characters
- Algorithm: BCrypt with 10 rounds (default strength)

⚠️ **Security Best Practices:**
- Never store plain-text passwords
- Use BCrypt (not MD5, SHA1)
- Don't commit password hashes to public repos
- Use environment variables for sensitive data

⚠️ **Development Only:**
- This tool is for **development and testing only**
- Not used in production runtime
- Can be excluded from production builds if needed

---

### Common Use Cases

#### 1. Creating Admin User
```bash
# Run the tool
mvn compile exec:java -Dexec.mainClass="stu.datn.ebook_store.util.PasswordEncoderUtil"

# Copy the generated hash
# Insert into database
INSERT INTO users (username, password, email, role_id, is_active) 
VALUES ('admin', '$2a$10$...copied-hash...', 'admin@ebook.com', 1, 1);
```

#### 2. Resetting User Password
```bash
# Generate new hash for "newpassword123"
# Update in database
UPDATE users 
SET password = '$2a$10$...new-hash...' 
WHERE username = 'john.doe';
```

#### 3. Testing Authentication
```java
@Test
public void testPasswordMatching() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String rawPassword = "admin123";
    String storedHash = "$2a$10$xK5Z8r3FqGh7YcM2Nv4wD...";
    
    assertTrue(encoder.matches(rawPassword, storedHash));
}
```

---

### Integration with Application

The application uses BCrypt in production via:
- **SecurityConfig:** `@Bean public PasswordEncoder passwordEncoder()`
- **UserService:** Password encoding on registration
- **Spring Security:** Password verification on login

This utility tool simply provides a convenient way to generate hashes outside the running application.

---

### Troubleshooting

**Problem:** "Class not found" error
```bash
# Solution: Compile first
mvn clean compile
mvn exec:java -Dexec.mainClass="stu.datn.ebook_store.util.PasswordEncoderUtil"
```

**Problem:** Different hash each time
- **Expected behavior** - BCrypt uses random salt
- Hashes are still valid and will match

**Problem:** Need to encode many passwords
- Edit the `passwords` array in the code
- Or call the tool multiple times

---

## 🔮 Future Utilities

Potential future tools to add to this folder:

- [ ] **DataSeeder.java** - Populate database with test data
- [ ] **ImageResizer.java** - Bulk resize book covers
- [ ] **SlugGenerator.java** - Generate URL-friendly slugs
- [ ] **ISBNValidator.java** - Validate book ISBNs
- [ ] **DatabaseMigration.java** - Migration helper tool
- [ ] **LogAnalyzer.java** - Parse and analyze application logs

---

## 📝 Contributing

When adding new utility tools:
1. Add clear documentation in this README
2. Include usage examples
3. Mark as dev-only in comments
4. Add to `.gitignore` if generates output files
5. Keep utilities simple and focused

---

**Last Updated:** 30/11/2025  
**Maintained by:** Development Team  
**Status:** Active Development

