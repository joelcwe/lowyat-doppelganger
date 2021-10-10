#!/bin/bash
# Usage: STACK_VERSION=7.13.4 NAMESPACE=default ./create_kibana_encryption_key.sh

# ELASTICSEARCH
encryptionkey=$([ ! -z "$KIBANA_KEY" ] && echo "$KIBANA_KEY" || docker run --rm docker.elastic.co/elasticsearch/elasticsearch:"$STACK_VERSION" /bin/sh -c "< /dev/urandom tr -cd _A-Za-z0-9 | head -c50") && \
kubectl -n "$NAMESPACE" create secret generic kibana-encryption-key --from-literal=encryptionkey="$encryptionkey"

