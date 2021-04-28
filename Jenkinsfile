/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

make(
    useEventSpy: false,
    javaVersion: 'Java 8',
    mavenVersion: 'Maven 3.6.x',
    mavenOptions: '-D skipTests -D skip-functional-test',
    downstreamJobName: 'extra-tests',
    artifactsForDownstream: '.zion/repository/com/sonatype/insight/brain/**',
    runFeatureBranchPolicyEvaluations: true,
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
    },
    onUnstable: {
        pushDockerImageIfDeployBranch()
    }
)

def pushDockerImageIfDeployBranch() {
    //If the branch isn't master or the project name isn't snapshot, skip the image build and deploy.
    if (!isDeployBranch(env, 'master') || !currentBuild.fullProjectName.contains("snapshot")) {
        echo 'Skipping push of docker image for non-deploy branch or release'
        return
    }
    def version = getMavenProjectVersion('.')
    dir("nexus-iq-server") {
        withSonatypeDockerRegistry() {
            def shortImage = "iq/snapshot:${version.split("-")[0]}-${env.BUILD_NUMBER}"
            sh "docker build --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --build-arg IQ_SERVER_VERSION=${version} --tag ${shortImage} ."
            def fullImage = "${sonatypeDockerRegistryId()}/${shortImage}"
            def latest = "${sonatypeDockerRegistryId()}/iq/snapshot:latest"
            runSafely "docker tag ${shortImage} ${fullImage}"
            runSafely "docker push ${fullImage}"
            // Also tag as latest
            runSafely "docker tag ${shortImage} ${latest}"
            runSafely "docker push ${latest}"
        }
    }
}
