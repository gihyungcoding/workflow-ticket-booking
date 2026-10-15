FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN ./gradlew bootJar -x test --no-daemon \
 && cp "$(ls build/libs/*.jar | grep -v -- -plain.jar)" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
