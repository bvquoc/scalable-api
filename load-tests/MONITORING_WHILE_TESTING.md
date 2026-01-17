# Xem Realtime Metrics Khi Load Testing

**Hướng dẫn xem metrics realtime trong Grafana và Prometheus khi chạy load test**

## 🎯 Mục Đích

Khi chạy load test, bạn cần xem realtime metrics để:
- Theo dõi performance trong thời gian thực
- Phát hiện bottlenecks ngay lập tức
- Validate cache hit rate, error rate, response time
- Monitor resource usage (CPU, memory, connections)

## 🚀 Quick Start (2 phút)

### Bước 1: Khởi Động Monitoring

```bash
cd /Users/quocbui/src/uit/DA2/scalable-api

# Khởi động Prometheus + Grafana
docker-compose -f docker-compose-monitoring.yml up -d

# Kiểm tra
docker-compose -f docker-compose-monitoring.yml ps
```

### Bước 2: Mở Grafana Dashboard

```bash
# Mở browser
open http://localhost:3000

# Login:
#   Username: admin
#   Password: admin

# Dashboard: "Spring Boot Metrics" (tự động load)
```

### Bước 3: Chạy Load Test & Xem Realtime

**Terminal 1: Chạy Test**
```bash
cd load-tests/jmeter
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev
```

**Browser: Xem Metrics**
- Dashboard tự động refresh mỗi 10 giây
- Hoặc click "Refresh" để update ngay
- Xem các panels realtime

---

## 📊 Metrics Cần Theo Dõi

### 1. Request Rate (Requests/Second)

**Grafana Panel:** "HTTP Requests Rate"

**PromQL:**
```promql
sum(rate(http_server_requests_seconds_count[1m])) by (method, uri)
```

**Ý nghĩa:**
- Tăng khi test chạy
- Cho biết throughput hiện tại
- So sánh với target (>1,000 req/s)

### 2. Response Time p95

**Grafana Panel:** "HTTP Request Duration (p95)"

**PromQL:**
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, method, uri)
)
```

**Ý nghĩa:**
- Nên < 200ms (target)
- Tăng khi system bị overload
- Phát hiện slow endpoints

### 3. Error Rate

**Grafana Panel:** "Error Rate"

**PromQL:**
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) 
/ 
sum(rate(http_server_requests_seconds_count[1m])) * 100
```

**Ý nghĩa:**
- Nên < 1% (target)
- Tăng khi system có vấn đề
- Phân biệt với rate limiting (429)

### 4. Cache Hit Rate

**Grafana Panel:** "Cache Hit Rate"

**PromQL:**
```promql
sum(rate(cache_gets_total{result="hit"}[5m])) 
/ 
sum(rate(cache_gets_total[5m])) * 100
```

**Ý nghĩa:**
- Nên > 90% (target)
- Thấp = nhiều DB queries
- Ảnh hưởng trực tiếp đến performance

### 5. DB Connections

**Grafana Panel:** "HikariCP Active Connections"

**PromQL:**
```promql
hikaricp_connections_active
```

**Ý nghĩa:**
- Không vượt pool size (20)
- Tăng = nhiều DB queries
- Exhaustion = bottleneck

### 6. JVM Heap Usage

**Grafana Panel:** "JVM Memory Usage"

**PromQL:**
```promql
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

**Ý nghĩa:**
- Không tăng liên tục (memory leak)
- Peak usage trong test
- GC frequency

---

## 🔍 Cách Xem Realtime

### Option 1: Grafana Dashboard (Recommended)

**Ưu điểm:**
- ✅ Visual, dễ xem
- ✅ Tự động refresh
- ✅ Multiple panels cùng lúc
- ✅ Historical data

**Cách dùng:**
1. Mở http://localhost:3000
2. Dashboard: "Spring Boot Metrics"
3. Time range: "Last 5 minutes" (khi test)
4. Auto-refresh: 10s hoặc 5s

**Screenshot các panels quan trọng:**
- Request Rate (line chart)
- Response Time p95 (line chart)
- Error Rate (line chart với thresholds)
- Cache Hit Rate (gauge hoặc line chart)
- DB Connections (line chart)
- JVM Heap (line chart)

### Option 2: Prometheus UI

**Ưu điểm:**
- ✅ Query trực tiếp
- ✅ Test PromQL queries
- ✅ Export data

**Cách dùng:**
1. Mở http://localhost:9090
2. Tab "Graph"
3. Nhập PromQL query
4. Click "Execute"
5. Xem graph

**Ví dụ queries:**
```promql
# Request rate
sum(rate(http_server_requests_seconds_count[1m]))

# Response time p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) 
/ 
sum(rate(http_server_requests_seconds_count[1m])) * 100
```

### Option 3: Command Line (Quick Check)

**Ưu điểm:**
- ✅ Nhanh
- ✅ Scriptable
- ✅ Automation

**Ví dụ:**
```bash
# Request rate
watch -n 2 'curl -s "http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count[1m]))" | jq ".data.result[0].value[1]"'

# Response time p95
watch -n 2 'curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_server_requests_seconds_bucket[5m]))" | jq ".data.result[0].value[1]"'

# Error rate
watch -n 2 'curl -s "http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m]))/sum(rate(http_server_requests_seconds_count[1m]))*100" | jq ".data.result[0].value[1]"'
```

---

## 📈 Workflow Khi Test

### Trước Khi Test

```bash
# 1. Khởi động monitoring
docker-compose -f docker-compose-monitoring.yml up -d

# 2. Kiểm tra Prometheus đang scrape
open http://localhost:9090/targets
# Target "scalable-api" phải "UP"

# 3. Mở Grafana dashboard
open http://localhost:3000
# Dashboard: "Spring Boot Metrics"
# Time range: "Last 5 minutes"
# Auto-refresh: 10s
```

### Trong Khi Test

**Terminal 1: Load Test**
```bash
cd load-tests/jmeter
jmeter -n -t baseline-test.jmx ...
```

**Browser: Grafana Dashboard**
- Xem metrics realtime
- Phát hiện anomalies
- Ghi chú observations

**Terminal 2: Watch Metrics (Optional)**
```bash
# Quick check
curl -s "http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count[1m]))" | jq
```

### Sau Khi Test

**Export Metrics:**
```bash
# Export Prometheus snapshot
curl http://localhost:9090/api/v1/query?query=up > metrics-snapshot.json

# Export Grafana dashboard
curl -u admin:admin http://localhost:3000/api/dashboards/db/spring-boot-metrics > dashboard-export.json
```

**Analyze:**
1. Review Grafana dashboard history
2. Compare với baseline
3. Identify bottlenecks
4. Document findings

---

## 🎯 Best Practices

### 1. Time Range

- **Khi test:** "Last 5 minutes" hoặc "Last 15 minutes"
- **Sau test:** "Last 1 hour" để xem toàn bộ test

### 2. Refresh Interval

- **Realtime:** 5s hoặc 10s
- **Normal:** 30s
- **Historical:** Manual refresh

### 3. Multiple Views

- **Grafana:** Overall dashboard
- **Prometheus:** Deep dive queries
- **Command line:** Quick checks

### 4. Alert Thresholds

Set thresholds trong Grafana:
- Response Time p95: Yellow > 200ms, Red > 500ms
- Error Rate: Yellow > 1%, Red > 5%
- Cache Hit Rate: Yellow < 90%, Red < 80%

---

## 🔧 Troubleshooting

### Metrics không hiển thị?

1. **Kiểm tra Prometheus đang scrape:**
   ```bash
   curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.labels.job=="scalable-api")'
   ```

2. **Kiểm tra Spring Boot endpoint:**
   ```bash
   curl http://localhost:8080/actuator/prometheus | head -5
   ```

3. **Kiểm tra time range:** Chọn "Last 5 minutes"

### Dashboard không update?

1. Click "Refresh" button
2. Kiểm tra auto-refresh enabled
3. Kiểm tra Prometheus có data không

### Query không trả về data?

1. Kiểm tra metric name đúng không
2. Kiểm tra time range
3. Test query trong Prometheus UI trước

---

## 📚 Tài Liệu Tham Khảo

- **Chi tiết đầy đủ:** [../monitoring/MONITORING_GUIDE.md](../monitoring/MONITORING_GUIDE.md)
- **Quick start:** [../monitoring/QUICK_MONITORING.md](../monitoring/QUICK_MONITORING.md)
- **PromQL queries:** [../monitoring/MONITORING_GUIDE.md#5-promql-queries-hữu-ích](../monitoring/MONITORING_GUIDE.md#5-promql-queries-hữu-ích)

---

**Tip:** Mở Grafana dashboard trước khi chạy test để xem baseline, sau đó theo dõi metrics khi test chạy!

