FROM amazoncorretto:17

EXPOSE 8123:8123

RUN mkdir /app
COPY build/install/tasks-api/ /app/

COPY service-account.json /app/bin/service-account.json
ENV GOOGLE_APPLICATION_CREDENTIALS="/app/bin/service-account.json"

WORKDIR /app/bin
CMD ["./tasks-api"]