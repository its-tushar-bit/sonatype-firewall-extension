/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { STATE_GO } from 'MainRoot/reduxUiRouter/routerActions';
import { returnToAddOrRequestWaiverOriginPage } from 'MainRoot/waivers/waiverActions';

describe('waiverActions', () => {
  describe('returnToAddOrRequestWaiverOriginPage', () => {
    it('returns Nexus One violation detail origins to the native child state with preserved params', () => {
      const dispatch = jest.fn();
      const getState = () => ({
        router: {
          prevState: { name: 'nexusOneViolationDetail.waivers' },
          prevParams: {
            id: 'previous-id',
            type: 'security',
            sidebarReference: 'violations-list',
            sidebarId: 'sidebar-1',
            page: '2',
          },
          currentParams: {
            violationId: 'violation-123',
          },
        },
      });

      returnToAddOrRequestWaiverOriginPage()(dispatch, getState);

      expect(dispatch).toHaveBeenCalledWith({
        type: STATE_GO,
        payload: {
          to: 'nexusOneViolationDetail.waivers',
          params: {
            id: 'violation-123',
            type: 'security',
            sidebarReference: 'violations-list',
            sidebarId: 'sidebar-1',
            page: '2',
          },
          options: undefined,
        },
      });
    });

    it('normalizes the abstract Nexus One violation detail parent to overview', () => {
      const dispatch = jest.fn();
      const getState = () => ({
        router: {
          prevState: { name: 'nexusOneViolationDetail' },
          prevParams: {
            id: 'previous-id',
            type: 'security',
          },
          currentParams: {
            violationId: 'violation-123',
          },
        },
      });

      returnToAddOrRequestWaiverOriginPage()(dispatch, getState);

      expect(dispatch).toHaveBeenCalledWith({
        type: STATE_GO,
        payload: {
          to: 'nexusOneViolationDetail.overview',
          params: {
            id: 'violation-123',
            type: 'security',
          },
          options: undefined,
        },
      });
    });

    it('does not navigate when neither current nor previous violation id is available', () => {
      const dispatch = jest.fn();
      const getState = () => ({
        router: {
          prevState: { name: 'nexusOneViolationDetail.overview' },
          prevParams: {
            type: 'security',
          },
          currentParams: {},
        },
      });

      returnToAddOrRequestWaiverOriginPage()(dispatch, getState);

      expect(dispatch).not.toHaveBeenCalled();
    });
  });
});
