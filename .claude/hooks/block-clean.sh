#!/bin/bash
# PreToolUse(Bash, IDE terminal): deny a Gradle `clean` in this checkout. The developer's app runs from this checkout's
# build/, and `clean` deletes the classes under it. Clones and worktrees are other checkouts, so `clean` stays allowed there.
input=$(cat)
cmd=$(jq -r '.tool_input.command // empty' <<<"$input")
grep -qE 'gradle' <<<"$cmd" && grep -qE '(^|[[:space:]:])clean([[:space:]]|$)' <<<"$cmd" || exit 0

# The checkout the app runs from: the main working tree of this project's repository.
common=$(git -C "$CLAUDE_PROJECT_DIR" rev-parse --path-format=absolute --git-common-dir 2>/dev/null) || exit 0
protected=$(cd "$(dirname "$common")" && pwd -P)

# Bash starts in the session's directory; the IDE terminal in the IDE project, which is this checkout unless named.
if [ "$(jq -r '.tool_name' <<<"$input")" = Bash ]; then
  dir=$(jq -r '.cwd // empty' <<<"$input")
else
  dir=$(jq -r '.tool_input.projectPath // empty' <<<"$input")
fi
[ -n "$dir" ] || dir=$protected

# Physical path of a directory, or empty when it cannot be resolved (a variable, a missing directory).
resolve() {
  case "$2" in
    *'$'* | *'`'*) return ;;
    '~'*) (cd "$HOME${2#\~}" 2>/dev/null && pwd -P) ;;
    /*) (cd "$2" 2>/dev/null && pwd -P) ;;
    *) (cd "$1/$2" 2>/dev/null && pwd -P) ;;
  esac
}

# One shell command per line: split at &&, ||, ;, and |.
segs=${cmd//'&&'/$'\n'}
segs=${segs//'||'/$'\n'}
segs=${segs//;/$'\n'}
segs=${segs//|/$'\n'}

deny=
while IFS= read -r seg; do
  seg=$(sed -E 's/^[[:space:]]+|[[:space:]]+$//g' <<<"$seg" | tr -d "\"'")
  if [[ $seg =~ ^cd[[:space:]]+([^[:space:]]+) ]]; then
    dir=$(resolve "$dir" "${BASH_REMATCH[1]}")
    continue
  fi
  [[ $seg =~ (^|[[:space:]/])gradlew?([[:space:]]|$) ]] || continue
  tasks=$(sed -E 's/(-x|--exclude-task)[[:space:]=]+:?clean([[:space:]]|$)/ /g' <<<"$seg")
  grep -qE '(^|[[:space:]:])clean([[:space:]]|$)' <<<"$tasks" || continue
  target=$dir
  if [[ $seg =~ (-p|--project-dir)[[:space:]=]+([^[:space:]]+) ]]; then
    target=$([ -n "$dir" ] && resolve "$dir" "${BASH_REMATCH[2]}")
  fi
  # A directory that cannot be resolved may be this checkout: deny rather than guess.
  top=$([ -n "$target" ] && git -C "$target" rev-parse --show-toplevel 2>/dev/null)
  top=$([ -n "$top" ] && cd "$top" && pwd -P)
  if [ -z "$target" ] || [ "$top" = "$protected" ]; then deny=1; break; fi
done <<<"$segs"

[ -n "$deny" ] || exit 0
jq -n --arg r "No Gradle clean in $protected: the developer's running app loads its classes from its build/, and clean deletes them. For the clean test gate use the local-ci skill (bash .skills/local-ci/scripts/local-ci.sh), which runs in a throwaway clone; a plain ./gradlew test here is fine. A clean build of this checkout is for the user to run." \
  '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $r}}'
