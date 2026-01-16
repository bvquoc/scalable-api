# Monitoring Setup Guide

This directory contains the configuration files for Prometheus and Grafana monitoring.

## Quick Start

1. **Start the main application stack:**

   ```bash
   docker-compose up -d
   ```

2. **Start your Spring Boot application:**

   ```bash
   mvn spring-boot:run
   ```

3. **Start the monitoring stack:**

   ```bash
   docker-compose -f docker-compose-monitoring.yml up -d
   ```

4. **Access the UIs:**
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000 (admin/admin)

## Files

- `prometheus.yml` - Prometheus configuration for scraping Spring Boot metrics
- `grafana/provisioning/datasources/prometheus.yml` - Grafana datasource configuration
- `grafana/provisioning/dashboards/dashboard.yml` - Dashboard provisioning configuration
- `grafana/dashboards/spring-boot-dashboard.json` - Pre-built Spring Boot metrics dashboard

## Troubleshooting

### Prometheus can't reach the application

If Prometheus shows the target as DOWN:

1. **Check if the app is running:**

   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. **On Linux**, you may need to use the host's IP address instead of `host.docker.internal`:

   - Find your host IP: `ip addr show docker0` or `hostname -I | awk '{print $1}'`
   - Update `prometheus.yml` to use that IP instead of `host.docker.internal:8080`

3. **Alternative**: Use host network mode (Linux only):
   ```yaml
   network_mode: "host"
   ```
   And update `prometheus.yml` to use `localhost:8080`

### Grafana dashboard not showing data

1. Check that Prometheus is scraping: http://localhost:9090/targets
2. Verify the datasource in Grafana: Configuration → Data Sources → Prometheus
3. Check dashboard time range (top right corner)
4. Verify metrics exist in Prometheus: http://localhost:9090/graph

### Network issues

If you get network errors, ensure the main docker-compose network exists:

```bash
docker network ls | grep app-network
```

If it doesn't exist, start the main stack first:

```bash
docker-compose up -d
```
