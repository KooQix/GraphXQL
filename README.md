# Requirements

-   maven cli needs to be installed
-   Hadoop needs to be installed (see config/manage_clusters & config/install_hadoop to install hadoop using LXD containers)
-   Spark needs to be installed (see config/install_spark)

# Installation

Create graphxql directories

    cd config/installation
    sudo chmod +x install.sh
    ./install.sh

Create mvn package: \
inside root dir (where pom.xml is)

    mvn package

scp target/graphxql-1.0-SNAPSHOT.jar

# Configuration

Add to $SPARK_HOME/conf/spark-defaults.conf

    spark.yarn.appMasterEnv.GRAPHXQL_HOME /graphxql

Add to $HADOOP_HOME/etc/hadoop/core-site.xml (considering that hadoop-namenode is set in /etc/hosts for each node)

    <property>
    		<name>fs.defaultFS</name>
    		<value>hdfs://hadoop-namenode:9000/</value>
    </property>

# Run

    $SPARK_HOME/bin/spark-submit --class dev.kooqix.App --master yarn --deploy-mode cluster graphxql.jar

# References

Apache Avro https://spark.apache.org/docs/latest/sql-data-sources-avro.html \
Java and Avro https://avro.apache.org/docs/1.11.1/getting-started-java/ \
Spark https://spark.apache.org/docs/1.3.1/sql-programming-guide.html

Manage HDFS Java \
https://hadoop.apache.org/docs/r2.6.0/api/org/apache/hadoop/fs/FileUtil.html \
https://javadeveloperzone.com/hadoop/java-read-write-files-hdfs-example/
