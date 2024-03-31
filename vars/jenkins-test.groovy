pipeline {
    agent any
    
    stages {
        stage('Lint') {
            steps {
                script {
                    def services = ['Receiver', 'Storage', 'Processing']
                    services.each { service ->
                        sh "pylint ${service}.py || exit 0"
                        def pylint_score = sh(script: "pylint ${service}.py | grep -oP '(?<=rated at ).*(?=/10)'", returnStdout: true).trim()
                        if (pylint_score.toInteger() < 5) {
                            error "${service} pylint score is below 5"
                        }
                    }
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                // Perform security scan using your chosen tool (e.g., Trivy for Docker image scanning)
                // Provide reasoning for tool choice
                sh 'trivy --image your_docker_image:latest' // Example command, replace with your actual command
                // Add reasoning for tool choice
                // Trivy scans for vulnerabilities in Docker images which is important for ensuring the security of the application.
                // It provides comprehensive vulnerability scanning with a large CVE database.
            }
        }
        
        stage('Package') {
            steps {
                // Build and push Docker images for each service
                script {
                    def services = ['Receiver', 'Storage', 'Processing']
                    services.each { service ->
                        sh "docker build -t your_dockerhub_account/${service}:latest ."
                        sh "docker push your_dockerhub_account/${service}:latest"
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps {
                // Remote deployment to 3855 VM using docker-compose up -d
                // Make sure you have SSH credentials configured in Jenkins
                sshagent(['your_ssh_credentials']) {
                    sh 'scp -r docker-compose.yml user@3855-VM-IP:~/your_app_dir/' // Copy docker-compose.yml to 3855 VM
                    sh 'ssh user@3855-VM-IP "cd ~/your_app_dir/ && docker-compose pull && docker-compose up -d"'
                }
            }
        }
    }
    
    triggers {
        pollSCM('* * * * *') // Poll SCM every minute for changes
    }
}
