#!/bin/sh
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

# Execute this script with all destination image dependencies
# before apk add has been run previously.
# This will ensure that all dependencies of the packages will
# be discovered correctly by the script

echo "Installing and logging dependencies"
apk add --no-cache "$@" > /tmp/apk-add.log

# Install grep (if not installed before)
apk add grep

# Collect packages in an array (works in bash or ash, like Alpine's /bin/sh)
packages=$(grep -oP 'Installing \K[^ ]+' /tmp/apk-add.log)

# Clean up
rm /tmp/apk-add.log

DEST="/tmp/package-content"

# Create the destination directory
mkdir -p "$DEST"

# Loop through the packages to get the content specs
for pkg in $packages; do
  echo "Processing package: $pkg"

  # Get the list of files, skip the first line (package header)
  apk info -L "$pkg" | tail -n +2 | while read -r filepath; do
    # Skip empty lines (defensive)
    [ -z "$filepath" ] && continue

    # Define the destination path
    target="$DEST/$filepath"

    # Create parent directory in destination
    mkdir -p "$(dirname "$target")"

    # Copy the file, preserving symlinks and metadata
    cp -a -- "/$filepath" "$target"
  done
done

# Clean up - move /usr/sbin stuff to /usr/bin,
# because /usr/sbin is usually a symbolic link to /usr/bin anyway
if [ -d "$DEST/usr/sbin" ]; then
  echo "Moving contents from usr/sbin to usr/bin..."

  # Ensure usr/bin exists
  mkdir -p "$DEST/usr/bin"

  # Move files and directories, preserving structure
  mv "$DEST/usr/sbin/"* "$DEST/usr/bin/"

  # Optionally remove the now-empty usr/sbin
  rmdir "$DEST/usr/sbin" 2>/dev/null || echo "Note: usr/sbin not empty, not removed."
fi
