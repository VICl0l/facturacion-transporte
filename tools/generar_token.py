import argparse
import base64
import json
import subprocess
import time
import uuid
from pathlib import Path


def codificar(valor):
    contenido = json.dumps(
        valor, separators=(",", ":")
    ).encode("utf-8")

    return base64.urlsafe_b64encode(contenido).rstrip(b"=")


parser = argparse.ArgumentParser()
parser.add_argument("rol", choices=["facturador", "auditor"])
args = parser.parse_args()

clave = Path.home() / ".config/transporte/jwt/privateKey.pem"

if not clave.is_file():
    raise SystemExit(f"No se encontró la clave privada: {clave}")

ahora = int(time.time())

cabecera = codificar({
    "alg": "RS256",
    "typ": "JWT"
})

contenido = codificar({
    "iss": "transporte-facturacion",
    "sub": f"usuario-{args.rol}",
    "upn": f"usuario-{args.rol}",
    "groups": [args.rol],
    "iat": ahora,
    "exp": ahora + 3600,
    "jti": str(uuid.uuid4())
})

mensaje = cabecera + b"." + contenido

resultado = subprocess.run(
    [
        "openssl", "dgst", "-sha256",
        "-sign", str(clave)
    ],
    input=mensaje,
    capture_output=True,
    check=True
)

firma = base64.urlsafe_b64encode(
    resultado.stdout
).rstrip(b"=")

print((mensaje + b"." + firma).decode("ascii"))
