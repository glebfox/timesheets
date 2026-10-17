#!/bin/bash
# Sets a single-select field (Status, Priority, Size, Jmix) of an issue on the Timesheets board, user project #3.
# Usage: bash set-board-field.sh <issue-number> <field> <option>
# DRY_RUN=1 resolves the ids and prints them without changing the board.
set -euo pipefail
[ $# -eq 3 ] || { echo "usage: $0 <issue-number> <field> <option>" >&2; exit 64; }
issue=$1 field=$2 option=$3
owner=glebfox repo=timesheets board=3

item_id=$(gh api graphql -F n="$issue" -f query="
  query(\$n: Int!) { repository(owner: \"$owner\", name: \"$repo\") { issue(number: \$n) {
    projectItems(first: 20) { nodes { id project { number } } } } } }" \
  --jq ".data.repository.issue.projectItems.nodes[] | select(.project.number == $board) | .id")
if [ -z "$item_id" ]; then
  echo "#$issue is not on the Timesheets board. Add it: gh project item-add $board --owner $owner --url https://github.com/$owner/$repo/issues/$issue" >&2
  exit 1
fi

ids=$(gh project field-list "$board" --owner "$owner" --format json |
  jq -r --arg f "$field" --arg o "$option" '.fields[] | select(.name == $f) | "\(.id) \(.options[]? | select(.name == $o) | .id)"')
field_id=${ids%% *} option_id=${ids#* }
if [ -z "$ids" ] || [ "$option_id" = "$ids" ]; then
  echo "The board has no option \"$option\" in field \"$field\"." >&2
  exit 1
fi
project_id=$(gh project view "$board" --owner "$owner" --format json --jq .id)

if [ -n "${DRY_RUN:-}" ]; then
  echo "dry run: item=$item_id project=$project_id field=$field_id option=$option_id"
  exit 0
fi
gh project item-edit --id "$item_id" --project-id "$project_id" --field-id "$field_id" --single-select-option-id "$option_id" >/dev/null
echo "#$issue: $field = $option"
