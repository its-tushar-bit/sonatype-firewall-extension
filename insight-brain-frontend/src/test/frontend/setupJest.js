/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '@testing-library/jest-dom';
import customMatchers from './customMatchers';
import $ from 'jquery';
import { enableMapSet } from 'immer';
import { TextEncoder, TextDecoder } from 'util';

// Polyfill TextEncoder/TextDecoder for jsdom (react-router requires these)
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

// Enable Immer MapSet plugin for Redux Toolkit with Set/Map support
enableMapSet();

global.$ = $;
global.jQuery = $;
global.CLM_SERVER_VERSION = '1.200.0-SNAPSHOT';

for (const [name, matcherProvider] of Object.entries(customMatchers)) {
  expect.extend({ [name]: matcherProvider().compare });
}

beforeEach(function () {
  Range.prototype.getBoundingClientRect = jest.fn().mockReturnValue({
    bottom: 0,
    height: 0,
    left: 0,
    right: 0,
    top: 0,
    width: 0,
  });
});

afterEach(function () {
  jest.restoreAllMocks();
  jest.useRealTimers();
});
