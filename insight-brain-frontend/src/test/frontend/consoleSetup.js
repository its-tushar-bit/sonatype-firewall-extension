/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Console filtering setup — first file registered in jest.config.js
// setupFilesAfterEnv, so its module-load-time code runs before any test
// module (including React) is required.
//
// In CI, suppress all console output to reduce build log noise. Without this,
// Jest's default reporter prints a full stack trace for every captured
// console.warn / console.error call that fires during a passing test; on
// insight-brain this originally produced ~38 MB of stack frames per CI log
// (~77% of the log). With IS_CI=true, no console output is emitted at all —
// test failures are still reported via the regular Jest failure path and
// jest-junit XML, which are unaffected.
//
// Locally (no CI env var), pattern-based filtering is applied: only messages
// matching one of the regexes in IGNORED are suppressed, so developers still
// see console output from tests they're debugging. The IGNORED list starts
// empty — add a pattern here when a particular warning is known-safe and
// noisy enough to bury real signal.
//
// CI=true is set via frontend-maven-plugin <environmentVariables> in
// insight-brain-frontend/pom.xml, so every Maven-driven Jest run is treated
// as CI. Developers running Jest directly (e.g. `yarn jest` in watch mode)
// will have CI unset and therefore get full console output.
//
// ── Implementation note: direct assignment, not jest.spyOn ────────────────
// Earlier iterations of this file used `jest.spyOn(console, 'error')...` in
// either module-load-time or beforeEach scope. Both failed partially:
//
//   * Module-load-time spyOn was wiped by setupJest.js's
//     `afterEach(() => jest.restoreAllMocks())` after the first test in each
//     file, leaving subsequent tests' console output uncaught.
//   * beforeEach spyOn survived restoreAllMocks, but React (and other libs)
//     capture references to `console.error` at their OWN module-load time to
//     avoid recursion in dev-mode warnings. Since beforeEach runs AFTER test
//     modules are imported, React's captured reference still pointed at the
//     original CustomConsole and ~1600 React act() warnings per CI run
//     bypassed our spy entirely.
//
// Direct property assignment on `console.*` at module-load time avoids both
// problems:
//
//   * It is not a jest.spyOn mock, so jest.restoreAllMocks leaves it alone.
//   * It runs BEFORE any test module imports React, so React's internal
//     `const consoleError = console.error;` captures our filter.
//   * Tests that do their own `jest.spyOn(console, 'error')` still work —
//     spyOn wraps whatever is currently at `console.error` (our filter),
//     and restoreAllMocks restores to that same value, preserving our
//     filter for subsequent tests in the file.
//
// Pattern adapted from nexus-internal's __jest__/consoleSetup.js, which uses
// jest.spyOn because its setup.js doesn't call restoreAllMocks and its
// React imports happen to land after the spy install. Switching to direct
// assignment is strictly safer and works regardless of import order.
const IS_CI = process.env.CI === 'true';

const original = {
  error: console.error,
  warn: console.warn,
  log: console.log,
  debug: console.debug,
};

// Per-level regex lists of messages to suppress in local (non-CI) runs.
// Intentionally empty — add patterns here as the project accumulates known
// noisy-but-expected warnings. In CI, all output is suppressed regardless.
const IGNORED = {
  error: [],
  warn: [],
  log: [],
  debug: [],
};

function makeFiltered(level) {
  return (...args) => {
    if (IS_CI) return;

    // Stringify args so regex matching sees the full message, including
    // objects/arrays that React / Redux sometimes log.
    const text = args
      .map((arg) => {
        if (typeof arg === 'object') {
          try {
            return JSON.stringify(arg);
          } catch {
            return String(arg);
          }
        }
        return String(arg);
      })
      .join(' ');

    const shouldIgnore = IGNORED[level].some((re) => re.test(text));
    if (!shouldIgnore) original[level](...args);
  };
}

console.error = makeFiltered('error');
console.warn = makeFiltered('warn');
console.log = makeFiltered('log');
console.debug = makeFiltered('debug');
