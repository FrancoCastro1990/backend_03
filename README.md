# 🏦 **Backend_03 - Backend Central con Autenticación JWT**

## 🎯 **Descripción**

Backend central del sistema BFF que proporciona **autenticación JWT completa** y servicios de datos para los 3 BFFs especializados (Web, Mobile, ATM). Implementa el patrón **Backend for Frontend** sirviendo como fuente única de verdad para autenticación y datos.

## 🔐 **Autenticación JWT Implementada**

### Credenciales por Canal
```json
{
  "web": {"username": "admin", "password": "admin123"},
  "mobile": {"username": "mobile", "password": "mobile123"},
  "atm": {"username": "atm", "password": "atm123"}
}
```

### Obtener Token JWT
```bash
# Token para Web BFF
curl -X POST http://localhost:8084/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Token para Mobile BFF
curl -X POST http://localhost:8084/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"mobile","password":"mobile123"}'

# Token para ATM BFF
curl -X POST http://localhost:8084/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"atm","password":"atm123"}'
```

### Usar Token en APIs
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8084/api/accounts/124
```

## 📋 Requerimientos Implementados

### 1. ✅ Configurar Proyecto Spring Batch
- **Spring Boot 3.3.2** con Spring Batch
- **3 Jobs independientes** con Steps configurados
- **Repository GitHub** versionado

### 2. ✅ Implementar Procesamiento de Datos  
- **Lectura CSV** con FlatFileItemReader
- **Transformaciones y validaciones** con ItemProcessor
- **Escritura en PostgreSQL** con ItemWriter personalizado

### 3. ✅ Manejo de Errores y Excepciones
- **CustomSkipPolicy** para datos incorrectos y mal clasificados
- **Validaciones de consistencia** (fechas, montos, tipos)
- **Skip y retry logic** para garantizar integridad

### 4. ✅ Políticas Personalizadas y Tolerancia a Fallos
- **Skip policies personalizadas** por tipo de error
- **Retry logic** con límites configurables
- **Logging detallado** de errores y anomalías

### 5. ✅ Políticas de Escalamiento
- **Multi-threading** con ThreadPoolTaskExecutor
- **Configuración optimizada**: 3 core threads, 5 max threads
- **Chunk processing** con tamaño optimizado por job

## 🎯 Jobs Implementados

### Job 1: `dailyReportJob`
- **Archivo**: `transacciones.csv` 
- **Requerimiento**: "Procesar transacciones diarias para detectar anomalías y generar un resumen"
- **Implementación**: 
  - Detecta anomalías (montos extremos, fechas inválidas)
  - Genera resumen por logging
  - Guarda en tabla `transaction_legacy`

### Job 2: `monthlyInterestJob`  
- **Archivo**: `intereses.csv`
- **Requerimiento**: "Aplicar intereses sobre cuentas y actualizar el saldo final en base de datos"
- **Implementación**:
  - Calcula intereses mensuales por tipo de cuenta
  - Actualiza balance con UPSERT logic
  - Guarda/actualiza en tabla `account`

### Job 3: `annualAccountsJob`
- **Archivo**: `cuentas_anuales.csv` 
- **Requerimiento**: "Compilar datos anuales para cada cuenta y generar un informe detallado para auditorías"
- **Implementación**:
  - Compila datos anuales por cuenta
  - Genera informe detallado por logging
  - Guarda en tabla `annual_account_data`

## 🛠️ Requisitos

- **Java 17**
- **Maven 3.9+** 
- **Docker** (para PostgreSQL)
- **PostgreSQL 16**

## 🚀 Instalación y Configuración

### 1. Iniciar Base de Datos
```bash
docker compose up -d
```

### 2. Compilar y Ejecutar
```bash
# Windows
.\mvnw spring-boot:run

# Linux/Mac  
./mvnw spring-boot:run
```

La aplicación inicia en **http://localhost:8080**

## 🧪 Cómo Probar Cada Job

### Prueba Individual de Jobs

#### 1. Daily Report Job (Detecta Anomalías)
```bash
curl "http://localhost:8080/jobs/run?name=dailyReportJob"
```
**Resultado esperado**: `Started dailyReportJob with status COMPLETED`

**Verifica en logs**: 
- 🚨 Anomalías detectadas
- 📋 Resúmenes generados  
- ✅ Transacciones normales procesadas

#### 2. Monthly Interest Job (Calcula Intereses)
```bash
curl "http://localhost:8080/jobs/run?name=monthlyInterestJob"  
```
**Resultado esperado**: `Started monthlyInterestJob with status COMPLETED`

**Verifica en logs**:
- 💰 Intereses calculados por cuenta
- ✅ Balances finales actualizados
- 🔄 UPSERT logic funcionando

#### 3. Annual Accounts Job (Informe Auditorías)
```bash
curl "http://localhost:8080/jobs/run?name=annualAccountsJob"
```
**Resultado esperado**: `Started annualAccountsJob with status COMPLETED`

**Verifica en logs**:
- 📊 Informes anuales generados
- 🔍 Cuentas marcadas para auditoría 
- ✅ Datos compilados correctamente

### Ver Logs en Tiempo Real
```bash
# Ver logs detallados
tail -f logs/batch-processing.log

# En Windows
Get-Content logs/batch-processing.log -Wait
```

## 📊 Verificación de Datos en Base

### Conectar a PostgreSQL
```bash
docker exec -it backend_03-db-1 psql -U postgres -d batchdb
```

### Verificar Datos Procesados
```sql
-- Transacciones procesadas (dailyReportJob)
SELECT COUNT(*) FROM transaction_legacy;
SELECT * FROM transaction_legacy LIMIT 5;

-- Cuentas con intereses (monthlyInterestJob) 
SELECT COUNT(*) FROM account;
SELECT account_number, balance FROM account LIMIT 5;

-- Datos anuales compilados (annualAccountsJob)
SELECT COUNT(*) FROM annual_account_data;
SELECT account_number, year, total_deposits FROM annual_account_data LIMIT 5;
```

## 🏗️ Arquitectura Técnica

### Stack Tecnológico
- **Spring Boot 3.3.2** + **Spring Batch**
- **PostgreSQL 16** + **Flyway**
- **Docker** + **Maven**

### Estructura Simplificada
```
src/main/java/com/bankxyz/batch/
├── BatchApplication.java
├── job/BatchJobsConfig.java       # 3 Jobs configurados
├── processor/                     # 3 Procesadores independientes
│   ├── TransactionProcessor.java  
│   ├── AccountProcessor.java
│   └── CuentaAnualProcessor.java
├── writer/AccountUpsertWriter.java # Writer personalizado UPSERT
├── model/                         # 3 Entidades JPA
└── web/JobController.java         # REST endpoints

src/main/resources/
├── db/migration/V1__init_schema.sql # Solo tablas necesarias
└── application.yaml

data/                              # 3 Archivos CSV
├── transacciones.csv
├── intereses.csv  
└── cuentas_anuales.csv
```

### Características Técnicas

**✅ Procesamiento Independiente**: Cada CSV se procesa sin dependencias  
**✅ Multi-threading**: 3 core threads, 5 max threads  
**✅ Fault Tolerance**: Skip policies y retry logic  
**✅ UPSERT Logic**: Evita errores de clave duplicada  
**✅ Validaciones de Negocio**: Fechas, montos, tipos de cuenta  
**✅ Detección de Anomalías**: Automática con logging  
**✅ Escalabilidad**: Chunk processing optimizado  

## 📝 Logs y Monitoreo

### Ubicación de Logs
- **Aplicación**: `logs/batch-processing.log`
- **Spring Boot**: Consola estándar

### Métricas Disponibles  
- ⏱️ **Duración total** por job
- 📖 **Registros leídos/procesados/escritos**
- ❌ **Errores y omisiones**
- 🚀 **Throughput** (registros/segundo)
- ✅ **Tasa de éxito** porcentual

## 🔗 **Integración con BFFs**

Este backend central es consumido por los **3 BFFs especializados**:

### 🌐 **Web BFF** (Puerto 8081)
- **Propósito**: Interfaces web completas
- **Datos**: Información completa de cuentas
- **Endpoint**: `http://localhost:8081/web/accounts/*`

### 📱 **Mobile BFF** (Puerto 8082)
- **Propósito**: Aplicaciones móviles
- **Datos**: Información simplificada
- **Endpoint**: `http://localhost:8082/mobile/accounts/*`

### 🏧 **ATM BFF** (Puerto 8083)
- **Propósito**: Cajeros automáticos
- **Datos**: Solo información crítica (balance)
- **Endpoint**: `http://localhost:8083/atm/accounts/*/balance`

### Arquitectura de Comunicación
```
Web BFF ──┐
          ├──► Backend_03 (JWT Auth + Data)
Mobile BFF ┘
ATM BFF ───┘
```

## 🎯 **Estado del Proyecto**

**1. ✅ Proyecto Spring Batch Configurado**
- [x] Spring Batch jobs configurados
- [x] Repository GitHub versionado  
- [x] Steps para leer, procesar, escribir

**2. ✅ Procesamiento de Datos Implementado** 
- [x] Lectura de archivos CSV
- [x] Transformaciones con ItemProcessor
- [x] Validaciones y manejo de errores
- [x] Escritura en PostgreSQL

**3. ✅ Manejo de Errores y Excepciones**
- [x] Datos incorrectos y mal clasificados manejados
- [x] Reglas de consistencia implementadas
- [x] Skip policies personalizadas

**4. ✅ Políticas Personalizadas y Tolerancia a Fallos**
- [x] Políticas personalizadas implementadas
- [x] Tolerancia a fallos correcta
- [x] Retry logic configurado

**5. ✅ Políticas de Escalamiento**
- [x] Multi-threading implementado
- [x] Parámetros optimizados (3-5 threads)
- [x] Chunk processing configurado

### 🎯 Jobs Funcionando Correctamente

- ✅ **dailyReportJob**: COMPLETED - Detecta anomalías  
- ✅ **monthlyInterestJob**: COMPLETED - Actualiza saldos
- ✅ **annualAccountsJob**: COMPLETED - Compila informes
