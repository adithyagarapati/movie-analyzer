FROM node:18-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build

EXPOSE 3000

ENV BACKEND_API_URL="http://localhost:8080"

CMD ["npm", "start"]