# API de órdenes de transporte

API REST para manejar órdenes de transporte, conductores y la asignación de un conductor a cada orden (con su PDF y su imagen de evidencia). Hecha con Java 17 y Spring Boot 3.

La base de datos es H2 en memoria, así que no hay nada que instalar: cada vez que levantas la app arranca limpia.

## Qué necesitas

- JDK 17 o superior
- Maven 3.9 o superior

## Cómo correrla

```bash
mvn spring-boot:run
```

Queda en `http://localhost:8080`. La documentación interactiva (Swagger) está en:

```
http://localhost:8080/swagger-ui.html
```

## Cómo correr las pruebas

```bash
mvn test
```

Hay pruebas unitarias con JUnit 5 y Mockito (servicios, flujo de estados, almacenamiento de archivos, JWT) y una prueba de integración que recorre el flujo completo con la app levantada.

## Cómo usarla

Todas las rutas, salvo las de `/api/auth` y Swagger, piden un token JWT. Primero te registras y guardas el token:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"carlos","password":"clave-segura-1"}'
```

Después lo mandas en cada petición con el header `Authorization: Bearer <token>`. Si ya tienes usuario, `POST /api/auth/login` te da un token nuevo. Dura 60 minutos.

Un recorrido típico:

```bash
TOKEN="pega-aqui-el-token"

#  Crear un conductor
curl -X POST http://localhost:8080/api/drivers \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Ana Perez","licenseNumber":"LIC-001"}'

#  Crear una orden
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"origin":"Puebla","destination":"Ciudad de Mexico"}'

#  Asignarle el conductor (usa los ids que devolvieron los pasos anteriores)
curl -X POST http://localhost:8080/api/orders/<orderId>/assignment \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"driverId":"<driverId>"}'

# Subir el PDF y la imagen
curl -X POST http://localhost:8080/api/orders/<orderId>/assignment/pdf \
  -H "Authorization: Bearer $TOKEN" -F "file=@guia.pdf;type=application/pdf"

curl -X POST http://localhost:8080/api/orders/<orderId>/assignment/image \
  -H "Authorization: Bearer $TOKEN" -F "file=@evidencia.png;type=image/png"

#  Mover la orden de estado
curl -X PATCH http://localhost:8080/api/orders/<orderId>/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"IN_TRANSIT"}'
```

## Endpoints

| Método | Ruta | Para qué sirve |
|--------|------|----------------|
| POST | `/api/auth/register` | Crear usuario y recibir token |
| POST | `/api/auth/login` | Iniciar sesión y recibir token |
| POST | `/api/orders` | Crear una orden (nace en `CREATED`) |
| GET | `/api/orders/{id}` | Consultar una orden |
| GET | `/api/orders` | Listar órdenes con filtros |
| PATCH | `/api/orders/{id}/status` | Cambiar el estado de la orden |
| POST | `/api/drivers` | Crear un conductor |
| GET | `/api/drivers/active` | Listar los conductores activos |
| POST | `/api/orders/{id}/assignment` | Asignar un conductor a la orden |
| GET | `/api/orders/{id}/assignment` | Ver la asignación de la orden |
| POST | `/api/orders/{id}/assignment/pdf` | Adjuntar un PDF |
| POST | `/api/orders/{id}/assignment/image` | Adjuntar una imagen (.png, .jpg) |

Filtros de `GET /api/orders` (todos opcionales y combinables):

- `status`: `CREATED`, `IN_TRANSIT`, `DELIVERED` o `CANCELLED`
- `createdFrom` y `createdTo`: fechas `yyyy-MM-dd`, incluyen el día completo
- `origin` y `destination`: texto que debe estar contenido en el origen o destino (sin importar mayúsculas)

## Reglas que conviene saber

**Estados de la orden.** Solo se puede avanzar así:

```
CREATED ──► IN_TRANSIT ──► DELIVERED
   │             │
   └──► CANCELLED ◄┘
```

`DELIVERED` y `CANCELLED` son finales. Cualquier otro cambio responde `409`.

**Asignación.** Solo se puede asignar un conductor si está activo y la orden sigue en `CREATED`. Una orden tiene una sola asignación. Para crear un conductor inactivo, manda `"active": false`.

**Archivos.** Se guardan en la carpeta `uploads/` (se puede cambiar con la variable `STORAGE_DIR`) con un nombre generado, y la asignación solo guarda ese nombre. El PDF debe ser `.pdf` con tipo `application/pdf`; la imagen, `.png` o `.jpg/.jpeg`. Máximo 5 MB. Si subes otro archivo del mismo tipo, reemplaza al anterior.

**Errores.** Todos tienen el mismo formato:

```json
{
  "timestamp": "2026-10-31T10:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Hay errores de validación en la petición",
  "path": "/api/orders",
  "details": ["origin: el origen es obligatorio"]
}
```

## Configuración

Está en `src/main/resources/application.properties`. Lo que probablemente quieras cambiar en un entorno real se puede pasar por variable de entorno:

| Variable | Qué hace | Valor por defecto |
|----------|----------|-------------------|
| `JWT_SECRET` | Secreto para firmar los tokens (mínimo 32 caracteres) | uno de desarrollo, cámbialo |
| `STORAGE_DIR` | Carpeta donde se guardan los archivos | `uploads` |

Los logs salen en JSON, una línea por evento. Cada petición lleva un `requestId` (también va en el header `X-Request-Id`) para poder seguirla en los logs.

## Cómo está organizado

```
src/main/java/com/transportes/api
├── auth         registro y login
├── security     JWT, filtro y configuración de Spring Security
├── user         usuarios de la API
├── order        órdenes, estados y filtros
├── driver       conductores
├── assignment   asignación conductor-orden y sus archivos
├── storage      guardado de PDF e imágenes en disco
├── config       configuración de Swagger
└── common       manejo de errores y logging
```

Cada módulo sigue la misma estructura: controlador, servicio, repositorio, entidad, DTOs y un mapper escrito a mano.

## Notas

- Como la base es en memoria, al reiniciar se pierden usuarios, conductores y órdenes (los archivos de `uploads/` sí se quedan en disco).

## Ejecución con Docker

Este proyecto incluye un `Dockerfile` multi-stage build para empaquetar y ejecutar la aplicación de forma contenerizada.

### Construir la imagen de Docker
Ubicado en la raíz del proyecto (donde se encuentra el `Dockerfile`), ejecutar:

```bash
docker build -t transport-orders-api .
```


### Levantar el contenedor
La aplicación requiere una clave secreta para la validación de seguridad (JWT). Debes pasarla mediante la variable de entorno JWT_SECRET al momento de levantar el contenedor:

```bash
docker run --name transport-api -p 8080:8080 -e JWT_SECRET=TU_TOKEN_JWT_VALIDO transport-orders-api
```


