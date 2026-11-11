# 码域CRM 前端镜像（PC + H5 + 反代）
FROM docker.1ms.run/library/node:18-alpine AS h5builder
WORKDIR /build
COPY freecrm-app/package.json freecrm-app/pnpm-workspace.yaml ./
RUN npm config set registry https://registry.npmmirror.com && npm i -g pnpm@9 && pnpm install --no-frozen-lockfile
COPY freecrm-app .
RUN pnpm build:h5

FROM docker.1ms.run/library/node:18-alpine AS webbuilder
WORKDIR /build
COPY freecrm-web/package.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install
COPY freecrm-web .
RUN npm run build

FROM docker.1ms.run/library/nginx:1.25-alpine
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=webbuilder /build/dist /usr/share/nginx/html/pc
COPY --from=h5builder /build/dist/build/h5 /usr/share/nginx/html/m
EXPOSE 80
