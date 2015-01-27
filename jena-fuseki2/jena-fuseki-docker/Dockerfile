#   Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  See the NOTICE file distributed with
#   this work for additional information regarding copyright ownership.
#   The ASF licenses this file to You under the Apache License, Version 2.0
#   (the "License"); you may not use this file except in compliance with
#   the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

FROM java:7-jdk


# Config and data
VOLUME /fuseki
ENV FUSEKI_BASE /fuseki


# Installation folder
ENV FUSEKI_HOME /jena-fuseki

ENV MAVEN_REPOSITORY https://repository.apache.org/content/groups/snapshots/
ENV PACKAGE_PATH org/apache/jena/jena-fuseki-dist
ENV FUSEKI_VERSION 2.0.0-SNAPSHOT


ENV BUILD_DEPS curl xqilla
ENV DEPS pwgen

# As "localhost" is often inaccessible within Docker container,
# we'll enable basic-auth with a random admin password 
# (which we'll generate on start-up)
COPY shiro.ini /jena-fuseki/shiro.ini
COPY docker-entrypoint.sh /
RUN chmod 755 /docker-entrypoint.sh

# Evil hack below..
# Download the latest Fuseki2 SNAPSHOT version from Maven repository
# .. but without using Maven. So we'll XQuery the maven-metadata.xml
# instead to find the latest version.

WORKDIR /tmp
RUN export DEBIAN_FRONTEND=noninteractive && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update && \
    apt-get install -y $BUILD_DEPS $DEPS && \
    MAVEN_REPOSITORY_PATH=$MAVEN_REPOSITORY/$PACKAGE_PATH/$FUSEKI_VERSION && \
    curl -fO $MAVEN_REPOSITORY_PATH/maven-metadata.xml && \
    echo '/metadata/versioning/snapshotVersions/snapshotVersion[1]/value/text()' > xquery && \
    SNAPSHOT_VERSION=$(xqilla -i maven-metadata.xml xquery) && \
    FILE=jena-fuseki-dist-${SNAPSHOT_VERSION}.tar.gz && \
    URL=$MAVEN_REPOSITORY_PATH/$FILE && \
    echo Downloading $URL && \
    curl -f -O $URL && \
    curl -f -O $URL.sha1 && \
    echo " " $FILE >> $FILE.sha1 && \
    cat $FILE.sha1 && \
    sha1sum -c $FILE.sha1 && \
    tar xzf $FILE && \
    rm jena-fuseki-*/*war && \
    mv jena-fuseki-*/* $FUSEKI_HOME && \
    apt-get remove -y --purge $BUILD_DEPS && \
    apt-get autoremove -y && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

COPY load.sh /jena-fuseki/

# Where we start our server from
WORKDIR /jena-fuseki
EXPOSE 3030
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/jena-fuseki/fuseki-server"]

