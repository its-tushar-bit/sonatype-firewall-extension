/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

/**
 This feature branch Jenkinsfile is intended for validation rather than producing releasable artifacts. It by default
 excludes "slow" tests which are test classes that take longer than 50 seconds to run. Heavy tests are distributed
 across multiple agents to maximize parallelization.

 All of these optimizations can be toggled using Build with Parameters.

 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  1. INITIALIZE                                                                               │
 │     • Set env vars                                                                           │
 │     • Configure MAVEN_CMD                                                                    │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  2. PREPARE                                                                                 │
 │     • Install Node/Yarn                                                                     │
 │     • install-node-and-yarn                                                                 │
 │     • yarn install                                                                          │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  3. BUILD                                                                                    │
 │     • install -DskipTests                                                                    │
 │     • Uses -T 1C for parallel builds                                                      │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  4. STASH TEST ARTIFACTS                                                                     │
 │     • Stash SNAPSHOT JARs and test classes for distributed agents                            │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  5. TEST                                                               ║ PARALLEL ║          │
 ├─────────────────────────┬───────────────────────┬───────────────────────────────────────────┤
 │  Frontend Tests         │  Static Analysis      │  Policy Evaluation (conditional)          │
 │  ──────────────         │  ───────────────      │  ────────────────────────────             │
 │  • main agent           │  • agent: iq          │  • nexusPolicyEvaluation                  │
 │  • Jest                 │  • Spotless Check     │                                           │
 │                         │  • License Check      │                                           │
 ├──────────────┬──────────┬──────────┬──────────┬──────────┬──────────┬────────────────────┤
 │  Postgres    │  Surefire│  Failsafe│  Failsafe│  Failsafe│  Failsafe│  Playwright        │
 │  Tests       │  ────────│  1 (A)   │  2 (B-L) │  3 (M-R) │  4 (S-Z) │  Functional Tests  │
 │  • main      │  • main  │  • iq    │  • iq    │  • iq    │  • iq    │  • iq              │
 │  • Postgres  │  • test  │  27%     │  25%     │  25%     │  21%     │  • -Psanity        │
 ├──────────────┴──────────┴──────────┴──────────┴──────────┴──────────┴────────────────────┤
 │  MTIQ Tests • main agent • surefire+failsafe                                            │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  6. TEST - SLOW (conditional: includeSlowTests)                        ║ PARALLEL ║           │
 ├──────────────────────────────────────────────┬────────────────────────────────────────────────┤
 │  Slow Tests - Backend                        │  Slow Tests - MTIQ                             │
 └──────────────────────────────────────────────┴────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  7. COLLECT RESULTS                                                                           │
 │     • junit: Collect test reports (surefire, failsafe, jest)                                  │
 │     • archiveArtifacts: Test report files                                                     │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  8. BUILD MTIQ IMAGE                                          ║ CONDITIONAL ║                 │
 │     When: mtiqImagePushEnabled OR *_mtiq job                                                  │
 │     • Build Docker image                                                                      │
 │     • Push to RSC registry                                                                    │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  POST ACTIONS                                                                                 │
 ├───────────────────────────────────────────────────────────────────────────────────────────────┤
 │  always:                                                                                      │
 │    • Echo "Pipeline completed"                                                                │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘


 ╔═══════════════════════════════════════════════════════════════════════════════════════════════╗
 ║  LEGEND                                                                                       ║
 ╠═══════════════════════════════════════════════════════════════════════════════════════════════╣
 ║  -T 1C       = 100% of CPU cores (16 threads on 16-core)                                      ║
 ║  -T 2        = 2 Maven threads                                                                ║
 ║  SlowTest    = Tests >100s excluded by default (param: includeSlowTests)                      ║
 ║  FE          = insight-brain-frontend                                                         ║
 ║  MTIQ        = nexus-mtiq-server (Multi-Tenant IQ)                                            ║
 ║  agent: iq-large = Runs on separate distributed agent                                          ║
 ╚═══════════════════════════════════════════════════════════════════════════════════════════════╝
 */

// Configure build parameters and job properties
configureBranchJob()

// Agent label for distributed test stages
// Instance type here: https://github.com/sonatype/bnr-jenkins-casc/blob/1ba07ca6a0576a8fa5ed0ff672a7b5c42587fdc2/clouds/zion.yaml#L242
def DISTRIBUTED_TEST_AGENT = 'iq-large'

pipeline {
  agent {
    label 'iq-large'
  }

  options {
    timestamps()
    buildDiscarder(logRotator(
        numToKeepStr: '10'
    ))
    // Increase timeout for Jakarta EE 11 migration branch - tests take longer due to framework changes
    timeout(time: 150, unit: 'MINUTES')
  }

  stages {
    stage('Initialize') {
      steps {
        script {

          echo "Pipeline Configuration:"
          echo "  Branch: ${env.BRANCH_NAME ?: gitBranch(env)}"
          echo "  Bundling enabled: ${isBundlingEnabled()}"
          echo "  Include slow tests: ${params.includeSlowTests}"

          env.BUILD_DIR = env.WORKSPACE

        }
      }
    }

    stage('Prepare: Install Node/Yarn') {
      steps {
        script {
          dir(env.BUILD_DIR) {
              // Install node/yarn binaries and download npm dependencies
              mvn getFrontEndInstallConfig(),
                  'com.github.eirslett:frontend-maven-plugin:install-node-and-yarn@install-node-and-yarn com.github.' +
                      'eirslett:frontend-maven-plugin:yarn@yarn-install'
          }
        }
      }
    }

    stage('Build') {
      steps {
        script {
          dir(env.BUILD_DIR) {
            withSonatypeDockerRegistry() {
              withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
                       "TESTCONTAINERS_RYUK_DISABLED=true"]) {
                // 'clean' is needed because feature-branch workspaces persist across force-pushes;
                // without it stale resources from prior tips (e.g. renamed migration files) remain in target/.
                mvn getMavenBuildConfig(), 'clean install'

                // Stash immediately after build (eliminates separate stage overhead)
                stashTestArtifacts()
              }
            }
          }
        }
      }
    }

    // Stash Test Artifacts stage removed - now integrated into Build stage above

    stage('Test') {
      parallel {
        // Frontend tests on main agent (benefits from cached node/yarn)
        stage('Frontend Tests') {
          steps {
            script {
              dir(env.BUILD_DIR) {
                // Run Jest tests using cached node/yarn from Prepare stage
                mvn getFrontEndTestConfig(),
                    'com.github.eirslett:frontend-maven-plugin:yarn@jest'
              }
            }
          }
        }

        stage('Policy Evaluation') {
          when {
            expression { shouldRunPolicyEvaluation() }
          }
          steps {
            script {
              dir(env.BUILD_DIR) {
                def reachabilityConfig = [
                    javaAnalysis: [
                        enable: true,
                        includes: [
                            [pattern: 'nexus-iq-server/target/insight-brain-service-*.jar']
                        ],
                        entrypointStrategy: 'JAVA_MAIN',
                        namespaces: [
                            [namespace: 'com.sonatype.insight']
                        ]
                    ],
                    jsAnalysis: [
                        enable: true,
                        node: [
                            executable: "${env.WORKSPACE}/insight-brain-frontend/node/node"
                        ],
                        projectDirectory: 'insight-brain-frontend',
                        sourceFiles: [
                            [pattern: 'src/main/**']
                        ],
                        excludeFiles: [
                            [pattern: 'src/test/**']
                        ]
                    ]
                ]
                nexusPolicyEvaluation(
                    iqStage: 'develop',
                    iqApplication: 'insight-brain',
                    iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
                    iqModuleExcludes: [[moduleExclude: '**/test/**'], [moduleExclude: '**/test-classes/**/module.xml']],
                    failBuildOnNetworkError: true,
                    reachability: reachabilityConfig
                )
              }
            }
          }
        }

        // Static analysis on smaller distributed agent
        stage('Static Analysis') {
          agent { label 'iq' }
          steps {
            script {
              runDistributedStaticAnalysis()
            }
          }
          post {
            failure {
              script {
                // Ensure static analysis failures fail the entire build
                currentBuild.result = 'FAILURE'
                error('Static analysis (Spotless/License) failed - see logs for violations')
              }
            }
          }
        }

        // Backend tests (Surefire + MTIQ + Postgres) on distributed agent
        stage('Postgres + MTIQ Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            // Increased timeout for Jakarta EE 11 migration - tests take longer due to framework changes
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedBackendTests()
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Failsafe Tests - split across 4 agents by test class name pattern
        // Uses -Dit.test=%regex[pattern] syntax (same as Jenkinsfile.main)
        // Distribution: A (574, 27%), B-L (537, 25%), M-R (538, 25%), S-Z (440, 21%)
        stage('Failsafe Tests 1 (A)') {
          agent { label DISTRIBUTED_TEST_AGENT }
          steps {
            script {
              runDistributedFailsafeTests('.*/A.*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        stage('Failsafe Tests 2 (B-L)') {
          agent { label DISTRIBUTED_TEST_AGENT }
          steps {
            script {
              runDistributedFailsafeTests('.*/[B-L].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        stage('Failsafe Tests 3 (M-R)') {
          agent { label DISTRIBUTED_TEST_AGENT }
          steps {
            script {
              runDistributedFailsafeTests('.*/[M-R].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        stage('Failsafe Tests 4 (S-Z)') {
          agent { label DISTRIBUTED_TEST_AGENT }
          steps {
            script {
              runDistributedFailsafeTests('.*/[S-Z].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        stage('Playwright Functional Tests') {
          when {
            expression { !params.skipPlaywrightTests }
          }
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 45, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedPlaywrightTests()
            }
          }
          post {
            always {
              junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
              archiveArtifacts(artifacts: 'insight-brain-playwright-test/target/playwright-screenshots/**',
                               allowEmptyArchive: true)
              archiveArtifacts(artifacts: 'insight-brain-playwright-test/target/playwright-traces/**',
                               allowEmptyArchive: true)
              archiveArtifacts(artifacts: 'insight-brain-playwright-test/target/playwright-diagnostics/**',
                               allowEmptyArchive: true)
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }
      }
    }

    stage('Test - Slow') {
      when {
        expression { params.includeSlowTests }
      }
      parallel {
        stage('Slow Tests - Backend') {
          steps {
            script {
              dir(env.BUILD_DIR) {
                withSonatypeDockerRegistry() {
                  withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
                           "TESTCONTAINERS_RYUK_DISABLED=true"]) {
                    def opts = buildSlowTestMavenOptions('!insight-brain-frontend,!nexus-mtiq-server')
                    mvnDirectForTests(opts, 'surefire:test failsafe:integration-test failsafe:verify')
                  }
                }
              }
            }
          }
        }

        stage('Slow Tests - MTIQ') {
          steps {
            script {
              dir(env.BUILD_DIR) {
                withSonatypeDockerRegistry() {
                  withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
                           "TESTCONTAINERS_RYUK_DISABLED=true"]) {
                    def opts = buildSlowTestMavenOptions('nexus-mtiq-server')
                    mvnDirectForTests(opts, 'surefire:test failsafe:integration-test failsafe:verify')
                  }
                }
              }
            }
          }
        }
      }
    }

    stage('Collect Results') {
      when {
        expression { return true } // Always run to collect results even if tests fail
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            // Unstash JaCoCo data from distributed test agents
            // Exec files go into build dir so report-aggregate can find them
            // XML files go into separate directories for recordCoverage to merge
            def distributedStages = [
                'Postgres + MTIQ Tests',
                'Failsafe Tests 1 (A)',
                'Failsafe Tests 2 (B-L)',
                'Failsafe Tests 3 (M-R)',
                'Failsafe Tests 4 (S-Z)',
                'Playwright Functional Tests'
            ]
            distributedStages.each { stageName ->
              String stashName = "jacoco-${stageName.replaceAll('[^a-zA-Z0-9]', '-')}"
              try {
                // Unstash exec files directly into build dir (report-aggregate needs them in place)
                unstash stashName
                // Also unstash into separate dir for XML aggregation (avoid overwrites)
                dir("jacoco-results/${stashName}") { unstash stashName }
              } catch (e) {
                echo "No JaCoCo coverage data from ${stageName}: ${e.message}"
              }
            }

            junit(
                testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, ' +
                    '**/target/karma-reports/*.xml, **/target/jest-reports/*.xml',
                allowEmptyResults: true,
                healthScaleFactor: 1.0
            )
            archiveArtifacts(
                artifacts: '**/target/*-reports/**',
                excludes: '**/TEST-*.xml, **/*-output.txt',
                allowEmptyArchive: true,
                fingerprint: false
            )
            // Archive JaCoCo coverage data files for offline analysis
            archiveArtifacts(
                artifacts: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml',
                allowEmptyArchive: true,
                fingerprint: false
            )
            // Single recordCoverage call merges XML reports from all distributed agents + slow tests
            recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']])
          }
        }
      }
    }

    stage('Build MTIQ Image') {
      when {
        expression {
          return params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq')
        }
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            def imageVersion = mtiqImageVersion()
            def shouldPush = params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq')
            pushMTIQDockerImage(shouldPush, imageVersion)
            env.MTIQ_BUILD_SUCCEEDED = 'true'
          }
        }
      }
    }

    stage('Push to ECR Dev') {
      when {
        expression {
          return env.MTIQ_BUILD_SUCCEEDED == 'true' &&
            (params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq'))
        }
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            try {
              def ecr = load 'jenkins/ecr-helpers.groovy'
              ecr.pushToEcrCached()  // branch auto-detected via gitBranch(env)
            } catch (Exception e) {
              echo "WARNING: ECR push failed (non-fatal): ${e.message}"
            }
          }
        }
      }
    }
  }

  post {
    always {
      echo 'Pipeline completed'
    }
  }
}

// =============================================================================
// Helper Functions
// =============================================================================

void configureBranchJob() {
  String projName = currentBuild.fullProjectName
  boolean mtiqImagePushEnabledByDefault = (projName.toLowerCase().endsWith('/main') || projName.endsWith('_mtiq'))

  List params = [
      choice(
          name: 'buildMode',
          choices: ['FEATURE', 'MAIN'],
          description: 'Build mode: FEATURE excludes SlowTest and uses build caching. ' +
              'MAIN runs all tests including SlowTest, no caching. ' +
              'Both modes run the Playwright functional/UI sanity suite. ' +
              'Changing this requires a new build to take effect.'
      ),
      booleanParam(defaultValue: false,
          description: 'If checked will run policy evaluation with full reachability analysis (same as Main)',
          name: 'policyEvaluationEnabled'),
      booleanParam(defaultValue: mtiqImagePushEnabledByDefault,
          description: 'If checked will push the MTIQ Docker image to RSC for this branch (not available on Main)',
          name: 'mtiqImagePushEnabled'),
      booleanParam(defaultValue: false,
          description: 'If checked will run ReferencePolicyImportIntegrationTest for snapshot builds of feature ' +
              'branches',
          name: 'runRefPolicyImportIntTest'),
      booleanParam(defaultValue: false,
          description: 'If checked will create bundled artifacts (shade, jreleaser, MTIQ assembly). Required for ' +
              'releases and MTIQ image push.',
          name: 'bundlingEnabled'),
      booleanParam(defaultValue: false,
          description: 'If checked will include slow tests (tests taking >100 seconds)',
          name: 'includeSlowTests'),

      booleanParam(name: 'skipPlaywrightTests', defaultValue: false,
          description: 'Skip: Playwright Functional Tests (insight-brain-playwright-test)'),
      choice(name: 'playwrightTraceMode', choices: ['on-failure', 'always', 'off'],
          description: 'Playwright trace: on-failure (routine CI); always when debugging flakes; off to disable.')
  ]

  def propertyList = [copyArtifactPermission("/${projName}"), parameters(params)]

  propertyList.add(disableConcurrentBuilds(abortPrevious: true))

  properties(propertyList)
}

Map getMavenBuildConfig() {
  def opts = []

  // Base options
  opts << "--no-transfer-progress"
  opts << "-T 1C"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Skip options for faster builds
  opts << "-DskipTests"
  opts << "-Dspotless.apply.skip=true"
  opts << "-Dspotless.check.skip=true"
  opts << "-Dsource.skip=true"
  opts << "-Dmaven.javadoc.skip=true"
  // Note: Do NOT skip install - forked test processes need artifacts in local repo

  // Bundling
  if (!isBundlingEnabled()) {
    opts << "-Dshade.skip=true"
    opts << "-DskipAssembly=true"
  }

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for frontend install stage (node/yarn/npm dependencies)
Map getFrontEndInstallConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for frontend test stages (Jest)
Map getFrontEndTestConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for MTIQ Docker image build (insight-brain module with jdks profile)
Map getMtiqBuildConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl :insight-brain"
  opts << "-Pjdks"
  opts << "-Pjenkins"
  opts << "-DskipTests"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for MTIQ jreleaser assembly
Map getMtiqAssembleConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl :nexus-mtiq-server"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

/**
 * Execute Maven directly using withMaven, bypassing the shared library mvn helper.
 * This gives us control over JAVA_TOOL_OPTIONS to prevent the Jenkins Maven Event Spy
 * from being loaded into forked test JVMs.
 */
void mvnDirectForTests(String mavenOptions, String goals) {
  String localRepo = "${env.WORKSPACE}/.zion/repository"
  // This file has config for sonatype.repo.sonatype.app now
  String npmConfigFileId = 'rsc-ro-npmrc'

  configFileProvider([configFile(fileId: npmConfigFileId, variable: 'NPM_CONFIG_USERCONFIG')]) {
    withMaven(jdk: 'OpenJDK 25', maven: 'Maven 3.9.x', mavenSettingsConfig: 'private-settings.xml',
        publisherStrategy: 'EXPLICIT') {
      String mvnCmdLine = "mvn jacoco:prepare-agent ${goals} jacoco:report -Dmaven.repo.local='${localRepo}'"
      mvnCmdLine += " -DnpmRegistryURL="
      mvnCmdLine += " -DyarnDownloadRoot=https://sonatype.repo.sonatype.app/repository/yarn-binaries/"
      mvnCmdLine += " -DnodeDownloadRoot=https://sonatype.repo.sonatype.app/repository/nodejs-dist/"
      mvnCmdLine += " -DnpmDownloadRoot=https://sonatype.repo.sonatype.app/repository/npm-all/npm/-/"
      mvnCmdLine += " -DserverId=sonatype.repo.sonatype.app"
      mvnCmdLine += " ${mavenOptions}"

      sh mvnCmdLine
    }
  }
}

/**
 * Build Maven options string for SlowTest category tests.
 */
String buildSlowTestMavenOptions(String moduleList) {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl '${moduleList}'"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Test configuration
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"
  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"

  // Include ONLY SlowTest category
  opts << "-Dgroups=SlowTest"

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

String mtiqImageVersion() {
  def dateSection = new Date().format("yyyyMMddHHmm", TimeZone.getTimeZone('UTC'))
  def buildNumSection = env.BUILD_NUMBER
  def gitShortHashSection = env.GIT_COMMIT.take(8)

  String branch = gitBranch(env).replaceAll(/[^\w.-]/, '_').take(30)
  return "branch-${branch}-${env.BUILD_NUMBER}"
}

void pushMTIQDockerImage(boolean pushMtiqImage, String imageVersion) {
  if (currentBuild.fullProjectName.contains("insight-brain/release")) {
    echo 'Skipping MTIQ docker image for IQ on-premise release'
    return
  }

  echo "MTIQ image version: ${imageVersion}"
  String iqVersion = getMavenProjectVersion('.')
  echo "iqVersion:'${iqVersion}'"

  mvn getMtiqBuildConfig(), 'jdks:setup-jdks install'
  mvn getMtiqAssembleConfig(), 'jreleaser:assemble'

  dir("nexus-mtiq-server") {
    withSonatypeDockerRegistry() {
      echo "pushMtiqImage: $pushMtiqImage"
      def pushOption = pushMtiqImage ? " --push " : ""

      sh "docker buildx create --use --driver-opt image=${sonatypeDockerRegistryId()}/moby/buildkit"
      sh """docker buildx build --platform=linux/amd64,linux/arm64 \
            --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} \
            --build-arg IQ_SERVER_VERSION=${iqVersion} \
            ${pushOption} \
            --tag ${sonatypeDockerRegistryId()}/mtiq/server:${imageVersion} ."""
    }
  }
}

boolean isBundlingEnabled() {
  return params.bundlingEnabled
}

boolean shouldRunPolicyEvaluation() {
  return params.policyEvaluationEnabled
}

/**
 * Stash the minimum artifacts required to run tests on a remote agent.
 *
 * This creates a stash containing:
 * 1. SNAPSHOT JARs from local Maven repository (~260MB)
 * 2. Compiled test classes from target directories (~70MB)
 * 3. pom.xml files for Maven project structure
 *
 * Remote agents can unstash these and run tests without rebuilding.
 * External dependencies are resolved from Nexus (already cached on agents).
 */
void stashTestArtifacts() {
  def version = getMavenProjectVersion('.')
  def stashDir = "${env.BUILD_DIR}/.test-stash"

  echo "Stashing test artifacts for distributed testing..."
  echo "  Project version: ${version}"
  echo "  Build dir: ${env.BUILD_DIR}"
  echo "  Workspace: ${env.WORKSPACE}"

  // Create a minimal .m2 structure with only SNAPSHOT artifacts
  // Use parallel copies for speed
  sh """
    set -e
    rm -rf ${stashDir}
    mkdir -p ${stashDir}/m2-snapshot
    mkdir -p ${stashDir}/test-classes

    # Find the local Maven repository - check multiple possible locations
    SNAPSHOT_SRC=""
    for repo_path in "${env.WORKSPACE}/.zion/repository" "${env.WORKSPACE}/.m2/repository" "\$HOME/.m2/repository"; do
      if [ -d "\$repo_path/com/sonatype/insight/brain" ]; then
        SNAPSHOT_SRC="\$repo_path/com/sonatype/insight/brain"
        echo "  Found SNAPSHOT artifacts in: \$repo_path"
        break
      fi
    done

    if [ -z "\$SNAPSHOT_SRC" ]; then
      echo "ERROR: Could not find insight-brain artifacts in any Maven repository location"
      exit 1
    fi

    SNAPSHOT_DST="${stashDir}/m2-snapshot/com/sonatype/insight/brain"

    # Parallel copy of SNAPSHOT modules (run all copies in background, then wait)
    echo "Copying SNAPSHOT artifacts in parallel..."
    pids=""
    copied_count=0
    for module_dir in \$SNAPSHOT_SRC/*/; do
      module_name=\$(basename "\$module_dir")
      version_dir="\${module_dir}${version}"
      if [ -d "\$version_dir" ]; then
        mkdir -p "\$SNAPSHOT_DST/\$module_name"
        cp -r "\$version_dir" "\$SNAPSHOT_DST/\$module_name/" &
        pids="\$pids \$!"
        copied_count=\$((copied_count + 1))
      fi
    done
    # Wait for all SNAPSHOT copies to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Copied \$copied_count SNAPSHOT module(s)"

    if [ \$copied_count -eq 0 ]; then
      echo "ERROR: No SNAPSHOT artifacts found for version ${version}"
      exit 1
    fi

    # Parallel copy of classes and test-classes directories
    echo "Copying classes/test-classes in parallel..."
    pids=""
    for module in insight-brain-service insight-brain-db insight-brain-data insight-brain-policy \\
                  insight-brain-common insight-brain-event insight-brain-tenancy nexus-mtiq-server \\
                  insight-brain-playwright-test; do
      mkdir -p "${stashDir}/test-classes/\$module/target"
      if [ -d "\$module/target/test-classes" ]; then
        cp -r "\$module/target/test-classes" "${stashDir}/test-classes/\$module/target/" &
        pids="\$pids \$!"
      fi
      if [ -d "\$module/target/classes" ]; then
        cp -r "\$module/target/classes" "${stashDir}/test-classes/\$module/target/" &
        pids="\$pids \$!"
      fi
    done
    # Wait for all class copies to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Copied classes directories"

    # Note: Frontend module not stashed - frontend tests run on main agent with cached node/yarn

    # Copy pom.xml files (fast, no need to parallelize)
    find . -name 'pom.xml' -not -path '*/target/*' -not -path '*/.test-stash/*' | while read pom; do
      dir=\$(dirname "\$pom")
      mkdir -p "${stashDir}/test-classes/\$dir"
      cp "\$pom" "${stashDir}/test-classes/\$dir/"
    done

    # Report sizes
    echo ""
    echo "=== Stash sizes ==="
    du -sh ${stashDir}/m2-snapshot || echo "m2-snapshot: empty"
    du -sh ${stashDir}/test-classes || echo "test-classes: empty"
  """

  // Create single combined stash (faster than two separate stashes)
  dir(stashDir) {
    stash name: 'test-artifacts', includes: '**'
  }

  echo "Test artifacts stashed successfully."
}

/**
 * Unstash and set up test artifacts on a remote agent.
 *
 * This restores the stashed artifacts and configures the Maven local repository
 * to include the SNAPSHOT artifacts from the build agent.
 *
 * @return String The path to use for maven.repo.local
 */
String unstashTestArtifacts() {
  def localRepo = "${env.WORKSPACE}/.m2/repository"

  echo "Unstashing test artifacts on remote agent..."

  // Unstash the combined test artifacts
  unstash 'test-artifacts'

  // Move artifacts into place using parallel operations
  sh """
    set -e
    mkdir -p ${localRepo}

    # Start parallel restore operations
    pids=""

    # Merge the SNAPSHOT artifacts into the local repo (background)
    if [ -d "m2-snapshot/com" ]; then
      cp -r m2-snapshot/com ${localRepo}/ &
      pids="\$pids \$!"
    fi

    # Restore classes/test-classes to their original locations (background)
    if [ -d "test-classes" ]; then
      (
        cd test-classes
        find . -type d -name 'target' | while read target_dir; do
          module_dir=\$(dirname "\$target_dir")
          mkdir -p "${env.WORKSPACE}/\$module_dir"
          cp -r "\$target_dir" "${env.WORKSPACE}/\$module_dir/"
        done
        find . -name 'pom.xml' | while read pom; do
          dir=\$(dirname "\$pom")
          mkdir -p "${env.WORKSPACE}/\$dir"
          cp "\$pom" "${env.WORKSPACE}/\$dir/"
        done
      ) &
      pids="\$pids \$!"

      # Restore frontend module (node, node_modules, src)
      if [ -d "test-classes/insight-brain-frontend" ]; then
        mkdir -p "${env.WORKSPACE}/insight-brain-frontend"
        for dir in node node_modules src; do
          if [ -d "test-classes/insight-brain-frontend/\$dir" ]; then
            cp -r "test-classes/insight-brain-frontend/\$dir" "${env.WORKSPACE}/insight-brain-frontend/" &
            pids="\$pids \$!"
          fi
        done
      fi
    fi

    # Wait for all restore operations to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Restored all artifacts"

    # Cleanup temp directories
    rm -rf m2-snapshot test-classes

    echo ""
    echo "=== Local repo size ==="
    du -sh ${localRepo}
  """

  echo "Test artifacts restored successfully."
  return localRepo
}

/**
 * Run static analysis (Spotless, License Check) on a distributed agent.
 *
 * This runs on a fresh checkout - no unstash needed since static analysis
 * only requires source code, not compiled artifacts.
 */
void runDistributedStaticAnalysis() {
  echo "Running distributed static analysis..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  // Run Spotless check and License Check
  // These only need source code, not compiled artifacts
  mvnDirectForDistributedTests(buildStaticAnalysisMavenOptions(),
      'spotless:check license:check',
      localRepo)
}

/**
 * Build Maven options for distributed static analysis.
 */
String buildStaticAnalysisMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 2"
  opts << "-Ppre-check"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Disable build cache for static analysis
  opts << "-Dmaven.build.cache.enabled=false"
  opts << "-Dmaven.build.cache.remote.enabled=false"
  opts << "-Dmaven.build.cache.remote.save.enabled=false"

  return opts.join(' ')
}

/**
 * Run frontend tests (Jest) on a distributed agent.
 *
 * This function:
 * 1. Unstashes the build artifacts from the main build agent
 * 2. Runs Jest tests
 */
void runDistributedFrontendTests() {
  echo "Running distributed frontend tests..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  // Run Jest tests using Maven
  def opts = buildDistributedFrontendTestMavenOptions()
  mvnDirectForDistributedTests(opts,
      'com.github.eirslett:frontend-maven-plugin:yarn@jest',
      localRepo)
}

/**
 * Build Maven options for distributed frontend test execution.
 */
String buildDistributedFrontendTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return opts.join(' ')
}

/**
 * Run backend tests (Surefire + MTIQ + Postgres) on a distributed agent.
 *
 * This function:
 * 1. Unstashes the build artifacts from the main build agent
 * 2. Runs surefire tests (excluding PostgresTestCategory)
 * 3. Runs postgres tests (PostgresTestCategory only)
 * 4. Runs MTIQ tests
 */
void runDistributedBackendTests() {
  echo "Running distributed backend tests (Surefire + MTIQ + Postgres)..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {

      // Run surefire tests (excluding PostgresTestCategory)
      echo "Running Surefire tests..."
      def surefireOpts = buildDistributedSurefireTestMavenOptions()
      mvnDirectForDistributedTests(surefireOpts, 'surefire:test', localRepo)

      // Run postgres tests (PostgresTestCategory only)
      echo "Running Postgres tests..."
      def postgresOpts = buildDistributedPostgresTestMavenOptions()
      mvnDirectForDistributedTests(postgresOpts, 'surefire:test failsafe:integration-test failsafe:verify', localRepo)

      // Run MTIQ tests
      echo "Running MTIQ tests..."
      def mtiqOpts = buildDistributedMtiqTestMavenOptions()
      mvnDirectForDistributedTests(mtiqOpts, 'surefire:test failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/**
 * Build Maven options for distributed surefire test execution.
 */
String buildDistributedSurefireTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl '!insight-brain-frontend,!nexus-mtiq-server'"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Test configuration
  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"

  // Excluded test groups - exclude PostgresTestCategory (run separately) and SlowTest
  def excludedGroups = ['PostgresTestCategory']
  if (!params.includeSlowTests) {
    excludedGroups << "SlowTest"
  }
  if (!params.runRefPolicyImportIntTest) {
    excludedGroups << "ReferencePolicyImportIntegrationTest"
  }
  opts << "-DexcludedGroups=${excludedGroups.join(',')}"

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Build Maven options for distributed postgres test execution.
 */
String buildDistributedPostgresTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl '!insight-brain-frontend,!nexus-mtiq-server'"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Test configuration
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"
  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"

  // Include ONLY PostgresTestCategory
  opts << "-Dgroups=PostgresTestCategory"

  // Excluded test groups
  def excludedGroups = []
  if (!params.includeSlowTests) {
    excludedGroups << "SlowTest"
  }
  if (!params.runRefPolicyImportIntTest) {
    excludedGroups << "ReferencePolicyImportIntegrationTest"
  }
  if (excludedGroups) {
    opts << "-DexcludedGroups=${excludedGroups.join(',')}"
  }

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Build Maven options for distributed MTIQ test execution.
 */
String buildDistributedMtiqTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl 'nexus-mtiq-server'"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Test configuration
  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  // Excluded test groups
  def excludedGroups = []
  if (!params.includeSlowTests) {
    excludedGroups << "SlowTest"
  }
  if (!params.runRefPolicyImportIntTest) {
    excludedGroups << "ReferencePolicyImportIntegrationTest"
  }
  if (excludedGroups) {
    opts << "-DexcludedGroups=${excludedGroups.join(',')}"
  }

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Run failsafe tests on a distributed agent.
 *
 * This function:
 * 1. Unstashes the build artifacts from the main build agent
 * 2. Runs failsafe tests matching the specified pattern
 *
 * @param testPattern The test pattern to run (e.g., '[A-G]*Test.java')
 */
void runDistributedFailsafeTests(String testPattern) {
  echo "Running distributed failsafe tests with pattern: ${testPattern}"
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  // Debug: show what we have after unstash
  sh """
    echo "=== Workspace contents after unstash ==="
    ls -la ${env.WORKSPACE}/
    echo ""
    echo "=== Module directories ==="
    ls -la ${env.WORKSPACE}/insight-brain-service/ 2>/dev/null || echo "insight-brain-service not found"
    echo ""
    echo "=== Test classes check ==="
    ls -la ${env.WORKSPACE}/insight-brain-service/target/test-classes/ 2>/dev/null | head -10 || echo "No test-classes found"
    echo ""
    echo "=== Pom files ==="
    find ${env.WORKSPACE} -name 'pom.xml' -not -path '*/.m2/*' | head -10
  """

  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {

      def opts = buildDistributedTestMavenOptions(['PostgresTestCategory'],
          '!insight-brain-frontend,!nexus-mtiq-server', testPattern)
      mvnDirectForDistributedTests(opts, 'failsafe:integration-test failsafe:verify', localRepo)
    }
  }

  // Debug: show test reports
  sh """
    echo "=== Test reports ==="
    find ${env.WORKSPACE} -path '*/failsafe-reports/*.xml' 2>/dev/null | head -10 || echo "No failsafe reports found"
  """
}

/**
 * Build Maven options for distributed test execution.
 */
String buildDistributedTestMavenOptions(List<String> additionalExcludedGroups, String moduleList,
    String testPattern) {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl '${moduleList}'"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Test pattern - only run tests matching this pattern
  // Using %regex[...] syntax (same as Jenkinsfile.main)
  opts << "-Dit.test=%regex[${testPattern}]"

  // Test configuration
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  // Excluded test groups
  def excludedGroups = [] + additionalExcludedGroups
  if (!params.includeSlowTests) {
    excludedGroups << "SlowTest"
  }
  if (!params.runRefPolicyImportIntTest) {
    excludedGroups << "ReferencePolicyImportIntegrationTest"
  }
  if (excludedGroups) {
    opts << "-DexcludedGroups=${excludedGroups.join(',')}"
  }

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Execute Maven for distributed tests with explicit local repo.
 */
void mvnDirectForDistributedTests(String mavenOptions, String goals, String localRepo) {
  // This file has config for sonatype.repo.sonatype.app now
  String npmConfigFileId = 'rsc-ro-npmrc'

  configFileProvider([configFile(fileId: npmConfigFileId, variable: 'NPM_CONFIG_USERCONFIG')]) {
    withMaven(jdk: 'OpenJDK 25', maven: 'Maven 3.9.x', mavenSettingsConfig: 'private-settings.xml',
        publisherStrategy: 'EXPLICIT') {
      String mvnCmdLine = "mvn jacoco:prepare-agent ${goals} jacoco:report -Dmaven.repo.local='${localRepo}'"
      mvnCmdLine += " -DnpmRegistryURL="
      mvnCmdLine += " -DyarnDownloadRoot=https://sonatype.repo.sonatype.app/repository/yarn-binaries/"
      mvnCmdLine += " -DnodeDownloadRoot=https://sonatype.repo.sonatype.app/repository/nodejs-dist/"
      mvnCmdLine += " -DnpmDownloadRoot=https://sonatype.repo.sonatype.app/repository/npm-all/npm/-/"
      mvnCmdLine += " -DserverId=sonatype.repo.sonatype.app"
      mvnCmdLine += " ${mavenOptions}"

      sh mvnCmdLine
    }
  }
}

/**
 * Run the Playwright UI sanity suite (insight-brain-playwright-test) on a distributed agent.
 * Runs only tests tagged with @Category(SanityTest) via the -Psanity profile.
 */
void runDistributedPlaywrightTests() {
  echo "Running distributed Playwright tests..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {

      def opts = buildPlaywrightTestMavenOptions()
      mvnDirectForDistributedTests(opts, 'failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for Playwright sanity test execution. */
String buildPlaywrightTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl 'insight-brain-playwright-test'"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-Psanity"
  opts << "-Dit.test='%regex[.*Test.class]'"
  opts << "-DdetectTestEntityLeaks"

  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  opts << "-Dplaywright.trace=${params.playwrightTraceMode ?: 'on-failure'}"

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}
