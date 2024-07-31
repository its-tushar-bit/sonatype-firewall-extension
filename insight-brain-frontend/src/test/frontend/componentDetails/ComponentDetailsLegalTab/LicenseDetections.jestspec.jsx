/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import React from 'react';
import LicenseDetections from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetections';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('LicenseDetections', () => {
  let renderComponent,
    minimalProps,
    loadLicensesMock,
    reviewObligationsClickHandlerSpy,
    toggleShowEditLicensesPopoverSpy,
    stateGoSpy;

  beforeEach(() => {
    loadLicensesMock = jest.fn();

    reviewObligationsClickHandlerSpy = jest.fn('reviewObligationsClickHandler').mockReturnValue({});
    toggleShowEditLicensesPopoverSpy = jest.fn('toggleShowEditLicensesPopover').mockReturnValue({});
    stateGoSpy = jest.fn('stateGo');

    minimalProps = {
      declaredLicenses: null,
      effectiveLicenses: null,
      observedLicenses: null,
      loadLicenses: loadLicensesMock,
      loading: false,
      loadError: null,
      toggleShowEditLicensesPopover: toggleShowEditLicensesPopoverSpy,
      identificationSource: 'Sonatype',
      isAdvancedLegalPackSupported: false,
      stateGo: stateGoSpy,
      reviewObligationsClickHandler: reviewObligationsClickHandlerSpy,
    };

    renderComponent = (additionalProps = {}) => {
      render(<LicenseDetections {...minimalProps} {...additionalProps} />);
    };
  });

  it('calls loadLicensesMock on mount', function () {
    renderComponent();
    expect(loadLicensesMock).toHaveBeenCalledTimes(1);
  });

  it('renders external external link', function () {
    jest.spyOn(routerSelectors, 'selectIsPrioritiesPageContainer').mockReturnValue(true);

    renderComponent({ isAdvancedLegalPackSupported: true, getReviewObligationsHref: () => 'someUrl' });

    const link = screen.getByRole('link', { name: 'Review Obligations' });

    expect(link).toBeVisible();
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('href', 'someUrl');
  });

  it('Hides the content on load', () => {
    renderComponent({ loading: true });

    expect(screen.getByText('Loading…')).toBeInTheDocument();
    expect(screen.queryByText('License Detections')).not.toBeInTheDocument();
  });

  it('renders an NxButton with label `Edit`', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument();
  });

  it('renders an NxButton with label `Review Obligations` if ALP feature is enabled', () => {
    renderComponent({ isAdvancedLegalPackSupported: true });
    expect(screen.getByRole('button', { name: 'Review Obligations' })).toBeInTheDocument();
  });

  it('will not render an NxButton with label `Review Obligations` if ALP feature is not enabled', () => {
    renderComponent({ isAdvancedLegalPackSupported: false });
    expect(screen.queryByRole('button', { name: 'Review Obligations' })).not.toBeInTheDocument();
  });

  it('will navigate to ALP after clicking `Review Obligations`', () => {
    const applicationReportMetadataProps = {
      applicationId: 'test',
      stageId: 'test',
      componentHash: 'abc',
      reviewObligationsClickHandler: reviewObligationsClickHandlerSpy,
    };
    renderComponent({
      isAdvancedLegalPackSupported: true,
      ...applicationReportMetadataProps,
    });

    const button = screen.getByRole('button', { name: 'Review Obligations' });
    expect(button).toBeInTheDocument();

    fireEvent.click(button);
    expect(reviewObligationsClickHandlerSpy).toHaveBeenCalledTimes(1);
  });

  it('calls `toggleShowEditLicensesPopoverSpy` when `Edit` button clicked', () => {
    renderComponent();
    const button = screen.getByRole('button', { name: 'Edit' });

    fireEvent.click(button);

    expect(toggleShowEditLicensesPopoverSpy).toHaveBeenCalledTimes(1);
  });

  describe('Shows the correct status', () => {
    it('no override', () => {
      renderComponent();

      // expect(status).toExist();
      expect(screen.getByTestId('status-subtitle')).toHaveTextContent('open');
    });

    it('app level', () => {
      renderComponent({
        licenseOverride: [
          {
            ownerId: 'wencelapp2.0',
            ownerName: 'wencel app 2.0',
            ownerType: 'application',
            licenseOverride: {
              status: 'OPEN',
            },
          },
          {
            ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
            ownerName: 'wencel org',
            ownerType: 'organization',
            licenseOverride: null,
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            licenseOverride: null,
          },
        ],
      });

      expect(screen.getByTestId('status-subtitle')).toHaveTextContent('open');
    });

    it('org level', () => {
      renderComponent({
        licenseOverride: [
          {
            ownerId: 'wencelapp2.0',
            ownerName: 'wencel app 2.0',
            ownerType: 'application',
            licenseOverride: null,
          },
          {
            ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
            ownerName: 'wencel org',
            ownerType: 'organization',
            licenseOverride: {
              status: 'OVERRIDDEN',
            },
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            licenseOverride: null,
          },
        ],
      });

      expect(screen.getByTestId('status-subtitle')).toHaveTextContent('overridden');
    });

    it('root level', () => {
      renderComponent({
        licenseOverride: [
          {
            ownerId: 'wencelapp2.0',
            ownerName: 'wencel app 2.0',
            ownerType: 'application',
            licenseOverride: null,
          },
          {
            ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
            ownerName: 'wencel org',
            ownerType: 'organization',
            licenseOverride: null,
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            licenseOverride: {
              status: 'SELECTED',
            },
          },
        ],
      });

      expect(screen.getByTestId('status-subtitle')).toHaveTextContent('selected');
    });
  });

  describe('renders the licenses sections', () => {
    it('Effective Licenses section', () => {
      renderComponent({
        effectiveLicenses: [
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id1',
                  licenseName: 'ELicense 1',
                },
                threatLevel: 2,
              },
            ],
          },
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id2',
                  licenseName: 'ELicense 2',
                },
                threatLevel: 7,
              },
            ],
          },
        ],
        declaredLicenses: [
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id3',
                  licenseName: 'DLicense 1',
                },
                threatLevel: 5,
              },
            ],
          },
        ],
        observedLicenses: [
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id4',
                  licenseName: 'OLicense 1',
                },
                threatLevel: null,
              },
            ],
          },
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id5',
                  licenseName: 'OLicense 2',
                },
                threatLevel: 0,
              },
            ],
          },
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id6',
                  licenseName: 'OLicense 3',
                },
                threatLevel: 6,
              },
            ],
          },
          {
            licenses: [
              {
                license: {
                  licenseId: 'Id1',
                  licenseName: 'ELicense 1',
                },
                threatLevel: 2,
              },
              {
                license: {
                  licenseId: 'Id6',
                  licenseName: 'OLicense 3',
                },
                threatLevel: 6,
              },
            ],
          },
        ],
      });
      const effectiveLicensesContainer = screen.getByTestId('effective-licenses-container');
      const effectiveLicenses = within(effectiveLicensesContainer).getAllByTestId('single-license-list-item__license');

      const declaredLicensesContainer = screen.getByTestId('declared-licenses-container');
      const declaredLicenses = within(declaredLicensesContainer).getAllByTestId('single-license-list-item__license');

      const observedLicensesContainer = screen.getByTestId('observed-licenses-container');
      const observedLicenses = within(observedLicensesContainer).getAllByTestId('single-license-list-item__license');

      const observedMultiLicensesContainer = screen.getByTestId('observed-licenses-container');
      const observedMultiLicenses = within(observedMultiLicensesContainer).getAllByTestId(
        'multi-license-list-item__license'
      );

      //Effective Licenses
      expect(effectiveLicenses.length).toBe(2);

      expect(within(effectiveLicenses[0]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level moderate'
      );
      expect(effectiveLicenses[0]).toHaveTextContent('ELicense 1');

      expect(within(effectiveLicenses[1]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level severe'
      );
      expect(effectiveLicenses[1]).toHaveTextContent('ELicense 2');

      //Declared Licenses
      expect(declaredLicenses.length).toBe(1);

      expect(within(declaredLicenses[0]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level severe'
      );
      expect(declaredLicenses[0]).toHaveTextContent('DLicense 1');

      //Observed Licenses
      expect(observedLicenses.length).toBe(3);

      expect(within(observedLicenses[0]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level unspecified'
      );
      expect(observedLicenses[0]).toHaveTextContent('OLicense 1');

      expect(within(observedLicenses[1]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level none'
      );
      expect(observedLicenses[1]).toHaveTextContent('OLicense 2');

      expect(within(observedLicenses[2]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level severe'
      );
      expect(observedLicenses[2]).toHaveTextContent('OLicense 3');

      //Observed Multi Licenses
      expect(observedMultiLicenses.length).toBe(2);

      expect(within(observedMultiLicenses[0]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level moderate'
      );
      expect(observedMultiLicenses[0]).toHaveTextContent('ELicense 1');

      expect(within(observedMultiLicenses[1]).getByRole('graphics-symbol', { hidden: true })).toHaveAttribute(
        'aria-label',
        'threat level severe'
      );
      expect(observedMultiLicenses[1]).toHaveTextContent('OLicense 3');
    });

    it('renders licences for claimed component', () => {
      const unspecifiedLicense = [
        {
          licenses: [
            {
              license: {
                licenseId: 'UNSPECIFIED',
                licenseName: 'Not Provided',
              },
              threatLevel: 5,
            },
          ],
        },
      ];

      renderComponent({
        effectiveLicenses: unspecifiedLicense,
        declaredLicenses: unspecifiedLicense,
        observedLicenses: unspecifiedLicense,
        identificationSource: 'Manual',
      });

      const effectiveLicensesContainer = screen.getByTestId('effective-licenses-container');
      const declaredLicensesContainer = screen.getByTestId('declared-licenses-container');
      const observedLicensesContainer = screen.getByTestId('observed-licenses-container');

      const effectiveLicenses = within(effectiveLicensesContainer).getAllByTestId('iq-legal-item');
      const declaredLicenses = within(declaredLicensesContainer).getAllByTestId('iq-legal-item');
      const observedLicenses = within(observedLicensesContainer).getAllByTestId('iq-legal-item');

      expect(effectiveLicenses.length).toBe(1);
      expect(effectiveLicenses[0]).toHaveTextContent('Not Provided');

      expect(declaredLicenses.length).toBe(1);
      expect(declaredLicenses[0]).toHaveTextContent('Not Provided');
      expect(declaredLicenses[0]).toHaveTextContent(' (Claimed Component)');

      expect(observedLicenses.length).toBe(1);
      expect(observedLicenses[0]).toHaveTextContent('Not Provided');
      expect(observedLicenses[0]).toHaveTextContent(' (Claimed Component)');
    });

    describe('Observed Licenses section with ALP detection', () => {
      it('renders the Get ALP detections alert if Observed Licenses are hidden', () => {
        renderComponent({ hiddenObservedLicenses: true });
        const linkToLearnMore = screen.getByRole('link', { name: /Learn More/i });
        expect(linkToLearnMore).toBeVisible();
        expect(linkToLearnMore).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/alp/extended-observed-license-detections'
        );
      });

      it('renders the Get ALP alert if ALP is not enabled and format supports ALP observed license detection', () => {
        renderComponent({
          isAdvancedLegalPackSupported: false,
          supportAlpObservedLicenses: true,
        });
        const alert = screen.getByRole('link', { name: /Learn More/i });
        expect(alert).toBeVisible();
        expect(alert).toHaveAttribute('href', 'https://links.sonatype.com/products/nxiq/doc/add-on-packs/alp');
      });
    });
  });
});
