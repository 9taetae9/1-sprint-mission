FROM amazoncorretto:17

WORKDIR /app

COPY . .

ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8
ENV JVM_OPTS=""

RUN ./gradlew clean build -x test

EXPOSE 80

CMD java $JVM_OPTS -jar build/libs/$PROJECT_NAME-$PROJECT_VERSION.jar