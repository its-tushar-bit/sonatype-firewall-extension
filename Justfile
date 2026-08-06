# Justfile for Nexus IQ Server (insight-brain) project

import? 'Justfile.local'

# Variables
mvn := env_var_or_default("MVN", "mvnd")
quick_opts := "-DskipTests -Pquick"
local_repo_dir := "./m2-directory"

# Default recipe (runs when you just type 'just')
default:
   @echo "By default this will use mvnd for faster builds. Configure environment variable MVN=mvn to use plain Maven"
   @echo "Note: insight-brain only works with mvnd 1: brew install mvndaemon/mvnd/mvnd@1"
   @just --list

# Build, install into m2
install:
   {{mvn}} install {{quick_opts}}

# Build, install into m2, test
test:
   {{mvn}} install

# Build, install into m2, using isolated local repository
install-isolated:
    {{mvn}} -pl clean install {{quick_opts}} -Dmaven.repo.local={{local_repo_dir}}

# Build, install into m2, without frontend, using isolated local repository
install-be-isolated:
    {{mvn}} -pl '!insight-brain-frontend' clean install {{quick_opts}} -Dmaven.repo.local={{local_repo_dir}}

# Process classes for OpenJPA use
process:
    {{mvn}} -pl '!insight-brain-frontend' process-classes {{quick_opts}}

# Check for license headers
license:
    {{mvn}} license:check

# Check formatting
style:
    {{mvn}} spotless:check

# Apply formatting to changed files
format:
    {{mvn}} spotless:apply

# Run all Playwright UI tests with Docker
func-test:
    {{mvn}} verify -pl insight-brain-playwright-test

# Run specific Playwright UI test (usage: just func-test-specific TestClassName#testMethodName)
func-test-specific TEST:
    {{mvn}} verify -pl insight-brain-playwright-test -Dit.test={{TEST}}

# Run a particular integration test in insight-brain-service
it name:
    {{mvn}} -pl 'insight-brain-service' -Dit.test={{name}} -Dmaven.repo.local={{local_repo_dir}} -Dskip.shaded=true verify

# Run all integration tests in insight-brain-service
run-all-its:
    {{mvn}} verify -pl '!insight-brain-frontend' -DforkCount=12 -DreuseForks=false -Dparallel=classes -DthreadCountClasses=12 -DargLine="-Xmx5g" -Dspotless.apply.skip=true -Dspotless.check.skip=true -Dmaven.repo.local={{local_repo_dir}}

# Run a particular integration test in nexus-mtiq-server
mtiq-it name:
    {{mvn}} -pl 'nexus-mtiq-server' -Dit.test={{name}} -Dmaven.repo.local={{local_repo_dir}} -Dskip.shaded=true verify

