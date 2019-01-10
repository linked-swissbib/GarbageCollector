FROM openjdk:8
ADD . /
WORKDIR /
RUN ./gradlew -q --no-scan --no-daemon --no-build-cache shadowJar

FROM openjdk:8-jre-alpine
COPY --from=0 /build/libs/garbage-collector-1.0-all.jar /app/
CMD java -jar /app/garbage-collector-1.0-all.jar
