/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
// eslint-disable-next-line no-unused-vars
var PolicyViolationAggregationResourceMockData = {
  getAverages: function() {
    return {
      evaluationCount: 3,
      securityViolations: {
        averageDiscovered: 1,
        averageDiscoveredCritical: 1
      },
      licenseViolations: {
        averageDiscovered: 12,
        averageDiscoveredCritical: 8
      },
      qualityViolations: {
        averageDiscovered: 6,
        averageDiscoveredCritical: 2
      },
      otherViolations: {
        averageDiscovered: 12,
        averageDiscoveredCritical: 11
      },
      totalViolations: {
        averageDiscovered: 31,
        averageDiscoveredCritical: 22
      }
    };
  },
  getEmptyAverages: function() {
    return {
      evaluationCount: 0,
      securityViolations: {
        averageDiscovered: 0,
        averageDiscoveredCritical: 0
      },
      licenseViolations: {
        averageDiscovered: 0,
        averageDiscoveredCritical: 0
      },
      qualityViolations: {
        averageDiscovered: 0,
        averageDiscoveredCritical: 0
      },
      otherViolations: {
        averageDiscovered: 0,
        averageDiscoveredCritical: 0
      },
      totalViolations: {
        averageDiscovered: 0,
        averageDiscoveredCritical: 0
      }
    };
  },
  getMttrData: function() {
    return [
      {'timePeriodName': 'Sep', 'mttrInSeconds': 1309714, 'criticalMttrInSeconds': 129714},
      {'timePeriodName': 'Oct', 'mttrInSeconds': 1299714, 'criticalMttrInSeconds': 1299714},
      {'timePeriodName': 'Nov', 'mttrInSeconds': 1289714, 'criticalMttrInSeconds': 1209714},
      {'timePeriodName': 'Dec', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'Jan', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'Feb', 'mttrInSeconds': 384000, 'criticalMttrInSeconds': 384000},
      {'timePeriodName': 'Mar', 'mttrInSeconds': 384000, 'criticalMttrInSeconds': 384000},
      {'timePeriodName': 'Apr', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'May', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'Jun', 'mttrInSeconds': 1209714, 'criticalMttrInSeconds': 1209714},
      {'timePeriodName': 'Jul', 'mttrInSeconds': 484000, 'criticalMttrInSeconds': 484000},
      {'timePeriodName': 'Aug', 'mttrInSeconds': null, 'criticalMttrInSeconds': null}
    ];
  },
  getPartialMttrData: function() {
    return [
      {'timePeriodName': 'Nov', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'Dec', 'mttrInSeconds': 1209714, 'criticalMttrInSeconds': 1209714},
      {'timePeriodName': 'Jan', 'mttrInSeconds': 484000, 'criticalMttrInSeconds': 484000},
      {'timePeriodName': 'Feb', 'mttrInSeconds': null, 'criticalMttrInSeconds': null},
      {'timePeriodName': 'Mar', 'mttrInSeconds': null, 'criticalMttrInSeconds': null}
    ];
  },
  getComponentCountsData: function() {
    return {
      componentsPerApplication: 32,
      componentsInTheMostApplications: [
        {componentDisplayName: 'SimpleJson 0.38.0', count: 1},
        {componentDisplayName: 'ch.qos.logback : logback-access : 0.6', count: 1},
        {componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3', count: 1},
        {componentDisplayName: 'commons-dbcp : commons-dbcp : 1.4', count: 2},
        {componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1', count: 2}
      ],
      componentsWithTheMostViolations: [
        {componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1', count: 1},
        {componentDisplayName: 'org.apache.geronimo.framework : geronimo-security : 2.1', count: 1},
        {componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15', count: 1},
        {componentDisplayName: 'tomcat : catalina-host-manager : 5.5.23', count: 2},
        {componentDisplayName: 'tomcat : tomcat-util : 5.5.23', count: 2}
      ]
    };
  },
  getPartialComponentCountsData: function() {
    return {
      componentsPerApplication: 32,
      componentsInTheMostApplications: [
        {componentDisplayName: 'SimpleJson 0.38.0', count: 1},
        {componentDisplayName: 'ch.qos.logback : logback-access : 0.6', count: 1},
        {componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3', count: 1}
      ],
      componentsWithTheMostViolations: [
        {componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1', count: 1},
        {componentDisplayName: 'org.apache.geronimo.framework : geronimo-security : 2.1', count: 1},
        {componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15', count: 1}
      ]
    };
  },
  getFullChartData: function() {
    return {
      mttrs: PolicyViolationAggregationResourceMockData.getMttrData(),
      averages: PolicyViolationAggregationResourceMockData.getAverages(),
      componentCounts: PolicyViolationAggregationResourceMockData.getComponentCountsData(),
      monthCount: 11,
      lastUpdated: 1507218887089
    };
  },
  getPartialChartData: function() {
    return {
      mttrs: PolicyViolationAggregationResourceMockData.getPartialMttrData(),
      averages: PolicyViolationAggregationResourceMockData.getEmptyAverages(),
      componentCounts: PolicyViolationAggregationResourceMockData.getPartialComponentCountsData()
    };
  }
};

export default PolicyViolationAggregationResourceMockData;
