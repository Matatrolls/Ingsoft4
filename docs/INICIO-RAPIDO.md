# INICIO RÁPIDO - SISTEMA SITM-MIO

**5 minutos para ejecutar el sistema completo**

---

## ⚡ Prerrequisitos

```bash
# Verificar Java 17+
java -version

# Verificar Maven
mvn -version

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

---

## 🚀 OPCIÓN 1: Demo Completo (Recomendado)

### Paso 1: Compilar

```bash
cd /opt/incoming/SITM-MIO
mvn clean compile
```

**Tiempo**: ~10 segundos
**Resultado**: BUILD SUCCESS ✅

### Paso 2: Iniciar Servidor (Terminal 1)

```bash
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ServerMain"
```

**Verás**:
```
Grafo cargado en memoria. Paradas: 2120, Rutas: 106
[ICE][SERVER] AdminChannel listo en tcp -h localhost -p 10000
```

✅ Servidor listo. **NO CERRAR ESTA TERMINAL**.

### Paso 3: Cliente Conductor (Terminal 2)

```bash
# Nueva terminal
cd /opt/incoming/SITM-MIO
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ClientMain"
```

**Interacción**:
```
Seleccione su rol: 2 (Conductor)
ID de bus: BUS-101
Menú → 1. Reportar evento → 1. Emergencia médica
Descripción: Pasajero necesita atención
```

**Resultado**:
```
✅ EVENTO ENVIADO EXITOSAMENTE
📡 Evento enviado al servidor centralizado
```

### Paso 4: Cliente Controlador (Terminal 3)

```bash
# Nueva terminal
cd /opt/incoming/SITM-MIO
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ClientMain"
```

**Interacción**:
```
Seleccione su rol: 3 (Controlador)
ID controlador: CTRL-001
```

**Resultado AUTOMÁTICO** (5-10 segundos después del reporte):
```
╔════════════════════════════════════════════════════════════╗
║  Bienvenido Controlador CTRL-001
║  Sistema de notificaciones activo
║  Polling automático de eventos Ice: ACTIVO
╚════════════════════════════════════════════════════════════╝

🔄 Thread de polling automático iniciado

[Esperando... cada 5 segundos revisa eventos nuevos]

🔔 ═══════════════════════════════════════════════════════════════
🔔 NUEVA NOTIFICACIÓN PARA CONTROLADOR CTRL-001
🔔 ═══════════════════════════════════════════════════════════════
⚠️ ALERTA: CRÍTICA - Emergencia médica en bus BUS-101 - Pasajero necesita atención
🔔 ═══════════════════════════════════════════════════════════════

🔔 Tienes 1 notificación(es) pendiente(s)
```

**También puedes consultar manualmente**:
```
Menú → 4. Ver eventos de buses
```

✅ **¡Comunicación Ice con notificaciones automáticas!** El controlador recibe alertas en tiempo real (polling cada 5 segundos) sin necesidad de consultar manualmente.

---

## 🛣️ OPCIÓN 2: Búsqueda de Rutas

### Cliente Usuario

```bash
cd /opt/incoming/SITM-MIO
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.ClientMain"
```

**Interacción**:
```
Seleccione su rol: 1 (Usuario)
Menú → 3. Ver información de ruta

Parada ORIGEN: terminal
→ Selecciona: 1 (Terminal Menga)

Parada DESTINO: univalle
→ Selecciona: 1 (Universidad del Valle)
```

**Resultado**:
```
🔍 Calculando ruta con Dijkstra...

RESULTADO:
📍 ORIGEN:  Terminal Menga
📍 DESTINO: Universidad del Valle

🚍 Paradas: 15
📏 Distancia: 12.35 km
⏱️  Tiempo estimado: 28.5 minutos
🚀 Velocidad promedio: 26.0 km/h
🚌 Líneas a tomar: T31 → P17
```

---

## 🧪 OPCIÓN 3: Tests Rápidos

### Test 1: Velocidades Históricas

```bash
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.TestArcVelocityCalculation"
```

**Tiempo**: ~5 segundos | **Procesa**: 15,000 datagramas

### Test 2: Análisis de Zonas

```bash
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.TestZoneAnalysis"
```

### Test 3: Streaming Tiempo Real

```bash
mvn exec:java -Dexec.mainClass="co.edu.icesi.mio.app.TestRealtimeStreaming"
```

---

## 📚 Documentación Completa

`docs/GUIA-EJECUCION-EXPERIMENTOS.md` - Guía experimental detallada

**¡Listo para usar!** 🎉
