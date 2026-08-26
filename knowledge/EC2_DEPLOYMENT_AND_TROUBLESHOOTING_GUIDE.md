# AWS EC2 Production Deployment & Troubleshooting Knowledge Base

This document details the complete production deployment architecture, live server configuration, and comprehensive issue resolution log for the **Product Management System (PMS)** on Amazon Web Services (AWS) EC2.

---

## 1. Live Environment & Infrastructure Overview

| Parameter | Specification |
|---|---|
| **Cloud Provider** | Amazon Web Services (AWS) |
| **Compute Service** | Amazon EC2 (Elastic Compute Cloud) |
| **Operating System** | Amazon Linux 2023 (AL2023) x86_64 |
| **Default OS User** | `ec2-user` |
| **EC2 Public IPv4** | `3.144.126.67` |
| **Secure Domain (SSL)** | `https://3-144-126-67.sslip.io` |
| **Frontend Application** | Angular 18 (SPA) served via Nginx on Ports `80` (HTTP redirect) & `443` (HTTPS) |
| **Backend API** | Spring Boot 3.x (Java 17, Maven) running internally on Port `8080` (Proxied via `/api/`) |
| **Cloud Database** | PostgreSQL 16 (Neon Serverless Cloud DB with SSL mode enabled) |
| **SSL/TLS Provider** | Let's Encrypt (Certbot Standalone / RSA 4096 / TLS 1.2 & 1.3) |

---

## 2. Production Architecture Diagram

```
[ Web Browser Client ]
        │  HTTPS (:443) / HTTP Auto-Redirect (:80)
        ▼
[ AWS EC2 Instance Security Group: Inbound 22, 80, 443 ]
        │
        ▼
[ Docker Container: product-management-frontend (Nginx) ]
        │
        ├── Static Angular Dist Files (/usr/share/nginx/html)
        ├── SSL Certificates mounted from local ./certs
        │
        └── Reverse Proxy /api/ (Internal Docker Network)
                 │
                 ▼
[ Docker Container: product-management-backend (Spring Boot: 8080) ]
                 │
                 ▼ TLS Connection
[ Neon Cloud PostgreSQL Database (ep-cool-block-atydsn8b.us-east-1) ]
```

---

## 3. Comprehensive Incident & Troubleshooting Log

During the EC2 setup and go-live, the following 10 issues were diagnosed, resolved, and documented:

### Issue 1: `sudo: apt: command not found` & `user 'ubuntu' does not exist`
* **Symptom:** Running Ubuntu `apt` commands failed on the EC2 shell.
* **Root Cause:** The AMI selected was **Amazon Linux 2023 / Amazon Linux 2**, which uses `dnf` / `yum` and user `ec2-user` instead of `apt` / `ubuntu`.
* **Resolution:** Switched package management to `sudo dnf install -y git docker` and assigned Docker permissions to `ec2-user`:
  ```bash
  sudo systemctl enable --now docker
  sudo usermod -aG docker ec2-user
  newgrp docker
  ```

---

### Issue 2: `no configuration file provided: not found`
* **Symptom:** Running `docker compose up -d --build` failed immediately.
* **Root Cause:** The cloned git repository root did not contain `docker-compose.yml` in the current working directory.
* **Resolution:** Created and verified `docker-compose.yml` defining both `backend` and `frontend` services with environment variables and health dependencies.

---

### Issue 3: `go-yaml load error in scanner: mapping values are not allowed in this context`
* **Symptom:** Docker Compose failed to parse `docker-compose.yml` after manual editing in `nano`.
* **Root Cause:** Tab characters and inconsistent space indentations were introduced during terminal pasting into `nano`.
* **Resolution:** Overwrote the file using clean `cat << 'EOF' > docker-compose.yml` syntax with exact 2-space YAML hierarchy.

---

### Issue 4: `compose build requires buildx 0.17.0 or later`
* **Symptom:** Docker Compose failed to initiate multi-stage builds.
* **Root Cause:** Amazon Linux default docker package did not ship with the modern `docker-buildx` plugin CLI component.
* **Resolution:** Installed standalone Docker Buildx binary directly into `/usr/local/lib/docker/cli-plugins/docker-buildx` and `~/.docker/cli-plugins/`.

---

### Issue 5: `failed to fetch metadata: fork/exec .../docker-buildx: exec format error`
* **Symptom:** Buildx binary execution failed on invocation.
* **Root Cause:** Downloaded binary had an architecture naming mismatch (`uname -m` outputs `x86_64`, whereas GitHub release assets use `amd64`).
* **Resolution:** Corrected architecture resolution in download script:
  ```bash
  ARCH=$(uname -m)
  if [ "$ARCH" = "x86_64" ]; then ARCH="amd64"; elif [ "$ARCH" = "aarch64" ]; then ARCH="arm64"; fi
  sudo curl -SL "https://github.com/docker/buildx/releases/download/v0.18.0/buildx-v0.18.0.linux-${ARCH}" -o /usr/local/lib/docker/cli-plugins/docker-buildx
  sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx
  ```

---

### Issue 6: `mkdir /home/ec2-user/.docker/buildx: permission denied`
* **Symptom:** Docker build failed to create build cache directories.
* **Root Cause:** Previous `sudo` commands created `/home/ec2-user/.docker` with `root:root` ownership.
* **Resolution:** Restored user ownership recursively:
  ```bash
  sudo chown -R ec2-user:ec2-user /home/ec2-user/.docker
  ```

---

### Issue 7: `Access to XMLHttpRequest at 'https://...onrender.com' blocked by CORS policy`
* **Symptom:** Login page displayed "Connecting to database... Free-tier server is waking up" and failed with `Error 0: Unknown Error`.
* **Root Cause:** Angular's production environment had hardcoded the old external Render URL (`https://product-management-backend-u76w.onrender.com/api`). Browsers accessing `http://3.144.126.67` were blocked by browser cross-origin preflight policies.
* **Resolution:**
  1. Updated `docker-compose.yml` frontend build args to pass `API_URL=/api`.
  2. Leveraged Nginx's reverse proxy rule `location /api/ { proxy_pass http://backend:8080/api/; }`.
  3. Rebuilt the frontend container without cache (`docker compose build --no-cache frontend`).

---

### Issue 8: SSL Certificate Issuance on Raw IP without Domain
* **Symptom:** Browser flagged connection as "Not Secure" (HTTP), and raw IP addresses cannot receive public Let's Encrypt certificates directly.
* **Root Cause:** Standard CA certificate authorities do not issue free DV (Domain Validation) SSL certificates to raw IPv4 addresses.
* **Resolution:** Used wildcard dynamic DNS domain **`3-144-126-67.sslip.io`**, which resolves natively to `3.144.126.67`, allowing Certbot to validate ACME challenges and issue trusted SSL certificates for HTTPS.

---

### Issue 9: `cannot load certificate "/etc/letsencrypt/live/...": No such file or directory` (SELinux & Symlink Dereferencing)
* **Symptom:** Frontend container went into a crash/restart loop (`Restarting (1)`).
* **Root Cause:** 
  1. Let's Encrypt certificates in `/etc/letsencrypt/live/` are symbolic links to `/etc/letsencrypt/archive/`, protected with root-only `700` permissions.
  2. Amazon Linux enforces SELinux file contexts, preventing Docker from reading cross-volume root symlinks.
* **Resolution:**
  1. Copied certificate files into a local project `./certs` directory using `cp -L` (dereference symlinks to copy actual files):
     ```bash
     mkdir -p ./certs
     sudo cp -L /etc/letsencrypt/live/3-144-126-67.sslip.io/fullchain.pem ./certs/fullchain.pem
     sudo cp -L /etc/letsencrypt/live/3-144-126-67.sslip.io/privkey.pem ./certs/privkey.pem
     sudo chmod 755 ./certs ./certs/*.pem
     sudo chown -R ec2-user:ec2-user ./certs
     ```
  2. Mounted `./certs:/etc/nginx/certs:ro` and `./product-management-system-frontend/nginx.conf:/etc/nginx/conf.d/default.conf:ro` in `docker-compose.yml`.

---

### Issue 10: AWS Dynamic IP Re-allocation Risk
* **Symptom:** Stopping and starting EC2 instances changes the public IP, breaking URLs and DNS.
* **Resolution:** Documented AWS Elastic IP allocation to guarantee a permanent, unchanging static public IPv4 address.

---

## 4. Production Configuration Artifacts

### A. Production `docker-compose.yml`
```yaml
services:
  backend:
    build:
      context: ./product-management-system-backend
      dockerfile: Dockerfile
    container_name: product-management-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://ep-cool-block-atydsn8b.c-9.us-east-1.aws.neon.tech/neondb?sslmode=require
      - SPRING_DATASOURCE_USERNAME=neondb_owner
      - SPRING_DATASOURCE_PASSWORD=npg_hQoR9X2Fgrlt
      - CORS_ALLOWED_ORIGINS=*
      - JWT_COOKIE_SECURE=true
      - JWT_COOKIE_SAME_SITE=None
    restart: unless-stopped

  frontend:
    build:
      context: ./product-management-system-frontend
      dockerfile: Dockerfile
      args:
        - API_URL=/api
    container_name: product-management-frontend
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./product-management-system-frontend/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on:
      - backend
    restart: unless-stopped
```

### B. Production `nginx.conf` (SSL & Reverse Proxy)
```nginx
server {
    listen 80;
    server_name 3-144-126-67.sslip.io localhost;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name 3-144-126-67.sslip.io localhost;

    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;

    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    error_page 500 502 503 504 /50x.html;
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}
```

---

## 5. Maintenance & Operational Commands

### Check Service Status & Logs
```bash
# View container health
docker compose ps

# Live backend logs (Spring Boot)
docker compose logs -f backend

# Live frontend & proxy logs (Nginx)
docker compose logs -f frontend
```

### Deploying New Code Changes
```bash
git pull origin main
docker compose up -d --build
```

### SSL Certificate Renewal (Every 90 Days)
```bash
docker compose stop frontend
sudo certbot renew
sudo cp -L /etc/letsencrypt/live/3-144-126-67.sslip.io/fullchain.pem ./certs/fullchain.pem
sudo cp -L /etc/letsencrypt/live/3-144-126-67.sslip.io/privkey.pem ./certs/privkey.pem
sudo chmod 755 ./certs ./certs/*.pem
docker compose up -d frontend
```
