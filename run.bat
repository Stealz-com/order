@echo off
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/order_db?createDatabaseIfNotExist=true
set SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn spring-boot:run
