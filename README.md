# filesyncserver
File synchronization service for systems like [Everdo](https://everdo.net/)

## How to run
* Download the image from:
```
https://hub.docker.com/r/juanmbq/filesyncserver
```
* Run:
```
docker run --name fileserver --rm -it -p 8443:8443 -e api.key="<my key>" -v <my path>:/data juanmbq/filesyncserver:latest
```


Or build the image yourself:
```
mvn clean package dockerfile:build
```
