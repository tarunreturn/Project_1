# Netflix Deployment using Jenkins, Ansible, and Terraform

## Objective
Deploy a Netflix-like application with an automated CI/CD pipeline using Jenkins, Ansible, S3 for artifact backup, and HCP Terraform for server creation. The deployment simulates a high-availability application scenario, aiming to streamline build, testing, and deployment for scalable operations.

<img src="https://i.imghippo.com/files/x4056TWw.png" alt="" border="0">

## Components and Tools
- **Jenkins**: CI/CD tool to automate the build, test, and deployment process.
- **Ansible**: Configuration management tool for automating application deployment.
- **AWS S3**: Used for storing and backing up build artifacts.
- **Terraform (HCP)**: Infrastructure as Code (IaC) tool for provisioning servers.
- **Maven**: Build automation tool for compiling and packaging Java projects.
- **Git**: Version control system for managing code repositories.
- **Prometheus**: Monitoring tool to collect and store metrics from servers.
- **Grafana**: Visualization tool for monitoring data with dashboards.
- **Node Exporter**: Agent installed on servers to collect metrics for Prometheus.

## Infrastructure Setup with HCP Terraform

### Provisioning
Use Terraform scripts to create:
- **Jenkins Server**: Runs Jenkins to automate CI/CD tasks.
- **Ansible Server**: Hosts the Ansible playbook for deployment.
- **TOMCAT Servers**: Serve as the deployment targets for the application.

### Terraform Code (`main.tf`)
```hcl
provider "aws" {
  region = "us-east-1"
}

resource "aws_instance" "one" {
  count = 4
  ami = "ami-0ddc798b3f1a5117e"
  instance_type = "t2.micro"
  key_name = "Awsdevops"
  vpc_security_group_ids = ["sg-053d3c714ed5a9a87"]
  tags = {
    Name = var.instance_names[count.index]
  }
}

variable "instance_names" {
  default = ["jenkins", "ansible", "tomcat-1", "tomcat-2"]
}
```

## Prerequisites and Configuration

### 1. HCP Terraform Setup
- **Install Terraform**: Download and install the Terraform CLI from the official website.
- **HCP Account**: Create an account in HashiCorp Cloud Platform (HCP) and configure access credentials.
- **Terraform Configuration**: Define the infrastructure resources (Jenkins, Ansible, and Tomcat servers) in `.tf` files.
- **Authentication**: Set up authentication to HCP using API tokens or other relevant methods.

### 2. Jenkins Setup
- **Jenkins Installation**: Install Jenkins on the server using automated scripts available on [GitHub](https://github.com/tarunreturn).
- **Plugins Required**:
  - Pipeline Stage View: Visualize pipeline stages.
  - AWS S3 Plugin: Upload build artifacts to S3.
  - SSH Agent Plugin: Use SSH keys for secure server access.
  - Ansible Plugin: Integrate Jenkins with Ansible.
  - Slack Notification Plugin: Send notifications to Slack.

### 3. GitHub Setup
- **Repository**: Create or use an existing GitHub repository for storing the source code.
- **Access Token**: Generate a GitHub Personal Access Token (PAT) for private repositories (not required for public repos).

### 4. Ansible Server Configuration
- **Install Ansible**: Install Ansible manually or using Terraform.
- **Playbook Directory**: Place the `deploy.yml` playbook under `/etc/ansible/`.
- **SSH Access**: Set up SSH keys for secure communication.

### 5. AWS S3 Bucket Configuration
- **Create S3 Bucket**: Create a bucket (`myartifact325`) to store `.war` files.
- **Permissions**: Ensure IAM roles have `s3:PutObject` permission.
- **Enable Versioning**: Keep multiple versions of artifacts.

### 6. Prometheus and Grafana Setup
- **Prometheus Installation**: Install and configure Prometheus to scrape metrics from Jenkins and Tomcat servers.
  - Configure `prometheus.yml` for scrape targets.
- **Grafana Installation**: Install Grafana and configure it to use Prometheus as a data source.
- **Node Exporter**: Install on all servers to collect metrics (CPU, memory, disk I/O).

### 7. SSH and Security Configurations
- **SSH Keys**: Set up keys for Jenkins to connect to servers.
- **Security Groups**: Allow necessary ports (e.g., 8080 for Tomcat, 22 for SSH).

### 8. Slack Configuration for Notifications
- **Create Slack Channel**: Create a `#ci-cd` channel.
- **Generate Token**: Obtain a token via Incoming Webhook or Slack App.
- **Slack Plugin**: Configure Jenkins to send notifications.

# Jenkins Pipeline Stages

The pipeline automates the deployment process in the following stages:

1. Checkout Stage: Pulls code from GitHub.
2. Maven Build Stage: Compiles the Java application.
3. Maven Test Stage: Runs unit tests to validate code changes.
4. Artifact Stage: Packages the application into a .war file for deployment.
5. Uploading Artifact to S3 for Backup: Uploads the .war file to an S3 bucket (myartifact325).
6. Uploading Artifact to Ansible Server: Transfers the .war file to the Ansible server.
7. Run Ansible Playbook: Deploys the application by running the Ansible playbook on the Tomcat servers.


### Pipeline Code
```groovy
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
                s3Upload consoleLogLevel: 'INFO', entries: [[
                    bucket: 'myartifact325',
                    sourceFile: 'target/NETFLIX-1.2.2.war',
                    selectedRegion: 'us-east-1'
                ]]
            }
        }
        stage('Uploading Artifact to Ansible Server') {
            steps {
                script {
                    sshagent(['ansible']) {
                        sh 'scp -o StrictHostKeyChecking=no target/NETFLIX-1.2.2.war root@172.31.87.52:/root/artifact'
                    }
                }
            }
        }
        stage('Run Ansible Playbook') {
            steps {
                script {
                    sshagent(['ansible']) {
                        sh 'ssh -o StrictHostKeyChecking=no root@172.31.87.52 \"ansible-playbook /etc/ansible/deploy.yml\"'
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(channel: '#ci-cd', message: "✅ Build and deployment of NETFLIX 1.2.2 was successful.", color: 'good')
        }
        failure {
            slackSend(channel: '#ci-cd', message: "❌ Build or deployment of NETFLIX 1.2.2 failed.", color: 'danger')
        }
    }
}
```
# DEATAIL EXPLAINATION OF PIPELINE CODE :

## 1. Checkout Stage

- **Purpose**: Pulls the project code from GitHub.
- **Details**:
  - Utilizes a parameterized branch variable to allow building from different branches.
- **groovy**:
  ```groovy
  git branch: '$branch', url: 'https://github.com/tarunreturn/jenkins-java-project.git'

## 2. Maven Build Stage
- **Purpose**: Compiles the Java application
- Shell step: `sh 'mvn compile'`
  
## 3.  Maven Test Stage
- **Purpose**: Runs unit tests to validate code changes.
- Shell step: `sh 'mvn test'`
  
## 4.  Artifact Stage
- **Purpose**: Packages the application into a .war file for deployment
- Shell step: `sh 'mvn package'`
  
## 5.  Uploading Artifact to S3 for Backup
- **Purpose**: Uploads the .war artifact to an S3 bucket (myartifact325), allowing backups and easy access to artifacts.
    ```groovy
  s3Upload {
    bucket: 'myartifact325',
    sourceFile: 'target/NETFLIX-1.2.2.war',
    selectedRegion: 'us-east-1
    }
    
## 6  Uploading Artifact to Ansible Server
- **Purpose**: Securely transfers the .war file to the Ansible server using sshagent with predefined credentials.
- Shell step: `scp -o StrictHostKeyChecking=no target/NETFLIX-1.2.2.war root@172.31.87.52:/root/artifact`
  
## 7  Run Ansible Playbook
- **Purpose**: Remotely runs the Ansible playbook on the Ansible server to deploy the application.
- Shell step: `ssh -o StrictHostKeyChecking=no root@172.31.87.52 'ansible-playbook /etc/ansible/deploy.yml'`
  
  ### Ansible Playbook (`deploy.yml`)
```yaml
- hosts: all
  tasks:
    - name: Deploying artifact to Tomcat
      copy:
        src: /root/artifact/NETFLIX-1.2.2.war
        dest: /root/tomcat/webapps
```
## 8.   Post-Build Notifications
- **Purpose**: After a successful or failed deployment, a message is sent to a Slack channel (#ci-cd) to notify the team.
    ```groovy
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

# OUTPUT
---

### **Server Creation**
![Server Creation Image 1](https://i.imghippo.com/files/vYn5866WOI.png)  
![Server Creation Image 2](https://i.imghippo.com/files/hfzS7105jM.png)

---

### **Ansible Worker Nodes**
![Ansible Worker Nodes](https://i.imghippo.com/files/KYuU8342QSs.png)

---

### **Jenkins Build Output**
![Jenkins Build Output](https://i.imghippo.com/files/hEb7019TI.png)

---

### **Artifact Upload to S3 Bucket**
![Artifact Upload](https://i.imghippo.com/files/KOjN9407pv.png)

---

### **Tomcat Servers Output**
![Tomcat Server 1](https://i.imghippo.com/files/tkP6445Ss.png)  
![Tomcat Server 2](https://i.imghippo.com/files/XEYf9843eas.png)

---

### **Slack Notification**
![Slack Notification](https://i.imghippo.com/files/rPXr7175pI.png)

---

### **Jenkins Monitoring Dashboard**
![Jenkins Monitoring Dashboard](https://i.imghippo.com/files/Sxkk3232Gyc.png)

---

### **Tomcat_1 Server Monitoring Dashboard**
![Tomcat_1 Monitoring Dashboard](https://i.imghippo.com/files/ZMT8037Xig.png)

---

### **Tomcat_2 Server Monitoring Dashboard**
![Tomcat_2 Monitoring Dashboard](https://i.imghippo.com/files/WwL7865yA.png)

---



## Conclusion
This pipeline and deployment setup offers automated, continuous integration and delivery for a Netflix-like application. The combination of Terraform for infrastructure, Jenkins for CI/CD, S3 for artifact storage, and Ansible for deployment ensures a robust, scalable deployment pipeline.

---

## Contact Information
- **GitHub**: [tarunreturn](https://github.com/tarunreturn)
- **LinkedIn**: [Tarun Kumar](https://www.linkedin.com/in/tarun-kumar-50a331287)
- **Email**: tarunkumar8367053296@gmail.com
