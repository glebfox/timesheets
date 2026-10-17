#!/bin/bash
# PostToolUse(Write): give a file Claude creates the Apache License header, as `./gradlew spotlessApply` would.
# Spotless's IDE hook formats only the one file; files outside the Spotless targets are left untouched.
f=$(jq -r '.tool_input.file_path // empty')
case "$f" in
  "$CLAUDE_PROJECT_DIR"/src/* | "$CLAUDE_PROJECT_DIR"/*.gradle) ;;
  *) exit 0 ;;
esac
cd "$CLAUDE_PROJECT_DIR" && ./gradlew -q spotlessApply -PspotlessIdeHook="$f"
