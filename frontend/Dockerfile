### Stage 1: build the Vue application
FROM node:18 AS build
WORKDIR /usr/src/app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
# Build the production-ready static files
RUN npm run build

### Stage 2: serve the built files with nginx
FROM nginx:1.25-alpine
COPY --from=build /usr/src/app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
