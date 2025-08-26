# Banco XYZ - Modernización de Procesos Batch (Spring Batch)

Proyecto listo para ejecutar que cumple la actividad: **recrear 3 procesos batch** sobre datos legacy usando **Spring Batch**.

## ✅ Procesos implementados (Jobs)
1. **Reporte de Transacciones Diarias (`dailyReportJob`)**  
   Lee `transacciones.csv`, detecta anomalías simples y persiste filas de reporte diario en `daily_transaction_report`.
2. **Cálculo de Intereses Mensuales (`monthlyInterestJob`)**  
   Lee `intereses.csv`, aplica una tasa según tipo de cuenta y guarda resultados en `monthly_interest`.
3. **Estados de Cuenta Anuales (`annualStatementJob`)**  
   Lee `transacciones.csv` y genera filas base para un estado anual en `annual_statement` (agregación posterior vía SQL/BI).
4. **Procesamiento de Cuentas Anuales (`annualAccountsJob`)**  
   Lee `cuentas_anuales.csv` y procesa datos de cuentas anuales con validaciones específicas.

> Validaciones y manejo de errores: parseo de fechas y montos, omisión (`skip`) de filas inválidas, tolerancia a fallos con `skipLimit`. Procesamiento paralelo configurado con TaskExecutor de 3 hilos.

## 📁 Estructura principal
```
src/main/java/com/bankxyz/batch
 ├─ BatchApplication.java
 ├─ config/AppProperties.java
 ├─ model/ (entities JPA)
 ├─ dto/ (mapeo CSV)
 ├─ repository/
 ├─ job/BatchJobsConfig.java (readers, processors, writers, steps, jobs)
 └─ web/JobController.java (endpoint para ejecutar jobs)
src/main/resources
 ├─ application.yaml
 └─ db/migration/V1__init_schema.sql
docker-compose.yml
```

## � Transformaciones CSV Realizadas

Este proyecto ha sido actualizado con **archivos CSV transformados** que están incluidos en la carpeta `data/`. Los archivos fueron transformados desde datos legacy para coincidir con los DTOs existentes del sistema Spring Batch.

### 📋 Archivos Transformados

#### `transacciones.csv` (10 registros)
- **Formato**: `tx_id,account_number,tx_date,description,amount`
- **Origen**: Datos legacy transformados al formato `TransactionCsv`
- **Problemas preservados**: Montos vacíos, formatos de fecha inconsistentes, duplicados
- **Usado por**: `dailyReportJob` y `annualStatementJob`

#### `intereses.csv` (8 registros) 
- **Formato**: `account_number,owner_name,type,balance`
- **Origen**: Datos legacy transformados al formato `AccountCsv`
- **Problemas preservados**: Tipos en español, balances faltantes
- **Usado por**: `monthlyInterestJob`

#### `cuentas_anuales.csv` (9 registros)
- **Formato**: `numero_cuenta,nombre_propietario,fecha_apertura,balance_inicial,monto_interes_anual`
- **Origen**: Datos legacy transformados al formato `CuentaAnualCsv`
- **Problemas preservados**: Formatos de fecha legacy, montos cero
- **Usado por**: `annualAccountsJob`

### 🎯 Beneficios de la Transformación

✅ **Compatibilidad Total**: Los archivos funcionan perfectamente con los DTOs existentes  
✅ **Sin Variables de Entorno**: No necesitas configurar `BANK_DATA_DIR`  
✅ **Plug & Play**: Los archivos están listos en `data/` del proyecto  
✅ **Problemas Legacy Preservados**: Mantiene la complejidad real para testing  
✅ **Configuración Actualizada**: Rutas de archivos corregidas en `BatchJobsConfig.java`

## 🗃️ Datos legacy originales (Opcional)
Si prefieres usar los CSV del repositorio **bank_legacy_data** original, puedes:
1. Coloca los archivos en una carpeta local 
2. Define la variable de entorno `BANK_DATA_DIR` con esa ruta
3. Restaura las rutas originales en `BatchJobsConfig.java`

**Linux/Mac:**
```bash
export BANK_DATA_DIR=$PWD/data
```

**Windows (PowerShell):**
```powershell
$env:BANK_DATA_DIR = "$PWD\data"
```

**Windows (CMD):**
```cmd
set BANK_DATA_DIR=%CD%\data
```

## 🛠️ Requisitos
- Java 17
- Maven 3.9+
- Docker (opcional, para levantar PostgreSQL rápido)

## 🐘 Base de datos (PostgreSQL)
Inicia una base con Docker:

**Linux/Mac:**
```bash
docker compose up -d
```

**Windows:**
```powershell
docker compose up -d
```
Credenciales por defecto en `application.yaml`:
- url: `jdbc:postgresql://localhost:5432/batchdb`
- user: `postgres` / pass: `postgres`

Flyway crea las tablas al iniciar la app.

## 🚀 Cómo ejecutar

### Compilar y arrancar la aplicación:

**Linux/Mac:**
```bash
mvn -q spring-boot:run
```

**Windows (usando Maven Wrapper):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Windows (si tienes Maven instalado):**
```powershell
mvn -q spring-boot:run
```

### Ejecutar Jobs:
Ejecuta un Job desde el navegador o curl:

**Linux/Mac/Windows (PowerShell con curl instalado):**
```bash
# Daily report
curl "http://localhost:8080/jobs/run?name=dailyReportJob"

# Interés mensual
curl "http://localhost:8080/jobs/run?name=monthlyInterestJob"

# Estado anual
curl "http://localhost:8080/jobs/run?name=annualStatementJob"

# Cuentas anuales
curl "http://localhost:8080/jobs/run?name=annualAccountsJob"
```

### 🧪 Script de Pruebas Automatizado

Para ejecutar todos los jobs de forma secuencial y validar el sistema completo:

**Windows:**
```cmd
test_batch_requirements.bat
```

**Linux/Mac:**
```bash
./test_batch_requirements.sh
```

Este script ejecuta automáticamente los 4 jobs, valida los resultados y proporciona un reporte completo del rendimiento del sistema.

## � Características Avanzadas Implementadas

### 🎯 Spring Batch Profesional
- **4 Jobs Configurados**: Procesamiento completo del ciclo bancario
- **Procesamiento Paralelo**: TaskExecutor con 3 hilos para máximo rendimiento
- **Chunk Processing**: Configurado con tamaño óptimo de 5 registros
- **Fault Tolerance**: Skip policies y retry logic personalizados

### 🛡️ Manejo de Errores Robusto
- **CustomSkipPolicy**: Manejo inteligente de registros inválidos
- **Validaciones de Negocio**: Formatos de cuenta, montos y fechas
- **Detección de Anomalías**: Identificación automática de transacciones sospechosas
- **Logging Detallado**: Trazabilidad completa en `logs/batch-processing.log`

### 📈 Monitoreo y Observabilidad
- **BatchJobListener**: Métricas en tiempo real de jobs
- **BatchStepListener**: Estadísticas detalladas por step
- **Performance Tracking**: Duración, throughput y eficiencia
- **Error Reporting**: Reportes automáticos de fallos y anomalías

### 🔄 Compatibilidad Legacy
- **Preservación de Problemas**: Mantiene la complejidad real de datos legacy
- **Transformación Inteligente**: Archivos adaptados sin perder características
- **Validación Flexible**: Manejo de formatos inconsistentes
- **Migración Gradual**: Soporte para datos legacy y nuevos formatos

### 📝 Logs y Debugging
```bash
# Ver logs en tiempo real
tail -f logs/batch-processing.log

# Verificar estado de la base de datos
docker compose logs postgres
```

## 🏗️ Arquitectura Técnica

### Stack Tecnológico
- **Spring Boot 3.3.2**: Framework base
- **Spring Batch**: Motor de procesamiento
- **PostgreSQL 16**: Base de datos principal
- **Flyway**: Migración de esquemas
- **Hibernate/JPA**: ORM
- **Docker**: Containerización de BD

### Patrones Implementados
- **Reader-Processor-Writer**: Patrón estándar Spring Batch
- **Skip Pattern**: Tolerancia a fallos selectiva
- **Listener Pattern**: Observabilidad y métricas
- **Configuration Pattern**: Configuración centralizada

---

## 🎉 Estado del Proyecto

**✅ COMPLETO Y FUNCIONAL**

- [x] 4 Jobs Spring Batch implementados y funcionando
- [x] Archivos CSV transformados y compatibles
- [x] Procesamiento paralelo optimizado
- [x] Manejo robusto de errores y anomalías
- [x] Scripts de prueba automatizados
- [x] Documentación completa
- [x] Base de datos configurada con Docker
- [x] Logs y monitoreo implementados

**🚀 Listo para producción con datos legacy reales**

