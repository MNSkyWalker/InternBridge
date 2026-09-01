# InternBridge - DevOps Pipeline Project


## Project Overview
**InternBridge** is a complete DevOps pipeline project designed to demonstrate an end-to-end CI/CD workflow. It integrates multiple tools to ensure code quality, automated building, artifact< storage, and system monitoring.

### Key Features:
- **Continuous integration** with Jenkins
- **Code quality analysis** with SonarQube
- **Artifact management** with Nexus Repository
- **System monitoring** with Prometheus
- **Visualization dashboards** with Grafana
- **All services containerized** with Docker

------------------------------------------------------------------------

## Architecture 

┌─────────────────────────────────────────────────────────────────────┐
│ GitHub Repository │                                                 |
│ (MNSkyWalker/InternBridge) │                                        |
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Jenkins (CI/CD) │                                                   |
│ http://localhost:8080 │                                             |
│                                                                     |
│ ┌──────────────┐ ┌────────────────┐ ┌─────────────────────┐         |
│ │Code Checkout │ │  Build & Test  │ |  Quality Gate Check │         │
│ └──────────────┘ └────────────────┘ └─────────────────────┘         │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
        ┌──────────────────┐ ┌──────────────┐ ┌───────────────────────┐
        │    SonarQube     │ │    Nexus     │ │     Monitoring        │
        │   Code Quality   │ │   Artifact   │ │  Prometheus & Grafana │
        │  localhost:9000  │ │     Repo     │ │ localhost:9090 / 3000 │
        └──────────────────┘ │localhost:8081│ │ localhost:9090 / 3000 │
                             └──────────────┘ └───────────────────────┘
------------------------------------------------------------------------
## Prerequisites

Before you begin, ensure you have the following installed:
┌────────────────────┌─────────┌───────────────────────────────┐
|       Tool         | Version |          Purpose              |
|--------------------|-------- |-------------------------------|
| **Docker**         | 20.10+  | Container runtime             |
| **Docker Compose** | 2.0+    | Multi-container orchestration |
| **Git**            | 2.30+   | Version control               |
| **Java**           | 11+     | For Maven builds              |
| **Maven**          | 3.6+    | Build automation              |
└────────────────────└─────────└───────────────────────────────┘

### Hardware Requirements
- **Minimum RAM**: 4 GB (6.5 GB recommended)
- **Disk Space**: 20 GB (for containers and artifacts)
- **OS**: Ubuntu 22.04 LTS (or Linux distribution)

------------------------------------------------------------------------

## Installation & Setup

### 1️⃣ Clone the Repository

```bash```
git clone https://github.com/MNSkyWalker/InternBridge.git
cd InternBridge 

2️⃣ Start All Services with Docker Compose
Create a ```docker-compose.yml``` file in the project root:
version: '3'
services:
  jenkins:
    image: myjenkins-blueocean:2.568.2-1
    container_name: jenkins-blueocean
    ports:
      - "8080:8080"
      - "50000:50000"
    networks:
      - devops-network
    mem_limit: 1g

  sonarqube:
    image: sonarqube:lts-community
    container_name: sonarqube
    ports:
      - "9000:9000"
    networks:
      - devops-network
    mem_limit: 2g

  nexus:
    image: sonatype/nexus3
    container_name: nexus
    ports:
      - "8081:8081"
    networks:
      - devops-network
    mem_limit: 1.5g
    volumes:
      - nexus_data:/nexus-data

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - devops-network
    mem_limit: 512m

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - devops-network
    mem_limit: 512m

networks:
  devops-network:
    driver: bridge

volumes:
  nexus_data:
  prometheus_data:
  grafana_data:

  Then proceed to run : 
  
  docker-compose up -d

3️⃣ Access the Services
Service	URL	Default Credentials
Jenkins	http://localhost:8080	Initial password: docker exec jenkins-blueocean cat /var/jenkins_home/secrets/initialAdminPassword
SonarQube	http://localhost:9000	admin / admin (change on first login)
Nexus	http://localhost:8081	admin / docker exec nexus cat /nexus-data/admin.password
Prometheus	http://localhost:9090	No authentication
Grafana	http://localhost:3000	admin / admin (change on first login)

CI/CD Pipeline ( Jenkins ):

The Jenkins pipeline automates the entire build, test and deployment process ( through the creation of the Jenkinsfile ):

pipeline {
    agent any

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')
        NEXUS_URL = 'http://nexus:8081'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/MNSkyWalker/InternBridge.git', branch: 'master'
            }
        }

        stage('Build') {
            steps {
                dir('demo') {
                    sh 'mvn clean compile package'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('demo') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                dir('demo') {
                    sh 'mvn deploy'
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}

/Pipeline Stages 

|    Stage	        |   Description	                      | Tool
1|Checkout	        |Clones the repository from GitHub	  | Git
2|Build	            |Compiles and packages the application| Maven
3|SonarQube Analysis|Runs code quality checks	            | SonarQube Scanner
4|Quality Gate	    |Blocks deployment if quality fails	  | SonarQube
5|Deploy to Nexus	  |Uploads artifact to Nexus	          | Maven Deploy

/Monitoring Stack 
Prometheus Configuration (prometheus.yml) : 

global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']

  - job_name: 'jenkins'
    metrics_path: '/prometheus'
    static_configs:
      - targets: ['jenkins:8080']


/Grafana Dashboards

After starting Grafana:

1-Log in at http://localhost:3000 (admin/admin)
2-Add a data source:
-Type: Prometheus
-URL: http://prometheus:9090
-Click Save & Test
3-Import Dashboards:
-Node Exporter Full: ID 1860
-Prometheus Stats: ID 3662
-Jenkins Monitoring: ID 9964

/Running the Project 

Now into the process of quick start : 

# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

For an individual service management : 

# Start a specific service
docker start [container-name]

# Stop a specific service
docker stop [container-name]

# Check logs
docker logs -f [container-name]

# Check resource usage
docker stats

/Troubleshooting

Common Issues:

     Issue	                      Solution
Jenkins blank page	      Check memory: free -h, restart Jenkins: docker restart jenkins-blueocean
SonarQube not starting	  Increase memory: -m 2g, wait 3-5 minutes for initialization
Nexus connection reset	  Check memory: docker stats nexus, increase limit to -m 1.5g
Prometheus 203/EXEC error	Fix ExecStart path in service file to point to binary
Port already in use	      Stop conflicting service: sudo lsof -i :[port]
Out of Memory errors	    Set memory limits in docker-compose.yml or run fewer services

/Reset everything: 

# Stop and remove all containers
docker-compose down -v

# Remove all volumes
docker volume prune -f

# Start fresh
docker-compose up -d

/Project Structure 

The project structure should look like the following structure : 

InternBridge/
├── demo/
│   ├── pom.xml                 # Maven project configuration
│   └── src/
│       ├── main/
│       │   └── java/           # Source code
│       └── test/
│           └── java/           # Test files
├── Jenkinsfile                 # Jenkins pipeline definition
├── docker-compose.yml          # Container orchestration
├── prometheus.yml              # Prometheus configuration
├── .gitignore                  # Git ignore rules
└── README.md                   # This file

/Contributing:

Fork the repository

Create a feature branch: git checkout -b feature/amazing-feature

Commit your changes: git commit -m 'Add amazing feature'

Push to the branch: git push origin feature/amazing-feature

Open a Pull Request

/Acknowledgments:

Jenkins - CI/CD Automation
SonarQube - Code Quality
Sonatype Nexus - Artifact Repository
Prometheus - Monitoring
Grafana - Visualization
Docker - Containerization
