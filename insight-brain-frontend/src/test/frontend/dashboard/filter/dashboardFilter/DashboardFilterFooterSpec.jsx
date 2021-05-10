/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxButton, NxErrorAlert } from '@sonatype/react-shared-components';

import DashboardFilterFooter from '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterFooter';
import * as enzymeUtils from '../../../enzymeUtils';

describe('DashboardFilter footer', function () {
  const getShallowComponent = enzymeUtils.getShallowComponent(DashboardFilterFooter, {});

  it('renders buttons', function () {
    const footer = getShallowComponent(),
      applyBtn = footer.find('#dashboard-filter-apply').dive(),
      revertBtn = footer.find('#dashboard-filter-revert').dive(),
      saveBtn = footer.find('#dashboard-filter-save').dive();

    expect(applyBtn).toHaveClassName('nx-btn--primary', 'nx-btn');
    expect(revertBtn).toHaveClassName('nx-btn--tertiary', 'nx-btn');
    expect(saveBtn).toHaveClassName('nx-btn');
  });

  it('changes disabled class in apply button depending on filtersAreDirty and needsAcknowledgement', function () {
    let footer, applyBtn;

    // !needsAcknowledgement && !filtersAreDirty
    footer = getShallowComponent({
      needsAcknowledgement: false,
      filtersAreDirty: false,
    });
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).toHaveClassName('disabled');

    // needsAcknowledgement && !filtersAreDirty
    footer = getShallowComponent({
      needsAcknowledgement: true,
      filtersAreDirty: false,
    });
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).not.toHaveClassName('disabled');

    // needsAcknowledgement && filtersAreDirty
    footer = getShallowComponent({
      needsAcknowledgement: true,
      filtersAreDirty: true,
    });
    applyBtn = footer.find('#dashboard-filter-apply');
    expect(applyBtn).not.toHaveClassName('disabled');

    // !needsAcknowledgement && filtersAreDirty
    footer = getShallowComponent({
      needsAcknowledgement: false,
      filtersAreDirty: true,
    });
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

  it('adds an empty tooltip to the apply btn if it is enabled and needs acknowledgement is true', function () {
    const fullFilter = getShallowComponent({ filtersAreDirty: false, needsAcknowledgement: true }),
      applyBtnTooltip = fullFilter.find('#dashboard-filter-apply-tooltip'),
      applyBtn = applyBtnTooltip.find(NxButton);

    expect(applyBtnTooltip).toHaveTagName('NxTooltip');
    expect(applyBtnTooltip).toHaveProp('title', '');
    expect(applyBtn).not.toHaveClassName('disabled');
  });

  it('disables the revert button if the filters are not dirty', function () {
    let footer, revertBtn;

    footer = getShallowComponent({ filtersAreDirty: false });
    revertBtn = footer.find('#dashboard-filter-revert');
    expect(revertBtn).toHaveClassName('disabled');

    footer = getShallowComponent({ filtersAreDirty: true });
    revertBtn = footer.find('#dashboard-filter-revert');
    expect(revertBtn).not.toHaveClassName('disabled');
  });

  describe('Apply button onClick handler', function () {
    let onApplyCurrentFilter;
    beforeEach(function () {
      onApplyCurrentFilter = jasmine.createSpy('onApplyCurrentFilter');
    });

    it('calls onApplyCurrentFilter callback if filters are dirty', function () {
      const shallowRender = getShallowComponent({
        filtersAreDirty: true,
        needsAcknowledgement: false,
        onApplyCurrentFilter,
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).toHaveBeenCalled();
    });

    it('calls onApplyCurrentFilter if filters are not dirty but needs acknowledgement', function () {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        needsAcknowledgement: true,
        onApplyCurrentFilter,
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).toHaveBeenCalled();
    });

    it("doesn't call onApplyCurrentFilter if filters are not dirty and doesn't need acknowledgement", function () {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        needsAcknowledgement: false,
        onApplyCurrentFilter,
      });

      shallowRender.find('#dashboard-filter-apply').simulate('click');
      expect(onApplyCurrentFilter).not.toHaveBeenCalled();
    });
  });

  describe('Filter error buttons', function () {
    let onApplyCurrentFilter;
    beforeEach(function () {
      onApplyCurrentFilter = jasmine.createSpy('onApplyCurrentFilter');
    });

    it('shows the filter error buttons if applyFilterError is true', function () {
      const footer = getShallowComponent({
        applyFilterError: true,
        onApplyCurrentFilter,
      });
      const noErrorSaveButton = footer.find('#dashboard-filter-save');
      const errorFooter = footer.find(NxErrorAlert);

      expect(errorFooter).toExist();
      expect(noErrorSaveButton).not.toExist();
    });

    it('retries to apply filters if you click the filter error retry button', function () {
      const footer = getShallowComponent({
        applyFilterError: true,
        filtersAreDirty: false,
        needsAcknowledgement: true,
        onApplyCurrentFilter,
      });

      const noErrorSaveButton = footer.find('#dashboard-filter-save');
      const errorFooter = footer.find(NxErrorAlert);

      expect(errorFooter).toExist();
      expect(noErrorSaveButton).not.toExist();
      const errorButton = errorFooter.find('#dashboard-filter-retry-button');
      expect(onApplyCurrentFilter).not.toHaveBeenCalled();
      errorButton.simulate('click');
      expect(onApplyCurrentFilter).toHaveBeenCalled();
    });

    it('calls the onCancel function if you click the filter error cancel button', function () {
      const onApplyCancelSpy = jasmine.createSpy('onCancelApplyFilter');
      const footer = getShallowComponent({
        applyFilterError: true,
        filtersAreDirty: false,
        needsAcknowledgement: true,
        onApplyCurrentFilter,
        onCancelApplyFilter: onApplyCancelSpy,
      });

      const noErrorSaveButton = footer.find('#dashboard-filter-save');
      const errorFooter = footer.find(NxErrorAlert);

      expect(errorFooter).toExist();
      expect(noErrorSaveButton).not.toExist();
      const cancelButton = errorFooter.find('#dashboard-filter-cancel-button');
      expect(onApplyCancelSpy).not.toHaveBeenCalled();
      cancelButton.simulate('click');
      expect(onApplyCancelSpy).toHaveBeenCalled();
    });

    it('does not show the filter error buttons if applyFilterError is false', function () {
      const footer = getShallowComponent({
        applyFilterError: false,
        onApplyCurrentFilter,
      });

      const noErrorSaveButton = footer.find('#dashboard-filter-save');
      const errorFooter = footer.find(NxErrorAlert);

      expect(errorFooter).not.toExist();
      expect(noErrorSaveButton).toExist();
    });
  });

  describe('Revert button onClick handler', function () {
    it('calls revert callback and is not styled as disabled if filters are dirty', function () {
      const revert = jasmine.createSpy('revert'),
        shallowRender = getShallowComponent({
          revert,
          filtersAreDirty: true,
        });

      const revertButton = shallowRender.find('#dashboard-filter-revert');
      expect(revertButton).not.toHaveClassName('disabled');
      revertButton.simulate('click');
      expect(revert).toHaveBeenCalled();
    });

    it('does not call revert callback and styled as disabled if filters are not dirty', function () {
      const revert = jasmine.createSpy('revert'),
        shallowRender = getShallowComponent({
          revert,
          filtersAreDirty: false,
        });

      const revertButton = shallowRender.find('#dashboard-filter-revert');
      expect(revertButton).toHaveClassName('disabled');
      revertButton.simulate('click');
      expect(revert).not.toHaveBeenCalled();
    });
  });

  describe('Save button onClick handler', function () {
    let setDisplaySaveFilterModal;
    beforeEach(function () {
      setDisplaySaveFilterModal = jasmine.createSpy('setDisplaySaveFilterModal');
    });

    it('calls setDisplaySaveFilterModal callback if filters are not dirty', function () {
      const shallowRender = getShallowComponent({
        filtersAreDirty: false,
        setDisplaySaveFilterModal,
      });

      shallowRender.find('#dashboard-filter-save').simulate('click');
      expect(setDisplaySaveFilterModal).toHaveBeenCalledWith(true);
    });

    it("doesn't call setDisplaySaveFilterModal if filters are dirty", function () {
      const shallowRender = getShallowComponent({
        filtersAreDirty: true,
        setDisplaySaveFilterModal,
      });

      shallowRender.find('#dashboard-filter-save').simulate('click');
      expect(setDisplaySaveFilterModal).not.toHaveBeenCalled();
    });
  });

  describe('Save button', function () {
    it('is disabled with tooltip when filters are dirty', function () {
      const shallowRender = getShallowComponent({ filtersAreDirty: true }),
        saveBtnTooltip = shallowRender.find('#dashboard-filter-save-tooltip'),
        saveBtn = saveBtnTooltip.find(NxButton);

      expect(saveBtnTooltip).toHaveTagName('NxTooltip');
      expect(saveBtnTooltip).toHaveProp('title', 'Please apply filter before saving');
      expect(saveBtn).toHaveClassName('disabled');
    });

    it('is enabled with no tooltip when filters are not dirty', function () {
      const shallowRender = getShallowComponent({ filtersAreDirty: false }),
        saveBtnTooltip = shallowRender.find('#dashboard-filter-save-tooltip'),
        saveBtn = saveBtnTooltip.find(NxButton);

      expect(saveBtnTooltip).toHaveTagName('NxTooltip');
      expect(saveBtnTooltip).toHaveProp('title', '');
      expect(saveBtn).not.toHaveClassName('disabled');
    });
  });
});
