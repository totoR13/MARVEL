FROM python:3.9

RUN apt-get update && \
    apt-get install -q --no-install-recommends -y openjdk-11-jdk git && \
    apt autoremove --purge -y && apt clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# install python packages
RUN python3 -m pip install --upgrade pip
RUN python3 -m pip install --no-cache-dir SQLAlchemy psycopg2 psycopg2-binary
RUN python3 -m pip install --no-cache-dir -U androguard

RUN mkdir /keytools
RUN keytool -genkey -v -keystore /keytools/my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000 -storepass test123 -keypass test123 -dname CN=Test

# Upload needed files
RUN git clone https://github.com/Sable/android-platforms.git /jars/resources/platforms
COPY ./Binaries/MARVELoid-1.0.jar /jars/transformer.jar
COPY ./Docker/main.py /scripts/main.py
COPY ./Docker/models.py /scripts/models.py
COPY ./Docker/DbManager.py /scripts/DbManager.py

# Set the working directory (to be used when mounting files from the host).
WORKDIR /workdir
VOLUME /workdir

ENTRYPOINT ["python3", "/scripts/main.py"]