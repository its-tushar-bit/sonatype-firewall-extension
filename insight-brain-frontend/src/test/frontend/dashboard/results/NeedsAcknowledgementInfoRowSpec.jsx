/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import NeedsAcknowledgementInfoRow from '../../../../main/frontend/dashboard/results/NeedsAcknowledgementInfoRow';
import { NxInfoAlert, NxTableCell } from '@sonatype/react-shared-components';

describe('NeedsAcknowledgementInfoRowSpec', function() {
  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(NeedsAcknowledgementInfoRow, { colSpan: 6 });
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('has informative text', () => {
    const component = getShallowComponent();
    const tableCell = component.find(NxTableCell);
    const infoAlert = tableCell.find(NxInfoAlert);

    expect(tableCell.prop('colSpan')).toEqual(6);
    expect(infoAlert).toHaveText('Select your filter criteria on the left, and click \'apply\' to see results.');
  });
});
