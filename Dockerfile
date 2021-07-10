# syntax=docker/dockerfile:1
# multi-stage docker file, separate build and run steps
# https://docs.docker.com/develop/develop-images/multistage-build/

# build step
FROM clojure:openjdk-11-lein-2.9.6-slim-buster
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app.jar

# run step
FROM openjdk:11-jre-slim-buster
RUN mkdir /volume
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY --from=0 /usr/src/app/app.jar /usr/src/app/
CMD ["java", "-jar", "app.jar"]
