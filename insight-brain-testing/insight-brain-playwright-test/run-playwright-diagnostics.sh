#!/bin/bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

# Playwright Infrastructure Diagnostics Script
# This script runs diagnostic tests to verify TestCLMServer infrastructure is working

set -e

cd "$(dirname "$0")"

echo "════════════════════════════════════════════════════════════════"
echo "  Playwright Infrastructure Diagnostics"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "This will verify:"
echo "  ✓ TestCLMServer starts correctly"
echo "  ✓ baseUrlFromTest is set properly"
echo "  ✓ Server is reachable"
echo "  ✓ Admin credentials work"
echo "  ✓ Playwright can connect"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Run the quick sanity check first
echo "Step 1: Running quick baseUrl sanity check..."
echo "────────────────────────────────────────────────────────────────"
mvn verify \
    -Dit.test=InfrastructureDiagnosticTest#testBaseUrlSanityCheck \
    -Dheadless=true \
    -q

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Quick sanity check PASSED"
    echo ""
else
    echo ""
    echo "❌ Quick sanity check FAILED"
    echo ""
    echo "This likely means:"
    echo "  - You're running the standalone framework (playrdemo) instead of main project"
    echo "  - Or TestCLMServer static block didn't run"
    echo ""
    exit 1
fi

# Run the comprehensive diagnostic
echo "Step 2: Running comprehensive infrastructure diagnostics..."
echo "────────────────────────────────────────────────────────────────"
mvn verify \
    -Dit.test=InfrastructureDiagnosticTest#testInfrastructureDiagnostics \
    -Dheadless=true \
    -q

if [ $? -eq 0 ]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  ✅ ALL DIAGNOSTICS PASSED"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "Your Playwright infrastructure is working correctly!"
    echo ""
    echo "Next steps:"
    echo "  1. Run your actual tests: mvn verify -Dit.test=LoginPlaywrightTest"
    echo "  2. View test logs: tail -f target/test-logs/playwright-tests.log"
    echo "  3. View Allure report: mvn allure:serve"
    echo ""
else
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  ❌ DIAGNOSTICS FAILED"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "Check the detailed output above to see what failed."
    echo ""
    echo "Common issues:"
    echo "  - Server not starting: Check TestCLMServer configuration"
    echo "  - Credentials wrong: Verify ADMIN_USERNAME/ADMIN_PASSWORD"
    echo "  - Port conflict: Another service using port 8070"
    echo ""
    echo "View detailed logs:"
    echo "  cat target/test-logs/playwright-tests.log"
    echo ""
    exit 1
fi
