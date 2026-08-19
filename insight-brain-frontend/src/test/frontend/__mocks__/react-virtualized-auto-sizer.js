/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const React = require('react');

/**
 * Mock for react-virtualized-auto-sizer that fixes a CJS/ESM interop bug.
 *
 * @nivo/core's CJS bundle does:
 *   var a = require("react-virtualized-auto-sizer"); jsx(a, ...)
 * — treating the module namespace object as a component. This mock exports the
 * component function directly as module.exports so that pattern works.
 *
 * The real AutoSizer detects 0x0 dimensions in jsdom and bails out (doesn't
 * render children). This mock preserves that behavior — @nivo charts won't
 * render their content in tests, which is the same behavior as before the
 * @nivo 0.84 → 0.99 upgrade.
 */
function AutoSizer() {
  return React.createElement('div', { 'data-testid': 'auto-sizer' });
}

AutoSizer.displayName = 'AutoSizer';

module.exports = Object.assign(AutoSizer, {
  default: AutoSizer,
  __esModule: true,
});
