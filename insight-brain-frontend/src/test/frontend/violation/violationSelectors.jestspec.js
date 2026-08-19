/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectViolationFilteredSimilarWaivers, selectViolationSlice } from 'MainRoot/violation/violationSelectors';
import moment from 'moment';

describe('violationSelectors', function () {
  describe('selectViolationFilteredSimilarWaivers', () => {
    it('is composed from the following selector', () => {
      expect(selectViolationFilteredSimilarWaivers.dependencies).toEqual([selectViolationSlice]);
    });
    const futureDate = moment().add(7, 'days');
    const pastDate = moment().subtract(7, 'days');
    const similarWaivers = [
      {
        comment: '',
        expiryTime: futureDate,
        matcherStrategy: 'EXACT_COMPONENT',
        name: 'exact active waiver with no comment ',
      },
      {
        comment: 'some comment',
        expiryTime: futureDate,
        matcherStrategy: 'EXACT_COMPONENT',
        name: 'exact active waiver with comment ',
      },
      {
        comment: '',
        expiryTime: null,
        matcherStrategy: 'ALL_COMPONENTS',
        name: 'not exact active waiver with no comment ',
      },
      {
        comment: 'some comment',
        expiryTime: null,
        matcherStrategy: 'ALL_VERSIONS',
        name: 'not exact active waiver with comment ',
      },
      {
        comment: '',
        expiryTime: pastDate,
        matcherStrategy: 'EXACT_COMPONENT',
        name: 'exact expired waiver with no comment ',
      },
      {
        comment: 'some comment',
        expiryTime: pastDate,
        matcherStrategy: 'EXACT_COMPONENT',
        name: 'exact expired waiver with comment ',
      },
      {
        comment: '',
        expiryTime: pastDate,
        matcherStrategy: 'ALL_COMPONENTS',
        name: 'not exact expired waiver with no comment ',
      },
      {
        comment: 'some comment',
        expiryTime: pastDate,
        matcherStrategy: 'ALL_VERSIONS',
        name: 'not exact expired waiver with comment ',
      },
    ];

    it('selects all similarWaivers when no filter is active', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['']),
      });

      expect(actualSelection).toEqual(similarWaivers);
    });

    it('selects selected similarWaivers when active waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['active']),
      });

      expect(actualSelection).toEqual([
        {
          comment: '',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with no comment ',
        },
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
        {
          comment: '',
          expiryTime: null,
          matcherStrategy: 'ALL_COMPONENTS',
          name: 'not exact active waiver with no comment ',
        },
        {
          comment: 'some comment',
          expiryTime: null,
          matcherStrategy: 'ALL_VERSIONS',
          name: 'not exact active waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when exact waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['exact']),
      });

      expect(actualSelection).toEqual([
        {
          comment: '',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with no comment ',
        },
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
        {
          comment: '',
          expiryTime: pastDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact expired waiver with no comment ',
        },
        {
          comment: 'some comment',
          expiryTime: pastDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact expired waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when commented waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['comment']),
      });

      expect(actualSelection).toEqual([
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
        {
          comment: 'some comment',
          expiryTime: null,
          matcherStrategy: 'ALL_VERSIONS',
          name: 'not exact active waiver with comment ',
        },
        {
          comment: 'some comment',
          expiryTime: pastDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact expired waiver with comment ',
        },
        {
          comment: 'some comment',
          expiryTime: pastDate,
          matcherStrategy: 'ALL_VERSIONS',
          name: 'not exact expired waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when active and exact waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['active', 'exact']),
      });

      expect(actualSelection).toEqual([
        {
          comment: '',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with no comment ',
        },
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when active and commented waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['active', 'comment']),
      });

      expect(actualSelection).toEqual([
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
        {
          comment: 'some comment',
          expiryTime: null,
          matcherStrategy: 'ALL_VERSIONS',
          name: 'not exact active waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when exact and commented waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['exact', 'comment']),
      });

      expect(actualSelection).toEqual([
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
        {
          comment: 'some comment',
          expiryTime: pastDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact expired waiver with comment ',
        },
      ]);
    });

    it('selects selected similarWaivers when active, exact and commented waivers filter is enabled', () => {
      const actualSelection = selectViolationFilteredSimilarWaivers.resultFunc({
        similarWaivers,
        similarWaiversFilterSelectedIds: new Set(['active', 'exact', 'comment']),
      });
      expect(actualSelection).toEqual([
        {
          comment: 'some comment',
          expiryTime: futureDate,
          matcherStrategy: 'EXACT_COMPONENT',
          name: 'exact active waiver with comment ',
        },
      ]);
    });
  });
});
