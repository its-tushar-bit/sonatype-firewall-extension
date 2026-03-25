/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render } from 'TestRoot/SpecUtil';
import React from 'react';
import SbomVulnerabilityDetails from 'MainRoot/sbomManager/features/componentDetails/vulnerabilitiesDrawer/SbomVulnerabilityDetails';
import { cleanup, getByText, queryByText } from '@testing-library/react';

describe('SbomVulnerabilityDetails', function () {
  let renderComponent;
  const vulnerabilityDetailsJson = {
    identifier: 'CVE-2014-0114',
    vulnerabilityLink: 'http://foohost/',
    source: {
      longName: 'National Vulnerability Database',
      shortName: 'CVE',
    },
    categories: ['configuration', 'privileged'],
  };
  const componentName = 'pkg:a/b/c';

  const assertLinkProperties = (linkNode, text, href, target, rel) => {
    expect(getByText(linkNode, text)).toBeInTheDocument();
    expect(linkNode.getAttribute('href')).toBe(href);
    expect(linkNode.getAttribute('target')).toBe(target);
    expect(linkNode.getAttribute('rel')).toBe(rel);
  };

  const readOnlyByIndex = (container, idx) => {
    return container.querySelectorAll('.nx-read-only')[idx];
  };

  beforeEach(function () {
    renderComponent = (additionalPreloadedState) => {
      cleanup();
      return render(
        <SbomVulnerabilityDetails
          vulnerabilityDetails={{ ...vulnerabilityDetailsJson, ...additionalPreloadedState }}
          componentName={componentName}
        />
      );
    };
  });

  it('renders a div.sbom-vulnerability-details', function () {
    const component = renderComponent();
    const container = component.container;
    expect(container.querySelector('div.sbom-vulnerability-details')).toBeInTheDocument();
  });

  it('renders an H2 header with the identifier', function () {
    const component = renderComponent();
    const container = component.container;
    const refIdHeader = container.querySelector('h2.nx-h2 .sbom-vulnerability-details__vulnerability-id');
    expect(getByText(refIdHeader, 'CVE-2014-0114')).toBeInTheDocument();
  });

  //////////// LEFT COLUMN //////////////

  describe('left side column', function () {
    function leftSide(component) {
      return component.container.querySelectorAll('.nx-grid-col')[0];
    }

    describe('first nx-read-only (Issue)', function () {
      let leftSideColumn;
      beforeEach(function () {
        leftSideColumn = leftSide(renderComponent());
      });

      afterEach(() => cleanup());

      it('is labelled "Issue"', function () {
        let readOnlyNode = readOnlyByIndex(leftSideColumn, 0);
        expect(getByText(readOnlyNode, 'Issue')).toBeInTheDocument();
      });

      it('contains a link with the identifier as its text and the vulnerabilityLink as its href', function () {
        let readOnlyNode = readOnlyByIndex(leftSideColumn, 0);

        const link = readOnlyNode.querySelector('.nx-read-only__data a');

        expect(link.getAttribute('class')).toContain('nx-text-link');
        expect(link.getAttribute('target')).toBe('_blank');
        expect(link.getAttribute('rel')).toBe('noreferrer');
        expect(link.getAttribute('href')).toBe('http://foohost/');
        expect(getByText(link, 'CVE-2014-0114')).toBeInTheDocument();
      });

      it('contains a multiple issues(main and aliases)', function () {
        const leftSideColumn = leftSide(renderComponent({ vulnIds: ['TEST-123', 'TEST-456'] }));
        const readOnlyNodeWithMultipleIssues = readOnlyByIndex(leftSideColumn, 0);
        const dataNodes = readOnlyNodeWithMultipleIssues.querySelectorAll('.nx-read-only__data');
        expect(getByText(dataNodes[1], 'TEST-123')).toBeInTheDocument();
        expect(getByText(dataNodes[2], 'TEST-456')).toBeInTheDocument();
      });

      it('renders main issue without a link with vulnIds', function () {
        const leftSideColumn = leftSide(
          renderComponent({ vulnIds: ['TEST-123', 'TEST-456'], vulnerabilityLink: null })
        );
        const readOnlyNodeWithMultipleIssues = readOnlyByIndex(leftSideColumn, 0);
        const link = readOnlyNodeWithMultipleIssues.querySelector('.nx-read-only__data a');
        expect(link).not.toBeInTheDocument();

        const dataNodes = readOnlyNodeWithMultipleIssues.querySelectorAll('.nx-read-only__data');
        expect(getByText(dataNodes[0], 'CVE-2014-0114')).toBeInTheDocument();
        expect(getByText(dataNodes[1], 'TEST-123')).toBeInTheDocument();
        expect(getByText(dataNodes[2], 'TEST-456')).toBeInTheDocument();
      });
    });

    describe('second nx-read-only (Severity)', function () {
      const testSeverityObject = (extraProperties) => {
        return {
          mainSeverity: { source: 'cve_cvss_3', score: 9.7 },
          ...extraProperties,
        };
      };

      const leftFragmentWithSeverity = (severityObject) => leftSide(renderComponent(severityObject));

      const getSeverityFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 1);

      const assertIfSeverityPresentInDocument = (severityFragment, source, score) => {
        expect(getByText(severityFragment, source)).toBeInTheDocument();
        expect(
          getByText(severityFragment.querySelector('.iq-vulnerability-details__sub-description-desc'), score)
        ).toBeInTheDocument();
      };

      it('is an empty render if mainSeverity is undefined and severityScores is undefined or empty', function () {
        const leftSideColumnNoMainSeverity = leftSide(renderComponent({ mainSeverity: null }));
        expect(queryByText(leftSideColumnNoMainSeverity, 'Severity')).not.toBeInTheDocument();

        const leftSideColumnNoMainSeverityOrScores = leftSide(
          renderComponent({ mainSeverity: null, severityScores: [] })
        );
        expect(queryByText(leftSideColumnNoMainSeverityOrScores, 'Severity')).not.toBeInTheDocument();

        const noSeverityInfo = leftSide(renderComponent());
        expect(queryByText(noSeverityInfo, 'Severity')).not.toBeInTheDocument();
      });

      it('is labelled "Severity"', function () {
        const leftSideColumnWithSeverityAndScores = leftSide(
          renderComponent({ mainSeverity: undefined, severityScores: [{ source: 'cve_cvss_3', score: 9.7 }] })
        );
        expect(queryByText(leftSideColumnWithSeverityAndScores, 'Severity')).toBeInTheDocument();
        expect(queryByText(leftSideColumnWithSeverityAndScores, 'CVE CVSS 3')).toBeInTheDocument();
      });

      it('contains an iq-vulnerability-details__sub-description-container for each severity, with the main one first', function () {
        const additionalSeverityScores = [
          { source: 'cve_cvss_4', sourceLabel: 'CVE CVSS 4', score: 9.7 },
          { source: 'cve_cvss_31', sourceLabel: 'CVE CVSS 3.1', score: 9.0 },
          { source: 'cve_cvss_2', sourceLabel: 'CVE CVSS 2.0', score: 1 },
          { source: 'sonatype_cvss_4', sourceLabel: 'Sonatype CVSS 4', score: 9.6 },
          { source: 'sonatype_cvss_31', sourceLabel: 'Sonatype CVSS 3.1', score: 5.0 },
          { source: 'sonatype_cvss_3', sourceLabel: 'Sonatype CVSS 3', score: 5.5 },
          { source: 'sonatype_cvss_2', sourceLabel: 'Sonatype CVSS 2.0', score: 8.0 },
        ];

        const leftSideFragment = leftFragmentWithSeverity(
          testSeverityObject({ severityScores: additionalSeverityScores })
        );
        const severityFragment = getSeverityFragment(leftSideFragment);

        const severitiesList = severityFragment.querySelectorAll(
          '.iq-vulnerability-details__sub-description-container'
        );
        expect(severitiesList.length).toEqual(8);
        assertIfSeverityPresentInDocument(severitiesList[0], 'CVE CVSS 3', '9.7');
        assertIfSeverityPresentInDocument(severitiesList[1], 'CVE CVSS 4', '9.7');
        assertIfSeverityPresentInDocument(severitiesList[2], 'CVE CVSS 3.1', '9.0');
        assertIfSeverityPresentInDocument(severitiesList[3], 'CVE CVSS 2.0', '1.0');
        assertIfSeverityPresentInDocument(severitiesList[4], 'Sonatype CVSS 4', '9.6');
        assertIfSeverityPresentInDocument(severitiesList[5], 'Sonatype CVSS 3.1', '5.0');
        assertIfSeverityPresentInDocument(severitiesList[6], 'Sonatype CVSS 3', '5.5');
        assertIfSeverityPresentInDocument(severitiesList[7], 'Sonatype CVSS 2.0', '8.0');
      });

      it('renders an .iq-vulnerability-details__sub-description-term--unknown when there is no source', function () {
        const leftSideColumnWithSeverityAndScores = leftSide(renderComponent({ mainSeverity: { score: 2 } }));
        assertIfSeverityPresentInDocument(leftSideColumnWithSeverityAndScores, 'Unknown', '2.0');
      });

      it('renders an .iq-vulnerability-details__sub-description-term--unknown when source is empty', function () {
        const leftSideColumnWithSeverityAndScores = leftSide(
          renderComponent({ mainSeverity: { score: 2, source: '' } })
        );
        assertIfSeverityPresentInDocument(leftSideColumnWithSeverityAndScores, 'Unknown', '2.0');
      });
    });

    describe('third nx-read-only (KEV)', function () {
      const leftFragmentWithKev = (kevObject) => leftSide(renderComponent(kevObject));

      const getKevFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 1);

      const getKevFragmentWithObject = (kevData) => getKevFragment(leftFragmentWithKev({ kevData }));

      it('is an empty render if kevData is undefined or empty', function () {
        expect(queryByText(leftFragmentWithKev({ kevData: null }), 'Known to be Exploited')).not.toBeInTheDocument();

        expect(queryByText(leftFragmentWithKev({ kevData: {} }), 'Known to be Exploited')).not.toBeInTheDocument();
      });

      it('is labelled "Known to be Exploited"', function () {
        const kevData = {
          isKev: true,
        };
        expect(queryByText(getKevFragmentWithObject(kevData), 'Known to be Exploited')).toBeInTheDocument();
      });

      it('is labelled "Not Listed"', function () {
        const kevData = {
          isKev: false,
        };
        expect(queryByText(getKevFragmentWithObject(kevData), 'Not listed')).toBeInTheDocument();
      });
    });

    describe('fourth nx-read-only (EPSS Score)', function () {
      const leftFragmentWithEPSSScore = (sourceObject) => leftSide(renderComponent(sourceObject));

      const getEPSSScoreFragmentWithObject = (source) => leftFragmentWithEPSSScore(source);

      const getEPSSScoreFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 2);

      it('renders -- when EPSS Score is undefined', function () {
        expect(queryByText(getEPSSScoreFragment(leftFragmentWithEPSSScore()), 'EPSS Score')).toBeInTheDocument();
      });

      it('is labelled "EPSS Score" and displays value', function () {
        const sourceFragment = getEPSSScoreFragmentWithObject({ epssData: { currentScore: 0.0456 } });
        expect(getByText(sourceFragment, 'EPSS Score')).toBeInTheDocument();
        expect(getByText(sourceFragment, '4.56%')).toBeInTheDocument();
      });
    });

    describe('fifth nx-read-only (Weakness)', function () {
      const testWeaknessObject = (extraProperties) => {
        return {
          weakness: { cweSource: 'foo', cweIds: [{ id: '1', uri: 'http://cwe/' }] },
          ...extraProperties,
        };
      };

      const leftFragmentWithWeakness = (weaknessObject) => leftSide(renderComponent(weaknessObject));

      const getWeaknessFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 3);

      const getWeaknessFragmentWithObject = (weakness) => getWeaknessFragment(leftFragmentWithWeakness(weakness));

      it('is an empty render if weakness is undefined or weakness.cweIds is empty', function () {
        expect(queryByText(getWeaknessFragmentWithObject({ weakness: undefined }), 'Weakness')).not.toBeInTheDocument();
        expect(
          queryByText(getWeaknessFragmentWithObject({ weakness: { cweSource: 'foo', cweIds: [] } }), 'Weakness')
        ).not.toBeInTheDocument();
        expect(queryByText(getWeaknessFragmentWithObject({}), 'Weakness')).not.toBeInTheDocument();
      });

      it('is labelled "Weakness"', function () {
        const fragment = getWeaknessFragmentWithObject(testWeaknessObject());
        expect(getByText(fragment, 'Weakness')).toBeInTheDocument();
      });

      it('renders the cweSource and "CWE" as the term', function () {
        const fragment = getWeaknessFragmentWithObject(testWeaknessObject());
        expect(getByText(fragment, 'foo CWE')).toBeInTheDocument();
      });

      it('renders a desc for each CWE id linking to its href', function () {
        const fragment = getWeaknessFragmentWithObject({
          weakness: {
            cweSource: 'foo',
            cweIds: [
              { id: '1', uri: 'http://cwe/' },
              { id: '27', uri: 'http://cwe2/' },
            ],
          },
        });

        const descs = fragment.querySelectorAll('.iq-vulnerability-details__sub-description-desc');

        expect(descs.length).toBe(2);

        assertLinkProperties(descs[0].querySelector('a'), '1', 'http://cwe/', '_blank', 'noreferrer');
        assertLinkProperties(descs[1].querySelector('a'), '27', 'http://cwe2/', '_blank', 'noreferrer');
      });
    });

    describe('sixth nx-read-only (Source)', function () {
      const leftFragmentWithSource = (sourceObject) => leftSide(renderComponent(sourceObject));

      const getSourceFragmentWithObject = (source) => leftFragmentWithSource(source);

      const getSourceFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 3);

      it('is an empty render if source is undefined', function () {
        expect(queryByText(getSourceFragmentWithObject({ source: undefined }), 'Source')).not.toBeInTheDocument();
      });

      it('is labelled "Source and displays the source name"', function () {
        const sourceFragment = getSourceFragment(leftFragmentWithSource());
        expect(getByText(sourceFragment, 'Source')).toBeInTheDocument();
        expect(getByText(sourceFragment, 'National Vulnerability Database')).toBeInTheDocument();
      });
    });

    describe('seventh nx-read-only (Categories)', function () {
      const leftFragmentWithCategories = (categoriesObject) => leftSide(renderComponent(categoriesObject));

      const getCategoriesFragment = (leftSideFragment) => readOnlyByIndex(leftSideFragment, 4);

      const getCategoriesFragmentWithObject = (categories) =>
        getCategoriesFragment(leftFragmentWithCategories(categories));

      it('is an empty render if categories is undefined or empty', function () {
        expect(
          queryByText(leftFragmentWithCategories({ categories: undefined }), 'Categories')
        ).not.toBeInTheDocument();
      });

      it('is labelled "Categories"', function () {
        expect(queryByText(getCategoriesFragmentWithObject({}), 'Categories')).toBeInTheDocument();
      });

      it('renders the capitalized name of each category as a data item', function () {
        const categoryData = getCategoriesFragmentWithObject({}).querySelectorAll('.nx-read-only__data');

        expect(categoryData.length).toBe(2);
        expect(getByText(categoryData[0], 'Configuration')).toBeInTheDocument();
        expect(getByText(categoryData[1], 'Privileged')).toBeInTheDocument();
      });
    });
  });

  //////////////////// RIGHT COLUMN //////////////////////////

  describe('right side column', function () {
    let rightSideColumn;

    function rightSide(component) {
      return component.container.querySelectorAll('.nx-grid-col')[1];
    }

    beforeEach(function () {
      rightSideColumn = rightSide(renderComponent());
    });

    afterEach(() => cleanup());

    const rightFragmentWithExtraProperties = (extraProperties) => rightSide(renderComponent(extraProperties));

    const getDescriptionWithExtraDetails = (extraDetails) =>
      rightSide(
        renderComponent({
          description: "You're foobarred",
          ...extraDetails,
        })
      );

    describe('first nx-read-only (Description)', function () {
      it('is an empty render if description is undefined', function () {
        expect(
          queryByText(rightFragmentWithExtraProperties({ description: undefined }), 'Description')
        ).not.toBeInTheDocument();
        expect(queryByText(rightSideColumn, 'Description')).not.toBeInTheDocument();
      });

      it('is labeled "Description" if there is no source shortName', function () {
        expect(queryByText(getDescriptionWithExtraDetails({ source: undefined }), 'Description')).toBeInTheDocument();
      });

      it('includes the source shortName in the label if present', function () {
        expect(
          queryByText(getDescriptionWithExtraDetails({ source: { shortName: 'foo' } }), 'Description from foo')
        ).toBeInTheDocument();
      });

      it('sets the data to a paragraph with the description', function () {
        const render = getDescriptionWithExtraDetails({ description: 'bad things happen' });
        expect(queryByText(render, 'bad things happen')).toBeInTheDocument();
      });
    });

    describe('second nx-read-only (Explanation)', function () {
      const getExplanationWithExtraDetails = (extraDetails) => rightSide(renderComponent(extraDetails));

      it('is an empty render if explanationMarkdown and componentExplanationMarkdown are undefined', function () {
        expect(queryByText(getExplanationWithExtraDetails(), "You're foobarred")).not.toBeInTheDocument();
        expect(
          queryByText(
            getExplanationWithExtraDetails({ componentExplanationMarkdown: "You're foobarred" }),
            "You're foobarred"
          )
        ).toBeInTheDocument();

        expect(
          queryByText(
            getExplanationWithExtraDetails({
              explanationMarkdown: "You're foobarred",
              componentExplanationMarkdown: 'No, really',
            }),
            "You're foobarred"
          )
        ).toBeInTheDocument();
      });

      it('is labeled "Explanation"', function () {
        expect(
          queryByText(getExplanationWithExtraDetails({ explanationMarkdown: 'asdf' }), 'Explanation')
        ).toBeInTheDocument();
      });

      it(
        'sets the data to a concatenation of the markdown renders of the explanationMarkdown and ' +
          'componentExplanationMarkdown',
        function () {
          const render = getExplanationWithExtraDetails({
            explanationMarkdown: '**Bad** things _happen_',
            componentExplanationMarkdown: '`<script>alert("pwnd")</script>`',
          });
          const data = render.querySelector('.nx-read-only__data');

          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[0].innerHTML).toBe(
            '<strong>Bad</strong> things <em>happen</em>'
          );
          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[1].innerHTML).toBe(
            '<code>&lt;script&gt;alert("pwnd")&lt;/script&gt;</code>'
          );
        }
      );

      it('prepends the malicious code warning if present categories contains malicious_code', function () {
        const render = getExplanationWithExtraDetails({
          explanationMarkdown: '**Bad** things _happen_',
          componentExplanationMarkdown: '`<script>alert("pwnd")</script>`',
          categories: ['foobar', 'malicious_code'],
        });
        const firstChild = render.querySelector('.nx-read-only__data');
        const warningParagraph = firstChild.querySelector('.iq-vulnerability-details__warning');
        expect(warningParagraph).not.toBeNull();
        // Check that the warning paragraph contains an icon (any SVG with nx-icon class)
        const icon = warningParagraph.querySelector('svg.nx-icon');
        expect(icon).not.toBeNull();
        const warningMsg = firstChild.querySelector('span');
        expect(queryByText(warningMsg, 'Malicious Code')).toBeInTheDocument();
        expect(queryByText(warningMsg.querySelector('strong'), 'Warning:')).toBeInTheDocument();
      });

      it('prepends the malicious code warning regardless of the text case of the category name', function () {
        const render = getExplanationWithExtraDetails({
          explanationMarkdown: '**Bad** things _happen_',
          componentExplanationMarkdown: '`<script>alert("pwnd")</script>`',
          categories: ['foobar', 'mAlicIouS_COde'],
        });
        const firstChild = render.querySelector('.nx-read-only__data');

        const warningMsg = firstChild.querySelector('span');
        expect(queryByText(warningMsg, 'Malicious Code')).toBeInTheDocument();
        expect(queryByText(warningMsg.querySelector('strong'), 'Warning:')).toBeInTheDocument();
      });
    });

    describe('third nx-read-only (Detection)', function () {
      const getDetectionWithExtraDetails = (extraDetails) => rightSide(renderComponent(extraDetails));
      it('is an empty render if detectionMarkdown and componentDetectionMarkdown are undefined', function () {
        expect(
          queryByText(getDetectionWithExtraDetails({ detectionMarkdown: "You're foobarred" }), "You're foobarred")
        ).toBeInTheDocument();
        expect(
          queryByText(
            getDetectionWithExtraDetails({ componentDetectionMarkdown: "You're foobarred" }),
            "You're foobarred"
          )
        ).toBeInTheDocument();

        expect(
          queryByText(
            getDetectionWithExtraDetails({
              detectionMarkdown: "You're foobarred",
              componentDetectionMarkdown: 'No, really',
            }),
            'No, really'
          )
        ).toBeInTheDocument();
      });

      it('is labeled "Detection"', function () {
        expect(
          queryByText(getDetectionWithExtraDetails({ detectionMarkdown: 'asdf' }), 'Detection')
        ).toBeInTheDocument();
      });

      it(
        'sets the data to a concatenation of the markdown renders of the detectionMarkdown ' +
          'and componentDetectionMarkdown',
        function () {
          const render = getDetectionWithExtraDetails({
            detectionMarkdown: '**Bad** things _happen_',
            componentDetectionMarkdown: '`<script>alert("pwnd")</script>`',
          });
          const data = render.querySelector('.nx-read-only__data');

          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[0].innerHTML).toBe(
            '<strong>Bad</strong> things <em>happen</em>'
          );
          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[1].innerHTML).toBe(
            '<code>&lt;script&gt;alert("pwnd")&lt;/script&gt;</code>'
          );
        }
      );
    });

    describe('fourth nx-read-only (Recommendations)', function () {
      const getRecommendationsWithExtraDetails = (extraDetails) => rightSide(renderComponent(extraDetails));

      it('is an empty render if recommendationMarkdown and componentRecommendationMarkdown are undefined', function () {
        expect(queryByText(getRecommendationsWithExtraDetails(), 'Recommendation')).not.toBeInTheDocument();
      });

      it('is labeled "Recommendation"', function () {
        expect(
          queryByText(getRecommendationsWithExtraDetails({ recommendationMarkdown: 'asdf' }), 'Recommendation')
        ).toBeInTheDocument();
      });

      it(
        'sets the data to a concatenation of the markdown renders of the recommendationMarkdown and ' +
          'componentRecommendationMarkdown',
        function () {
          const render = getRecommendationsWithExtraDetails({
            recommendationMarkdown: '**Bad** things _happen_',
            componentRecommendationMarkdown: '`<script>alert("pwnd")</script>`',
          });
          const data = render.querySelector('.nx-read-only__data');

          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[0].innerHTML).toBe(
            '<strong>Bad</strong> things <em>happen</em>'
          );
          expect(data.querySelectorAll('.iq-vulnerability-details__html-detail p')[1].innerHTML).toBe(
            '<code>&lt;script&gt;alert("pwnd")&lt;/script&gt;</code>'
          );
        }
      );
    });

    describe('fifth nx-read-only (Version Affected)', function () {
      const getVersionWithExtraDetails = (extraDetails) =>
        rightSide(
          renderComponent({
            vulnerableVersionRanges: ['[2.0.0-RC1, 2.6.7.1)', '[2.7.0-RC1, 2.7.9.1)', '[2.8.0-RC1, 2.8.8.1)'],
            ...extraDetails,
          })
        );

      it('is an empty render if rootCauses is empty or undefined', function () {
        expect(queryByText(rightSide(renderComponent()), 'Version Affected')).not.toBeInTheDocument();
        expect(
          queryByText(getVersionWithExtraDetails({ vulnerableVersionRanges: [] }), 'Version Affected')
        ).not.toBeInTheDocument();
        expect(
          queryByText(getVersionWithExtraDetails({ vulnerableVersionRanges: undefined }), 'Version Affected')
        ).not.toBeInTheDocument();
      });

      it('is labeled "Version Affected"', function () {
        expect(queryByText(getVersionWithExtraDetails(), 'Version Affected')).toBeInTheDocument();
      });

      it('renders an entry for each version range', function () {
        const data = getVersionWithExtraDetails().querySelectorAll('.nx-read-only__data');

        expect(data.length).toBe(3);
        expect(queryByText(data[0], '[2.0.0-RC1, 2.6.7.1)')).toBeInTheDocument();
        expect(queryByText(data[1], '[2.7.0-RC1, 2.7.9.1)')).toBeInTheDocument();
        expect(queryByText(data[2], '[2.8.0-RC1, 2.8.8.1)')).toBeInTheDocument();
      });
    });

    describe('sixth nx-read-only (Root Causes)', function () {
      const getRootCausesWithExtraDetails = (extraDetails) =>
        rightSide(
          renderComponent({
            rootCauses: [
              { listOfPaths: ['a', 'b'], versionRange: '[1,2]' },
              { listOfPaths: ['c', 'd'], versionRange: '[1,3000)' },
            ],
            ...extraDetails,
          })
        );

      it('is an empty render if rootCauses is empty or undefined', function () {
        expect(queryByText(rightSide(renderComponent()), 'Root Cause')).not.toBeInTheDocument();
        expect(queryByText(getRootCausesWithExtraDetails({ rootCauses: [] }), 'Root Cause')).not.toBeInTheDocument();
        expect(
          queryByText(getRootCausesWithExtraDetails({ rootCauses: undefined }), 'Root Cause')
        ).not.toBeInTheDocument();
      });

      it('is labeled "Root Cause"', function () {
        expect(
          queryByText(getRootCausesWithExtraDetails().querySelector('.nx-read-only__label'), 'Root Cause')
        ).toBeInTheDocument();
        expect(getRootCausesWithExtraDetails().querySelector('svg').getAttribute('class')).toContain('fa-circle-info');
      });

      it(
        'renders a div for each root cause containing a series of iq-vulnerability-details__root-cause-paths ' +
          'followed by a iq-vulnerability-details__root-cause_version-range',
        function () {
          const data = getRootCausesWithExtraDetails().querySelectorAll('.nx-read-only__data');
          expect(data.length).toBe(2);

          const firstReadOnlyData = data[0].querySelectorAll('.iq-vulnerability-details__root-cause-path');
          expect(firstReadOnlyData.length).toBe(2);
          expect(queryByText(firstReadOnlyData[0], 'a')).toBeInTheDocument();
          expect(queryByText(firstReadOnlyData[1], 'b')).toBeInTheDocument();
          expect(
            queryByText(data[0].querySelector('.iq-vulnerability-details__root-cause-version-range'), '[1,2]')
          ).toBeInTheDocument();

          const secondReadOnlyData = data[1].querySelectorAll('.iq-vulnerability-details__root-cause-path');
          expect(secondReadOnlyData.length).toBe(2);
          expect(queryByText(secondReadOnlyData[0], 'c')).toBeInTheDocument();
          expect(queryByText(secondReadOnlyData[1], 'd')).toBeInTheDocument();
          expect(
            queryByText(data[1].querySelector('.iq-vulnerability-details__root-cause-version-range'), '[1,3000)')
          ).toBeInTheDocument();
        }
      );
    });

    describe('seventh nx-read-only (Advisories)', function () {
      const getAdvisoriesCausesWithExtraDetails = (extraDetails) =>
        rightSide(
          renderComponent({
            advisories: [{ referenceType: 'UNKNOWN', url: 'http://foo/' }],
            ...extraDetails,
          })
        );

      it('is an empty render if advisories is undefined or empty', function () {
        expect(
          queryByText(getAdvisoriesCausesWithExtraDetails({ advisories: undefined }), 'Advisories')
        ).not.toBeInTheDocument();
        expect(
          queryByText(getAdvisoriesCausesWithExtraDetails({ advisories: [] }), 'Advisories')
        ).not.toBeInTheDocument();
        expect(queryByText(rightSide(renderComponent()), 'Advisories')).not.toBeInTheDocument();
      });

      it('renders nothing if advisories missing data', function () {
        const advisories = [
          {
            referenceType: 'ATTACK',
          },
          {
            referenceType: null,
            url: 'null-url',
          },
        ];

        expect(queryByText(getAdvisoriesCausesWithExtraDetails({ advisories }), 'Advisories')).not.toBeInTheDocument();
      });

      it('renders nothing if advisories all have UNKNOWN source, and no URL', function () {
        expect(
          queryByText(getAdvisoriesCausesWithExtraDetails({ advisories: [{ referenceType: 'UNKNOWN' }] }), 'Advisories')
        ).not.toBeInTheDocument();
      });

      it('renders a series of nx-read-only__data containing only links if all links have an UNKNOWN referenceType', function () {
        const advisories = [
            { referenceType: 'UNKNOWN', url: 'http://asdf/' },
            { referenceType: 'UNKNOWN', url: 'http://qwerty/' },
          ],
          render = getAdvisoriesCausesWithExtraDetails({ advisories });

        const readOnlyData = render.querySelectorAll('.nx-read-only__data');
        expect(readOnlyData.length).toBe(2);
        assertLinkProperties(
          readOnlyData[0].querySelector('a'),
          'http://asdf/',
          'http://asdf/',
          '_blank',
          'noreferrer'
        );
        assertLinkProperties(
          readOnlyData[1].querySelector('a'),
          'http://qwerty/',
          'http://qwerty/',
          '_blank',
          'noreferrer'
        );
      });

      it('renders a sub-description-list with links and references types if some referenceTypes are not UNKNOWN', function () {
        const advisories = [
            { referenceType: 'PROJECT', url: 'http://asdf/' },
            { referenceType: 'UNKNOWN', url: 'http://qwerty/' },
            { referenceType: 'ATTACK', url: 'http://foobar/' },
          ],
          render = getAdvisoriesCausesWithExtraDetails({ advisories });

        const readOnlyData = render.querySelector('.nx-read-only__data');
        const subList = readOnlyData.querySelectorAll('div.iq-vulnerability-details__sub-description-container');

        expect(subList.length).toBe(3);

        const firstChild = subList[0];
        const secondChild = subList[1];
        const thirdChild = subList[2];

        expect(
          queryByText(firstChild.querySelector('dt.iq-vulnerability-details__sub-description-term'), 'Project')
        ).toBeInTheDocument();

        assertLinkProperties(
          firstChild.querySelector('dd.iq-vulnerability-details__sub-description-desc a'),
          'http://asdf/',
          'http://asdf/',
          '_blank',
          'noreferrer'
        );

        expect(
          queryByText(secondChild.querySelector('dt.iq-vulnerability-details__sub-description-term'), 'Unknown')
        ).toBeInTheDocument();

        assertLinkProperties(
          secondChild.querySelector('dd.iq-vulnerability-details__sub-description-desc a'),
          'http://qwerty/',
          'http://qwerty/',
          '_blank',
          'noreferrer'
        );

        expect(
          queryByText(thirdChild.querySelector('dt.iq-vulnerability-details__sub-description-term'), 'Attack')
        ).toBeInTheDocument();

        assertLinkProperties(
          thirdChild.querySelector('dd.iq-vulnerability-details__sub-description-desc a'),
          'http://foobar/',
          'http://foobar/',
          '_blank',
          'noreferrer'
        );
      });
    });

    describe('eighth nx-read-only (CVSS details)', function () {
      const getCvssDetailsWithExtraDetails = (extraDetails) =>
        rightSide(
          renderComponent({
            mainSeverity: { source: 'cve_cvss_3', vector: '1:2:3', score: 9.8 },
            ...extraDetails,
          })
        );

      it('is an empty render if mainSeverity is undefined or does not include source nor vector', function () {
        expect(
          queryByText(getCvssDetailsWithExtraDetails({ mainSeverity: undefined }), 'CVSS Details')
        ).not.toBeInTheDocument();
        expect(
          queryByText(getCvssDetailsWithExtraDetails({ mainSeverity: { score: 10 } }), 'CVSS Details')
        ).not.toBeInTheDocument();

        expect(
          queryByText(getCvssDetailsWithExtraDetails({ mainSeverity: { score: 10, source: '' } }), 'CVSS Details')
        ).not.toBeInTheDocument();

        expect(
          queryByText(
            getCvssDetailsWithExtraDetails({ mainSeverity: { score: 10, source: 'cve_cvss_2' } }),
            'CVSS Details'
          )
        ).toBeInTheDocument();

        expect(
          queryByText(getCvssDetailsWithExtraDetails({ mainSeverity: { score: 10, vector: '1:23' } }), 'CVSS Details')
        ).toBeInTheDocument();

        expect(
          queryByText(
            getCvssDetailsWithExtraDetails({ mainSeverity: { score: 10, source: '', vector: '1:23' } }),
            'CVSS Details'
          )
        ).toBeInTheDocument();
      });
      it('is labeled "CVSS Details"', function () {
        expect(queryByText(getCvssDetailsWithExtraDetails(), 'CVSS Details')).toBeInTheDocument();
      });

      it('sets the first item in the sub list with the source as the term and the severity as the desc', function () {
        const subList = getCvssDetailsWithExtraDetails().querySelector(
          '.iq-vulnerability-details__sub-description-list'
        );

        const firstItem = subList.querySelectorAll('.iq-vulnerability-details__sub-description-container')[0];

        expect(
          getByText(firstItem.querySelector('.iq-vulnerability-details__sub-description-term'), 'CVE CVSS 3')
        ).toBeInTheDocument();
        expect(
          getByText(firstItem.querySelector('.iq-vulnerability-details__sub-description-desc'), '9.8')
        ).toBeInTheDocument();
      });

      it('does not render the first item if the source is not set', function () {
        const subList = getCvssDetailsWithExtraDetails({
          mainSeverity: { score: 9.8, vector: '1:2:3' },
        }).querySelector('.iq-vulnerability-details__sub-description-list');

        expect(queryByText(subList, '9.8')).not.toBeInTheDocument();
      });

      it('does not render the first item if the source is empty', function () {
        const subList = getCvssDetailsWithExtraDetails({
          mainSeverity: { source: '', score: 9.8, vector: '1:2:3' },
        }).querySelector('.iq-vulnerability-details__sub-description-list');
        expect(queryByText(subList, '9.8')).not.toBeInTheDocument();
      });

      it('sets the second item in the sub list based on the vector', function () {
        const subList = getCvssDetailsWithExtraDetails().querySelector(
          '.iq-vulnerability-details__sub-description-list'
        );
        const lastItem = subList.querySelectorAll('.iq-vulnerability-details__sub-description-container')[1];

        expect(
          queryByText(lastItem.querySelector('.iq-vulnerability-details__sub-description-term'), 'CVSS Vector')
        ).toBeInTheDocument();
        expect(
          queryByText(lastItem.querySelector('.iq-vulnerability-details__sub-description-desc'), '1:2:3')
        ).toBeInTheDocument();
      });

      it('does not render the second item if the vector is not set', function () {
        const subList = getCvssDetailsWithExtraDetails({
          mainSeverity: { score: 9.8, source: 'cve_cvss_2' },
        }).querySelector('.iq-vulnerability-details__sub-description-list');
        expect(queryByText(subList, 'CVSS Vector')).not.toBeInTheDocument();
      });
    });
  });
});
