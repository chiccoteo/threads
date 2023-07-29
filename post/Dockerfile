FROM gradle:7.4.1-jdk11-alpine as builder
WORKDIR /
COPY . /
RUN gradle clean build --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /
COPY --from=builder build/libs/*.jar ./info.jar
EXPOSE 8280
ENTRYPOINT ["java", "-jar", "info.jar"]
