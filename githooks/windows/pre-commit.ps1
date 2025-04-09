#!powershell
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
  #

# Define colors for output
$RED = "`e[1;31m"
$GREEN = "`e[1;32m"
$NC = "`e[0m"

# Get all staged files
$staged_files = git diff --cached --name-only --diff-filter=d

# Filter JavaScript, JSX, CSS, SCSS, and Markdown files
$js_files = $staged_files | Select-String -Pattern '\.(js|jsx)$'
$css_scss_files = $staged_files | Select-String -Pattern '\.(css|scss)$'
$md_files = $staged_files | Select-String -Pattern '\.md$'

# Run Prettier on the filtered files
$found_files = $false
if ($js_files -or $css_scss_files -or $md_files) {
  $found_files = $true
  Write-Host "${GREEN}Running Prettier...${NC}"
  $files_to_format = ($js_files, $css_scss_files, $md_files) -join "`n"
  $files_to_format -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" } | ForEach-Object {
    ./insight-brain-frontend/node_modules/.bin/prettier --write $_
  }
  $prettier_exit_code = $LASTEXITCODE
} else {
  $prettier_exit_code = 0
}

# Check if Prettier succeeded
if ($prettier_exit_code -ne 0) {
  Write-Host "${RED}Prettier failed.${NC}"
  exit 1
} else {
  if ($found_files) {
    Write-Host "${GREEN}Prettier succeeded.${NC}"
  }
}

# Add the formatted files back to the staging area
$files_to_format -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" } | ForEach-Object {
  git add $_
}

exit 0
