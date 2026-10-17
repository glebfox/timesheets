#!/bin/bash
# PostToolUse(Write, IDE create_new_file): give a new file the Apache License header, as `./gradlew spotlessApply`
# would. Spotless's IDE hook formats only that file, in the checkout that holds it; files outside Spotless's
# targets (see build.gradle) do not start Gradle at all.
input=$(cat)
. "$(dirname "$0")/lib.sh"
if [ "$(jq -r '.tool_name' <<<"$input")" = Write ]; then
  f=$(jq -r '.tool_input.file_path // empty' <<<"$input")
else
  root=$(jq -r '.tool_input.projectPath // empty' <<<"$input")
  f="${root:-$main_checkout}/$(jq -r '.tool_input.pathInProject // empty' <<<"$input")"
fi
[ -f "$f" ] && c=$(checkout_of "$f") && f=$(physical "$f") || exit 0
rel=${f#"$c"/}
case "$rel" in
  src/main/frontend/generated/* | src/main/bundles/*) exit 0 ;;
  src/*.java | src/*.xml | src/*.xsd | src/*.html | src/*.css | src/*.properties) ;;
  */*) exit 0 ;;
  *.gradle) ;;
  *) exit 0 ;;
esac
# --rerun-tasks: Gradle otherwise skips Spotless as up to date when the file matches an earlier run's input.
cd "$c" && ./gradlew -q spotlessApply --rerun-tasks -PspotlessIdeHook="$f"
