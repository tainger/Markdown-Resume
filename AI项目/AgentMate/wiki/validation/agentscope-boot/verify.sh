#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
cd "$(dirname "$0")"
validation_repo="${VALIDATION_MAVEN_REPO:-/private/tmp/agentmate-validation/m2}"
mvn -B -gs settings.xml -s settings.xml "-Dmaven.repo.local=$validation_repo" package
validation_state="$(mktemp -d "${TMPDIR:-/tmp}/agentmate-probe.XXXXXX")"
trap 'rm -rf "$validation_state"' EXIT
validation_jar="target/agentscope-boot-validation-0.0.1.jar"
java -jar "$validation_jar" --validation.probe=write "--validation.state-dir=$validation_state"
java -jar "$validation_jar" --validation.probe=read "--validation.state-dir=$validation_state"
