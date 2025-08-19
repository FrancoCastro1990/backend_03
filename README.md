# Banco XYZ - Modernización de Procesos Batch (Spring Batch)

Proyecto listo para ejecutar que cumple la actividad: **recrear 3 procesos batch** sobre datos legacy usando **Spring Batch**.

## ✅ Procesos implementados (Jobs)
1. **Reporte de Transacciones Diarias (`dailyReportJob`)**  
   Lee `transactions.csv`, detecta anomalías simples y persiste filas de reporte diario en `daily_transaction_report`.
2. **Cálculo de Intereses Mensuales (`monthlyInterestJob`)**  
   Lee `accounts.csv`, aplica una tasa según tipo de cuenta y guarda resultados en `monthly_interest`.
3. **Estados de Cuenta Anuales (`annualStatementJob`)**  
   Lee `transactions.csv` y genera filas base para un estado anual en `annual_statement` (agregación posterior vía SQL/BI).

> Validaciones y manejo de errores: parseo de fechas y montos, omisión (`skip`) de filas inválidas, tolerancia a fallos con `skipLimit`.

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

## 🗃️ Datos legacy
Este proyecto espera los CSV del repositorio **bank_legacy_data**.  
Coloca los archivos en una carpeta local y define la variable de entorno `BANK_DATA_DIR` con esa ruta.  
- `accounts.csv`
- `transactions.csv`

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

# Interés mensual (puedes forzar el mes con -Dmonth=2025-08)
curl "http://localhost:8080/jobs/run?name=monthlyInterestJob"

# Estado anual
curl "http://localhost:8080/jobs/run?name=annualStatementJob"
```

