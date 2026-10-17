# 1단계: React 빌드
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2단계: Nginx가 빌드 결과를 서빙하고 /api 는 백엔드로 넘깁니다 (nginx/nginx.conf).
FROM nginx:1.28-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
