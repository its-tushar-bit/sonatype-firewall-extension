/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTable, NxTableHead, NxTableBody, NxTableRow, NxTableCell } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

import ApplicationReportRawDataTable from '../../../../main/frontend/applicationReport/rawData/ApplicationReportRawDataTable';
import RawLicenseTooltip from '../../../../main/frontend/applicationReport/rawData/RawLicenseDisplay';

describe('ApplicationReportRawDataTable', () => {
  let getShallowComponent, minimalProps;
  beforeEach(() => {
    minimalProps = {
      displayedEntries: [],
      rawSortConfiguration: {
        key: 'derivedComponentName',
        sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
        dir: 'asc',
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportRawDataTable, minimalProps);
  });

  describe('on render', () => {
    it('sets the emptyMessage prop on the table body', () => {
      const body = getShallowComponent().find(NxTableBody);
      expect(body).toHaveProp('emptyMessage', 'No Data Available');
    });

    describe('with results', () => {
      let openModalSpy;
      beforeEach(() => {
        openModalSpy = jasmine.createSpy('openModal');
        const displayedEntries = [
          {
            key: 'null1',
            derivedComponentName: 'C-3PO',
            securityCode: 'we are doomed',
            cvssScore: '9',
            license: {
              declaredLicenses: ['Public Domain'],
              observedLicenses: ['Apache-1.1', 'Apache-2.0'],
            },
          },
          {
            key: 'null2',
            derivedComponentName: 'R2-D2',
            securityCode: '',
            cvssScore: '2.3',
            license: {
              declaredLicenses: [],
              observedLicenses: ['Apache-1.1'],
            },
          },
        ];

        minimalProps = {
          ...minimalProps,
          displayedEntries,
          openModal: openModalSpy,
        };

        getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportRawDataTable, minimalProps);
      });

      describe('loading', () => {
        it('shows loading indicator', () => {
          const component = getShallowComponent({ pendingLoadsSize: 2 });
          const body = component.find(NxTableBody);

          expect(body).toHaveProp('isLoading', true);
        });
        it('does not show loading indicator', () => {
          const component = getShallowComponent({ pendingLoadsSize: 0 });
          const body = component.find(NxTableBody);

          expect(body).toHaveProp('isLoading', false);
        });
      });

      it('shows table with results', () => {
        const component = getShallowComponent();
        const rows = component.find(NxTableBody).children();

        expect(rows.length).toBe(2);
      });

      it('sets key from data to each row', () => {
        const component = getShallowComponent();
        const rows = component.find(NxTableBody).find(NxTableRow);

        expect(rows.at(0).key()).toBe('null1');
        expect(rows.at(1).key()).toBe('null2');
      });

      it('shows tooltip', () => {
        const component = getShallowComponent();
        const licenseCell = component.find(NxTableBody).find(NxTableRow).children().at(1);
        const tooltip = licenseCell.find(RawLicenseTooltip);

        expect(tooltip).toExist();
      });

      it('shows modal on security code click', () => {
        const component = getShallowComponent();
        const securityCell = component.find(NxTableBody).find(NxTableRow).children().at(2);

        securityCell.children().simulate('click');

        expect(openModalSpy).toHaveBeenCalled();
      });

      describe('filter', () => {
        it('triggers filter value change', () => {
          const setRawDataStringFieldFilterSpy = jasmine.createSpy('setRawDataStringFieldFilter');
          const component = getShallowComponent({ setRawDataStringFieldFilter: setRawDataStringFieldFilterSpy });
          const componenNameFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(0);
          const input = componenNameFilterCell.children();

          input.simulate('change', { value: 'c' });

          expect(setRawDataStringFieldFilterSpy).toHaveBeenCalledWith('derivedComponentName', { value: 'c' });
        });

        it('applies value to component name filter', () => {
          const component = getShallowComponent({ derivedComponentNameSubstringFilter: 'c' });
          const componenNameFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(0);
          const input = componenNameFilterCell.children();

          expect(input).toHaveProp('value', 'c');
        });

        describe('cvss score', () => {
          it('sets provided cvss score filter values as filter value', () => {
            const component = getShallowComponent({
              cvssScore: ['3', '7'],
            });
            const cvssFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(3);
            const min = cvssFilterCell.children().at(0);
            const max = cvssFilterCell.children().at(1);

            expect(min).toHaveProp('value', '3');
            expect(max).toHaveProp('value', '7');
          });

          it('sets cvss score filter inputs with empty values if cvss score is not provided', () => {
            const component = getShallowComponent({
              cvssScore: null,
            });
            const cvssFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(3);
            const min = cvssFilterCell.children().at(0);
            const max = cvssFilterCell.children().at(1);

            expect(min).toHaveProp('value', '');
            expect(max).toHaveProp('value', '');
          });

          it('calls min numeric filter if provided value is valid', () => {
            const setRawDataNumericMinFilterSpy = jasmine.createSpy('setRawDataNumericMinFilter');
            const component = getShallowComponent({
              setRawDataNumericMinFilter: setRawDataNumericMinFilterSpy,
            });
            const cvssFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(3);

            const input = cvssFilterCell.children().first();
            input.simulate('change', '5');

            expect(setRawDataNumericMinFilterSpy).toHaveBeenCalledWith('cvssScore', '5');
          });

          it('calls max numeric filter if provided value is valid', () => {
            const setRawDataNumericMaxFilterSpy = jasmine.createSpy('setRawDataNumericMaxFilter');
            const component = getShallowComponent({
              setRawDataNumericMaxFilter: setRawDataNumericMaxFilterSpy,
            });
            const cvssFilterCell = component.find(NxTableHead).children().at(1).find(NxTableCell).at(3);

            const input = cvssFilterCell.children().at(1);
            input.simulate('change', '9');

            expect(setRawDataNumericMaxFilterSpy).toHaveBeenCalledWith('cvssScore', '9');
          });
        });
      });

      describe('sorting', () => {
        function getHeaders(props) {
          const table = getShallowComponent(props).find(NxTable);
          const sortingRow = table.find(NxTableHead).find(NxTableRow).first();
          return sortingRow.find(NxTableCell);
        }

        it('renders an NxTable with headers', () => {
          const headers = getHeaders();

          expect(headers.length).toEqual(4);
          expect(headers.at(0)).toHaveProp('children', 'Component');
          expect(headers.at(1)).toHaveProp('children', 'License');
          expect(headers.at(2)).toHaveProp('children', 'Security Issue');
          expect(headers.at(3)).toHaveProp('children', 'CVSS Score');
        });

        it('default sorting is enabled on component name column', () => {
          const headers = getHeaders({
            rawSortConfiguration: {
              key: 'derivedComponentName',
              sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
              dir: 'asc',
            },
          });

          expect(headers.at(0)).toHaveProp('sortDir', 'asc');
          expect(headers.at(1)).toHaveProp('sortDir', null);
          expect(headers.at(2)).toHaveProp('sortDir', null);
          expect(headers.at(3)).toHaveProp('sortDir', null);
        });

        it('triggers sort change', () => {
          const setRawSortingParametersSpy = jasmine.createSpy('setRawSortingParameters');

          const component = getShallowComponent({
            setRawSortingParameters: setRawSortingParametersSpy,
          });

          const componentNameCell = component.find(NxTableHead).children().at(0).children().first();
          expect(componentNameCell).toHaveProp('isSortable');

          componentNameCell.simulate('click');

          expect(setRawSortingParametersSpy).toHaveBeenCalledWith(
            'derivedComponentName',
            ['-derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
            'desc'
          );
        });

        it('renders sorting based on given rawSortConfiguration', () => {
          let headers = getHeaders({
            rawSortConfiguration: { dir: 'desc', key: 'derivedComponentName' },
          });

          expect(headers.at(0)).toHaveProp('sortDir', 'desc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'asc', key: 'derivedComponentName' },
          });

          expect(headers.at(0)).toHaveProp('sortDir', 'asc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'desc', key: 'licenseSortKey' },
          });

          expect(headers.at(1)).toHaveProp('sortDir', 'desc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'asc', key: 'licenseSortKey' },
          });

          expect(headers.at(1)).toHaveProp('sortDir', 'asc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'desc', key: 'securityCode' },
          });

          expect(headers.at(2)).toHaveProp('sortDir', 'desc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'asc', key: 'securityCode' },
          });

          expect(headers.at(2)).toHaveProp('sortDir', 'asc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'desc', key: 'cvssScore' },
          });

          expect(headers.at(3)).toHaveProp('sortDir', 'desc');

          headers = getHeaders({
            rawSortConfiguration: { dir: 'asc', key: 'cvssScore' },
          });

          expect(headers.at(3)).toHaveProp('sortDir', 'asc');
        });
      });
    });

    describe('load error', () => {
      it('sets the error prop on the table body', () => {
        const component = getShallowComponent({ loadError: 'load error' });
        const body = component.find(NxTableBody);

        expect(body).toHaveProp('error', 'load error');
      });

      it('sets retryHandler prop on the table body', () => {
        const loadReportRawDataSpy = jasmine.createSpy('loadReportRawData');
        const component = getShallowComponent({
          loadReportRawData: loadReportRawDataSpy,
          loadError: 'load error',
        });
        const body = component.find(NxTableBody);

        expect(body).toHaveProp('retryHandler', loadReportRawDataSpy);
      });
    });
  });
});
