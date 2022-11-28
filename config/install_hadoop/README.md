# Requirements

-   Instructions from manage_clusters_hadoop/README.md

-   Connect to host

# Download Hadoop (version 3.3.4)

    $ wget https://archive.apache.org/dist/hadoop/core/hadoop-3.3.4/hadoop-3.3.4.tar.gz

    $ tar -xvf hadoop-3.3.4.tar.gz

    $ sudo mkdir -p /exports/hadoop

    $ sudo chown cc:wheel /exports/hadoop

    $ mv hadoop-3.3.4/* /exports/hadoop

/exports/hadoop will be shared and mounted by Hadoop nodes at /opt/hadoop

# Configuration

hadoop-namenode, hadoop-datanode-i will be added to /etc/hosts \
Resource manager and history manager will be on the namenode here

-   Go inside hadoop directory/etc/hadoop (Hadoop directory will be inside /opt on nodes)

-   **core-site.xml**

        <configuration>
        	<property>
        		<name>fs.default.name</name>
        		<value>hdfs://hadoop-namenode:9000/</value>
        	</property>
        	<property>
        		<name>dfs.permissions</name>
        		<value>false</value>
        	</property>
        </configuration>

-   **hdfs-site.xml**

        <configuration>
        	<property>
        		<name>dfs.replication</name>
        		<value>2</value>
        	</property>

        	<property>
        			<name>dfs.namenode.name.dir</name>
        			<value>/home/ubuntu/hdfs-metadata</value>
        	</property>
        	<property>
        			<name>dfs.blocksize</name>
        			<value>67108864</value>
        	</property>
        	<property>
        			<name>dfs.namenode.handler.count</name>
        			<value>100</value>
        	</property>
        	<property>
        			<name>dfs.datanode.data.dir</name>
        			<value>/home/ubuntu/hdfs-data</value>
        	</property>

        	<property>
        		<name>dfs.permissions.enabled</name>
        		<value>true</value>
        	</property>
        </configuration>

-   **mapred-site.xml**

        <configuration>
        	<property>
        		<name>mapred.job.tracker</name>
        		<value>hadoop-namenode:9001</value>
        	</property>
        	<property>
        		<name>mapreduce.framework.name</name>
        		<value>yarn</value>
        	</property>
        	<property>
        		<name>mapreduce.jobhistory.address</name>
        		<value>hadoop-namenode:10020</value> </property>
        	<property>
        	<name>mapreduce.jobhistory.webapp.address</name>
        	<value>hadoop-namenode:19888</value>
        	</property>
        	<property>
        		<name>yarn.app.mapreduce.am.env</name>
        		<value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
        	</property>
        	<property>
        			<name>mapreduce.map.env</name>
        			<value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
        	</property>
        	<property>
        			<name>mapreduce.reduce.env</name>
        			<value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
        	</property>
        </configuration>

-   **hadoop-env.sh** (default jdk version installed is 11 when 'sudo apt install default-jdk', may change if needed)

        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
        export HADOOP_OPTS=-Djava.net.preferIPv4Stack=true
        export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop
        export HDFS_NAMENODE_USER="root"
        export HDFS_DATANODE_USER="root"
        export HDFS_SECONDARYNAMENODE_USER="root"
        export YARN_RESOURCEMANAGER_USER="root"
        export YARN_NODEMANAGER_USER="root"

-   **yarn-site.xml**

        <configuration>
        	<property>
        		<name>yarn.nodemanager.aux-services</name>
        		<value>mapreduce_shuffle</value>
        	</property>


        	<property>
        		<name>yarn.resourcemanager.hostname</name>
        		<value>hadoop-namenode</value>
        	</property>
        	<property>
        		<name>yarn.nodemanager.log-dirs</name>
        		<value>/home/ubuntu/hdfs-logs</value>
        	</property>
        	<property>
        		<name>yarn.nodemanager.vmem-check-enabled</name>
        		<value>false</value>
        	</property>


        	<property>
        		<name>yarn.nodemanager.resource.memory-mb</name>
        		<value>12000</value>
        	</property>

        	<property>
        		<name>yarn.app.mapreduce.am.resource.mb</name>
        		<value>11000</value>
        	</property>
        	<property>
        		<name>yarn.nodemanager.resource.cpu-vcores</name>
        		<value>3</value>
        	</property>
        	<property>
        		<name>yarn.app.mapreduce.am.resource.cpu-vcores</name>
        		<value>3</value>
        	</property>
        </configuration>

-   Verify taken into account (inside namenode)

        $ yarn node -list (get node-id)
        $ yarn node -status node-id

-   else (not taken into account)

        $ cd $HADOOP_HOME/sbin
        $ ./stop-yarn.sh
        $ ./start-yarn.sh

# Mount hadoop folder

On the host:

    $ sudo apt install nfs-kernel-server
    $ sudo vim /etc/idmapd.conf
    # uncomment line 6
    Domain = localdomain
    $ sudo vim /etc/exports
    /exports/hadoop 10.133.76.0/24(rw,no_root_squash)
    $ sudo systemctl restart nfs-server.service
    $ sudo systemctl status nfs-server.service

# Init hadoop cluster

-   Init:

        sudo ./init_hadoop_cluster.sh <num_cluster 1 | 2 | 3> <ip host> <file master_ip> <file slaves_ip>

-   Follow instructions during initialization

# References

-   https://www.edureka.co/blog/setting-up-a-multi-node-cluster-in-hadoop-2-x/

-   https://www.tutorialspoint.com/hadoop/hadoop_multi_node_cluster.htm

# Resources

## Yarn documentation

https://hadoop.apache.org/docs/r3.1.0/hadoop-yarn/hadoop-yarn-site/ResourceModel.html
