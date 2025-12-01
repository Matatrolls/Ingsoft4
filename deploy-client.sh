#!/bin/bash

# ============================================================================
# Script de Despliegue del Cliente SITM-MIO
# ============================================================================
# Este script despliega el cliente del Sistema de Gestión del MIO
# en un computador remoto usando SSH/SCP
# ============================================================================

# Colores para mensajes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuración (modificar según necesidades)
CLIENT_HOST="${CLIENT_HOST:-}"          # IP o hostname del cliente
CLIENT_USER="${CLIENT_USER:-}"          # Usuario SSH
CLIENT_PORT="${CLIENT_PORT:-22}"        # Puerto SSH (default: 22)
REMOTE_DIR="${REMOTE_DIR:-~/sitm-mio}"  # Directorio remoto de destino

# Archivos a desplegar
JAR_FILE="target/sitm-mio-client.jar"
RUN_SCRIPT="run-client.sh"

# ============================================================================
# Funciones
# ============================================================================

print_usage() {
    echo "Uso: $0 <usuario@host> [opciones]"
    echo ""
    echo "Ejemplo:"
    echo "  $0 usuario@192.168.1.101"
    echo "  $0 usuario@192.168.1.101 -p 2222 -d /opt/sitm-mio"
    echo ""
    echo "Variables de entorno:"
    echo "  CLIENT_HOST    - Host del cliente (IP o hostname)"
    echo "  CLIENT_USER    - Usuario SSH"
    echo "  CLIENT_PORT    - Puerto SSH (default: 22)"
    echo "  REMOTE_DIR     - Directorio remoto (default: ~/sitm-mio)"
    echo ""
    echo "Opciones:"
    echo "  -p, --port PORT        Puerto SSH (default: 22)"
    echo "  -d, --dir DIRECTORY    Directorio remoto de destino"
    echo "  -h, --help             Mostrar esta ayuda"
}

check_requirements() {
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}Error: No se encontró $JAR_FILE${NC}"
        echo "Ejecuta primero: mvn clean package"
        exit 1
    fi

    if ! command -v scp &> /dev/null; then
        echo -e "${RED}Error: scp no está instalado${NC}"
        exit 1
    fi
}

# ============================================================================
# Procesamiento de argumentos
# ============================================================================

if [ $# -eq 0 ]; then
    print_usage
    exit 1
fi

# Primer argumento: usuario@host
if [[ "$1" =~ ^[^@]+@[^@]+$ ]]; then
    CLIENT_USER="${1%@*}"
    CLIENT_HOST="${1#*@}"
    shift
else
    echo -e "${RED}Error: Formato incorrecto. Use: usuario@host${NC}"
    print_usage
    exit 1
fi

# Procesar opciones adicionales
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            CLIENT_PORT="$2"
            shift 2
            ;;
        -d|--dir)
            REMOTE_DIR="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Opción desconocida: $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# ============================================================================
# Despliegue
# ============================================================================

echo -e "${GREEN}=== Despliegue del Cliente SITM-MIO ===${NC}"
echo ""
echo "Configuración:"
echo "  Host:       $CLIENT_HOST"
echo "  Usuario:    $CLIENT_USER"
echo "  Puerto:     $CLIENT_PORT"
echo "  Directorio: $REMOTE_DIR"
echo ""

# Verificar requisitos
check_requirements

# Crear directorio remoto si no existe
echo -e "${YELLOW}[1/4] Creando directorio remoto...${NC}"
ssh -p "$CLIENT_PORT" "${CLIENT_USER}@${CLIENT_HOST}" "mkdir -p $REMOTE_DIR" || {
    echo -e "${RED}Error al crear directorio remoto${NC}"
    exit 1
}

# Copiar JAR del cliente
echo -e "${YELLOW}[2/4] Transfiriendo JAR del cliente ($(du -h $JAR_FILE | cut -f1))...${NC}"
scp -P "$CLIENT_PORT" "$JAR_FILE" "${CLIENT_USER}@${CLIENT_HOST}:${REMOTE_DIR}/" || {
    echo -e "${RED}Error al transferir archivo JAR${NC}"
    exit 1
}

# Copiar script de ejecución
echo -e "${YELLOW}[3/4] Transfiriendo script de ejecución...${NC}"
scp -P "$CLIENT_PORT" "$RUN_SCRIPT" "${CLIENT_USER}@${CLIENT_HOST}:${REMOTE_DIR}/" || {
    echo -e "${RED}Error al transferir script de ejecución${NC}"
    exit 1
}

# Dar permisos de ejecución
echo -e "${YELLOW}[4/4] Configurando permisos...${NC}"
ssh -p "$CLIENT_PORT" "${CLIENT_USER}@${CLIENT_HOST}" "chmod +x ${REMOTE_DIR}/${RUN_SCRIPT}" || {
    echo -e "${RED}Error al configurar permisos${NC}"
    exit 1
}

echo ""
echo -e "${GREEN}✓ Despliegue completado exitosamente!${NC}"
echo ""
echo "Para ejecutar el cliente:"
echo "  ssh -p $CLIENT_PORT ${CLIENT_USER}@${CLIENT_HOST}"
echo "  cd $REMOTE_DIR"
echo "  ./$RUN_SCRIPT"
echo ""
