/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ComponentCountsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/componentCountsChart/ComponentCountsChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('componentCountsChart', () => {
  let activeApplicationCount, componentCounts, getShallow;

  beforeEach(() => {
    activeApplicationCount = 3;
    componentCounts = {
      componentsPerApplication: 32,
      componentsInTheMostApplications: [
        { componentDisplayName: 'SimpleJson 0.38.0', count: 1 },
        {
          componentDisplayName: 'ch.qos.logback : logback-access : 0.6',
          count: 1,
        },
        {
          componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3',
          count: 1,
        },
        { componentDisplayName: 'commons-dbcp : commons-dbcp : 1.4', count: 2 },
        {
          componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
          count: 2,
        },
      ],
      componentsWithTheMostViolations: [
        {
          componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
          count: 1,
        },
        {
          componentDisplayName: 'org.apache.geronimo.framework : geronimo-security : 2.1',
          count: 1,
        },
        {
          componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15',
          count: 1,
        },
        {
          componentDisplayName: 'tomcat : catalina-host-manager : 5.5.23',
          count: 2,
        },
        { componentDisplayName: 'tomcat : tomcat-util : 5.5.23', count: 2 },
      ],
    };

    getShallow = getShallowComponent(ComponentCountsChart, {
      activeApplicationCount,
      componentCounts,
    });
  });

  describe('when single application', () => {
    let component, isSingleApplicationReport, singleApplicationName;
    beforeEach(() => {
      isSingleApplicationReport = true;
      singleApplicationName = 'test application name';
      component = getShallow({
        isSingleApplicationReport,
        singleApplicationName,
      });
    });

    it('renders description', () => {
      const description = component.find('.nx-tile-header__subtitle');
      expect(description).toHaveText(
        `This data is based on the latest Lifecycle evaluations. ${singleApplicationName} contains ${componentCounts.componentsPerApplication} components.`
      );
    });

    it('renders chart container', () => {
      expect(component.find('#components-in-most-applications-chart')).not.toExist();
      expect(component.find('#component-with-most-violations-chart')).toExist();
    });
  });

  describe('when not single application', () => {
    let component, isSingleApplicationReport, singleApplicationName;
    beforeEach(() => {
      isSingleApplicationReport = false;
      singleApplicationName = null;
      component = getShallow({
        isSingleApplicationReport,
        singleApplicationName,
      });
    });

    it('renders description', () => {
      const description = component.find('.nx-tile-header__subtitle');
      expect(description).toHaveText(
        `This data is based on the latest Lifecycle evaluations of ${activeApplicationCount} applications. On average, there are ${componentCounts.componentsPerApplication} components per application.`
      );
    });

    it('renders chart container', () => {
      expect(component.find('#components-in-most-applications-chart')).toExist();
      expect(component.find('#component-with-most-violations-chart')).toExist();
    });
  });
});
