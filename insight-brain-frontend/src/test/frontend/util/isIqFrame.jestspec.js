/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import isIqIframe from '../../../main/frontend/util/isIqFrame';

describe('isIqIframe', function () {
  it('returns false if the window is its own top', function () {
    const win = { document: {} };

    win.top = win;

    expect(isIqIframe(win)).toBe(false);
  });

  it('returns true if the window is not its own top and the top document is accessible', function () {
    const top = { document: {} },
      win = { top };

    expect(isIqIframe(win)).toBe(true);
  });

  it('returns false if accessing the top document errors out', function () {
    const top = {
        get document() {
          throw new ReferenceError('Not Allowed');
        },
      },
      win = { top };

    expect(isIqIframe(win)).toBe(false);
  });
});
