# MARVELoid Docker Image

This folder contains the script to build a docker image to run the MARVELoid protection process on a set of apks.

## Prerequisites

We highly recommmend the latest version of [docker](https://docs.docker.com/engine/install/) and [docker-compose](https://docs.docker.com/compose/) tools.
In particular, `docker` is mandatory to build and execute the MARVELoid image, but `docker-compose` is only needed to run the `docker-compose.yml` example.

## Build

To build the MARVELoid docker image, run the following commands:
```console
$ cd <path/to/MARVELoid>
$ ls
Binary  Docker  Example  Experiments  README.md  Tools
$ docker build -t marveloid -f ./Docker/Dockerfile .
[...]
$ docker image ls
REPOSITORY                            TAG       IMAGE ID       CREATED          SIZE
marveloid                             latest    0234d99507e4   39 minutes ago   2.88GB
python                                3.9       cba42c28d9b8   5 weeks ago      886MB
```

## Usage
The MARVELoid docker image saves the results on a postgresql db, located at `host` host.

```
$ docker run marveloid --help
usage: main.py [-h] [-o OUTPUT_FOLDER] [-k] [-p PROCESSES] [--host HOST]
               [--permutations PERMUTATIONS]
               APK_FOLDER

Run the MARVELoid analysis

positional arguments:
  APK_FOLDER            The path of the input apks

optional arguments:
  -h, --help            show this help message and exit
  -o OUTPUT_FOLDER, --output_folder OUTPUT_FOLDER
                        The folder containing all the output results. The
                        default value is `/workdir/output`
  -k, --keep_output_files
                        Flag to remove or not the protected apks and the
                        protection metadata.
  -p PROCESSES, --processes PROCESSES
                        The number of processes. The default value is `4`
  --host HOST           The host name of the postgresql db. The default value
                        is '127.0.0.1'
  --permutations PERMUTATIONS
                        A ';' separated list of permutation, a tuple in the
                        form 'p1,p2,p3' of 3 percentage values for the
                        MARVELoid tool, where p1 is the change for the code
                        splitting, p2 is a chance for the IAT with encryption
                        and p3 is a chance for the base IAT. The default value
                        is '5,5,5'

```

## Example
We prepared a `Makefile` and a `docker-compose.yml` with an example.

Before running the docker-compose file, you have to export the environment variables i)`INPUT_APP_PATH` (with the path of the folder containing the input apks), and ii) `MARVELOID_RESULTS` (with the path of the output folder).

On Ubuntu 20.04, run the following command:
```console
$ ls <path/to/input/folder>
app1.apk app2.apk [...] 
$ export INPUT_APP_PATH=<path/to/input/folder>
$ export MARVELOID_RESULTS=</path/to/output/folder>
```

The `docker-compose.yml` performs the MARVELoid analysis. 
In particular, it creates a container for i) the MAVELoid analysis (i.e., `executor` service), and ii) a postgresql database (i.e., `postgres` service). 

On an Ubuntu 20.04 machine:
* Run the analysis:  
```console 
$ make startup
$ docker ps
CONTAINER ID   IMAGE             COMMAND                  CREATED          STATUS          PORTS                                       NAMES
ef39b4844542   docker_executor   "python3 /scripts/ma…"   44 minutes ago   Up 5 seconds                                               docker_executor_1
27a930d8ae67   postgres:latest   "docker-entrypoint.s…"   47 minutes ago   Up 6 seconds   0.0.0.0:5432->5432/tcp, :::5432->5432/tcp   docker_postgres_1
```
* Stop the docker containers:
```console
$ make stop
```
* Stop and remove the docker containers and all the database files (*it requires root priviledges*):
```console
$ make down
```

**Note** The containers are executed in detach mode.
To verify the protection logs, you can print the logs of the `executor` service running `docker logs docker_executor_1 -f`. 
When the MARVELoid protection is completed, the `executor` container exit.
```
$ docker ps
CONTAINER ID   IMAGE             COMMAND                  CREATED          STATUS          PORTS                                       NAMES
ef39b4844542   docker_executor   "python3 /scripts/ma…"   44 minutes ago   Exited (0) 1 minutes ago                                               docker_executor_1
27a930d8ae67   postgres:latest   "docker-entrypoint.s…"   47 minutes ago   Up 2 minutes   0.0.0.0:5432->5432/tcp, :::5432->5432/tcp   docker_postgres_1
```




