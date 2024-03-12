/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  formatGroupUsers,
  formatMembersForSaving,
  formatMembersForTransferList,
  removeFormatting,
  sortByDisplayName,
} from 'MainRoot/util/formatGroupUsers';

describe('administrators utils', () => {
  const dataWithId = [
    { internalName: 'some name', id: 'some nameGROUP', type: 'GROUP' },
    { internalName: 'some other name', id: 'some other nameUSER', type: 'USER' },
  ];
  const dataWithoutId = [
    { internalName: 'some name', type: 'GROUP' },
    { internalName: 'some other name', type: 'USER' },
  ];

  const dataWithGroup = [
    {
      internalName: 'some name',
      id: 'some name',
      displayName: 'some name (Group)',
      type: 'GROUP',
    },
    {
      internalName: 'some other name',
      id: 'some other name',
      displayName: 'a some other name (Group) (some other name)',
      type: 'USER',
    },
  ];

  const dataWithoutGroup = [
    {
      internalName: 'some name',
      id: 'some name',
      displayName: 'some name',
      type: 'GROUP',
    },
    {
      internalName: 'some other name',
      id: 'some other name',
      displayName: 'a some other name (Group)',
      type: 'USER',
    },
  ];

  it('formatMembersForTransferList', () => {
    expect(formatMembersForTransferList(dataWithoutId)).toEqual(dataWithId);
  });

  it('formatMembersForSaving', () => {
    expect(formatMembersForSaving(dataWithId)).toEqual(dataWithoutId);
  });

  it('formatGroupUsers', () => {
    expect(formatGroupUsers(dataWithoutGroup)).toEqual(dataWithGroup);
  });

  it('removeFormatting', () => {
    expect(removeFormatting(dataWithGroup[0])).toEqual(dataWithoutGroup[0]);
  });

  it('sortByDisplayName', () => {
    expect(sortByDisplayName(dataWithoutGroup)).toEqual([
      {
        internalName: 'some other name',
        id: 'some other name',
        displayName: 'a some other name (Group)',
        type: 'USER',
      },
      {
        internalName: 'some name',
        id: 'some name',
        displayName: 'some name',
        type: 'GROUP',
      },
    ]);
  });
});
