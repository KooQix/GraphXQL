# Requirements

-   Connect to the chameleon cloud instance via ssh

-   lxd needs to be installed

# LXD

-   lxd needs to be initialized

        sudo lxd init

-   lvm storage pool needs to be created (with max size 200GB and every time a container is created, by default root partition has 20GB)

        lxc storage create storage-lvm lvm size=200GB volume.size=24GB

# Init cluster

    ./manage_clusters.sh <command> <num_cluster 1 | 2 |3>

## Commands

-   create
-   start
-   stop
-   delete
