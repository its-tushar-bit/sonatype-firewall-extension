/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

configureBranchJob()
make(
    deployBranch: 'main',
    useEventSpy: false,
    javaVersion: 'Java 8',
    mavenVersion: 'Maven 3.6.x',
    mavenOptions: '-D skipTests -D skip-functional-test',
    snapshotBuildAndTest: { Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J ->
      runAllTests(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
    },
    releaseBuild: { Map<String, ?> mavenCommon, String keystoreCredentialsId, boolean useInstall4J ->
      buildAndSkipTests(mavenConfig, keystoreCredentialsId, false, useInstall4J)
    },
    releaseFromCommit: true,
    runFeatureBranchPolicyEvaluations: true,
    iqPolicyEvaluation: { stage ->
        nexusPolicyEvaluation iqStage: stage, iqApplication: 'insight-brain',
          iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
          failBuildOnNetworkError: true
    },
    distFiles: [
      includes: [
        'nexus-iq-server/target/*.zip*',
        'nexus-iq-server/target/*.tar.gz*',
        'nexus-iq-cli/target/*.jar*',
        'nexus-iq-diagnostics/target/*.jar*'
      ],
      excludes: [
        '**/*-sources.jar*',
        '**/*-javadoc.jar*',
        '**/*proguard*',
        '**/original-*'
      ]
    ],
    usePMD: true,
    useCheckstyle: true,
    releaseRetentionPolicy: RetentionPolicy.TEN_BUILDS,
    onSuccess: {
        pushDockerImageIfDeployBranch()
        saveGitCommitHashIfMainSnapshotBuild()
    },
    onUnstable: {
        pushDockerImageIfDeployBranch()
    },
    getGitCommitHash: { String snapshotBuildNumber ->
      return readBuildArtifact('insight/insight-brain/master-snapshot', snapshotBuildNumber, 'artifacts/git_commit_hash.txt')
    }
)

void configureBranchJob() {
  // Use the project name to determine the branch
  String projName = currentBuild.fullProjectName
  boolean applitoolsEnabledByDefault = (projName.toLowerCase().contains('master') || projName.endsWith('_ui'))
  List params = [booleanParam(defaultValue: applitoolsEnabledByDefault,
      description: 'If checked will enable Applitools EyesCheck.',
      name: 'applitoolsEnabled')]

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
        run(name:'snapshotBuild', filter: 'SUCCESSFUL', projectName: 'insight/insight-brain/master-snapshot', description: 'The snapshot build to release from.')
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

    String version = getMavenProjectVersion('.')
    String shortImage = "${version.split("-")[0]}-${env.BUILD_NUMBER}"
    String containerName = "iq/snapshot"
    echo "version:'${version}'"
    echo "buildnum: ${env.BUILD_NUMBER}"

    dir("nexus-iq-server") {
        withSonatypeDockerRegistry() {
            sh "docker build --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --build-arg IQ_SERVER_VERSION=${version} --tag ${containerName}:${shortImage} ."
            String fullImage = "${sonatypeDockerRegistryId()}/${containerName}:${shortImage}"
            String latest = "${sonatypeDockerRegistryId()}/${containerName}:latest"
            runSafely "docker tag ${containerName}:${shortImage} ${fullImage}"
            runSafely "docker push ${fullImage}"
            // Also tag as latest
            runSafely "docker tag ${containerName}:${shortImage} ${latest}"
            runSafely "docker push ${latest}"
        }
    }
    build('job': 'ops/sonatype-lifecycle/docker-ops-nexus-iq-server/staging',
              parameters: [string(
                name: 'IQ_VERSION', value: shortImage), string(name:'CONTAINER_NAME',
                value: containerName)], propagate: false)

    build('job': 'ops/sonatype-lifecycle/ops-terraform-ecs-iq-server/staging',
              parameters: [string(
                name: 'environment', value: 'Staging'), string(name:'imageUrl',
                value: "docker-all.repo.sonatype.com/${containerName}:latest")], propagate: false)
}

/*
 * Archive as a text file the git commit hash which triggered this build.
 *
 * For main snapshot builds only, store the git commit hash.
 */
void saveGitCommitHashIfMainSnapshotBuild() {
  if (isDeployBranch(env, 'main') && currentBuild.fullProjectName.contains('snapshot')) {
    String filename = 'target/dist/git_commit_hash.txt'
    writeFile file: filename, text: env.GIT_COMMIT
    archiveAdditionalArtifacts(filename)
  }
}

void runAllTests(Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J) {
  buildAndTest(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
  // archive things for the parallel blocks which will copy
  // these artifacts to different agents for each parallel block
  runSafely 'zip --symlinks -q -r workspace.zip .'
  archiveArtifacts(artifacts: 'workspace.zip', fingerprint: false)
  parallel(getParallelTests())
}

Map<String, Closure> getParallelTests() {
  Map<String, Closure> testStages = [:]
  testStages << createGebTests()
  testStages << createFunctionalTests('Java Functional Tests A', '.*/[A-C].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests B', '.*/[D-H].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests C', '.*/[I-R].*Test.class')
  testStages << createFunctionalTests('Java Functional Tests D', '.*/[S-Z].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 A', 'Java 8', '.*/[A-H].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 B', 'Java 8', '.*/[I-P].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - Java 8 C', 'Java 8', '.*/[Q-Z].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 11 A', 'OpenJDK 11', '.*/[A-H].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 11 B', 'OpenJDK 11', '.*/[I-P].*Test.class')
  testStages << createUnitTests('Unit and Integration Tests - OpenJDK 11 C', 'OpenJDK 11', '.*/[Q-Z].*Test.class')
  return testStages
}

Map<String, Closure> createGebTests() {
  return ['Geb Tests': {
    node(InsightConstants.AGENT_LABEL) {
      stage('Geb Tests') {
        try {
          copyRepo()
          String mavenOptions = "-Dgeb.env=ci -Drun-functional-tests=docker -Ddocker.registry=${sonatypeDockerRegistryId()}"
          Map<String, ?> testConfig = testConfig(mavenOptions, 'insight-brain-functional-test/pom.xml')
          mvn testConfig, 'verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createFunctionalTests(String stageName, String regex) {
  return ["${stageName}": {
    node(InsightConstants.AGENT_LABEL) {
      stage(stageName) {
        try {
          withEnv(["APPLITOOLS_BATCH_ID=${env.GIT_COMMIT}"]) {
            copyRepo()
            withCredentials([string(credentialsId: 'APPLITOOLS_KEY', variable: 'applitoolsKey')]) {
              String mavenOptions = "'-Dit.test=%regex[${regex}]'"
              mavenOptions += ' -Drun-functional-tests=docker'
              mavenOptions += " -Dbrowser=chrome"
              mavenOptions += " -DapplitoolsKey=${applitoolsKey}"
              mavenOptions += " -DapplitoolsEnabled=${isEyesEnabled()}"
              mavenOptions += " -Ddocker.registry=${sonatypeDockerRegistryId()}"
              Map<String, ?> testConfig = testConfig(mavenOptions, 'insight-brain-java-functional-test/pom.xml')
              mvn testConfig, 'verify'
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

Map<String, Closure> createUnitTests(String stageName, String jdk, String regex) {
  return ["${stageName}": {
    node(InsightConstants.AGENT_LABEL){
      stage(stageName) {
        try {
          copyRepo()
          Map<String, ?> testConfig = testConfig(
                "-Dtest=%regex[${regex}] -Dit.test=%regex[${regex}] -Dskip-functional-test " +
                    "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage",
                null, jdk)
          mvn testConfig, 'install'
        }
        finally {
          if (jdk == 'Java 8' && stageName == 'Unit & Integration Tests - Java 8 A') {
            sonarAnalyze(env: env, sonarAnalysisPullRequestsOnly: !currentBuild.fullProjectName.contains("master"))
          }
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, ?> testConfig(String mavenOptions, String pomFile = null, String javaVersion = 'Java 8') {
  return mavenCommon(javaVersion: javaVersion, mavenVersion: 'Maven 3.6.x', useEventSpy: false,
      pomFile: pomFile, mavenOptions: mavenOptions)
}

void copyRepo() {
  copyArtifacts(projectName: currentBuild.fullProjectName, filter: 'workspace.zip', selector: specific(currentBuild.id),
      flatten: false)
  runSafely 'unzip -q -o workspace.zip'
}

void captureResultsAndCleanup() {
  archiveArtifacts(artifacts: '**/target/*-reports/**', excludes: '**/*.xml, **/*-output.txt')
  collectTestResults(['**/target/*-reports/*.xml'])
  deleteDir()
}

/**
 * Check to see if the Eyes Check should be enabled.  Defaults to true for the 'main' and any branch that ends in '_ui'
 * Can be overridden if a parameter has been defined and specified for the job.
 * @return true if enabled
 */
boolean isEyesEnabled() {
  // use the project name to determine the branch, a git checkout hasn't happened yet.  Multi-branch builds use
  // the branch name as the last part of the project name.
  def projName = currentBuild.fullProjectName
  // if the params value isn't set (or hasn't been added to the job yet) use the branch name default)
  return params.applitoolsEnabled == null ? (projName.toLowerCase().contains('master') || projName.endsWith('_ui')) :
      params.applitoolsEnabled
}
