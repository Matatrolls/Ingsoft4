
#!/bin/bash

# Script para ejecutar el Servidor SITM-MIO
# Uso: ./run-server.sh

echo "╔════════════════════════════════════════════════════════╗"
echo "║        SERVIDOR SITM-MIO                               ║"
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
echo "✓ Compilando proyecto..."
echo ""

# Compilar el proyecto
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Error al compilar el proyecto"
    exit 1
fi

echo "✓ Compilación exitosa"
echo ""
echo "════════════════════════════════════════════════════════"
echo " Iniciando Servidor..."
echo "════════════════════════════════════════════════════════"
echo ""

# Ejecutar el servidor
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ServerMain"
