#!/bin/bash
# Stop: keep messages_en and messages_ru_RU key-for-key in sync (docs/conventions.md, "User-visible text").
# Checks the checkout the session works in, and only while a bundle differs from origin/main (deletion included),
# so other turns cost a few git calls.
input=$(cat)
. "$(dirname "$0")/lib.sh"
root=$(checkout_of "$(jq -r '.cwd // empty' <<<"$input")") || root=$(checkout_of "$CLAUDE_PROJECT_DIR") || exit 0
cd "$root" || exit 0
dir=src/main/resources/com/glebfox/timesheets
active=$(jq -r '.stop_hook_active // false' <<<"$input")
git diff --quiet origin/main -- "$dir/messages_*.properties" 2>/dev/null && exit 0

keys() { [ -f "$1" ] && grep -E '^[^#! ][^=]*=' "$1" | cut -d= -f1 | sed 's/[[:space:]]*$//' | sort; }
d=$(diff <(keys "$dir/messages_en.properties") <(keys "$dir/messages_ru_RU.properties")) && exit 0
missing=$(echo "$d" | sed -n 's/^< \(.*\)/messages_ru_RU lacks: \1/p; s/^> \(.*\)/messages_en lacks: \1/p')

# Second stop in a row: Claude already had one chance, so tell the user instead of looping.
if [ "$active" = true ]; then
  jq -n --arg m "Message bundles are still out of sync:"$'\n'"$missing" '{systemMessage: $m}'
  exit 0
fi
echo "messages_en.properties and messages_ru_RU.properties must define the same keys:" >&2
echo "$missing" >&2
exit 2
