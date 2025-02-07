/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import DashboardHeader from '../../../../main/frontend/dashboard/results/DashboardHeader';
import DashboardFilter from '../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilter';
import ExportButton from '../../../../main/frontend/dashboard/results/dashboardSummary/ExportButton';
import { DEFAULT_FILTER_NAME } from '../../../../main/frontend/dashboard/filter/defaultFilter';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import { NxInfoAlert } from '@sonatype/react-shared-components';

describe('DashboardHeader', () => {
  let getShallowComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      filterSidebarOpen: true,
      isDashboardEnabled: true,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(DashboardHeader, minimalProps);
  });

  describe('on render', () => {
    describe('filter', () => {
      it('shows filter if filterSidebarOpen is true', () => {
        const component = getShallowComponent();
        const filter = component.find(DashboardFilter);

        expect(filter).toExist();
      });

      it('shows filter if filterSidebarOpen is false', () => {
        const component = getShallowComponent({ filterSidebarOpen: false });
        const filter = component.find(DashboardFilter);

        expect(filter.length).toBe(0);
      });

      it('triggers toggleFilterSidebar on filter button click', () => {
        const toggleSpy = jasmine.createSpy('toggleFilterSidebar');
        const component = getShallowComponent({
          toggleFilterSidebar: toggleSpy,
          filterSidebarOpen: false,
        });

        const button = component.find('#filter-toggle');

        button.simulate('click');

        expect(toggleSpy).toHaveBeenCalledWith(true);
      });

      it('shows default filter name if nothing were applied', () => {
        const component = getShallowComponent({
          filterSidebarOpen: true,
          appliedFilterName: null,
          showDirtyAsterisk: false,
        });

        const button = component.find('#filter-toggle');
        expect(button.text()).toBe(`Filter: ${DEFAULT_FILTER_NAME}`);
      });
    });

    describe('export button', () => {
      it('is rendered with default title', () => {
        const exportDataSpy = jasmine.createSpy('exportRequestData');
        const component = getShallowComponent({
          exportRequestData: exportDataSpy,
          exportTitle: 'components',
          exportUrl: 'https://export.data',
        });
        const exportButton = component.find(ExportButton);

        expect(exportButton).toHaveProp('exportRequestData', exportDataSpy);
        expect(exportButton).toHaveProp('exportTitle', 'components');
        expect(exportButton).toHaveProp('exportUrl', 'https://export.data');
      });
    });
  });

  describe('on Dashboard enabled', () => {
    it('does not show info alert', () => {
      const component = getShallowComponent({ isDashboardEnabled: true });
      const infoAlert = component.find(NxInfoAlert);
      const loadWrapper = component.find(LoadWrapper);

      expect(infoAlert).not.toExist();
      expect(loadWrapper).toExist();
    });
  });

  describe('on Dashboard disabled', () => {
    it('shows info alert', () => {
      const component = getShallowComponent({ isDashboardEnabled: false });
      const infoAlert = component.find(NxInfoAlert);
      const loadWrapper = component.find(LoadWrapper);

      expect(loadWrapper).not.toExist();
      expect(infoAlert).toExist();
      expect(infoAlert.text()).toBe('The Dashboard feature has been disabled by your administrator.');
    });
  });
});
