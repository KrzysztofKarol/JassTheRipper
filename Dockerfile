FROM eclipse-temurin:21-jdk-alpine AS build

RUN mkdir -p /usr/src/app
COPY . /usr/src/app/
WORKDIR /usr/src/app

RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

COPY --from=build /usr/src/app/build/libs/*-all.jar /app/app.jar
WORKDIR /app

ENV JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseZGC -XX:MaxGCPauseMillis=10"
ENV STRENGTH_LEVEL=HETZNER_CX23

CMD ["sh", "-c", "java $JAVA_OPTS -cp app.jar to.joeli.jass.client.rest.Server"]
