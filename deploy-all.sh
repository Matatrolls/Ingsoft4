#!/bin/bash

# ============================================================================
# Script de Despliegue Completo SITM-MIO
# ============================================================================
# Este script despliega tanto el servidor como el cliente en sus
# respectivos computadores remotos usando SSH/SCP
# ============================================================================

# Colores para mensajes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# Funciones
# ============================================================================

print_usage() {
    echo "Uso: $0 <usuario_servidor@host_servidor> <usuario_cliente@host_cliente> [opciones]"
    echo ""
    echo "Ejemplo:"
    echo "  $0 admin@192.168.1.100 admin@192.168.1.101"
    echo "  $0 admin@server.local admin@client.local --server-port 2222 --client-port 22"
    echo ""
    echo "Opciones:"
    echo "  --server-port PORT        Puerto SSH del servidor (default: 22)"
    echo "  --server-dir DIRECTORY    Directorio remoto del servidor (default: ~/sitm-mio)"
    echo "  --client-port PORT        Puerto SSH del cliente (default: 22)"
    echo "  --client-dir DIRECTORY    Directorio remoto del cliente (default: ~/sitm-mio)"
    echo "  -h, --help                Mostrar esta ayuda"
    echo ""
    echo "Nota: Este script ejecuta deploy-server.sh y deploy-client.sh"
}

# ============================================================================
# Procesamiento de argumentos
# ============================================================================

if [ $# -lt 2 ]; then
    print_usage
    exit 1
fi

SERVER_TARGET="$1"
CLIENT_TARGET="$2"
shift 2

SERVER_PORT="22"
CLIENT_PORT="22"
SERVER_DIR="~/sitm-mio"
CLIENT_DIR="~/sitm-mio"

# Procesar opciones adicionales
while [[ $# -gt 0 ]]; do
    case $1 in
        --server-port)
            SERVER_PORT="$2"
            shift 2
            ;;
        --server-dir)
            SERVER_DIR="$2"
            shift 2
            ;;
        --client-port)
            CLIENT_PORT="$2"
            shift 2
            ;;
        --client-dir)
            CLIENT_DIR="$2"
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

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         Despliegue Completo SITM-MIO                      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Verificar scripts de despliegue
if [ ! -f "deploy-server.sh" ] || [ ! -f "deploy-client.sh" ]; then
    echo -e "${RED}Error: No se encontraron los scripts deploy-server.sh o deploy-client.sh${NC}"
    exit 1
fi

# Despliegue del servidor
echo -e "${GREEN}═══ PASO 1/2: Desplegando Servidor ═══${NC}"
echo ""
./deploy-server.sh "$SERVER_TARGET" -p "$SERVER_PORT" -d "$SERVER_DIR"
SERVER_EXIT_CODE=$?

if [ $SERVER_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}Error en el despliegue del servidor${NC}"
    exit $SERVER_EXIT_CODE
fi

echo ""
echo ""

# Despliegue del cliente
echo -e "${GREEN}═══ PASO 2/2: Desplegando Cliente ═══${NC}"
echo ""
./deploy-client.sh "$CLIENT_TARGET" -p "$CLIENT_PORT" -d "$CLIENT_DIR"
CLIENT_EXIT_CODE=$?

if [ $CLIENT_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}Error en el despliegue del cliente${NC}"
    exit $CLIENT_EXIT_CODE
fi

# Resumen final
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           Despliegue Completado Exitosamente              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Servidor desplegado en:${NC} $SERVER_TARGET:$SERVER_DIR"
echo -e "${GREEN}Cliente desplegado en:${NC} $CLIENT_TARGET:$CLIENT_DIR"
echo ""
echo -e "${YELLOW}Pasos siguientes:${NC}"
echo ""
echo "1. Iniciar el servidor:"
echo "   ssh ${SERVER_TARGET}"
echo "   cd ${SERVER_DIR}"
echo "   ./run-server.sh"
echo ""
echo "2. Iniciar el cliente (en otra terminal):"
echo "   ssh ${CLIENT_TARGET}"
echo "   cd ${CLIENT_DIR}"
echo "   ./run-client.sh"
echo ""
