#!/bin/bash

# Create SSL directory if not exists
mkdir -p ssl

# Generate CA private key
openssl genrsa -out ssl/ca-key.pem 2048

# Generate CA certificate
openssl req -x509 -new -nodes -key ssl/ca-key.pem -sha256 -days 365 -out ssl/ca.pem -subj "/CN=MongoCA"

# Generate server private key
openssl genrsa -out ssl/server-key.pem 2048

# Generate server certificate signing request
openssl req -new -key ssl/server-key.pem -out ssl/server-csr.pem -subj "/CN=mongo1"

# Sign the server certificate with CA
openssl x509 -req -in ssl/server-csr.pem -CA ssl/ca.pem -CAkey ssl/ca-key.pem -CAcreateserial -out ssl/server.pem -days 365 -sha256

# Generate client private key
openssl genrsa -out ssl/client-key.pem 2048

# Generate client certificate signing request
openssl req -new -key ssl/client-key.pem -out ssl/client-csr.pem -subj "/CN=mongo-client"

# Sign the client certificate with CA
openssl x509 -req -in ssl/client-csr.pem -CA ssl/ca.pem -CAkey ssl/ca-key.pem -CAserial ssl/ca.srl -out ssl/client.pem -days 365 -sha256

# Generate Certificate Revocation List (CRL)
openssl ca -config /etc/ssl/openssl.cnf -gencrl -keyfile ssl/ca-key.pem -cert ssl/ca.pem -out ssl/ca.crl

# Clean up intermediate files
rm ssl/server-csr.pem ssl/client-csr.pem
