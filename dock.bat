docker build --no-cache=true -t cljsflappynginx .
docker stop cljsflappy
docker rm cljsflappy
docker run --name cljsflappy -p 8080:80 cljsflappynginx
