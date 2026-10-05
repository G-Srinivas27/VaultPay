# ============================================================
# VaultPay Frontend — Multi-stage Docker Build
# ============================================================
# Stage 1: Build the React app with Node.js → outputs static files
# Stage 2: Serve those files with nginx (fast production web server)
# ============================================================

# ──── Stage 1: BUILD ────
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first (dependency caching trick — same as backend)
COPY package*.json ./

# npm ci = "clean install" — faster and more reliable than npm install
# It installs EXACTLY what's in package-lock.json (no surprises)
RUN npm ci

# Copy source code
COPY . .

# Build the React app → outputs static files to /app/dist/
RUN npm run build

# ──── Stage 2: SERVE ────
# nginx:alpine is only ~5MB — extremely lightweight
FROM nginx:alpine

# Copy the built React files into nginx's default serve directory
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our custom nginx config (handles API proxy + SPA routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# nginx listens on port 80
EXPOSE 80

# Start nginx in foreground mode (required for Docker)
CMD ["nginx", "-g", "daemon off;"]
