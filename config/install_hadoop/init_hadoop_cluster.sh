#! /bin/bash

DEST_DIR_HADOOP=/opt/hadoop
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64


USAGE="./init_hadoop_cluster.sh <num_cluster 1 | 2> <ip host> <file master_ip> <file slaves_ip>"


if [[ $# -ne 4 ]]; then
	echo $USAGE
	exit -1
fi

base_hosts="
::1             localhost ip6-localhost ip6-loopback\n
ff02::1         ip6-allnodes\n
ff02::2         ip6-allrouters\n
"


	#################### Config ####################

#! 0 is namenode!
declare -a cluster

if [[ $1 == 1 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2")
elif [[ $1 == 2 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4")
elif [[ $1 == 3 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4" "hadoop-datanode-5" "hadoop-datanode-6" "hadoop-datanode-7" "hadoop-datanode-8")
fi


HADOOP_ENV_VARIABLES="# Hadoop env\nexport HADOOP_HOME=$DEST_DIR_HADOOP\nexport HADOOP_CONF_DIR=$DEST_DIR_HADOOP/etc/hadoop\nexport HADOOP_MAPRED_HOME=$DEST_DIR_HADOOP\nexport HADOOP_COMMON_HOME=$DEST_DIR_HADOOP\nexport HADOOP_HDFS_HOME=$DEST_DIR_HADOOP\nexport YARN_HOME=$DEST_DIR_HADOOP\nexport PATH=\$PATH:$DEST_DIR_HADOOP/bin\n\n## Java env\nexport JAVA_HOME=$JAVA_HOME\nexport PATH=\$PATH:\$HOME/bin:\$JAVA_HOME/bin"




	#################### Main ####################


	#################### Add Hadoop namenode / datanodes ####################

rm namenode datanode datanode_tmp authorized_keys 2> /dev/null


echo "hadoop-namenode" > namenode

# datanode
datanode=1
while IFS= read -r line
do
	echo -e "$line   hadoop-datanode-$datanode" >> datanode_tmp
	datanode=$(( $datanode+1 ))
done < "$4"

for (( j=1; j<$datanode; j++))
do
	echo "hadoop-datanode-$j" >> datanode
done


master_ip=`cat $3`

fstab_content="$2:/exports/hadoop $DEST_DIR_HADOOP nfs defaults 0 0"
mount_command="$2:/exports/hadoop $DEST_DIR_HADOOP"
echo $fstab_content > resources/fstab


# Create directory for metadata inside namenode
lxc exec "${cluster[0]}" -- mkdir /home/ubuntu/hdfs-metadata /home/ubuntu/hdfs-logs

for node in "${cluster[@]}"
do
	
		#################### Update and requirements ####################
	
	lxc exec $node -- sudo apt update && apt upgrade -y
	lxc exec $node -- sudo apt install default-jre default-jdk ssh nfs-common -y
	lxc exec $node -- sudo service ssh start

	# Hadoop
	echo -e "\nMounting Hadoop...\n"
	lxc exec $node -- mkdir -p $DEST_DIR_HADOOP /home/ubuntu/hdfs-data

	# Mount nfs volume on reboot
	lxc file push resources/fstab $node/etc/fstab

	# Mount
	lxc exec $node -- sudo mount -t nfs $mount_command

	lxc exec $node -- sudo df -hT

	lxc file push namenode $node$DEST_DIR_HADOOP/etc/hadoop/namenode
	lxc file push datanode $node$DEST_DIR_HADOOP/etc/hadoop/datanode
	lxc file push datanode $node$DEST_DIR_HADOOP/etc/hadoop/workers

	lxc file pull $node/root/.bashrc .
	echo -e $HADOOP_ENV_VARIABLES >> .bashrc
	lxc file push .bashrc $node/root/.bashrc
	rm .bashrc


	echo -e $base_hosts > hosts
	echo "$master_ip   hadoop-namenode" >> hosts
	cat datanode_tmp >> hosts
	lxc file push hosts $node/etc/hosts
	rm hosts
done

#################### Add IP addr ##############<######
	
rm datanode_tmp datanode namenode


echo -e "\nDone\n\n--------------------- Start Hadoop ---------------------\n"

echo -e "- Go to master node\n- $ cd \$HADOOP_HOME\n- $ hadoop namenode –format\n- $ cd \$HADOOP_HOME/sbin\n- $ ./start-all.sh"


