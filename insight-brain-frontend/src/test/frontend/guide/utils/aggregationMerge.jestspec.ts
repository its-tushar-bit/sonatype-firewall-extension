/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { mergeAggregations, type Aggregations } from '@guide/ui-core/utils';

describe('mergeAggregations (re-exported from @guide/ui-core/utils)', () => {
  it('returns undefined when both inputs are absent', () => {
    expect(mergeAggregations(null, undefined)).toBeUndefined();
    expect(mergeAggregations(undefined, undefined)).toBeUndefined();
  });

  it('returns search aggregations as-is when browse cache is unavailable', () => {
    const search: Aggregations = { byFormat: { npm: 2 } };
    expect(mergeAggregations(null, search)).toEqual(search);
  });

  it('returns browse aggregations when the search response omits them', () => {
    const browse: Aggregations = { byFormat: { npm: 5, maven: 3 } };
    expect(mergeAggregations(browse, undefined)).toEqual(browse);
  });

  it('zero-fills facet values present in browse but missing from search', () => {
    const browse: Aggregations = { byFormat: { npm: 5, maven: 3, pypi: 1 } };
    const search: Aggregations = { byFormat: { npm: 2 } };

    expect(mergeAggregations(browse, search)).toEqual({
      byFormat: { npm: 2, maven: 0, pypi: 0 },
    });
  });

  it('keeps facet values that are new in the search response', () => {
    const browse: Aggregations = { byFormat: { npm: 5 } };
    const search: Aggregations = { byFormat: { npm: 2, golang: 1 } };

    expect(mergeAggregations(browse, search)).toEqual({
      byFormat: { npm: 2, golang: 1 },
    });
  });
});
