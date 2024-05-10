/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _
import hudson.plugins.git.GitChangeSet
import hudson.scm.ChangeLogSet
import hudson.scm.ChangeLogSet.Entry

configureBranchJob()
make(
    // 2024-05-03: We are using c6i.2xlarge EC2 instances. I tried c6i.4xlarge with "--threads 8" and there was no difference.
    //agentLabel: 'iq-large'
    deployBranch: 'main',
    useEventSpy: false,
    javaVersion: 'OpenJDK 17',
    mavenVersion: 'Maven 3.9.x',
    mavenSettingsFile: 'private-settings-build-cache',
    mavenOptions: "-D skip-functional-test -D build.number=${env.BUILD_NUMBER} --threads 4",
    retentionPolicy: RetentionPolicy.FOUR_WEEKS_KEEP_ARTIFACTS,
    prepare: {
      if (currentBuild.fullProjectName.toLowerCase().contains('insight/insight-brain/master-snapshot')) {
        String fixVersion = 'brain-next'
        List<String> newFixVersions = ['saas-next']
        echo "Replacing '${fixVersion}' with [${newFixVersions.join(', ')}]"
        List<String> issues = getIssuesByFixVersion('CLM', fixVersion)
        issues.addAll(getIssuesByFixVersion('SDEV', fixVersion))
        issues.addAll(getIssuesByFixVersion('INT', fixVersion))
        issues.addAll(getIssuesByFixVersion('NEXUS', fixVersion))
        replaceFixVersionForIssues(issues, fixVersion, newFixVersions)
      }
    },
    snapshotBuildAndTest: { Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J ->
      echo "Using mavenVersion='${mavenCommon.get('mavenVersion')}'"

      /*
        Main is configured not to use the cache. We also skip tests on main because it runs the tests in parallel stages
        which requires copying the workspace. With cache enabled its actually faster to run the compilation and tests
        in a single stage.
       */
      String mavenOptions = mavenCommon.get('mavenOptions')
      if (isFastBuild()) {
        mavenOptions = addBuildCacheOptions(mavenOptions)
      }
      else {
        mavenOptions += " -DskipTests"
      }
      mavenCommon.put('mavenOptions', mavenOptions)

      withSonatypeDockerRegistry() {
        withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/"]) {
          runAllTests(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
        }
      }
    },
    releaseBuild: { Map<String, ?> mavenCommon, String keystoreCredentialsId, boolean useInstall4J ->
      withSonatypeDockerRegistry() {
        withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/"]) {
          buildAndSkipTests(mavenCommon, keystoreCredentialsId, false, useInstall4J)
        }
      }
    },
    releaseFromCommit: true,
    snapshotProjectName: 'insight/insight-brain/master-snapshot',
    githubProjectUrl: 'git@github.com:sonatype/insight-brain.git',
    runFeatureBranchPolicyEvaluations: true,
    iqPolicyEvaluation: { stage ->
        if (shouldRunPolicyEvaluation()) {
          nexusPolicyEvaluation iqStage: stage, iqApplication: 'insight-brain',
              iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
              //Test files inside the maven modules are excluded from the scan
              iqModuleExcludes: [[moduleExclude: '**/test/**'], [moduleExclude: '**/test-classes/**/module.xml']],
              failBuildOnNetworkError: true

          if (isSASTEnabled()) {
            runSastScan()
          }

          if (stage == 'release') {
            build(job: 'bnr/lifecycle-for-sonatype/generate-attribution-report',
                  parameters: [
                    string(name: 'applicationId', value: 'insight-brain'),
                    string(name: 'applicationName', value: 'Nexus Lifecycle'),
                    string(name: 'applicationVersion', value: params.version)
                  ]
              )
              copyArtifacts filter: "*insight-brain-${params.version}*.html",
                  projectName: 'bnr/lifecycle-for-sonatype/generate-attribution-report'
            }
          }
    },
    distFiles: [
      includes: [
        'nexus-iq-server/target/*.zip*',
        'nexus-iq-server/target/*.tar.gz*',
        'nexus-iq-cli/target/*.jar*',
        'nexus-iq-diagnostics/target/*.jar*',
        'nexus-mtiq-server/target/*.tar.gz*',
        "*insight-brain-${params.version}*.html"
      ],
      excludes: [
        '**/*-sources.jar*',
        '**/*-javadoc.jar*',
        '**/original-*'
      ]
    ],
    usePMD: true,
    useCheckstyle: true,
    releaseRetentionPolicy: RetentionPolicy.TEN_BUILDS,
    onSuccess: {
        postBuild()
    },
    onUnstable: {
        postBuild()
    }
)

void postBuild() {
  if (!isFastBuild()) {
    pushDockerImageIfDeployBranch()
    pushMTIQDockerImage()
  }
}

void configureBranchJob() {
  // Use the project name to determine the branch
  String projName = currentBuild.fullProjectName
  boolean mtiqImagePushEnabledByDefault = (projName.toLowerCase().contains('master') || projName.endsWith('_mtiq'))

  List params = [
      booleanParam(defaultValue: true,
          description: 'If checked will skip Slow Tests.',
          name: 'fastBuild'),
      booleanParam(defaultValue: mtiqImagePushEnabledByDefault,
          description: 'If checked will push the MTIQ Docker image to RSC for this branch',
          name: 'mtiqImagePushEnabled'),
      booleanParam(defaultValue: false,
                description: 'If checked will enable SAST analysis on Evaluate Policy step',
                name: 'sastAnalysisEnabled')
      ]

  // Jenkins unfortunately will overwrite any parameters defined at the folder level using this dynamic approach for
  // applitools. Therefore in order to support this workflow we need to mirror folder defined parameters here otherwise
  // they are erased completely from the release configuration.
  // See https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/release/
  if (projName.contains('insight/insight-brain/release')) {
    params += [
        booleanParam(name: 'hotfix',
            description: 'Check if this IQ Release is intended to be a hotfix e.g. urgent release to fix a severe bug' +
                '. (Used for internal metrics only.)'),
        stringParam(name: 'version', description: 'The version to release'),
        stringParam(name: 'nextVersion',
            description: 'The next SNAPSHOT version to use after the release. Optional as will be automatically be ' +
                'calculated if left blank.'),
        run(name:'snapshotBuild', filter: 'SUCCESSFUL', projectName: 'insight/insight-brain/master-snapshot',
            description: 'The snapshot build to release from.')
    ]
  }
  properties([
      copyArtifactPermission("/${projName}"),
      parameters(params)
  ])
}

void pushDockerImageIfDeployBranch() {
    //If the git repo branch name isn't main or the project name isn't snapshot, skip the image build and deploy.
    if (!isDeployBranch(env, 'main') || !currentBuild.fullProjectName.contains("snapshot")) {
        echo 'Skipping push of docker image for non-deploy branch or release'
        return
    }

    String iqVersion = getMavenProjectVersion('.')
    String imageVersion = "${iqVersion.split("-")[0]}-${env.BUILD_NUMBER}"
    echo "iqVersion:'${iqVersion}'"
    echo "buildnum: ${env.BUILD_NUMBER}"

    String imageName = 'iq/snapshot'
    String fullImage = "${sonatypeDockerRegistryId()}/${imageName}:${imageVersion}"

    dir("nexus-iq-server") {
        withSonatypeDockerRegistry() {
            sh "docker build --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --build-arg " +
                "IQ_SERVER_VERSION=${iqVersion} --tag ${imageName}:${imageVersion} ."
            String latest = "${sonatypeDockerRegistryId()}/${imageName}:latest"
            runSafely "docker tag ${imageName}:${imageVersion} ${fullImage}"
            runSafely "docker push ${fullImage}"
            // Also tag as latest
            runSafely "docker tag ${imageName}:${imageVersion} ${latest}"
            runSafely "docker push ${latest}"
        }
    }

    // Trigger downstream jobs for IQ
    String targetImage = "${sonatypeDockerRegistryId()}/iq/staging:${imageVersion}"
    build('job': 'ops/sonatype-lifecycle/docker-ops-nexus-iq-server/staging',
          parameters: [
            string(name: 'BASE_IMAGE', value: fullImage),
            string(name: 'TARGET_IMAGE', value: targetImage),
          ],
          propagate: false)

    build('job': 'ops/sonatype-lifecycle/ops-terraform-ecs-iq-server/staging',
          parameters: [
            string(name: 'environment', value: 'Staging'),
            string(name:'imageUrl', value: targetImage)
          ],
          propagate: false)
}

void pushMTIQDockerImage() {
    // MTIQ image push rules:
    // - any snapshot build on the `main` branch (iq on-prem release builds should not be processed)
    // - any branch ending with `_mtiq`
    // - any branch run manually with the parameter to push selected
    // Note: there is a cleanup policy on RSC to purge old MTIQ feature branches images.

    boolean isMainBuild = isDeployBranch(env, 'main')

    if (currentBuild.fullProjectName.contains("insight-brain/release")) {
        echo 'Skipping MTIQ docker image for IQ on-premise release'
        return
    }

    // Default version for the MTIQ image off the `main` branch is in the format 202307111354-1234-ABCDEFGH

    // First part of MTIQ version number (202307111354 in the example) is an unformatted date up to the minute
    def dateSection = new Date().format("yyyyMMddHHmm", TimeZone.getTimeZone('UTC'))

    // Second part of the MTIQ version number (1234 in the example) is the Jenkins snapshot build number
    def buildNumSection = env.BUILD_NUMBER

    // Third part of the MTIQ version number (ABCDEFGH in the example) is the Git short hash
    def gitShortHashSection = env.GIT_COMMIT.take(8)

    def imageVersion = "${dateSection}-${buildNumSection}-${gitShortHashSection}"

    // If we are on a feature branch (i.e. not `main`), then we use the branch name in the version number
    // as well as prefixing it with `branch-` to allow for easy identification
    if (!isMainBuild) {
      // get branch name, max 20 characters
      String branch = gitBranch(env).replaceAll(/[^\w.-]/, '_').take(30)
      imageVersion = "branch-${branch}-${env.BUILD_NUMBER}"
    }

    echo "MTIQ image version: ${imageVersion}"

    dir("nexus-mtiq-server") {
      withSonatypeDockerRegistry() {
        String imageName = 'mtiq/server'
        String fullImage = "${sonatypeDockerRegistryId()}/${imageName}:${imageVersion}"

        sh "docker build --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --tag " +
            "${imageName}:${imageVersion} ."
        runSafely "docker tag ${imageName}:${imageVersion} ${fullImage}"

        // Push for all `main` builds as well as any enabled branches by name or build parameter
        def pushMtiqImage = params.mtiqImagePushEnabled == null
          ? (isMainBuild || projName.endsWith('_mtiq')) : params.mtiqImagePushEnabled
        echo "pushMtiqImage: $pushMtiqImage"

        if (pushMtiqImage) {
          runSafely "docker push ${fullImage}"
        }
      }
    }

    // Successful builds on the `main` branch trigger the MTIQ job to bump the image version in the K8S deployment
    def isSuccess = currentBuild.currentResult == 'SUCCESS'
    if (isMainBuild && isSuccess) {
      build('job': '/insight/MTIQ/bump-mtiq-version',
          parameters: [ string(name: 'DOCKER_IMAGE_VERSION', value: imageVersion), string(name: 'IQ_COMMIT', value:
              env.GIT_COMMIT) ],
          wait: false,
          propagate: false)
    }
}

void runAllTests(Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J) {
  if (isFastBuild()) {
    echo "fastBuild enabled - skipping slow tests"
    String mavenOptions = mavenCommon.get('mavenOptions')
    mavenOptions += " -DexcludedGroups=SlowTest"
    mavenOptions += " -Dskip-functional-test"
    mavenOptions += " -Djasmine.tests.skip=true"
    mavenCommon.put('mavenOptions', mavenOptions)
    buildAndTest(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
    archiveArtifacts(artifacts: '**/target/*-reports/**', excludes: '**/TEST-*.xml, **/*-output.txt')
    collectTestResults(['**/target/*-reports/*.xml'])
  }
  else {
    echo "fastBuild disabled - Running all tests"
    buildAndTest(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
    // archive things for the parallel blocks which will copy
    // these artifacts to different agents for each parallel block
    runSafely 'zip --symlinks -q -r workspace.zip .'
    archiveArtifacts(artifacts: 'workspace.zip', fingerprint: false)
    parallel(getParallelTests())
  }
}

private String addBuildCacheOptions(String mavenOptions) {
  mavenOptions += ' -Dmaven.build.cache.remote.enabled=true'
  mavenOptions += ' -Dmaven.build.cache.remote.url=https://repo.sonatype.com/repository/insight-brain-build-cache'
  mavenOptions += ' -Dmaven.build.cache.remote.server.id=insight-brain-build-cache'
  mavenOptions += " -Dmaven.build.cache.remote.save.enabled=true"

  return mavenOptions
}

Map<String, Closure> getParallelTests() {
  Map<String, Closure> testStages = [:]
  testStages << createGebTests()
  testStages << createFunctionalTests('Java Functional Tests A', '.*/[A-B].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests B', '.*/[C-E].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests C', '.*/[F-M].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests D', '.*/[N-O].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests E', '.*/[P-R].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests F', '.*/[S-Z].*Test.class')
  testStages << createFunctionalTests('MTIQ Functional Tests', '.*/.*Test.class', 'nexus-mtiq-functional-test')
  testStages << createFrontendTests('Frontend Tests - Jasmine/Jest')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 A', 'Java 8', '.*/[A-C].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 B', 'Java 8', '.*/[D-K].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 C', 'Java 8', '.*/[L-P].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 D', 'Java 8', '.*/[R-Z].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 A', 'OpenJDK 17', '.*/[A-C].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 B', 'OpenJDK 17', '.*/[D-K].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 C', 'OpenJDK 17', '.*/[L-P].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 D', 'OpenJDK 17', '.*/[R-Z].*Test.class')
  testStages << createMtiqUnitTests('MTIQ Unit and Integration Tests - OpenJDK 17', 'OpenJDK 17')

  return testStages
}

Map<String, Closure> createGebTests() {
  return ['Geb Tests': {
    // 2024-05-03: We are using c6i.2xlarge EC2 instances. I tried c6i.4xlarge and there was no difference.
    node(InsightConstants.AGENT_LABEL) {
      stage('Geb Tests') {
        try {
          copyRepo()
          String mavenOptions = "-Dgeb.env=ci -Drun-functional-tests=docker -Ddocker." +
              "registry=${sonatypeDockerRegistryId()} " +
              "-Dfailsafe.rerunFailingTestsCount=2 -Dfailsafe.failOnFlakeCount=5 --threads 4"
          Map<String, ?> testConfig = testConfig(mavenOptions, 'insight-brain-functional-test/pom.xml')
          // We just want to execute tests so directly invoke goals. Docker goal is needed.
          mvn testConfig, 'docker:start failsafe:integration-test failsafe:verify docker:stop'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createFunctionalTests(
  String stageName,
  String regex,
  String mavenModule = 'insight-brain-java-functional-test'
) {
  return ["${stageName}": {
    node('iq-large') {
      stage(stageName) {
        try {
          withEnv(["APPLITOOLS_BATCH_ID=${env.GIT_COMMIT}"]) {
            copyRepo()
            withCredentials([string(credentialsId: 'APPLITOOLS_KEY', variable: 'applitoolsKey')]) {
              String mavenOptions = "'-Dit.test=%regex[${regex}]'"
              mavenOptions += ' -Drun-functional-tests=docker'
              mavenOptions += " -Dbrowser=chrome"
              mavenOptions += " -DapplitoolsKey=${applitoolsKey}"
              mavenOptions += " -DapplitoolsEnabled=true"
              mavenOptions += " -Ddocker.registry=${sonatypeDockerRegistryId()}"
              mavenOptions += " -DdetectTestEntityLeaks"
              mavenOptions += " -Dfailsafe.rerunFailingTestsCount=2"
              mavenOptions += " -Dfailsafe.failOnFlakeCount=5"
              mavenOptions += " --threads 4"
              Map<String, ?> testConfig = testConfig(mavenOptions, "${mavenModule}/pom.xml")
              // We just want to execute tests so directly invoke goals.
              mvn testConfig, 'failsafe:integration-test failsafe:verify'
            }
          }
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createFrontendTests(String stageName) {
  return ["${stageName}": {
    // 2024-05-03: We are using c6i.2xlarge EC2 instances. I tried c6i.4xlarge, there was a small difference (~2 mins),
    // but this stage is still faster then other parallel stages without using larger EC2 instances.
    node(InsightConstants.AGENT_LABEL){
      stage(stageName) {
        try {
          copyRepo()
          Map<String, ?> testConfig = testConfig(
              "-pl 'com.sonatype.insight.brain:insight-brain-frontend' " +
                  "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage --threads 4",
              null,  'OpenJDK 17')
          mvn testConfig, "com.github.eirslett:frontend-maven-plugin:yarn@jasmine " +
              "com.github.eirslett:frontend-maven-plugin:yarn@jest"
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createUnitTests(String stageName, String jdk, String regex) {
  return ["${stageName}": {
    node('iq-large'){
      stage(stageName) {
        try {
          copyRepo()
            Map<String, ?> testConfig = testConfig(
                // Note MTIQ & Frontend modules are excluded here as they run in their own stages
                "-pl '!nexus-mtiq-server' -pl '!insight-brain-frontend' " +
                "-Dtest=%regex[${regex}] -Dit.test=%regex[${regex}] " +
                "-Dskip-functional-test -DdetectTestEntityLeaks " +
                "-Dfailsafe.rerunFailingTestsCount=2 -Dfailsafe.failOnFlakeCount=5 " +
                "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage --threads 4",
                null, jdk)
          mvn testConfig, 'surefire:test failsafe:integration-test failsafe:verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createMtiqUnitTests(String stageName, String jdk) {
  return ["${stageName}": {
    node('iq-large'){
      stage(stageName) {
        try {
          copyRepo()
          Map<String, ?> testConfig = testConfig(
                "-pl com.sonatype.insight.brain:nexus-mtiq-server -Dskip-functional-test -DdetectTestEntityLeaks " +
                    "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage " +
                    "-Dfailsafe.rerunFailingTestsCount=2 -Dfailsafe.failOnFlakeCount=5 --threads 4",
                null, jdk)
          mvn testConfig, 'surefire:test failsafe:integration-test failsafe:verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, ?> testConfig(String mavenOptions, String pomFile = null, String javaVersion = 'OpenJDK 17') {
  return mavenCommon(javaVersion: javaVersion, mavenVersion: 'Maven 3.9.x', useEventSpy: false,
      pomFile: pomFile, mavenOptions: mavenOptions)
}

void copyRepo() {
  copyArtifacts(projectName: currentBuild.fullProjectName, filter: 'workspace.zip', selector: specific(currentBuild.id),
      flatten: false)
  runSafely 'unzip -q -o workspace.zip'
}

void captureResultsAndCleanup() {
  archiveArtifacts(artifacts: '**/target/*-reports/**', excludes: '**/TEST-*.xml, **/*-output.txt')
  collectTestResults(['**/target/*-reports/*.xml'])
  deleteDir()
}

void runSastScan() {
  pullAndExtractSastCli()
  runSastOnJars()
}

void pullAndExtractSastCli() {
  echo "Fetching and extracting SAST CLI"
  sonatypeZionGitConfig()
  sshagent(credentials: [sonatypeZionCredentialsId()]) {
    sh '''
      rm -rf sast-cli-dist
      git clone git@github.com:sonatype/sast-cli-dist.git
    '''
  }
  echo "Successfully extracted SAST CLI"
}

void runSastOnJars() {
  echo "Running SAST CLI on generated jars"
  echo "${pwd()}"
  sh "rm -rf ./bundle-jars && mkdir ./bundle-jars"
  def files = findFiles(glob: '**/nexus-iq-server*.tar.gz')
  files.each { f ->
    sh "tar -xvzf ${f.path} -C ./bundle-jars"
  }

  def bundleFiles = findFiles(glob: '**/bundle-jars/nexus-iq-*.jar')
  def sastFileParams = []

  bundleFiles.each { f ->
    sastFileParams.add(f.path)
  }

  echo "Detected jar files to analyze: ${sastFileParams.join(', ')}"
  withCredentials([usernamePassword(credentialsId: 'jenkins-saas-service-acct',
    usernameVariable: 'IQ_USERNAME', passwordVariable: 'IQ_PASSWORD')]) {
    runSafely "java -Xmx6g -Dspring.profiles.active=local_dev -jar ./sast-cli-dist/sast-cli.jar sast --include-packages=com.sonatype -a insight-brain --source . --host https://sonatype.sonatype.app/platform -u $IQ_USERNAME --password $IQ_PASSWORD ${sastFileParams.join(' ')}"
  }
  echo "Successfully ran SAST analysis on insight-brain bundle jars"
}

/**
 * Check to see if the SAST analysis should be enabled.
 * Can be overridden if a parameter has been defined and specified for the job.
 * @return true if enabled
 */
boolean isSASTEnabled() {
  // if the params value isn't set (or hasn't been added to the job yet), default to false
  return params.sastAnalysisEnabled ?: false
}

boolean isFastBuild() {
  return !isDeployBranch(env, 'main') && params.fastBuild
}

boolean shouldRunPolicyEvaluation() {
  return !isFastBuild() || hasDependenciesChanged()
}

boolean hasDependenciesChanged() {
  return currentBuild.changeSets?.find() { ChangeLogSet<? extends Entry> changeSet ->
    changeSet.items.find() { GitChangeSet item ->
      item.getAffectedPaths().find() { String path ->
        if (!path.contains('/test/data/') &&
            (path.contains('pom.xml')
            || path.contains('package.json')
            || path.contains('yarn.lock'))) {
          return true
        }
      }
    }
  }
}
