# Gatling Installation Guide

## Cài Đặt Maven (Required)

### macOS (Homebrew) - Recommended

```bash
# Cài đặt Maven
brew install maven

# Verify installation
mvn --version
```

**Expected output:**
```
Apache Maven 3.9.x
Maven home: /opt/homebrew/Cellar/maven/3.9.x
Java version: 21.x.x
```

### macOS (Manual Installation)

```bash
# Download Maven
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz
sudo mv apache-maven-3.9.6 /opt/maven

# Add to PATH
echo 'export PATH=/opt/maven/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Verify
mvn --version
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install maven

# Verify
mvn --version
```

## Cài Đặt Gatling Standalone (Alternative)

Nếu không muốn dùng Maven, có thể cài Gatling standalone:

```bash
# macOS (Homebrew)
brew install gatling

# Manual Installation
cd /tmp
wget https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.10.3/gatling-charts-highcharts-bundle-3.10.3.zip
unzip gatling-charts-highcharts-bundle-3.10.3.zip
sudo mv gatling-charts-highcharts-bundle-3.10.3 /opt/gatling

# Add to PATH
echo 'export PATH=/opt/gatling/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Verify
gatling.sh --version
```

## Verify Installation

### Check Maven

```bash
mvn --version
```

**Expected:**
- Maven 3.9+ installed
- Java 21 detected

### Check Java

```bash
java --version
```

**Expected:**
- Java 21 (LTS)

## Quick Test

Sau khi cài đặt Maven:

```bash
cd load-tests/gatling

# Test Maven
mvn --version

# Test compile
mvn clean compile

# Run Gatling test
mvn gatling:test \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev
```

## Troubleshooting

### Issue: "mvn: command not found"

**Solution:**
1. Cài đặt Maven (xem trên)
2. Verify PATH: `echo $PATH | grep maven`
3. Restart terminal sau khi cài đặt

### Issue: "Java version mismatch"

**Solution:**
```bash
# Check Java version
java --version

# Set JAVA_HOME if needed
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Issue: "Maven không tìm thấy dependencies"

**Solution:**
```bash
# Clean và download dependencies
mvn clean install -U
```

---

**Sau khi cài đặt:** Chạy `mvn --version` để verify, sau đó có thể chạy Gatling tests.

