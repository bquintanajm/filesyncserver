# Deprecated
This repository is deprecated now. The development continues in [Json Sync Server](https://github.com/jbequinn/jsonsyncserver)


# filesyncserver
File synchronization service for systems like [Everdo](https://everdo.net/)

## How to run
* Download the image from:
```
https://hub.docker.com/r/juanmbq/filesyncserver
```
* Run:
```
docker run --name fileserver --rm -it -p 8443:8443 -e api.key="<my key>" -v <my path>:/data juanmbq/filesyncserver:0.0.3-SNAPSHOT
```


Or build the image yourself:
```
mvn clean package dockerfile:build
```
