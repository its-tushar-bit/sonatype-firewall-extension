/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

make(
    useEventSpy: false,
    javaVersion: 'Java 8',
    mavenVersion: 'Maven 3.2.x',
    mavenOptions: '-D skipTests -D skip-functional-test',
    downstreamJobName: 'extra-tests',
    artifactsForDownstream: '.zion/repository/com/sonatype/insight/brain/**',
    iqPolicyEvaluation: { stage ->
      nexusPolicyEvaluation iqStage: stage, iqApplication: 'CLM-server',
          iqScanPatterns: [[scanPattern: 'scan_nothing']],
          failBuildOnNetworkError: true
      nexusPolicyEvaluation iqStage: stage, iqApplication: 'iq-server-frontend-assets',
          iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
          iqModuleExcludes: [[moduleExclude: '**']],
          failBuildOnNetworkError: true
    },
    distFiles: [
        includes: [
            'nexus-iq-server/target/*.zip*',
            'nexus-iq-server/target/*.tar.gz*',
            'nexus-iq-cli/target/*.jar*',
            'nexus-iq-diagnostics/target/*.jar*',
            'nexus-iq-integrator-scanner/target/*.jar*'
        ],
        excludes: [
            '**/*-sources.jar*',
            '**/*-javadoc.jar*',
            '**/*proguard*',
            '**/original-*'
        ]
    ],
    usePMD: true,
    useCheckstyle: true
)
