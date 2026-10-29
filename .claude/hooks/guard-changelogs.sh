#!/bin/bash
# Keep Liquibase changelogs that main already has unchanged: editing an applied changeset changes its checksum,
# and Liquibase then refuses to start against every database that applied it. A model change gets a new changelog.
# - PreToolUse of a call that names the files it writes (Edit, Write, IDE create or reformat): denied.
# - Around any other call that can change files (a shell command, an IDE patch or rename): the changelogs are
#   checksummed before and after, and only what that call changed is reported.
# "On main" means in origin/main as last fetched.
input=$(cat)
. "$(dirname "$0")/lib.sh"
[ -n "$main_checkout" ] || exit 0
dir=src/main/resources/com/glebfox/timesheets/liquibase/changelog
state=${TMPDIR:-/tmp}/claude-changelog-guard

get() { jq -r "$1" <<<"$input"; }
tool=$(get '.tool_name')
ide_root=$(get '.tool_input.projectPath // empty')
[ -n "$ide_root" ] || ide_root=$main_checkout

# Applied: present at the merge-base with origin/main, and without a validCheckSum marker.
applied() { # <checkout> <path relative to it>
  case "$2" in "$dir"/*) ;; *) return 1 ;; esac
  local base
  base=$(git -C "$1" merge-base HEAD origin/main 2>/dev/null) || return 1
  git -C "$1" cat-file -e "$base:$2" 2>/dev/null || return 1
  ! git -C "$1" show "$base:$2" | grep -q '<validCheckSum>'
}

# One "crc size path" line per guarded changelog in the checkouts given one per line.
sums() {
  while IFS= read -r c; do
    [ -n "$c" ] && [ -d "$c/$dir" ] && find "$c/$dir" -type f -exec cksum {} +
  done <<<"$1" | sort
}

case $(get '.hook_event_name') in
  PreToolUse)
    case $tool in
      Edit | Write | NotebookEdit) files=$(get '.tool_input.file_path // .tool_input.notebook_path // empty') ;;
      *__create_new_file) files="$ide_root/$(get '.tool_input.pathInProject')" ;;
      *__reformat_file) files=$(jq -r --arg r "$ide_root" '.tool_input.files[]? | "\($r)/\(.)"' <<<"$input") ;;
      *)
        roots=$({
          checkout_of "$CLAUDE_PROJECT_DIR"
          checkout_of "$(get '.cwd // empty')"
          [[ $tool == mcp__* ]] && checkout_of "$ide_root"
        } | sort -u)
        mkdir -p "$state" && find "$state" -type f -mmin +120 -delete 2>/dev/null
        { echo "$roots"; echo ---; sums "$roots"; } >"$state/$(get '.tool_use_id // .session_id')"
        exit 0
        ;;
    esac
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      c=$(checkout_of "$f") && p=$(physical "$f") || continue
      rel=${p#"$c"/}
      applied "$c" "$rel" || continue
      jq -n --arg r "$rel is already on main and applied to existing databases; editing it breaks their checksums. Add a new changelog under $dir/YYYY/MM/ instead." \
        '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $r}}'
      exit 0
    done <<<"$files"
    ;;
  PostToolUse | PostToolUseFailure)
    snap="$state/$(get '.tool_use_id // .session_id')"
    [ -f "$snap" ] || exit 0
    roots=$(sed '/^---$/,$d' "$snap")
    before=$(sed '1,/^---$/d' "$snap")
    rm -f "$snap"
    report=
    while IFS= read -r p; do
      [ -n "$p" ] || continue
      while IFS= read -r c; do
        case "$p" in "$c"/*) applied "$c" "${p#"$c"/}" && report="$report$p"$'\n' ;; esac
      done <<<"$roots"
    done < <(diff <(echo "$before") <(sums "$roots") | sed -n 's/^[<>] [0-9]* [0-9]* //p' | sort -u)
    [ -n "$report" ] || exit 0
    {
      echo "This call changed changelogs that are already on main and applied to existing databases:"
      printf '%s' "$report"
      echo "Undo this call's change to them, and put the model change in a new changelog."
    } >&2
    exit 2
    ;;
esac
exit 0
