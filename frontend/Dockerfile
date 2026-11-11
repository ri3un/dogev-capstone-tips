# 1단계: React 빌드
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# 빌드 때 지도 키를 코드에 넣음 (docker-compose.yml 의 build.args 로 루트 .env 값이 들어옴)
ARG VITE_NCP_MAP_KEY_ID
ENV VITE_NCP_MAP_KEY_ID=$VITE_NCP_MAP_KEY_ID
RUN npm run build

# 2단계: Nginx가 빌드 결과를 서빙하고 /api 는 백엔드로 넘깁니다 (nginx/nginx.conf).
FROM nginx:1.28-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
