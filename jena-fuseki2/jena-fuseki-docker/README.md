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

Fuseki allows uploading through the web interface and web services, but for large
datasets it is more efficient to load them directly using the command line tool.

This docker image includes a shell script `load.sh` that invokes the
[tdbloader](https://jena.apache.org/documentation/tdb/commands.html)
command line tool and load datasets from the docker volume `/staging`.

**WARNING**: Before data loading, you must either stop the Fuseki container, or
load the data into a non-existing dataset.

For help, try:

    docker run stain/jena-fuseki ./load.sh

You will most likely want to load from a folder on the host computer by using `-v`, and into a data volume that you can then use with the regular fuseki.


The example below assume you want to populate the Fuseki dataset 'chembl19' from the Docker data volume `fuseki-data` (see above) by loading the two files `cco.ttl.gz` and `void.ttl.gz` from `/home/stain/ops/chembl19` on the host computer:

    docker run --volumes-from fuseki-data -v /home/stain/ops/chembl19:/staging stain/jena-fuseki ./load.sh chembl19 cco.ttl.gz void.ttl.gz

**Tip:** You might find it benefitial to run data loading from the data staging
directory in order to use tab-completion etc. without exposing the path on the
host.

If you don't specify any filenames to `load.sh`, all filenames directly under `/staging` that
match these GLOB patterns will be loaded:

    *.rdf *.rdf.gz *.ttl *.ttl.gz *.owl *.owl.gz *.nt *.nt.gz *.nquads *.nquads.gz

`load.sh` populates directly into the default graph. To populate into named graphs, see the `tdbloader` section below.

**NOTE:**: If you load data into a brand new data volume, a new random admin
password will be generated. You can either check the output of the data
loading, or later override the password using `-e ADMIN_PASSWORD=pw123`.

## Loading with tdbloader

If you have more advanced requirements, like loading multiple datasets or named graphs, you can 
use [tdbloader](https://jena.apache.org/documentation/tdb/commands.html) directly together with 
a [TDB assembler file](https://jena.apache.org/documentation/tdb/assembler.html).

Note that Fuseki TDB datasets are sub-folders in `/fuseki/databases/`.

You will need to provide the assembler file on a mounted Docker volume together with the
data:

    docker run --volumes-from fuseki-data -v /home/stain/data:/staging stain/jena-fuseki ./tdbloader --desc=/staging/tdb.ttl

## Recognizing the dataset in Fuseki

If you loaded into an existing dataset, Fuseki should find the data after
(re)starting with the same data volume (see above):

    docker restart fuseki

If you created a brand new dataset, then in Fuseki go to *Manage datasets*, click *Add new dataset*, tick **Persistent** and provide the database name exactly as provided to `load.sh`, e.g. `chembl19`. 

It should be possible to load a new dataset in a running Fuseki server, as long as you don't register it until the data-loading has finished.


