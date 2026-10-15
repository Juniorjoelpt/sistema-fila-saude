# ---- build ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Variáveis VITE_* são embutidas no build: o Railway as repassa como build args.
ARG VITE_API_URL
ARG VITE_TENANT_ID
ARG VITE_TENANT_FIXO
ENV VITE_API_URL=$VITE_API_URL VITE_TENANT_ID=$VITE_TENANT_ID VITE_TENANT_FIXO=$VITE_TENANT_FIXO
RUN npm run build

# ---- runtime ----
FROM nginx:1.27-alpine
# O nginx oficial aplica envsubst em /etc/nginx/templates (usa $PORT do Railway).
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
