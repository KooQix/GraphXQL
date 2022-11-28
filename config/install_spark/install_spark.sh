#! /bin/bash

HADOOP_DIR_HOST=/exports/hadoop
HADOOP_DIR_NODES=/opt/hadoop
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
SPARK_LINK=https://dlcdn.apache.org/spark/spark-3.3.1/spark-3.3.1-bin-hadoop3.tgz

USAGE="./install_spark.sh <num cluster 1 | 2>"

if [[ $# -ne 1 ]]; then
	echo $USAGE
	exit -1
fi

if [[ $1 != 1 ]] && [[ $1 != 2 ]]; then
	echo $USAGE
	exit -1
fi

declare -a cluster

if [[ $1 == 1 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2")
elif [[ $1 == 2 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4")
elif [[ $1 == 3 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4" "hadoop-datanode-5" "hadoop-datanode-6" "hadoop-datanode-7" "hadoop-datanode-8")
fi


wget $SPARK_LINK

tar -xvf spark-3.3.1-bin-hadoop3.tgz

mkdir -p $HADOOP_DIR_HOST/sparkstaging $HADOOP_DIR_HOST/spark
mv spark-3.3.1-bin-hadoop3/* $HADOOP_DIR_HOST/spark
rm -r spark-3.3.1-bin-hadoop3.tgz spark-3.3.1-bin-hadoop3
cd $HADOOP_DIR_HOST/spark
cp conf/spark-env.sh.template conf/spark-env.sh

echo -e "
HADOOP_CONF_DIR=$HADOOP_DIR_NODES/etc/hadoop
YARN_CONF_DIR=$HADOOP_DIR_NODES/etc/hadoop
SPARK_LOCAL_IP=hadoop-namenode
" >> conf/spark-env.sh



cp conf/spark-defaults.conf.template conf/spark-defaults.conf

echo -e "spark.yarn.stagingDir   $HADOOP_DIR_NODES/sparkstaging" >> conf/spark-defaults.conf



# Add env variables to each node
rm envs_tmp 2> /dev/null
echo -e "
export HADOOP_PREFIX=$HADOOP_DIR_NODES/
export JAVA_HOME=$JAVA_HOME/
export SPARK_HOME=$HADOOP_DIR_NODES/spark/
" > envs_tmp

for node in "${cluster[@]}"
do
	lxc file push envs_tmp $node/etc/profile.d/hadoop.sh
done


rm envs_tmp