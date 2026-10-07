# ============================================================
# 🏗️ STAGE 1 - BUILDER
# ============================================================

# Utilizza Java 21 JRE per la fase di estrazione dei layer.
FROM eclipse-temurin:21-jre-jammy AS builder

# 📁 Directory di lavoro
WORKDIR /builder

# 📦 Copia il JAR generato da Maven
COPY ./target/*.jar application.jar

# 📦 Estrae il JAR nei layer Spring Boot.
#
# jarmode=tools:
# Utilizza il nuovo Spring Boot Tools JAR mode.
#
# extract:
# Estrae il contenuto del JAR.
#
# --layers:
# Estrae il contenuto separandolo nei layer:
# - dependencies
# - spring-boot-loader
# - snapshot-dependencies
# - application
#
# --destination extracted:
# Specifica la directory di destinazione.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


# ============================================================
# 🚀 STAGE 2 - RUNTIME
# ============================================================

# Immagine finale che eseguirà l'applicazione.
FROM eclipse-temurin:21-jre-jammy

# 📁 Directory di lavoro
WORKDIR /application

# 📦 Layer 1 - Dipendenze standard
COPY --from=builder /builder/extracted/dependencies/ ./

# 📦 Layer 2 - Spring Boot Loader
COPY --from=builder /builder/extracted/spring-boot-loader/ ./

# 📦 Layer 3 - Snapshot Dependencies
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./

# 📦 Layer 4 - Codice dell'applicazione
COPY --from=builder /builder/extracted/application/ ./


# ============================================================
# 🌐 PORTA
# ============================================================

EXPOSE 8080


# ============================================================
# ▶️ AVVIO
# ============================================================

# Start the application jar - this is not the uber jar used by the builder
# This jar only contains application code and references to the extracted jar files
# This layout is efficient to start up and AOT cache (and CDS) friendly
ENTRYPOINT ["java", "-jar", "application.jar"]