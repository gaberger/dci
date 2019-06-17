#!/bin/bash
# Install KubeOne
RELEASE=0.8.0
ARCHIVE=kubeone_${RELEASE}_linux_amd64.zip
curl -sLO https://github.com/kubermatic/kubeone/releases/download/v${RELEASE}/${ARCHIVE}
unzip ${ARCHIVE} kubeone
sudo mv kubeone /usr/local/bin
rm ${ARCHIVE}
