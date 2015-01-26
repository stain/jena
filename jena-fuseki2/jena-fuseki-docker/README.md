# Apache Jena Fuseki 2 docker image

**Docker image:** [stain/jena-fuseki](https://registry.hub.docker.com/u/stain/jena-fuseki/)

This is a [Docker](http://docker.io/) image for running 
[Apache Jena Fuseki](http://jena.apache.org/documentation/serving_data/) 2,
which is a [SPARQL 1.1](http://www.w3.org/TR/sparql11-overview/) server with a
web interface, backed by the 
[Apache Jena TDB](http://jena.apache.org/documentation/tdb/) RDF triple store.

Feel free to contact the [jena users
list](http://jena.apache.org/help_and_support/) for any questions on using
Jena or Fuseki.


## Use

To use this image, try:

    docker run -p 3030:3030 -it stain/jena-fuseki

The Apache Jena Fuseki should then be available at http://localhost:3030/

To load RDF graphs, you will need to log in as the `admin` user. To see the 
automatically generated admin password, see the output from above, or
use `docker logs` with the name of your container.

Note that the password is only generated on the first run, e.g. when the
volume `/fuseki` is an empty directory.

You can always specify the desired admin-password using the form 
`-e ADMIN_PASSWORD=pw123`:

    docker run -p 3030:3030 -e ADMIN_PASSWORD=pw123 -it stain/jena-fuseki


## Data persistence

Fuseki's data is stored in the Docker volume `/fuseki` within the container.
Note that unless you use `docker restart` or one of the mechanisms below, data
is lost between each run of the jena-fuseki image.

To store the data in a named Docker volume container (recommended), create it
first as:

    docker run --name fuseki-data -v /fuseki busybox

Then start fuseki using `--volumes-from`. This allows you to later upgrade the
jena-fuseki docker image without losing the data. The below also uses
`-d` to start the container in the background.

    docker run -d --name fuseki -p 3030:3030 --volumes-from fuseki-data stain/jena-fuseki

If you want to store fuseki data in a specified location on the host (e.g. for
disk space or speed requirements), specify it using `-v`:

    docker run -d --name fuseki -p 3030:3030 -v /ssd/data/fuseki:/fuseki -it stain/jena-fuseki

Note that the `/fuseki` volume must only be accessed from a single Fuseki 
container at a time.    

To check the logs for the container you gave `--name fuseki`, use:

    docker logs fuseki

To stop the named container, use:    

    docker stop fuseki

.. or press Ctrl-C if you started the container with `-it`.    

To restart the named container (it will remember the volume and port config)

    docker restart fuseki


## Data loading

TODO: Use tdbloader ?  Do we need jena-arq/bin/ in the image?


