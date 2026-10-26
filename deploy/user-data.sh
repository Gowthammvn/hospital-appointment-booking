#!/bin/bash
# First-boot setup for the EC2 server (Ubuntu 24.04): Java 21, MySQL 8, Nginx and a systemd service.
# deploy.sh replaces __DB_PASS__ with a random password before launching the instance.
set -euxo pipefail

timedatectl set-timezone Asia/Kolkata

# t3.micro has 1 GB RAM; swap keeps MySQL + the JVM from running out of memory
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-21-jre-headless mysql-server nginx

# MySQL 8: local-only database and user for the app
mysql -e "CREATE DATABASE IF NOT EXISTS hospital_db;
  CREATE USER IF NOT EXISTS 'hospital'@'localhost' IDENTIFIED BY '__DB_PASS__';
  GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'localhost';
  FLUSH PRIVILEGES;"

useradd --system --no-create-home --shell /usr/sbin/nologin hospital
mkdir -p /opt/hospital

cat > /opt/hospital/app.env <<'EOF'
DB_URL=jdbc:mysql://localhost:3306/hospital_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=hospital
DB_PASSWORD=__DB_PASS__
SPRING_THYMELEAF_CACHE=true
SERVER_FORWARD_HEADERS_STRATEGY=framework
EOF
chown -R hospital:hospital /opt/hospital
chmod 600 /opt/hospital/app.env

cat > /etc/systemd/system/hospital.service <<'EOF'
[Unit]
Description=Hospital Appointment Booking System
After=network.target mysql.service
Requires=mysql.service

[Service]
User=hospital
WorkingDirectory=/opt/hospital
EnvironmentFile=/opt/hospital/app.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/hospital/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable hospital   # started by deploy.sh once the jar is uploaded

# Nginx: public port 80 -> Spring Boot on 8080
cat > /etc/nginx/sites-available/hospital <<'EOF'
server {
    listen 80 default_server;
    server_name _;
    client_max_body_size 5m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
rm -f /etc/nginx/sites-enabled/default
ln -sf /etc/nginx/sites-available/hospital /etc/nginx/sites-enabled/hospital
systemctl reload nginx
