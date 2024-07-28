# 1. 베이스 이미지 설정 - Java 21 사용
FROM openjdk:21-jdk-slim

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. JAR 파일 복사
COPY build/libs/*.jar /app/app.jar

# 5. 실행 명령 설정
ENTRYPOINT ["java", "-jar", "/app/app.jar"]