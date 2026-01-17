# Gatling Quick Start

## Cài Đặt Maven (1 phút)

```bash
# macOS
brew install maven

# Verify
mvn --version
```

## Chạy Stress Test (2 phút)

```bash
cd load-tests/gatling

# Compile
mvn clean compile

# Run test
mvn gatling:test \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev
```

## Xem Kết Quả

Sau khi test chạy xong:
- HTML report: `target/gatling/stresstestsimulation-*/index.html`
- Mở trong browser: `open target/gatling/stresstestsimulation-*/index.html`

## Troubleshooting

### "mvn: command not found"
→ Cài đặt Maven: `brew install maven`

### "Java version mismatch"
→ Verify Java 21: `java --version`

### "Compilation failed"
→ Clean và rebuild: `mvn clean compile`

---

**Chi tiết:** Xem [README.md](./README.md) và [INSTALL.md](./INSTALL.md)

