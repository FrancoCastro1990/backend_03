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
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Token para Mobile BFF
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mobile","password":"mobile123"}'

# Token para ATM BFF
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"atm","password":"atm123"}'
```

### Usar Token en APIs
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8084/api/accounts/124
```

## ✅ **Características del Backend Central**

### 🔐 **Autenticación JWT**
- ✅ **JWT completo**: Generación y validación de tokens
- ✅ **Canales especializados**: Tokens específicos por canal (web, mobile, atm)
- ✅ **Spring Security**: Integración completa con Spring Security
- ✅ **Role-based Access**: Control de acceso por roles

### 📊 **Servicios de Datos**
- ✅ **API REST**: Endpoints para cuentas y transacciones
- ✅ **Base de datos PostgreSQL**: Persistencia de datos
- ✅ **Health checks**: Monitoreo de estado del servicio
- ✅ **Documentación**: Endpoints documentados

## 📊 **Endpoints de la API**

### 👤 **Información de Cuentas**
```bash
# Lista completa de cuentas
GET /api/accounts

# Datos detallados de una cuenta específica
GET /api/accounts/{accountNumber}

# Ejemplo de respuesta completa:
{
  "accountNumber": "124",
  "ownerName": "Diana Prince",
  "balance": 15000.00,
  "currency": "USD",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdDate": "2023-01-15",
  "lastTransaction": "2024-09-15"
}
```

### 💳 **Transacciones**
```bash
# Historial de transacciones de una cuenta
GET /api/accounts/{accountNumber}/transactions

# Con filtros opcionales
GET /api/accounts/{accountNumber}/transactions?startDate=2024-01-01&endDate=2024-09-15
```

### 🔍 **Búsqueda y Filtros**
```bash
# Búsqueda por propietario
GET /api/accounts/search?owner=Diana

# Filtros por tipo de cuenta
GET /api/accounts?type=CHECKING

# Paginación
GET /api/accounts?page=0&size=10&sort=balance,desc
```

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

La aplicación inicia en **http://localhost:8084**

## 🧪 **Ejemplos de Uso**

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

### 1. Verificación Completa de API
```bash
# Script completo de verificación
echo "=== VERIFICACIÓN BACKEND CENTRAL ==="

# 1. Obtener token
TOKEN=$(curl -s -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

echo "Token obtenido: ${TOKEN:0:50}..."

# 2. Verificar lista de cuentas
echo "Lista de cuentas:"
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/accounts | jq '.[0]'

# 3. Verificar datos de cuenta específica
echo "Datos de cuenta 124:"
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/accounts/124 | jq '.'

# 4. Verificar transacciones
echo "Transacciones de cuenta 124:"
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/accounts/124/transactions | jq '.[0]'
```

### 2. Verificación de Autenticación
```bash
# Verificar endpoint de información del usuario
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/auth/me
```

## 🏗️ Arquitectura Técnica

### Stack Tecnológico
- **Spring Boot 3.3.2** - Framework principal
- **Spring Security** - Autenticación y autorización JWT
- **PostgreSQL 16** - Base de datos
- **Spring Data JPA** - Acceso a datos
- **Docker** - Contenedorización

### Estructura del Proyecto
```
backend_03/
├── src/main/java/com/bankxyz/batch/
│   ├── BatchApplication.java
│   ├── controller/
│   │   ├── AuthController.java          # /api/auth/*
│   │   └── AccountController.java       # /api/accounts/*
│   ├── service/
│   │   ├── AuthService.java             # Lógica de autenticación
│   │   └── AccountService.java          # Lógica de cuentas
│   ├── repository/
│   │   └── AccountRepository.java       # Acceso a BD
│   └── model/
│       └── Account.java                 # Entidad JPA
├── src/main/resources/
│   ├── application.yml                  # Config puerto 8084
│   └── data.sql                         # Datos de prueba
└── pom.xml                              # Dependencias Maven
```

### Características Técnicas
- ✅ **JWT Authentication**: Tokens seguros con expiración
- ✅ **Role-based Security**: Control de acceso granular
- ✅ **Database Integration**: PostgreSQL con JPA
- ✅ **RESTful API**: Endpoints bien diseñados
- ✅ **Health Monitoring**: Actuator para monitoreo
- ✅ **Error Handling**: Manejo robusto de errores  

## � **Monitoreo y Health Checks**

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Métricas Disponibles
```bash
# Información del servicio
curl http://localhost:8084/actuator/info

# Métricas de rendimiento
curl http://localhost:8084/actuator/metrics
```

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

**✅ Proyecto Backend Central Completado**
- [x] Autenticación JWT implementada
- [x] API REST completa
- [x] Integración con PostgreSQL
- [x] Health checks operativos
- [x] Documentación completa
- [x] Integración con 3 BFFs

---

**🚀 Backend central operativo y sirviendo a todos los BFFs especializados**
