FROM maven:3.8.3-openjdk-17

WORKDIR /app

COPY target/casaapi-1.0.0.jar /app/casaapi.jar

ENTRYPOINT ["java", "-jar", "casaapi.jar"]