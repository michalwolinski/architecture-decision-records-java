#!/usr/bin/env bash
# Everything runs inside a pinned Maven + JDK 25 image, with the repository mounted read-only (except runs/).
#   ./run.sh test                                   unit tests + scripted agents, no API key
#   ./run.sh eval --agent oracle                    free: every task must pass in every arm
#   ./run.sh eval --agent claude --model claude-haiku-5-5 --reps 5 --budget-usd 3
#   ./run.sh report runs/<run-dir>
set -euo pipefail
cd "$(dirname "$0")"
MAVEN_IMAGE='maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976'
command="${1:-test}"
shift || true
key=()
if [ "eval" = "$command" ] && printf '%s\n' "$@" | grep -qx claude; then
    if [ -z "${ANTHROPIC_API_KEY:-}" ] && command -v security > /dev/null; then
        ANTHROPIC_API_KEY="$(security find-generic-password -s ANTHROPIC_API_KEY -w)"
    fi
    export ANTHROPIC_API_KEY
    key=(-e ANTHROPIC_API_KEY)
fi
mkdir -p runs target
args="$*"
docker run --rm "${key[@]+"${key[@]}"}" \
    -v "$PWD":/src:ro -v "$PWD/runs":/src/runs \
    -v adr-experiment-target:/src/target \
    -v adr-experiment-m2:/m2 \
    -w /src \
    -e MAVEN_ARGS="-B -ntp -Dmaven.repo.local=/m2" \
    "$MAVEN_IMAGE" bash -c "set -e; rm -rf /tmp/prep; mkdir -p /tmp/prep/src; cp -r fixture/grader/pom.xml fixture/grader/src /tmp/prep/; cp -r fixture/base/src/main /tmp/prep/src/;
        (cd /tmp/prep && mvn -q test -Dmaven.test.failure.ignore=true > /dev/null); chmod -R a+rwX /m2; case '$command' in
        test) mvn -q test $args ;;
        *) mvn -q compile exec:java -Dexec.args='$command $args' ;;
    esac"
