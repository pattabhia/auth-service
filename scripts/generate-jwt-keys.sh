#!/bin/bash

# Script to generate RSA key pair for JWT signing
# ✅ Generates RS256 (2048-bit RSA) keys

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SECRETS_DIR="${SCRIPT_DIR}/../secrets"

echo "🔐 Generating JWT RSA key pair..."

# Create secrets directory
mkdir -p "${SECRETS_DIR}"

# Generate private key (PKCS#8 format)
openssl genrsa -out "${SECRETS_DIR}/jwt-private.pem" 2048

# Generate public key from private key
openssl rsa -in "${SECRETS_DIR}/jwt-private.pem" -pubout -out "${SECRETS_DIR}/jwt-public.pem"

# Set restrictive permissions
chmod 600 "${SECRETS_DIR}/jwt-private.pem"
chmod 644 "${SECRETS_DIR}/jwt-public.pem"

echo "✅ JWT keys generated successfully!"
echo "   Private key: ${SECRETS_DIR}/jwt-private.pem"
echo "   Public key:  ${SECRETS_DIR}/jwt-public.pem"
echo ""
echo "⚠️  IMPORTANT: Keep the private key secure and never commit it to version control!"

