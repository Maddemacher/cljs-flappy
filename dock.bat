docker build --no-cache=true -t cljsflappynginx .
docker stop cljsflappy
docker rm cljsflappy
docker run -d --name cljsflappy -p 8000:80 cljsflappynginx
