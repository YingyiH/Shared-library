pipeline {
    agent any
    
    stages {
        stage('Lint') {
            steps {
               echo "Lint" 
            }
        }
        
        stage('Security Scan') {
            steps {
                echo "Security Scan"
            }
        }
        
        stage('Package') {
            steps {
                echo "Package" 
            }
        }
        
        stage('Deploy') {
            steps {
               echo "Deploy" 
            }
        }

    }
}
