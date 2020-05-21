/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxButton } from '@sonatype/react-shared-components';

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
        saveBtn = footer.find('#dashboard-filter-save').dive();

    expect(applyBtn).toHaveClassName('nx-btn--primary', 'nx-btn');

    expect(revertBtn).toHaveClassName('nx-btn--tertiary', 'nx-btn');

    expect(saveBtn).toHaveClassName('nx-btn--tertiary', 'nx-btn');
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
        applyBtnTooltip = fullFilter.find('#dashboard-filter-apply-tooltip'),
        applyBtn = applyBtnTooltip.find(NxButton);

    expect(applyBtnTooltip).toHaveTagName('NxTooltip');
    expect(applyBtnTooltip).toHaveProp('title', 'There are no changes to update.');
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

  describe('Apply button onClick handler', function() {
    let onApplyCurrentFilter;
    beforeEach(function() {
      onApplyCurrentFilter = jasmine.createSpy('onApplyCurrentFilter');
    });

    it('calls onApplyCurrentFilter callback if filters are dirty', function() {
      const shallowRender = getShallowComponent({
        filtersAreDirty: true,
        needsAcknowledgement: false,
        onApplyCurrentFilter
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).toHaveBeenCalled();
    });

    it('calls onApplyCurrentFilter if filters are not dirty but needs acknowledgement', function() {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        needsAcknowledgement: true,
        onApplyCurrentFilter
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).toHaveBeenCalled();
    });

    it('doesn\'t call onApplyCurrentFilter if filters are not dirty and doesn\'t need acknowledgement', function() {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        needsAcknowledgement: false,
        onApplyCurrentFilter
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).not.toHaveBeenCalled();
    });
  });

  describe('Revert button onClick handler', function() {
    it('calls revert callback', function() {
      const revert = jasmine.createSpy('revert'),
          shallowRender = getShallowComponent({ revert });

      shallowRender.find('#dashboard-filter-revert').simulate('click');
      expect(revert).toHaveBeenCalled();
    });
  });

  describe('Save button onClick handler', function() {
    let setDisplaySaveFilterModal;
    beforeEach(function() {
      setDisplaySaveFilterModal = jasmine.createSpy('setDisplaySaveFilterModal');
    });

    it('calls setDisplaySaveFilterModal callback if filters are not dirty', function() {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        setDisplaySaveFilterModal
      });

      shallowRender.find('#dashboard-filter-save').simulate('click');
      expect(setDisplaySaveFilterModal).toHaveBeenCalledWith(true);
    });

    it('doesn\'t call setDisplaySaveFilterModal if filters are dirty', function() {
      const shallowRender = getShallowComponent({
        filtersAreDirty: true,
        setDisplaySaveFilterModal
      });

      shallowRender.find('#dashboard-filter-save').simulate('click');
      expect(setDisplaySaveFilterModal).not.toHaveBeenCalled();
    });
  });

  describe('Save button', function() {
    it('is disabled with tooltip when filters are dirty', function() {
      const shallowRender = getShallowComponent({ filtersAreDirty: true }),
          saveBtnTooltip = shallowRender.find('#dashboard-filter-save-tooltip'),
          saveBtn = saveBtnTooltip.find(NxButton);

      expect(saveBtnTooltip).toHaveTagName('NxTooltip');
      expect(saveBtnTooltip).toHaveProp('title', 'Please apply filter before saving');
      expect(saveBtn).toHaveClassName('disabled');
    });

    it('is enabled with no tooltip when filters are not dirty', function() {
      const shallowRender = getShallowComponent({ filtersAreDirty: false }),
          saveBtnTooltip = shallowRender.find('#dashboard-filter-save-tooltip'),
          saveBtn = saveBtnTooltip.find(NxButton);

      expect(saveBtnTooltip).toHaveTagName('NxTooltip');
      expect(saveBtnTooltip).toHaveProp('title', '');
      expect(saveBtn).not.toHaveClassName('disabled');
    });
  });
});
