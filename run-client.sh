#!/bin/bash

# Script para ejecutar el Cliente SITM-MIO
# Uso: ./run-client.sh

echo "╔════════════════════════════════════════════════════════╗"
echo "║        CLIENTE SITM-MIO                                ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Verificar que Maven esté instalado
if ! command -v mvn &> /dev/null
then
    echo "❌ Maven no está instalado. Por favor instala Maven primero."
    exit 1
fi

echo "✓ Configurando JAVA_HOME: $JAVA_HOME"
echo ""
echo "⚠️  IMPORTANTE: Asegúrate de que el SERVIDOR esté corriendo"
echo "   Si no lo has iniciado, ejecuta en otra terminal:"
echo "   ./run-server.sh"
echo ""
read -p "¿El servidor está corriendo? (y/n): " response

if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Por favor inicia el servidor primero con: ./run-server.sh"
    exit 0
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo " Iniciando Cliente..."
echo "════════════════════════════════════════════════════════"
echo ""

# Ejecutar el cliente
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ClientMain"
