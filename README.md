# Facturación de transporte de carga pesada

Sistema de registro y consulta de facturas mediante dos microservicios
Quarkus, comunicación por eventos Kafka y persistencia en PostgreSQL.

Incluye autenticación JWT, autorización por roles y consulta de
conversión monetaria mediante una API externa.

## Proceso de negocio

Un sistema de origen informa que se creó una factura correspondiente
a un servicio de transporte de carga pesada.

1. Envía los datos al Producer mediante HTTP utilizando un token
   con el rol `facturador`.
2. El Producer valida los campos y la coherencia de los importes.
3. Construye y publica un evento `FacturaCreada` en Kafka.
4. El Consumer recibe y deserializa el evento Avro.
5. Guarda la factura en PostgreSQL dentro de una transacción.
6. La factura queda disponible para consulta HTTP con el rol `auditor`.
7. Opcionalmente, el auditor consulta su total convertido a otra moneda
   utilizando una tasa obtenida desde una API externa.

La respuesta exitosa del Producer confirma la publicación en Kafka.
La persistencia ocurre después, de forma asíncrona. Una factura puede
tardar un breve intervalo en estar disponible para consulta.

## Actores y roles

| Actor | Rol | Permisos |
|---|---|---|
| Sistema de origen u operador | `facturador` | Publicar eventos de creación de facturas |
| Usuario de consulta | `auditor` | Consultar facturas y conversiones monetarias |

## Alcance

El sistema registra información de facturas previamente creadas.

No realiza emisión tributaria, cobros ni cálculo automático de impuestos.
La conversión monetaria es informativa y no modifica los importes
originales guardados.

## Reglas de negocio

- Los campos obligatorios deben estar presentes.
- Subtotal e impuestos no pueden ser negativos.
- El total debe ser positivo.
- Subtotal más impuestos debe coincidir con el total.
- Los importes admiten hasta 12 dígitos enteros y 2 decimales.
- La moneda original debe contener tres letras mayúsculas.
- `facturaId` identifica la factura.
- `eventoId` identifica el evento.
- Repetir una factura existente no crea otra fila ni modifica sus datos.
- Se conserva la primera factura registrada para cada `facturaId`.
- El Consumer procesa eventos `FacturaCreada` de versión `1`.
- La conversión utiliza la última tasa disponible del proveedor.
- La moneda destino debe ser diferente de la moneda original.
- El resultado convertido se redondea a dos decimales.

## Arquitectura

```text
                       Schema Registry
                         ↑         ↑
                         │         │
Postman → Producer → Kafka → Consumer → PostgreSQL
                                ↑
                                │
                 Postman → Consulta HTTP
                                │
                                ↓
                    API externa Frankfurter
```

### Componentes

- **Producer:** recibe, valida y publica los datos de la factura.
- **Kafka:** almacena los eventos en el tópico `facturas-creadas`.
- **ZooKeeper:** coordina el broker en la configuración Kafka del laboratorio.
- **Schema Registry:** almacena el contrato Avro.
- **Consumer:** procesa los eventos, persiste las facturas y ofrece consultas.
- **PostgreSQL:** almacena las facturas procesadas.
- **Frankfurter:** proporciona tasas de cambio para la consulta de conversión.

Los servicios se ejecutan en K3s dentro de WSL.

Las imágenes de los microservicios se construyen con Podman y se importan
al runtime de K3s.

Kafka, ZooKeeper y PostgreSQL utilizan almacenamiento persistente mediante PVC.
Schema Registry almacena sus esquemas en Kafka.

## Tecnologías

- Java 21.0.12.1.
- Maven 3.9.12.
- Quarkus 3.33.4.
- Kafka, ZooKeeper y Schema Registry de Confluent Platform 7.7.1.
- Apache Avro.
- PostgreSQL 17.
- Hibernate ORM con Panache.
- Flyway.
- SmallRye JWT.
- Quarkus REST Client con Jackson.
- Podman 5.7.0.
- K3s sobre WSL.
- Python 3 y OpenSSL para generar tokens de laboratorio.

## Organización del repositorio

```text
.
├── README.md
├── .gitignore
├── tools/
│   └── generar_token.py
├── factura-producer/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── infra/k8s/
│   │   ├── zookeeper.yaml
│   │   ├── kafka.yaml
│   │   ├── schema-registry.yaml
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── src/main/
│       ├── avro/
│       ├── java/
│       └── resources/
└── factura-consumer/
    ├── Dockerfile
    ├── pom.xml
    ├── infra/k8s/
    │   ├── postgres.yaml
    │   ├── deployment.yaml
    │   └── service.yaml
    └── src/main/
        ├── avro/
        ├── java/
        └── resources/
            └── db/migration/
```

Las capas Java separan recursos REST, DTOs, mappers, servicios,
configuración, clientes externos y persistencia.

## Endpoints

| Servicio | Método | Ruta | Rol requerido |
|---|---|---|---|
| Producer | POST | `/api/facturas` | `facturador` |
| Consumer | GET | `/api/facturas/{facturaId}` | `auditor` |
| Consumer | GET | `/api/facturas/{facturaId}/conversion?monedaDestino=EUR` | `auditor` |

### Publicación de factura

Ejemplo de cuerpo JSON:

```json
{
  "facturaId": "9db7304b-6c12-4c1e-89fb-0ecb7eb3c589",
  "numeroFactura": "F001-000001",
  "fechaEmision": "2026-10-16",
  "clienteId": "CLI-001",
  "clienteNombre": "Distribuidora Industrial",
  "servicioTransporteId": "TRA-001",
  "origen": "Almacén central",
  "destino": "Planta industrial",
  "descripcionCarga": "Maquinaria industrial de 20 toneladas",
  "moneda": "USD",
  "subtotal": 2500.00,
  "impuestos": 0.00,
  "total": 2500.00
}
```

El Producer devuelve `200` cuando Kafka confirma la publicación,
incluyendo tópico, partición y offset.

Esta respuesta no confirma que el Consumer ya haya guardado la factura.

### Consulta de factura

Devuelve los datos guardados en PostgreSQL.

- Factura existente: `200`.
- Factura inexistente: `404`.

### Conversión monetaria

El Consumer obtiene la factura, consulta Frankfurter y calcula:

```text
totalConvertido = totalOriginal × tasa
```

La respuesta incluye:

- Identificador de factura.
- Moneda e importe originales.
- Moneda destino.
- Tasa y fecha de referencia.
- Total convertido.
- Fuente de la tasa.

La conversión no modifica PostgreSQL y no representa necesariamente
el tipo de cambio de la fecha de emisión.

## Seguridad

Los endpoints HTTP utilizan tokens JWT firmados con RS256.

- Sin token o con token inválido: `401 Unauthorized`.
- Con token válido y rol insuficiente: `403 Forbidden`.
- Con token válido y rol autorizado: acceso permitido.

El emisor configurado es `transporte-facturacion`.
El claim `groups` contiene los roles del usuario.

La clave privada se mantiene fuera del repositorio y de las imágenes.
Los microservicios contienen únicamente la clave pública de verificación.

### Preparar las claves

Ejecutar en WSL una sola vez por entorno:

```bash
mkdir -p ~/.config/transporte/jwt
chmod 700 ~/.config/transporte/jwt

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out ~/.config/transporte/jwt/privateKey.pem

chmod 600 ~/.config/transporte/jwt/privateKey.pem

openssl pkey \
  -in ~/.config/transporte/jwt/privateKey.pem \
  -pubout \
  -out ~/.config/transporte/jwt/publicKey.pem
```

Desde la raíz del repositorio:

```bash
cp ~/.config/transporte/jwt/publicKey.pem \
  factura-producer/src/main/resources/publicKey.pem

cp ~/.config/transporte/jwt/publicKey.pem \
  factura-consumer/src/main/resources/publicKey.pem
```

Después de cambiar la clave pública hay que reconstruir y desplegar
ambos microservicios.

No regenerar las claves en cada arranque.

### Generar tokens

Desde la raíz del repositorio:

```bash
python3 tools/generar_token.py facturador
```

```bash
python3 tools/generar_token.py auditor
```

Los tokens caducan en una hora.

En Postman, seleccionar **Authorization → Bearer Token** y pegar el token
correspondiente.

El generador es una herramienta del laboratorio; no implementa login
ni gestión de usuarios.

No publicar tokens ni claves privadas en el repositorio o en colecciones
exportadas de Postman.

## API externa

- Proveedor: Frankfurter.
- Documentación: https://frankfurter.dev/
- Consulta utilizada: `GET https://api.frankfurter.dev/v2/rate/{base}/{destino}`.
- No requiere API key.
- Tiempo de conexión configurado: 5 segundos.
- Tiempo de lectura configurado: 10 segundos.

Solo se envían los códigos de moneda al proveedor externo.
No se envían datos del cliente ni el contenido de la factura.

El servicio valida la respuesta y utiliza `BigDecimal` para calcular
la conversión.

### Errores de conversión

| Situación | Respuesta |
|---|---|
| Moneda destino mal formada o igual a la original | `400` |
| Par de monedas no disponible | `400` |
| Factura inexistente | `404` |
| Error de comunicación o respuesta externa inválida | `502` |
| Tiempo de espera externo agotado y reconocido por el cliente | `504` |

## Despliegue manual en K3s

Las instrucciones siguientes corresponden a una instalación nueva.
Ejecutar desde la raíz del repositorio, salvo indicación contraria.

### 1. Verificar el entorno

```bash
java -version
mvn -version
podman --version
kubectl get nodes
kubectl get storageclass
```

El nodo debe estar `Ready` y debe existir la StorageClass `local-path`.

### 2. Crear el namespace

```bash
kubectl create namespace facturacion --dry-run=client -o yaml \
  | kubectl apply -f -
```

### 3. Desplegar ZooKeeper y Kafka

```bash
kubectl apply -f factura-producer/infra/k8s/zookeeper.yaml

kubectl -n facturacion rollout status \
  statefulset/zookeeper --timeout=180s
```

```bash
kubectl apply -f factura-producer/infra/k8s/kafka.yaml

kubectl -n facturacion rollout status \
  statefulset/kafka --timeout=180s
```

Crear el tópico:

```bash
kubectl -n facturacion exec kafka-0 -- \
  kafka-topics \
  --bootstrap-server kafka:9092 \
  --create \
  --if-not-exists \
  --topic facturas-creadas \
  --partitions 1 \
  --replication-factor 1
```

### 4. Desplegar Schema Registry

```bash
kubectl apply -f factura-producer/infra/k8s/schema-registry.yaml

kubectl -n facturacion rollout status \
  deployment/schema-registry --timeout=180s
```

En otra terminal, mantener abierto:

```bash
kubectl -n facturacion port-forward svc/schema-registry 8081:8081
```

Preparar el registro del esquema:

```bash
python3 - <<'PY'
import json
from pathlib import Path

schema = Path(
    "factura-producer/src/main/avro/factura-creada.avsc"
).read_text(encoding="utf-8")

json.loads(schema)

Path("/tmp/factura-creada-schema.json").write_text(
    json.dumps({"schema": schema}),
    encoding="utf-8"
)
PY
```

Registrar:

```bash
curl --fail-with-body -sS \
  -X POST \
  http://localhost:8081/subjects/facturas-creadas-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data-binary @/tmp/factura-creada-schema.json
```

Comprobar:

```bash
curl --fail-with-body -sS \
  http://localhost:8081/subjects/facturas-creadas-value/versions/latest \
  | python3 -m json.tool
```

### 5. Preparar PostgreSQL

Crear las credenciales una sola vez:

```bash
read -s -p "Contraseña de PostgreSQL: " FACTURAS_DB_PASSWORD
echo

kubectl -n facturacion create secret generic postgres-facturas \
  --from-literal=POSTGRES_DB=facturacion \
  --from-literal=POSTGRES_USER=facturas_app \
  --from-literal=POSTGRES_PASSWORD="$FACTURAS_DB_PASSWORD"

unset FACTURAS_DB_PASSWORD
```

Cambiar el Secret posteriormente no cambia automáticamente
la contraseña de una base de datos ya inicializada.

Desplegar:

```bash
kubectl apply -f factura-consumer/infra/k8s/postgres.yaml

kubectl -n facturacion rollout status \
  statefulset/postgres --timeout=180s
```

### 6. Preparar las claves JWT

Completar la sección de preparación de claves de este README antes
de empaquetar las aplicaciones.

### 7. Empaquetar los microservicios

```bash
mvn -f factura-producer/pom.xml clean package
mvn -f factura-consumer/pom.xml clean package
```

Continuar únicamente si ambos comandos terminan correctamente.

### 8. Construir las imágenes

```bash
podman build \
  -t localhost/factura-producer:1.1 \
  factura-producer

podman build \
  -t localhost/factura-consumer:1.2 \
  factura-consumer
```

Las etiquetas anteriores deben coincidir con las de los manifiestos
Deployment.

### 9. Importar las imágenes en K3s

```bash
podman save localhost/factura-producer:1.1 \
  -o /tmp/factura-producer-1.1.tar

podman save localhost/factura-consumer:1.2 \
  -o /tmp/factura-consumer-1.2.tar
```

```bash
sudo k3s ctr images import /tmp/factura-producer-1.1.tar
sudo k3s ctr images import /tmp/factura-consumer-1.2.tar
```

Las imágenes de Podman no están disponibles automáticamente en K3s.

### 10. Desplegar los microservicios

```bash
kubectl apply -f factura-producer/infra/k8s/deployment.yaml
kubectl apply -f factura-producer/infra/k8s/service.yaml

kubectl apply -f factura-consumer/infra/k8s/deployment.yaml
kubectl apply -f factura-consumer/infra/k8s/service.yaml
```

```bash
kubectl -n facturacion rollout status \
  deployment/factura-producer --timeout=180s

kubectl -n facturacion rollout status \
  deployment/factura-consumer --timeout=180s
```

Flyway aplica las migraciones pendientes al iniciar el Consumer.

### 11. Verificar el estado

```bash
kubectl -n facturacion get pods
kubectl -n facturacion get pvc
kubectl -n facturacion get svc
```

Revisar los logs:

```bash
kubectl -n facturacion logs deployment/factura-producer --tail=100
kubectl -n facturacion logs deployment/factura-consumer --tail=100
```

## Acceso desde Windows

Ejecutar cada comando en una terminal WSL distinta y mantenerlas abiertas:

```bash
kubectl -n facturacion port-forward svc/factura-producer 18080:80
```

```bash
kubectl -n facturacion port-forward svc/factura-consumer 18081:80
```

URLs para Postman:

- Publicación: `http://localhost:18080/api/facturas`.
- Consulta: `http://localhost:18081/api/facturas/{facturaId}`.
- Conversión: `http://localhost:18081/api/facturas/{facturaId}/conversion?monedaDestino=EUR`.

Al reemplazar los Pods puede ser necesario reabrir los port-forward.

Los microservicios utilizan estas direcciones internas:

| Servicio | Dirección |
|---|---|
| Kafka | `kafka:9092` |
| Schema Registry | `http://schema-registry:8081` |
| PostgreSQL | `postgres:5432` |

Se pueden cerrar los port-forward directos de Kafka, Schema Registry
y PostgreSQL sin detener el sistema desplegado.

## Consultar PostgreSQL desde Ubuntu

No requiere port-forward:

```bash
kubectl -n facturacion exec postgres-0 -- \
  psql -U facturas_app -d facturacion \
  -c "SELECT factura_id, numero_factura, moneda, total FROM facturas;"
```

## Pruebas funcionales

### Flujo principal

1. Generar un token con rol `facturador`.
2. Publicar una factura válida mediante el Producer.
3. Comprobar la respuesta `200` con tópico, partición y offset.
4. Esperar el procesamiento asíncrono.
5. Consultar la factura con un token de `auditor`.
6. Verificar la fila correspondiente en PostgreSQL.
7. Consultar su conversión a una moneda diferente y soportada.

### Validaciones y duplicados

- Campo obligatorio vacío: `400`.
- Importe negativo: `400`.
- Total inconsistente: `400`.
- Factura inexistente: `404`.
- Repetir el POST con el mismo `facturaId`: no duplica la fila.
- La repetición no actualiza los datos de la factura existente.

### Autorización

| Operación | Token | Resultado esperado |
|---|---|---|
| Publicar factura | Sin token | `401` |
| Publicar factura | Auditor | `403` |
| Publicar factura válida | Facturador | `200` |
| Consultar factura existente | Sin token | `401` |
| Consultar factura existente | Facturador | `403` |
| Consultar factura existente | Auditor | `200` |
| Consultar conversión | Facturador | `403` |
| Consultar conversión válida | Auditor | `200` |

## Persistencia y procesamiento

Kafka conserva los mensajes según su política de retención.

Leer un evento no lo elimina del tópico. El grupo `factura-consumer`
registra su progreso mediante offsets.

El Consumer confirma el procesamiento después de completar
la transacción de PostgreSQL.

Si ocurre una interrupción después de guardar la factura y antes
de registrar el progreso en Kafka, el evento puede volver a entregarse.
La inserción protegida por `facturaId` evita duplicar la factura.

Detener los port-forward o reiniciar WSL no elimina los PVC.
Eliminar los PVC o la distribución WSL puede provocar pérdida de datos.

## Limitaciones y pendientes

- Completar las operaciones CRUD y sus reglas de negocio.
- Incorporar relaciones y repositorios de persistencia.
- Incorporar una estrategia de recuperación y mensajes fallidos:
  actualmente un error de procesamiento detiene el consumo.
- Ampliar las pruebas automatizadas.
- Automatizar los despliegues.
- Ensayar la instalación completa en un entorno limpio.
- Fijar revisiones o digests de las imágenes base para mejorar
  la reproducibilidad.
- El generador JWT no sustituye un sistema de autenticación de usuarios.
- La conversión monetaria depende de la disponibilidad y cobertura
  del proveedor externo.
- El almacenamiento local no constituye un respaldo.
- El laboratorio utiliza una instancia de cada componente.