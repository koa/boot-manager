FROM maven:3.5.3-jdk-8-alpine as build-env
ADD . /build/boot-manager
WORKDIR /build/boot-manager
ARG MAVEN_PROXY
COPY settings.xml.template /
ENV MAVEN_PROXY ${MAVEN_PROXY:-x}
RUN mkdir $HOME/.m2
RUN if [ $MAVEN_PROXY != "x" ]; then \
sed s#_MIRROR_URL_#$MAVEN_PROXY# <settings.xml.template >$HOME/.m2/settings.xml; \
fi
RUN mvn clean install
RUN mkdir -p /app
RUN mv target/boot-manager*.jar /app/boot-manager.jar

FROM distroless/java:1034974eb63a
COPY --from=build-env /app /app
WORKDIR /app
VOLUME /data
CMD ["boot-manager.jar","--spring.datasource.url", "jdbc:hsqldb:file:/data/db", "--spring.jpa.hibernate.ddl-auto", "update" ]