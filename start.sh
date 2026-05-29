#!/bin/bash
# =============================================================================
#  start.sh — Arranque del servidor Campoaras Configurador
#  Ubicación esperada: /opt/campoaras/start.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Rutas
# ---------------------------------------------------------------------------
APP_DIR="/opt/campoaras"
JAR_NAME="configurador-campoaras-1.0.0.jar"          # ajusta al nombre real del .jar
JAR_PATH="$APP_DIR/$JAR_NAME"
ENV_FILE="$APP_DIR/.env"
LOG_DIR="$APP_DIR/logs"
IMGS_DIR="$APP_DIR/imgs"
PID_FILE="$APP_DIR/app.pid"

# ---------------------------------------------------------------------------
# Comprobaciones previas
# ---------------------------------------------------------------------------
if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] No se encontró el JAR en $JAR_PATH"
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "[ERROR] No se encontró el fichero de variables de entorno en $ENV_FILE"
    exit 1
fi

mkdir -p "$LOG_DIR"
mkdir -p "$IMGS_DIR"

# ---------------------------------------------------------------------------
# Carga de variables de entorno desde el fichero .env de producción
# Se exportan para que la JVM las vea como variables del sistema
# ---------------------------------------------------------------------------
echo "[INFO] Cargando variables de entorno desde $ENV_FILE"
while IFS= read -r line || [ -n "$line" ]; do
    # Ignorar comentarios y líneas vacías
    [[ "$line" =~ ^\s*# ]] && continue
    [[ -z "${line// }" ]] && continue
    # Eliminar espacios alrededor del =
    line=$(echo "$line" | sed 's/[[:space:]]*=[[:space:]]*/=/')
    # Exportar sin interpretar el valor
    key="${line%%=*}"
    value="${line#*=}"
    export "$key=$value"
done < "$ENV_FILE"

# ---------------------------------------------------------------------------
# Opciones de la JVM
# ---------------------------------------------------------------------------
JVM_OPTS=(
    "-Xms256m"
    "-Xmx512m"
    "-XX:+UseG1GC"
    "-XX:+HeapDumpOnOutOfMemoryError"
    "-XX:HeapDumpPath=$LOG_DIR/heapdump.hprof"
    "-Dapp.imgs.path=$IMGS_DIR"              # ruta de imágenes en producción
    "-Dspring.profiles.active=prod"          # activa application-prod.yaml
    "-Dspring.config.additional-location=file:$APP_DIR/"  # carga yamls del directorio
)

# ---------------------------------------------------------------------------
# Arranque
# ---------------------------------------------------------------------------
echo "[INFO] Arrancando $JAR_NAME con perfil 'prod'"
exec java "${JVM_OPTS[@]}" -jar "$JAR_PATH" >> "$LOG_DIR/startup.log" 2>&1 &

echo $! > "$PID_FILE"
echo "[INFO] Servidor arrancado con PID $(cat $PID_FILE)"
