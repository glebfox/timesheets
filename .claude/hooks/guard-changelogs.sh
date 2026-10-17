#!/bin/bash
# Keep Liquibase changelogs that main already has unchanged: editing an applied changeset changes its checksum,
# and Liquibase then refuses to start against every database that applied it. A model change gets a new changelog.
# PreToolUse(Edit|Write) denies the edit; PostToolUse(Bash) reports a change a shell command made.
# Demo data in init-data/ is not guarded: docs/guidelines/database.md allows updating its inserts.
input=$(cat)
cd "$CLAUDE_PROJECT_DIR" || exit 0
dir=src/main/resources/com/glebfox/timesheets/liquibase/changelog
base=$(git merge-base HEAD origin/main 2>/dev/null) || exit 0

# A file that main already has and that carries no validCheckSum marker.
applied() {
  case "$1" in "$dir"/init-data/*) return 1 ;; esac
  git cat-file -e "$base:$1" 2>/dev/null || return 1
  ! git show "$base:$1" | grep -q '<validCheckSum>'
}

case $(jq -r '.hook_event_name' <<<"$input") in
  PreToolUse)
    f=$(jq -r '.tool_input.file_path // empty' <<<"$input")
    rel=${f#"$CLAUDE_PROJECT_DIR"/}
    case "$rel" in "$dir"/*) ;; *) exit 0 ;; esac
    applied "$rel" || exit 0
    jq -n --arg r "$rel is already on main and applied to existing databases; editing it breaks their checksums. Add a new changelog under $dir/YYYY/MM/ instead." \
      '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $r}}'
    ;;
  PostToolUse)
    changed=$(git diff --no-renames --name-only --diff-filter=MD "$base" -- "$dir" | while read -r rel; do applied "$rel" && echo "$rel"; done)
    [ -z "$changed" ] && exit 0
    {
      echo "These changelogs are already on main and applied to existing databases; this command changed them:"
      echo "$changed"
      echo "Restore them with: git checkout $base -- <file>, and put the model change in a new changelog."
    } >&2
    exit 2
    ;;
esac
exit 0
