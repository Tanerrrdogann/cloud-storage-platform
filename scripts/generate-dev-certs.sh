#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="$ROOT_DIR/.local-certs"
GATEWAY_RES="$ROOT_DIR/api-gateway/src/main/resources"
AUTH_RES="$ROOT_DIR/auth-system/src/main/resources"
PASSWORD="${DEV_CERT_PASSWORD:-changeit}"

mkdir -p "$CERT_DIR" "$GATEWAY_RES" "$AUTH_RES"

openssl req -x509 -newkey rsa:2048 -sha256 -days 3650 -nodes \
  -keyout "$CERT_DIR/rootCA-key.pem" \
  -out "$CERT_DIR/rootCA.pem" \
  -subj "/CN=Cloud Storage Platform Dev Root CA"

openssl req -newkey rsa:2048 -nodes \
  -keyout "$CERT_DIR/server-key.pem" \
  -out "$CERT_DIR/server.csr" \
  -subj "/CN=localhost"

cat > "$CERT_DIR/server.ext" <<EOF
subjectAltName=DNS:localhost,DNS:host.docker.internal,DNS:api-gateway,DNS:auth-service,IP:127.0.0.1
extendedKeyUsage=serverAuth,clientAuth
EOF

openssl x509 -req -in "$CERT_DIR/server.csr" \
  -CA "$CERT_DIR/rootCA.pem" \
  -CAkey "$CERT_DIR/rootCA-key.pem" \
  -CAcreateserial \
  -out "$CERT_DIR/server.pem" \
  -days 3650 \
  -sha256 \
  -extfile "$CERT_DIR/server.ext"

openssl pkcs12 -export \
  -in "$CERT_DIR/server.pem" \
  -inkey "$CERT_DIR/server-key.pem" \
  -name 1 \
  -out "$GATEWAY_RES/gateway-keystore.p12" \
  -passout "pass:$PASSWORD"

cp "$GATEWAY_RES/gateway-keystore.p12" "$GATEWAY_RES/gateway-client.p12"
cp "$GATEWAY_RES/gateway-keystore.p12" "$AUTH_RES/auth-server.p12"
cp "$GATEWAY_RES/gateway-keystore.p12" "$AUTH_RES/auth-truststore.p12"
cp "$CERT_DIR/rootCA.pem" "$GATEWAY_RES/rootCA.pem"
cp "$CERT_DIR/rootCA.pem" "$AUTH_RES/rootCA.pem"
cp "$CERT_DIR/server.pem" "$GATEWAY_RES/localhost+2.pem"
cp "$CERT_DIR/server-key.pem" "$GATEWAY_RES/localhost+2-key.pem"

echo "Generated local development certificates."
echo "Password: $PASSWORD"
