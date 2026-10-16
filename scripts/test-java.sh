#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" apps/codefile/app/src/main/java/com/TechHurts/CodeFile/ApiEndpoint.java tests/java/ApiEndpointTest.java
java -cp "$test_dir" com.TechHurts.CodeFile.ApiEndpointTest
javac -d "$test_dir" apps/codefile/app/src/main/java/com/TechHurts/CodeFile/GithubUrl.java tests/java/GithubUrlTest.java
java -cp "$test_dir" com.TechHurts.CodeFile.GithubUrlTest
