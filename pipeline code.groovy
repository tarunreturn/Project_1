pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                git branch: '$branch', url: 'https://github.com/tarunreturn/jenkins-java-project.git'
            }
        }
        stage('Maven Build') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Maven Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Artifact') {
            steps {
                sh 'mvn package'
            }
        }
        stage('Uploading Artifact to S3 for Backup') {
            steps {
                s3Upload consoleLogLevel: 'INFO', dontSetBuildResultOnFailure: false, dontWaitForConcurrentBuildCompletion: false, entries: [[
                    bucket: 'myartifact325',
                    excludedFile: '',
                    flatten: false,
                    gzipFiles: false,
                    keepForever: false,
                    managedArtifacts: false,
                    noUploadOnFailure: false,
                    selectedRegion: 'us-east-1',
                    showDirectlyInBrowser: false,
                    sourceFile: 'target/NETFLIX-1.2.2.war',
                    storageClass: 'STANDARD',
                    uploadFromSlave: false,
                    useServerSideEncryption: false
                ]], pluginFailureResultConstraint: 'FAILURE', profileName: 'S3', userMetadata: []
            }
        }
        stage('Uploading Artifact to Ansible Server') {
            steps {
                script {
                    sshagent(['ansible']) {
                        sh '''
                        scp -o StrictHostKeyChecking=no target/NETFLIX-1.2.2.war root@172.31.87.52:/root/artifact
                        '''
                    }
                }
            }
        }
        stage('Run Ansible Playbook') {
            steps {
                script {
                    sshagent(['ansible']) {
                        sh '''
                        ssh -o StrictHostKeyChecking=no root@172.31.87.52 'ansible-playbook /etc/ansible/deploy.yml'
                        '''
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(
                channel: '#ci-cd',
                message: "✅ Build and deployment of NETFLIX 1.2.2 was successful.",
                color: 'good'
            )
        }
        failure {
            slackSend(
                channel: '#ci-cd',
                message: "❌ Build or deployment of NETFLIX 1.2.2 failed.",
                color: 'danger'
            )
        }
    }
}
