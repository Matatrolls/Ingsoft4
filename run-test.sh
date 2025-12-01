#!/bin/bash

# Script para ejecutar la prueba automatizada del sistema de notificaciones
# Uso: ./run-test.sh

echo "╔════════════════════════════════════════════════════════╗"
echo "║   PRUEBA AUTOMATIZADA - SISTEMA DE NOTIFICACIONES      ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

echo "✓ Configurando JAVA_HOME: $JAVA_HOME"
echo "✓ Compilando y ejecutando prueba..."
echo ""

# Ejecutar la prueba
mvn clean compile exec:java -Dexec.mainClass="co.edu.icesi.mio.app.TestNotifications"

echo ""
echo "════════════════════════════════════════════════════════"
echo " Prueba finalizada"
echo "════════════════════════════════════════════════════════"
