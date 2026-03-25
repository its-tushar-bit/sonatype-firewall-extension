/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// force a consistent timezone for the tests so that dates and times render constantly regardless of the host timezone
process.env.TZ = 'America/New_York';

module.exports = {
  roots: ['<rootDir>/src/main/frontend', '<rootDir>/src/test/frontend'],
  // Auto-detect CPU cores (defaults to cores - 1)
  // maxWorkers: 2, // Old conservative setting for CI
  maxWorkers: '87.5%', // 14 workers on 16-core (freed cores from reduced Postgres test parallelization)
  workerIdleMemoryLimit: '1024MB', // Increased memory per worker for better performance
  transformIgnorePatterns: [
    '/node_modules/(?!(pretty-bytes|@react-hook|@sonatype|@nivo|d3-[^/]+|internmap|delaunator|robust-predicates|lodash-es|swagger-ui-react|swagger-client|react-syntax-highlighter)/)',
  ],
  transform: {
    '\\.[jt]sx?$': [
      'babel-jest',
      {
        presets: [['@babel/preset-env', { targets: { node: '16.16.0' } }], '@babel/preset-react'],
      },
    ],
  },
  testRegex: 'src/test/frontend/.*\\.jestspec\\.jsx?',
  testEnvironment: 'jsdom',
  moduleFileExtensions: ['js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ['<rootDir>/src/test/frontend/setupJest.js'],
  moduleNameMapper: {
    '\\.s?css$': '<rootDir>/src/test/frontend/__mocks__/styleMock.js',
    'img/nexus_auditor.svg$': '<rootDir>/src/test/frontend/__mocks__/nexus_auditor.svg',
    'img/nexus_firewall.svg$': '<rootDir>/src/test/frontend/__mocks__/nexus_firewall.svg',
    'img/nexus_lifecycle.svg$': '<rootDir>/src/test/frontend/__mocks__/nexus_lifecycle.svg',
    'img/sonatype.svg$': '<rootDir>/src/test/frontend/__mocks__/sonatype.svg',
    'sbomManager/assets/sbom-manager.svg$': '<rootDir>/src/test/frontend/__mocks__/sbom_manager.svg',
    '\\.(png|svg)$': '<rootDir>/src/test/frontend/__mocks__/imgMock.js',
    '\\.(html)$': '<rootDir>/src/test/frontend/__mocks__/htmlMock.js',
    '^react-virtualized-auto-sizer$': '<rootDir>/src/test/frontend/__mocks__/react-virtualized-auto-sizer.js',
    '^MainRoot/(.*)': '<rootDir>/src/main/frontend/$1',
    '^TestRoot/(.*)': '<rootDir>/src/test/frontend/$1',
  },
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputFile: '<rootDir>/target/jest-reports/jest-junit.xml',
      },
    ],
  ],
  // Performance optimizations
  testTimeout: 30000, // 30s timeout per test (default is 5s)
  maxConcurrency: 5, // Limit concurrent tests per worker
  cache: true, // Explicitly enable caching
  cacheDirectory: '<rootDir>/target/.jest-cache', // Use target dir for cache
};
