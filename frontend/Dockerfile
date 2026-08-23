# Stage 1: Build the React application
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies first (leverage Docker caching)
COPY frontend/package.json ./
COPY frontend/package-lock.json* ./
RUN npm install

# Copy source code relative to the repository root context
COPY frontend/ ./

# Set up build argument for backend URL
ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL:-http://localhost:8080}

# Build the production bundle
RUN npm run build

# Stage 2: Serve the static files with Nginx
FROM nginx:1.25-alpine

# Copy custom Nginx configuration
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy build artifacts to Nginx html directory
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
