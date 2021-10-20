/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import ManageComponentLabels from 'MainRoot/componentDetails/ManageComponentLabels/ManageComponentLabels';
import TransferList from 'MainRoot/componentDetails/TransferList/TransferList';

describe('ManageComponentLabels', () => {
  let minimalProps, getMounted, loadApplicableLabelsMock;

  beforeEach(function () {
    loadApplicableLabelsMock = jasmine.createSpy('loadApplicableLabels').and.returnValue([]);

    minimalProps = {
      applicableLabels: [],
      selectedLabels: [],
      loadApplicableLabels: loadApplicableLabelsMock,
      loading: false,
      loadError: null,
    };

    getMounted = enzymeUtils.getMountedComponent(ManageComponentLabels, minimalProps);
  });

  it('renders a nx-h2 title', () => {
    const component = getMounted();
    const content = component.find('.nx-h2');

    expect(content).toHaveText('Manage Labels');
  });

  it('calls loadApplicable action', () => {
    getMounted();
    expect(loadApplicableLabelsMock).toHaveBeenCalledTimes(1);
  });

  it('renders a TransferList', () => {
    const component = getMounted();

    expect(component.find(TransferList)).toExist();
  });

  it('renders a TransferList with correct props', () => {
    const component = getMounted({
      applicableLabels: [{ id: 1 }, { id: 2 }, { id: 3 }],
      selectedLabels: [{ id: 3 }],
    });

    expect(component.find(TransferList)).toExist();
    expect(component.find(TransferList)).toHaveProp('available', [{ id: 1 }, { id: 2 }]);
    expect(component.find(TransferList)).toHaveProp('selected', [{ id: 3 }]);
  });
});
