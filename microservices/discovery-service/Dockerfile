# ============================================================
# BUILDER
# ============================================================

FROM eclipse-temurin:21-jre-jammy AS builder

WORKDIR /builder

COPY ./target/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract \
    --layers \
    --destination extracted


# ============================================================
# RUNTIME
# ============================================================

FROM eclipse-temurin:21-jre-jammy

WORKDIR /application

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./


# ============================================================
# PORT
# ============================================================

EXPOSE 8080


# ============================================================
# START
# ============================================================

# Start the application jar - this is not the uber jar used by the builder
# This jar only contains application code and references to the extracted jar files
# This layout is efficient to start up and AOT cache (and CDS) friendly
ENTRYPOINT ["java", "-jar", "application.jar"]