/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxTooltip, NxButton } from '@sonatype/react-shared-components';

import DashboardFilterFooter from
  '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterFooter';
import * as enzymeUtils from '../../../enzymeUtils';

describe('DashboardFilter footer', function() {
  const getShallowComponent = enzymeUtils.getShallowComponent(DashboardFilterFooter, {});

  it('renders a section with the footer classes', function() {
    const fullFilter = getShallowComponent(),
        footer = fullFilter.find('.dashboard-filter-footer');

    expect(footer).toExist();
  });

  it('renders buttons', function () {
    const fullFilter = getShallowComponent(),
        footer = fullFilter.find('.dashboard-filter-footer'),
        applyBtn = footer.find('#dashboard-filter-apply').dive(),
        revertBtn = footer.find('#dashboard-filter-revert').dive(),
        clearBtn = footer.find('#dashboard-filter-clear').dive();

    expect(applyBtn).toHaveClassName('nx-btn--primary', 'nx-btn');

    expect(revertBtn).toHaveClassName('nx-btn--tertiary', 'nx-btn');

    expect(clearBtn).toHaveClassName('nx-btn--tertiary', 'nx-btn');
  });

  it('changes disabled class in apply button depending on filtersAreDirty and needsAcknowledgement', function () {
    let fullFilter, footer, applyBtn;

    // !needsAcknowledgement && !filtersAreDirty
    fullFilter = getShallowComponent({
      needsAcknowledgement: false,
      filtersAreDirty: false
    });
    footer = fullFilter.find('.dashboard-filter-footer');
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).toHaveClassName('disabled');

    // needsAcknowledgement && !filtersAreDirty
    fullFilter = getShallowComponent({
      needsAcknowledgement: true,
      filtersAreDirty: false
    });
    footer = fullFilter.find('.dashboard-filter-footer');
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).not.toHaveClassName('disabled');

    // needsAcknowledgement && filtersAreDirty
    fullFilter = getShallowComponent({
      needsAcknowledgement: true,
      filtersAreDirty: true
    });
    footer = fullFilter.find('.dashboard-filter-footer');
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).not.toHaveClassName('disabled');

    // !needsAcknowledgement && filtersAreDirty
    fullFilter = getShallowComponent({
      needsAcknowledgement: false,
      filtersAreDirty: true
    });
    footer = fullFilter.find('.dashboard-filter-footer');
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).not.toHaveClassName('disabled');
  });

  it('adds a tooltip to the apply btn if it is disabled', function () {
    const fullFilter = getShallowComponent({ filtersAreDirty: false }),
        footer = fullFilter.find('.dashboard-filter-footer'),
        tooltip = footer.find(NxTooltip),
        applyBtn = tooltip.find(NxButton);

    expect(tooltip).toHaveProp('title', 'There are no changes to update.');
    expect(applyBtn).toHaveClassName('disabled');
  });

  it('disables the revert button if the filters are not dirty', function() {
    let fullFilter, footer, revertBtn;

    fullFilter = getShallowComponent({ filtersAreDirty: false });
    footer = fullFilter.find('.dashboard-filter-footer');
    revertBtn = footer.find('#dashboard-filter-revert');
    expect(revertBtn).toHaveClassName('disabled');

    fullFilter = getShallowComponent({ filtersAreDirty: true });
    footer = fullFilter.find('.dashboard-filter-footer');
    revertBtn = footer.find('#dashboard-filter-revert');
    expect(revertBtn).not.toHaveClassName('disabled');
  });
});
