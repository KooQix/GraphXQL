#! /bin/bash



	#################### Configurations ####################

IMAGE=ubuntu/22.04
STORAGE_POOL=storage-lvm


# Instances characteristics

VM_CPU_CORES=3
VM_RAM=12GB
VM_DISK=24GB



USAGE="./manage_clusters_hadoop.sh <command> <num_cluster 1 | 2>"
COMMANDS="commands: create; start; stop; delete"

if [[ $# -ne 2 ]]; then
	echo $USAGE
	exit -1
fi


declare -a cluster

if [[ $2 == 1 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2")
elif [[ $2 == 2 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4")
elif [[ $2 == 3 ]]; then
	cluster=("hadoop-namenode" "hadoop-datanode-2" "hadoop-datanode-3" "hadoop-datanode-4" "hadoop-datanode-5" "hadoop-datanode-6" "hadoop-datanode-7" "hadoop-datanode-8")
fi




function create_cluster() {
	for node in "${cluster[@]}"
	do
		lxc launch images:$IMAGE --storage $STORAGE_POOL $node --config limits.cpu=$VM_CPU_CORES --config limits.memory=$VM_RAM

		lxc exec $node -- sudo apt update && apt upgrade -y
		lxc exec $node -- sudo apt install default-jre default-jdk curl ssh -y
		lxc exec $node -- sudo service ssh start

		lxc config set $node security.privileged true
		lxc config set $node raw.lxc 'lxc.apparmor.profile=unconfined'

		sudo systemctl restart apparmor.service
		lxc restart $node
	done

	# Set up ssh
	echo -e "\n--------------------- Set up ssh ---------------------\n"
	# For simplicity, gen one key
	ssh-keygen -t rsa -q -f "id_rsa" -N ""
	cat id_rsa.pub > authorized_keys

	for node in "${cluster[@]}"
	do
		echo -e "\nCreating keys\n"
		lxc exec $node -- mkdir -p /root/.ssh

		# Create key
		lxc file push id_rsa $node/root/.ssh/id_rsa
		lxc file push id_rsa.pub $node/root/.ssh/id_rsa.pub

		#  Share with nodes
		echo -e "\nSharing with nodes\n"

		# ssh-copy-id fails
		lxc file push authorized_keys $node/root/.ssh/authorized_keys
	done

	rm authorized_keys id_rsa id_rsa.pub

}


if [[ $1 == "create" ]]; then
	create_cluster

elif [[ $1 == "start" ]] || [[ $1 == "stop" ]] || [[ $1 == "delete" ]]; then

	for node in "${cluster[@]}"
	do
		lxc $1 $node
	done

else
	echo $USAGE
	echo $COMMANDS
	exit -1
fi