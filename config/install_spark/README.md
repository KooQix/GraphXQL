# Install spark

-   Copy install_spark.sh to host

        sudo chmod +x install_spark.sh
        sudo ./install_spark.sh <num cluster 1 | 2 |3>

# Spark link

https://spark.apache.org/downloads.html

# Spark commands

## spark-submit

https://sparkbyexamples.com/spark/spark-submit-command/

## Examples

    ./bin/spark-submit --class org.apache.spark.examples.SparkPi --master yarn --deploy-mode cluster --driver-memory 6g --executor-memory 6g --executor-cores 10 examples/jars/spark-examples*.jar 10

It shows result

    ./bin/spark-submit --class org.apache.spark.examples.SparkPi --master yarn  --driver-memory 6g --executor-memory 6g --executor-cores 10 examples/jars/spark-examples*.jar 10

WordCount

    create file res.txt
    add to hdfs

    javac -cp $SPARK_HOME/jars/spark-core_2.12-3.3.1.jar:$SPARK_HOME/jars/scala-library-2.12.15.jar WordCount.java

    jar cvf WordCount.jar WordCount.class

    $SPARK_HOME/bin/spark-submit --class WordCount --master yarn --driver-memory 6g --executor-memory 6g --executor-cores 15  WordCount.jar res.txt
