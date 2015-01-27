#!/bin/bash

extensions="rdf ttl owl nt nquads"
PATTERNS=""
for e in $extensions ; do
  PATTERNS="$PATTERNS *.$e *.$e.gz"
done

if [ $# -eq 0 ] || [ "$1" == "-h" ] || [ "$1" == "--help" ] ; then 
  echo "$0 [DB] [PATTERN ...]" 
  echo "Load one or more RDF files into Jena Fuseki TDB database DB."
  echo ""
  echo "Current directory is assumed to be /staging"
  echo ""
  echo 'PATTERNs can be a filename or a shell glob pattern like *ttl'
  echo ""
  echo "If no PATTERN are given, the default patterns are searched:"
  echo "$PATTERNS"
  exit 0
fi

cd /staging 2>/dev/null || echo "/staging not found" >&2
echo "Current directory:" $(pwd)

DB=$1
shift

if [ $# -eq 0 ] ; then 
  patterns="$PATTERNS"
else
  patterns="$@"
fi

files=""
for f in $patterns; do
  if [ -f $f ] ; then
    files="$files $f"
  else 
    if [ $# -gt 0 ] ; then 
      # User-specified file/pattern missing
      echo "WARNING: Not found: $f" >&2
    fi
  fi
done

if [ "$files" == "" ] ; then
  echo "No files found for: " >&2
  echo "$patterns" >&2
  exit 1
fi

echo "#########"
echo "Loading to Fuseki TDB database $DB:"
echo ""
echo $files
echo "#########"

exec java -cp $FUSEKI_HOME/fuseki-server.jar tdb.tdbloader --loc=$FUSEKI_BASE/$DB $files
