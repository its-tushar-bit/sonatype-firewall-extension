/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import SuccessMetricsReportList from '../../../../main/frontend/labs/successMetrics/SuccessMetricsReportList';
import SuccessMetricsReportListItem from '../../../../main/frontend/labs/successMetrics/SuccessMetricsReportListItem';
import * as enzymeUtils from '../../enzymeUtils';
import { NxButton } from '@sonatype/react-shared-components';

describe('successMetricsReportList', () => {
  let getShallow, initialProps;

  beforeEach(() => {
    initialProps = {
      loading: true,
      loadError: null,
      reports: [],
      isAddModalOpen: false,
    };

    getShallow = enzymeUtils.getShallowComponent(SuccessMetricsReportList, initialProps);
  });

  describe('when do not have any error', () => {
    describe('and does not have any report', () => {
      it('shows no reports message', () => {
        const component = getShallow();
        const element = component.find('.nx-list__item--empty');

        expect(component.find(SuccessMetricsReportListItem)).not.toExist();
        expect(element).toHaveText('No reports have been created.');
      });
    });
    describe('and have some reports', () => {
      let reports;
      beforeEach(() => {
        reports = [
          { id: '101', name: 'test 101' },
          { id: '202', name: 'test 202' },
          { id: '303', name: 'test 303' },
        ];
      });
      it('renders a list of reports', () => {
        const component = getShallow({ reports });

        expect(component.find(SuccessMetricsReportListItem).length).toBe(3);
        expect(component.find('.nx-list__item--empty')).not.toExist();
      });
    });
  });
  describe('when have an error', () => {
    it('pass error message to LoadWrapper', () => {
      const errorMsg = 'error message';
      const component = getShallow({ loadError: errorMsg });
      const loadWrapper = component.find(LoadWrapper);

      expect(loadWrapper).toHaveProp('error', errorMsg);
    });
  });
  describe('when click in add button', () => {
    it('calls toggleAddModal', () => {
      const toggleAddModalSpy = jasmine.createSpy('toggleAddModalSpy');
      const component = getShallow({ toggleAddModal: toggleAddModalSpy });
      const button = component.find(NxButton);
      button.simulate('click');

      expect(toggleAddModalSpy).toHaveBeenCalled();
    });
  });
});
