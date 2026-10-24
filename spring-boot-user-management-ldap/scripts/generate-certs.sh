#!/bin/bash
# =============================================================================
# generate-certs.sh — 重新生成 OpenLDAP 容器的三个 TLS 证书文件
#
# 背景：
#   data/certificates/ 下的 server.key / server.crt / dhparam.pem 是
#   osixia/openldap 容器首次启动时自动生成的（镜像内部用 cfssl 工具链），
#   目录已加入 .gitignore 不入库。本脚本用 openssl 复刻等效文件，
#   仅在需要手动重新生成时使用；正常情况下删除 data/ 后重启容器即可重建。
#
# 用法（Git Bash 或任意带 openssl 的环境）：
#   bash generate-certs.sh
#
# 生成结果（写入 data/certificates/）：
#   server.key                  服务器私钥（EC P-384，与容器生成的一致）
#   server.crt                  服务器证书（CN=openldap，SAN=DNS:openldap，
#                               由本地 CA 签发，有效期 365 天）
#   dhparam.pem                 DH 参数（2048 位）
#   springframework.org.ca.crt  CA 证书（容器内是指向 default-ca.pem 的
#                               符号链接，Windows 上无法用符号链接，改为拷贝）
# =============================================================================
set -e

CERT_DIR="$(cd "$(dirname "$0")/.." && pwd)/data/certificates"
CA_DIR="$CERT_DIR/.ca"
DAYS=365

mkdir -p "$CERT_DIR" "$CA_DIR"

echo "==> 1/4 生成 CA 私钥与 CA 证书（default-ca）"
openssl ecparam -name secp384r1 -genkey -noout -out "$CA_DIR/default-ca-key.pem"
openssl req -x509 -new -key "$CA_DIR/default-ca-key.pem" \
  -subj "/C=US/ST=New Mexico/L=Albuquerque/O=A1A Car Wash/OU=Information Technology Dep./CN=docker-light-baseimage" \
  -days $((DAYS * 10)) -out "$CA_DIR/default-ca.pem"

echo "==> 2/4 生成服务器私钥 server.key（EC P-384）"
openssl ecparam -name secp384r1 -genkey -noout -out "$CERT_DIR/server.key"

echo "==> 3/4 用 CA 签发服务器证书 server.crt（CN=openldap, SAN=DNS:openldap）"
openssl req -new -key "$CERT_DIR/server.key" \
  -subj "/C=US/ST=New Mexico/L=Albuquerque/O=A1A Car Wash/OU=Information Technology Dep./CN=openldap" \
  -out "$CA_DIR/server.csr"
printf "subjectAltName=DNS:openldap\n" > "$CA_DIR/san.ext"
openssl x509 -req -in "$CA_DIR/server.csr" \
  -CA "$CA_DIR/default-ca.pem" -CAkey "$CA_DIR/default-ca-key.pem" -CAcreateserial \
  -days $DAYS -extfile "$CA_DIR/san.ext" \
  -out "$CERT_DIR/server.crt"

echo "==> 4/4 生成 DH 参数 dhparam.pem（2048 位，较慢，请稍候）"
openssl dhparam -out "$CERT_DIR/dhparam.pem" 2048

# 容器内该文件是指向 CA 证书的符号链接；Windows 挂载目录里符号链接不可用，
# 直接拷贝一份 CA 证书，效果相同
cp "$CA_DIR/default-ca.pem" "$CERT_DIR/springframework.org.ca.crt"

# 清理中间文件（CA 私钥保留在 .ca/ 以便续签，注意勿提交）
rm -f "$CA_DIR/server.csr" "$CA_DIR/san.ext" "$CA_DIR/default-ca.srl"

echo ""
echo "完成，生成于 $CERT_DIR ："
ls -la "$CERT_DIR"
echo ""
echo "注意：.ca/ 目录包含 CA 私钥，仅供本地续签使用，切勿提交到 git。"
