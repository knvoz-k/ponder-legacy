#!/usr/bin/env bash
set -euo pipefail
ponder_project=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
ponder_local_jdk="$ponder_project/../../.dev/jdk-25.0.5+8"
if [[ -n "${PONDER_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$PONDER_JAVA_HOME"
elif [[ -x "$ponder_local_jdk/bin/java" ]]; then
    export JAVA_HOME="$ponder_local_jdk"
fi
ponder_proxy_args=()
# Java does not automatically honor the usual HTTP(S)_PROXY environment variables.
ponder_proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ "$ponder_proxy" =~ ^https?://([^/:@]+):([0-9]+)/?$ ]]; then
    ponder_proxy_args+=("-Dhttps.proxyHost=${BASH_REMATCH[1]}" "-Dhttps.proxyPort=${BASH_REMATCH[2]}")
    ponder_proxy_args+=("-Dhttp.proxyHost=${BASH_REMATCH[1]}" "-Dhttp.proxyPort=${BASH_REMATCH[2]}")
fi
cd -- "$ponder_project"
exec ./gradlew "${ponder_proxy_args[@]}" "$@"
