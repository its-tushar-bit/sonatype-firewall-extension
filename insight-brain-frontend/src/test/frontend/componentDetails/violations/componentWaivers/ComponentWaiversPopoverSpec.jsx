/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';

import ComponentWaiversPopover from '../../../../../main/frontend/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopover';
import IqPopover from '../../../../../main/frontend/react/IqPopover';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import DeleteWaiverModalContainer from '../../../../../main/frontend/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import ComponentWaiversPopoverTable from '../../../../../main/frontend/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopoverTable';

describe('ComponentWaiversPopover', function () {
  let minimalProps, getShallow;

  beforeEach(function () {
    const waivers = [];
    minimalProps = {
      componentName: 'a component',
      toggleComponentWaiversPopover: () => {},
      waivers,
      setWaiverToDelete: () => {},
    };

    getShallow = enzymeUtils.getShallowComponent(ComponentWaiversPopover, minimalProps);
  });

  it('renders a component', function () {
    const component = getShallow();
    expect(component).toExist();

    const popover = component.find(IqPopover);
    expect(popover).toHaveProp('size', 'automatic');
    expect(popover).toHaveProp('onClose', minimalProps.toggleComponentWaiversPopover);
  });

  it('renders a Header with title and close button', () => {
    const component = getShallow().dive();
    const title = component.find('.component-waivers-header__title-text');
    const closeButton = component.find(NxButton);

    expect(title).toHaveText('Component Waivers');
    expect(closeButton).toHaveProp('onClick', minimalProps.toggleComponentWaiversPopover);
    expect(closeButton).toHaveProp('variant', 'icon-only');
    expect(closeButton).toHaveProp('title', 'Close');
    expect(closeButton.find(NxFontAwesomeIcon)).toHaveProp('icon', faArrowToRight);
  });

  it('renders a DeleteWaiverModalContainer if waiverToDelete is not null', function () {
    let component, deleteWaiverModal;

    component = getShallow({ waiverToDelete: {} }).dive();
    deleteWaiverModal = component.find(DeleteWaiverModalContainer);
    expect(deleteWaiverModal).toExist();

    component = getShallow({ waiverToDelete: null }).dive();
    deleteWaiverModal = component.find(DeleteWaiverModalContainer);
    expect(deleteWaiverModal).not.toExist();
  });

  it('renders a ComponentWaiversTable with appropriate props', function () {
    const component = getShallow().dive();
    const table = component.find(ComponentWaiversPopoverTable);

    expect(table).toHaveProp('waivers', minimalProps.waivers);
    expect(table).toHaveProp('setWaiverToDelete', minimalProps.setWaiverToDelete);
    expect(table).toHaveProp('componentName', minimalProps.componentName);
  });
});
