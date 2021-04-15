/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import escapeHtmlString from '../../../main/frontend/util/escapeHtmlString';

describe('escapeHtmlString', function () {
  it('escapes HTML special characters in the string', function () {
    expect(escapeHtmlString('foo')).toBe('foo');
    expect(escapeHtmlString('<>&"\'')).toBe('&lt;&gt;&amp;"\'');
    expect(
      escapeHtmlString('<img src="nothing.png" onerror="alert(\'pwned!\')">')
    ).toBe('&lt;img src="nothing.png" onerror="alert(\'pwned!\')"&gt;');

    expect(escapeHtmlString('합기도')).toBe('합기도');
  });
});
