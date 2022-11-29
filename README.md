# Requirements

-   maven cli needs to be installed
-   Hadoop needs to be installed
-   Spark needs to be installed

# Installation

Export environment variables

    sudo chmod +x graphxql-env.sh
    ./graphxql-env.sh

Create graphxql directories

    cd config/installation
    sudo chmod +x install.sh
    ./install.sh

Create mvn package: \
inside root dir (where pom.xml is)

    mvn package

scp target/graphxql-1.0-SNAPSHOT.jar

# Run

    $SPARK_HOME/bin/spark-submit --class dev.kooqix.App --master yarn graphxql.jar

# References

Apache Avro https://spark.apache.org/docs/latest/sql-data-sources-avro.html \
Java and Avro https://avro.apache.org/docs/1.11.1/getting-started-java/
Spark https://spark.apache.org/docs/1.3.1/sql-programming-guide.html
