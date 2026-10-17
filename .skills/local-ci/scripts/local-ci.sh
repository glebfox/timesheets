#!/bin/bash
# Runs the checks of .github/workflows/tests.yml (spotlessCheck, then the tests on the test profile) against this
# checkout's current state: HEAD plus uncommitted and untracked changes. They run in a throwaway clone, so the app
# the developer runs from this checkout's build/ is never cleaned or rebuilt.
set -uo pipefail
root=$(git rev-parse --show-toplevel) || exit 1
work=$(mktemp -d "${TMPDIR:-/tmp}/local-ci.XXXXXX")

git clone --quiet --no-checkout "$root" "$work" &&
  git -C "$work" checkout --quiet --detach "$(git -C "$root" rev-parse HEAD)" || exit 1
git -C "$root" diff --binary HEAD >"$work/.local-ci.patch"
if [ -s "$work/.local-ci.patch" ]; then
  git -C "$work" apply "$work/.local-ci.patch" || { echo "Could not apply the uncommitted changes; clone kept at $work" >&2; exit 1; }
fi
rm -f "$work/.local-ci.patch"
git -C "$root" ls-files --others --exclude-standard -z | tar -C "$root" --null -T - -cf - | tar -C "$work" -xf -

echo "Running spotlessCheck and test in $work"
cd "$work" || exit 1
SPRING_PROFILES_ACTIVE=test ./gradlew --continue --console=plain spotlessCheck test >"$work/local-ci.log" 2>&1
status=$?

if [ $status -eq 0 ]; then
  tests=$(cat build/test-results/test/TEST-*.xml 2>/dev/null | grep -o 'tests="[0-9]*"' | grep -o '[0-9]*' | paste -sd+ - | bc)
  echo "PASSED: spotlessCheck and ${tests:-all} tests."
  rm -rf "$work"
  exit 0
fi

echo "FAILED (gradle exit $status). Clone kept at $work"
grep -E '^> Task .*FAILED|^      [^ @][^[:space:]]*$|FAILED$|There were failing tests' "$work/local-ci.log" | head -40
failed=$(grep -l -E '<(failure|error)' build/test-results/test/TEST-*.xml 2>/dev/null)
[ -n "$failed" ] && echo "Failing test classes:" && echo "$failed" | sed 's|.*/TEST-||; s|\.xml$||'
[ -f build/reports/tests/test/index.html ] && echo "Report: $work/build/reports/tests/test/index.html"
echo "Full log: $work/local-ci.log"
exit $status
