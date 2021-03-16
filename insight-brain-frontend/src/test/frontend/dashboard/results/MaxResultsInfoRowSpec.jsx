/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import MaxResultsInfoRow from '../../../../main/frontend/dashboard/results/MaxResultsInfoRow';
import { NxTableCell } from '@sonatype/react-shared-components';

describe('MaxResultsInfoRowSpec', function() {
  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(MaxResultsInfoRow, { colSpan: 6, maxResults: 10 });
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('has informative text', () => {
    const component = getShallowComponent();
    const tableCell = component.find(NxTableCell);
    const maxResultsShown = tableCell.find('#max-results-shown');

    expect(tableCell.prop('colSpan')).toEqual(6);
    expect(maxResultsShown).toHaveText('First 10 results shown');
  });
});
