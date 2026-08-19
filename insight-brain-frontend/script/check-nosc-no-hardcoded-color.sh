#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

set -euo pipefail

# Run from insight-brain-frontend/: yarn lint:check-nosc-colors
# Advisory (warn-only, exit 0) until P1-Cert; not wired into "yarn test" yet.
# Restoring stylelint for color literals is tracked separately (esbuild migration).

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${FRONTEND_ROOT}"

# Warn-only: hex literals must live under nosc/theme/ (canonical tokens).
# Scans both Nexus One UI bundles: shared nosc/ and the nexus-one/ entry bundle.
readonly HEX_PATTERN='#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?([0-9a-fA-F]{2})?\b'
readonly GREP_INCLUDES=(
  --include='*.ts'
  --include='*.tsx'
  --include='*.js'
  --include='*.jsx'
  --include='*.scss'
  --include='*.css'
)

scan_tree() {
  local root="$1"
  shift

  if [ ! -d "$root" ]; then
    return 0
  fi

  local args=(-rnE "${GREP_INCLUDES[@]}")
  if [ "$#" -gt 0 ]; then
    for dir in "$@"; do
      args+=(--exclude-dir="$dir")
    done
  fi
  args+=("$HEX_PATTERN" "$root")

  grep "${args[@]}" 2>/dev/null || true
}

ALL_HITS=""
append_hits() {
  local chunk
  chunk="$(scan_tree "$@")"
  if [ -n "$chunk" ]; then
    if [ -n "$ALL_HITS" ]; then
      ALL_HITS="${ALL_HITS}"$'\n'"${chunk}"
    else
      ALL_HITS="$chunk"
    fi
  fi
}

# nosc/theme/** is the only allowed hex source for the shared module tree.
append_hits "src/main/frontend/nosc" "theme"
# nexus-one/ has no theme/ subtree; colors come from MainRoot/nosc/theme/*.css.
append_hits "src/main/frontend/nexus-one"

if [ -z "$ALL_HITS" ]; then
  echo "check-nosc-no-hardcoded-color: clean (0 offenders in nosc/ and nexus-one/)" >&2
  exit 0
fi

echo "check-nosc-no-hardcoded-color: WARN — hex color literal(s) outside allowed token paths:" >&2
echo "$ALL_HITS" >&2
echo "(allowed: nosc/theme/**; nexus-one must use nosc theme CSS — warn-only until P1-Cert)" >&2
exit 0
