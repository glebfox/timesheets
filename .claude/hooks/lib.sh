# Sourced by the hook scripts: the checkouts of this project's repository. A session may work in a linked worktree,
# entered mid-session, while CLAUDE_PROJECT_DIR stays on the checkout it started in, so a hook finds the checkout
# from the path or directory it is about.

# The common git directory of the repository holding a directory, as a physical path.
repo_common() {
  local c
  c=$(git -C "$1" rev-parse --path-format=absolute --git-common-dir 2>/dev/null) && (cd "$c" && pwd -P)
}

project_common=$(repo_common "$CLAUDE_PROJECT_DIR")
# The main working tree: the developer's app runs from its build/, and it is the IDE project.
main_checkout=$([ -n "$project_common" ] && cd "$(dirname "$project_common")" && pwd -P)

# The root of the checkout of this repository (main working tree or linked worktree) that holds a path.
checkout_of() {
  local p=$1
  [ -n "$p" ] && [ -n "$project_common" ] || return 1
  while [ ! -d "$p" ]; do p=$(dirname "$p"); done
  [ "$(repo_common "$p")" = "$project_common" ] || return 1
  (cd "$(git -C "$p" rev-parse --show-toplevel)" && pwd -P)
}

# A path with its directory resolved to a physical path; empty when the directory does not exist.
physical() {
  local d
  d=$(cd "$(dirname "$1")" 2>/dev/null && pwd -P) && echo "$d/$(basename "$1")"
}
