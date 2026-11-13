# 前端镜像：构建静态资源后由 Nginx 托管，并反向代理后端接口
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
