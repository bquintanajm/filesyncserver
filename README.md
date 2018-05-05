# filesyncserver
File synchronization service for systems like [Everdo](https://everdo.net/)

## How to run
* Build the image with:
```
mvn clean package dockerfile:build
```

* Run:
```
docker run --name fileserver --rm -it -p 8443:8443 -e api.key="<my key>" -v <my path>:/data filesyncserver/filesyncserver:latest
```
