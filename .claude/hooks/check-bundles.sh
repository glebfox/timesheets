#!/bin/bash
# Stop: keep messages_en and messages_ru_RU key-for-key in sync (docs/conventions.md, "User-visible text").
# Runs the key comparison only while a bundle differs from origin/main, so other turns cost one git call.
dir=src/main/resources/com/glebfox/timesheets
cd "$CLAUDE_PROJECT_DIR" || exit 0
active=$(jq -r '.stop_hook_active // false')
git diff --quiet origin/main -- "$dir"/messages_*.properties 2>/dev/null && exit 0

keys() { grep -E '^[^#! ][^=]*=' "$1" | cut -d= -f1 | sed 's/[[:space:]]*$//' | sort; }
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
