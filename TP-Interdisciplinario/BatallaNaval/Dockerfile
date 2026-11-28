# ---------- Etapa 1: compilar con la JDK 17 ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir out \
 && javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java") \
 && jar cf BatallaNaval.jar -C out .

# ---------- Etapa 2: solo lo necesario para ejecutar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/BatallaNaval.jar .
ENV PUERTO=5000 \
    HISTORIAL=/app/data/historial_partidas.txt
EXPOSE 5000
CMD ["java", "-cp", "BatallaNaval.jar", "ar.edu.et32.batallanaval.servidor.MainServidor"]
