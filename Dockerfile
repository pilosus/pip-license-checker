FROM clojure:openjdk-16-lein-2.9.5-alpine

RUN mkdir -p /usr/src/app
RUN mkdir /volume
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps

COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app.jar
CMD ["java", "-jar", "app.jar"]
