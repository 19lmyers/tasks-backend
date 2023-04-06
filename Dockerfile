FROM amazoncorretto:17

EXPOSE 8123:8123

RUN mkdir /app

COPY build/install/tasks-backend /app/

ENV GOOGLE_APPLICATION_CREDENTIALS="/run/secrets/google_service_account"

WORKDIR /app/bin
CMD ["./tasks-backend"]