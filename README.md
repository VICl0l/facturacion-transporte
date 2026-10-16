# Facturación de transporte de carga pesada

Sistema de registro y consulta de facturas mediante dos microservicios
Quarkus, comunicación por eventos Kafka y persistencia en PostgreSQL.

## Proceso de negocio

Un sistema de origen informa que se creó una factura correspondiente
a un servicio de transporte de carga pesada.

1. Envía los datos al Producer mediante HTTP, utilizando un token
   con el rol `facturador`.
2. El Producer valida los campos y la coherencia de los importes.
3. Construye y publica un evento `FacturaCreada` en Kafka.
4. El Consumer recibe y deserializa el evento Avro.
5. Guarda la factura en PostgreSQL dentro de una transacción.
6. La factura queda disponible para consulta HTTP mediante un token
   con el rol `auditor`.

La respuesta exitosa del Producer confirma la publicación en Kafka.
La persistencia se realiza después, de forma asíncrona. Por ello,
una factura puede tardar un breve intervalo en estar disponible
para consulta.

## Actores y roles

| Actor | Rol | Permiso |
|---|---|---|
| Sistema de origen u operador | `facturador` | Publicar eventos de creación de facturas |
| Usuario de consulta | `auditor` | Consultar facturas guardadas |

## Alcance

El sistema registra información de facturas previamente creadas.

No realiza emisión tributaria, cobros ni cálculo automático
de impuestos.

## Reglas de negocio implementadas

- Los campos obligatorios deben estar presentes.
- Subtotal e impuestos no pueden ser negativos.
- El total debe ser positivo.
- Subtotal más impuestos debe coincidir con el total.
- Los importes admiten hasta 12 dígitos enteros y 2 decimales.
- La moneda se expresa con tres letras mayúsculas; actualmente
  se valida su formato, no su existencia en un catálogo.
- `facturaId` identifica la factura.
- `eventoId` identifica el evento.
- Repetir una factura existente no crea otra fila ni modifica
  sus datos: se conserva la primera factura registrada.
- El Consumer procesa eventos `FacturaCreada` de versión `1`.

## Arquitectura

```text
Postman → Producer → Kafka → Consumer → PostgreSQL
                                  ↑
                         Consulta HTTP desde Postman
```

- **Producer:** recibe, valida y publica los datos de la factura.
- **Kafka:** almacena los eventos en el tópico `facturas-creadas`.
- **Schema Registry:** almacena el contrato Avro utilizado para
  serializar y deserializar los eventos.
- **Consumer:** procesa los eventos y guarda las facturas.
- **PostgreSQL:** conserva las facturas para su consulta.

Los servicios se ejecutan en K3s dentro de WSL.
Las imágenes de los microservicios se construyen con Podman
y se importan al runtime de K3s.

Kafka, ZooKeeper y PostgreSQL utilizan almacenamiento persistente
mediante PVC.

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
- Podman 5.7.0.
- K3s sobre WSL.

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
│   └── src/main/
│       ├── avro/
│       ├── java/
│       └── resources/
└── factura-consumer/
    ├── Dockerfile
    ├── pom.xml
    ├── infra/k8s/
    └── src/main/
        ├── avro/
        ├── java/
        └── resources/
            └── db/migration/
```

- `src/main/avro`: contrato Avro en ambos proyectos.
- `infra/k8s`: manifiestos Kubernetes.
- `db/migration`: migraciones SQL del Consumer.
- `tools/generar_token.py`: generación local de tokens de laboratorio.

## Endpoints implementados

| Servicio | Método | Ruta | Rol | Función |
|---|---|---|---|---|
| Producer | POST | `/api/facturas` | `facturador` | Publicar `FacturaCreada` |
| Consumer | GET | `/api/facturas/{facturaId}` | `auditor` | Consultar una factura guardada |

El POST devuelve `200` cuando Kafka confirma la publicación.
El GET devuelve `200` si encuentra la factura y `404` si no existe.

## Acceso desde Windows

Con los servicios desplegados, ejecutar cada comando en una terminal
WSL distinta y mantener ambas abiertas:

```bash
kubectl -n facturacion port-forward svc/factura-producer 18080:80
```

```bash
kubectl -n facturacion port-forward svc/factura-consumer 18081:80
```

URLs para Postman:

- Producer: `http://localhost:18080/api/facturas`.
- Consumer: `http://localhost:18081/api/facturas/{facturaId}`.

Estos port-forward permiten el acceso desde Windows.
La comunicación entre los servicios utiliza las direcciones internas
de Kubernetes y no depende de ellos.

## Seguridad

Los endpoints HTTP utilizan tokens JWT firmados con RS256.

- Sin token o con token inválido: `401 Unauthorized`.
- Con token válido y rol insuficiente: `403 Forbidden`.
- Con token válido y rol autorizado: acceso permitido.

Los microservicios verifican los tokens con una clave pública.
La clave privada utilizada para firmarlos se mantiene fuera
del repositorio y de las imágenes.

### Preparar las claves del laboratorio

En WSL, generar las claves una sola vez para el entorno:

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

Desde la raíz del repositorio, copiar únicamente la clave pública:

```bash
cp ~/.config/transporte/jwt/publicKey.pem \
  factura-producer/src/main/resources/publicKey.pem

cp ~/.config/transporte/jwt/publicKey.pem \
  factura-consumer/src/main/resources/publicKey.pem
```

Ambos proyectos deben empaquetarse y desplegarse con esta clave pública
para aceptar los tokens firmados con la clave privada correspondiente.

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

En Postman, seleccionar **Authorization → Bearer Token** e introducir
el token correspondiente.

El generador es una herramienta de demostración del laboratorio;
no implementa login ni gestión de usuarios.

No publicar tokens ni claves privadas en GitHub o en colecciones
exportadas de Postman.

## Pruebas realizadas

- Publicación de una factura válida con rol `facturador`.
- Rechazo de campos obligatorios vacíos.
- Rechazo de importes negativos.
- Rechazo de un total inconsistente.
- Lectura del evento Avro en Kafka.
- Persistencia del evento mediante el Consumer.
- Repetición de factura sin duplicar filas.
- Consulta de factura existente con rol `auditor`.
- Respuesta `404` para factura inexistente.
- Respuesta `401` al acceder sin token.
- Respuesta `403` al acceder con un rol insuficiente.
- Ejecución de ambos microservicios en K3s.

## Limitaciones y pendientes

- Completar las operaciones CRUD y sus reglas de negocio.
- Integrar una API externa de negocio.
- Incorporar relaciones y repositorios de persistencia.
- Completar la documentación del despliegue desde un entorno limpio.
- Incorporar manejo de mensajes fallidos: actualmente un error
  de procesamiento detiene el consumo.
- Automatizar pruebas y despliegues.
- El almacenamiento local no constituye un respaldo.
- El laboratorio utiliza una instancia de cada componente.