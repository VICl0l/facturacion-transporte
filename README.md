# Facturación de transporte de carga pesada

Sistema de registro y consulta de facturas mediante dos microservicios
Quarkus, comunicación por eventos Kafka y persistencia en PostgreSQL.

## Proceso de negocio

Un sistema de origen informa que se creó una factura correspondiente
a un servicio de transporte de carga pesada.

1. Envía los datos al Producer mediante HTTP.
2. El Producer valida los campos y la coherencia de los importes.
3. Construye y publica un evento FacturaCreada en Kafka.
4. El Consumer recibe y deserializa el evento Avro.
5. Guarda la factura en PostgreSQL dentro de una transacción.
6. La factura queda disponible para consulta HTTP.

La respuesta exitosa del Producer confirma la publicación en Kafka.
La persistencia se realiza después, de forma asíncrona.

## Actores

- Sistema de origen u operador: envía la información de la factura.
- Usuario de consulta: consulta las facturas registradas.

Actualmente los endpoints no tienen autenticación.
Los permisos y roles están pendientes de implementación.

## Alcance

El sistema registra información de facturas previamente creadas.
No realiza emisión tributaria, cobros ni cálculo automático de impuestos.

## Reglas de negocio implementadas

- Los campos obligatorios deben estar presentes.
- Subtotal e impuestos no pueden ser negativos.
- El total debe ser positivo.
- Subtotal más impuestos debe coincidir con el total.
- Los importes admiten hasta dos decimales.
- La moneda se expresa con tres letras mayúsculas.
- facturaId identifica la factura.
- eventoId identifica el evento.
- Repetir una factura existente no crea otra fila ni modifica sus datos.
- El Consumer procesa eventos FacturaCreada de versión 1.

## Arquitectura

Postman → Producer → Kafka → Consumer → PostgreSQL

Producer y Consumer utilizan Schema Registry para interpretar
el contrato Avro.

Los servicios se ejecutan en K3s dentro de WSL.
Las imágenes de los microservicios se construyen con Podman.

## Tecnologías

- Java 21.0.12.1
- Maven 3.9.12
- Quarkus 3.33.4
- Kafka y Schema Registry de Confluent Platform 7.7.1
- PostgreSQL 17
- Podman 5.7.0
- K3s

## Organización

- factura-producer: recepción HTTP y publicación de eventos.
- factura-consumer: consumo, persistencia y consulta.
- src/main/avro: contrato Avro en cada proyecto.
- infra/k8s: manifiestos Kubernetes de cada proyecto.
- db/migration: migraciones Flyway dentro de los recursos del Consumer.

## Endpoints implementados

| Servicio | Método | Ruta | Función |
|---|---|---|---|
| Producer | POST | /api/facturas | Publicar FacturaCreada |
| Consumer | GET | /api/facturas/{facturaId} | Consultar factura guardada |

## Acceso desde Windows

Ejecutar cada comando en una terminal WSL y mantenerla abierta:

```bash
kubectl -n facturacion port-forward svc/factura-producer 18080:80
```

```bash
kubectl -n facturacion port-forward svc/factura-consumer 18081:80
```

- Publicación: http://localhost:18080/api/facturas
- Consulta: http://localhost:18081/api/facturas/{facturaId}

## Pruebas realizadas

- Publicación de factura válida.
- Rechazo de campos obligatorios vacíos.
- Rechazo de importes negativos.
- Rechazo de un total inconsistente.
- Lectura del evento Avro en Kafka.
- Persistencia del evento mediante el Consumer.
- Repetición de factura sin duplicar filas.
- Consulta de factura existente.
- Respuesta 404 para factura inexistente.
- Ejecución de ambos microservicios en K3s.

## Limitaciones actuales

- No existe CRUD completo.
- No hay autenticación ni autorización.
- No se integra una API externa de negocio.
- Un error de procesamiento detiene el consumo.
- No se ha implementado una cola de mensajes fallidos.
- El almacenamiento local no constituye un respaldo.
- El laboratorio utiliza una instancia de cada componente.

## Pendientes

- Documentar el despliegue completo desde un entorno limpio.
- Añadir tokens y roles.
- Integrar una API externa.
- Completar las operaciones de negocio y sus eventos.
- Incorporar relaciones y repositorios de persistencia.

## Despliegue manual en K3s

### Requisitos

- WSL con Java 21 y Maven 3.9.12.
- Podman.
- K3s activo y kubectl configurado.
- Nodo en estado Ready y StorageClass local-path disponible.

### 1. Crear el namespace

```bash
kubectl create namespace facturacion --dry-run=client -o yaml \
  | kubectl apply -f -
```

### 2. Desplegar la infraestructura Kafka

Desde la raíz del repositorio:

```bash
kubectl apply -f factura-producer/infra/k8s/zookeeper.yaml
kubectl -n facturacion rollout status statefulset/zookeeper --timeout=180s

kubectl apply -f factura-producer/infra/k8s/kafka.yaml
kubectl -n facturacion rollout status statefulset/kafka --timeout=180s

kubectl apply -f factura-producer/infra/k8s/schema-registry.yaml
kubectl -n facturacion rollout status deployment/schema-registry --timeout=180s
```

### 3. Crear el tópico

```bash
kubectl -n facturacion exec kafka-0 -- \
  kafka-topics --bootstrap-server kafka:9092 \
  --create --if-not-exists \
  --topic facturas-creadas --partitions 1 --replication-factor 1
```

### 4. Registrar el contrato Avro

En otra terminal, mantener abierto:

```bash
kubectl -n facturacion port-forward svc/schema-registry 8081:8081
```

Desde la raíz del repositorio:

```bash
python3 - <<'PY'
import json
from pathlib import Path

schema = Path(
    "factura-producer/src/main/avro/factura-creada.avsc"
).read_text(encoding="utf-8")

Path("/tmp/factura-creada-schema.json").write_text(
    json.dumps({"schema": schema}),
    encoding="utf-8"
)
PY

curl --fail-with-body -sS \
  -X POST \
  http://localhost:8081/subjects/facturas-creadas-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data-binary @/tmp/factura-creada-schema.json
```

### 5. Preparar PostgreSQL

Introducir una contraseña para la nueva instalación:

```bash
read -s -p "Contraseña de PostgreSQL: " FACTURAS_DB_PASSWORD
echo

kubectl -n facturacion create secret generic postgres-facturas \
  --from-literal=POSTGRES_DB=facturacion \
  --from-literal=POSTGRES_USER=facturas_app \
  --from-literal=POSTGRES_PASSWORD="$FACTURAS_DB_PASSWORD"

unset FACTURAS_DB_PASSWORD
```

El Secret se crea una sola vez. Cambiarlo posteriormente no cambia
automáticamente la contraseña de una base ya inicializada.

```bash
kubectl apply -f factura-consumer/infra/k8s/postgres.yaml
kubectl -n facturacion rollout status statefulset/postgres --timeout=180s
```

### 6. Construir las aplicaciones

Desde la raíz del repositorio:

```bash
mvn -f factura-producer/pom.xml clean package
mvn -f factura-consumer/pom.xml clean package
```

### 7. Construir e importar las imágenes

```bash
podman build -t localhost/factura-producer:1.0 factura-producer
podman build -t localhost/factura-consumer:1.0 factura-consumer

podman save localhost/factura-producer:1.0 \
  -o /tmp/factura-producer-1.0.tar

podman save localhost/factura-consumer:1.0 \
  -o /tmp/factura-consumer-1.0.tar

sudo k3s ctr images import /tmp/factura-producer-1.0.tar
sudo k3s ctr images import /tmp/factura-consumer-1.0.tar
```

### 8. Desplegar los microservicios

```bash
kubectl apply -f factura-producer/infra/k8s/deployment.yaml
kubectl apply -f factura-producer/infra/k8s/service.yaml

kubectl apply -f factura-consumer/infra/k8s/deployment.yaml
kubectl apply -f factura-consumer/infra/k8s/service.yaml

kubectl -n facturacion rollout status deployment/factura-producer --timeout=180s
kubectl -n facturacion rollout status deployment/factura-consumer --timeout=180s
```

Flyway crea las tablas cuando arranca el Consumer.

### 9. Comprobar el estado

```bash
kubectl -n facturacion get pods
kubectl -n facturacion get pvc
kubectl -n facturacion get svc
```

Abrir los port-forward de los microservicios descritos en la sección
de acceso desde Windows y ejecutar las pruebas con Postman.
