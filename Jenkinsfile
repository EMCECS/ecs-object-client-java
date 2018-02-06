pipeline {
    agent {
        docker { image 'gradle:4.5-jdk7' }
    }
    stages {
        stage('Test') {
            steps {
                sh 'gradle test -i'
            }
        }
    }
}