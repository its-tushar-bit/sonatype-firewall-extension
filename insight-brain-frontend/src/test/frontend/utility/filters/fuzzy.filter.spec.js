/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('fuzzy.filter.js', function () {
  var fuzzyFilter, nameFilter;

  beforeEach(angular.mock.module(utilityModule.name));

  beforeEach(inject([
    'fuzzyFilter',
    function (_fuzzyFilter) {
      fuzzyFilter = _fuzzyFilter;
      nameFilter = function (filter, names) {
        return fuzzyFilter(
          names.map((n) => ({ name: n })),
          filter,
          'name'
        ).map((o) => o.name);
      };
    },
  ]));

  it('ignores case', function () {
    expect(nameFilter('foo', ['FOO'])).toEqual(['FOO']);
  });

  it('is fuzzy', function () {
    expect(nameFilter('test', ['test-name'])).toEqual(['test-name']);
    expect(nameFilter('test', ['app-test-server'])).toEqual([
      'app-test-server',
    ]);
    expect(nameFilter('test', ['quality-test'])).toEqual(['quality-test']);
    expect(nameFilter('testung-app', ['testing-app'])).toEqual(['testing-app']);
  });

  it('is not too fuzzy', function () {
    expect(nameFilter('iq-server', ['pi-parent'])).toEqual([]);
    expect(nameFilter('foo', ['Bamboo Plugin'])).toEqual([]);
    expect(nameFilter('bingo', ['mongobee'])).toEqual([]);
    expect(nameFilter('adma', ['zamarchive-webapp'])).toEqual([]);
  });

  it('optionally extracts identifier property of results', function () {
    expect(
      fuzzyFilter(
        [
          { id: 'id-1', name: 'name-1' },
          { id: 'id-2', name: 'name-2' },
          { id: 'id-3', name: 'other' },
        ],
        'name',
        'name',
        'id'
      )
    ).toEqual(['id-1', 'id-2']);
  });
});
