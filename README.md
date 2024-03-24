# Casa Api

    git pull https://github.com/paulokowalski/casa-api.git
    mvn clean install    
    docker build -t casa-api .    
    docker run -d --name casa-api --restart always -p 8081:8080 casa-api .