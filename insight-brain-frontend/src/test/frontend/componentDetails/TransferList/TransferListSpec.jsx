/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import TransferList from 'MainRoot/componentDetails/TransferList/TransferList';
import TransferListHalf from 'MainRoot/componentDetails/TransferList/TransferListHalf';

describe('TransferList', () => {
  let minimalProps, getMounted, onAddItemMock;

  beforeEach(function () {
    onAddItemMock = jasmine.createSpy('onAddItem');

    minimalProps = {
      selected: [],
      available: [],
      onAddItem: onAddItemMock,
    };

    getMounted = enzymeUtils.getMountedComponent(TransferList, minimalProps);
  });

  it('renders two TransferListHalf', () => {
    const component = getMounted();
    const halfs = component.find('.iq-transfer-list');
    const firstHalf = halfs.find(TransferListHalf).at(0);
    const secondHalf = halfs.find(TransferListHalf).at(1);
    expect(firstHalf).toExist();
    expect(secondHalf).toExist();
  });

  it('renders two TransferListHalf with correct props', () => {
    const component = getMounted({
      available: [{ id: 1 }, { id: 2 }],
      selected: [{ id: 3 }],
    });
    const halfs = component.find('.iq-transfer-list');
    const firstHalf = halfs.find(TransferListHalf).at(0);
    const secondHalf = halfs.find(TransferListHalf).at(1);

    expect(firstHalf).toExist();
    expect(firstHalf).toHaveProp('items', [{ id: 1 }, { id: 2 }]);
    expect(firstHalf).toHaveProp('title', 'Available Labels');
    expect(firstHalf).toHaveProp('isInAvailableItems', true);
    expect(firstHalf).toHaveProp('onItemChange', onAddItemMock);

    expect(secondHalf).toExist();
    expect(secondHalf).toHaveProp('items', [{ id: 3 }]);
    expect(secondHalf).toHaveProp('title', 'Applied Labels');
    expect(secondHalf).toHaveProp('isInAvailableItems', false);
  });
});
