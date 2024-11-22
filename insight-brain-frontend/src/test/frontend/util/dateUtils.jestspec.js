/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { formatTimeAgo } from '../../../main/frontend/util/dateUtils';

describe('formatTimeAgo specs', function () {
  it('returns 1 day ago', function () {
    const date = new Date(2023, 2, 1);
    const now = new Date(2023, 2, 2);
    expect(formatTimeAgo(date, true, now)).toEqual('1d');
  });

  it('returns 15 days ago', function () {
    const date = new Date(2023, 2, 1);
    const now = new Date(2023, 2, 16);
    expect(formatTimeAgo(date, true, now)).toEqual('15d');
  });

  it('returns 1 month ago', function () {
    const date = new Date(2023, 4, 1);
    const now = new Date(2023, 5, 1);
    expect(formatTimeAgo(date, true, now)).toEqual('1mo');
  });

  it('returns 1 month ago', function () {
    const date = new Date(2022, 12, 15);
    const now = new Date(2023, 1, 15);
    expect(formatTimeAgo(date, true, now)).toEqual('1mo');
  });

  it('returns 17 days ago', function () {
    const date = new Date(2022, 12, 15);
    const now = new Date(2023, 1, 1);
    expect(formatTimeAgo(date, true, now)).toEqual('17d');
  });

  it('returns 1 month ago', function () {
    const date = new Date(2022, 12, 1);
    const now = new Date(2023, 1, 15);
    expect(formatTimeAgo(date, true, now)).toEqual('1mo');
  });

  it('returns 3 days ago', function () {
    const date = new Date(2023, 1, 4);
    const now = new Date(2023, 1, 7);
    expect(formatTimeAgo(date, true, now)).toEqual('3d');
  });

  it('returns 3 days ago', function () {
    const date = new Date(2023, 1, 28);
    const now = new Date(2023, 2, 3);
    expect(formatTimeAgo(date, true, now)).toEqual('3d');
  });

  it('returns 3 hours ago', function () {
    const date = new Date(2023, 2, 3, 6, 0, 0);
    const now = new Date(2023, 2, 3, 9, 0, 0);
    expect(formatTimeAgo(date, true, now)).toEqual('3h');
  });
});
