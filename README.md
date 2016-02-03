#clusterNet is a FREE cluster framework for develop java cluster applications.

#Description
clusterNet is initialy based on a novel reliable multicast transport protocol (level 4 of the OSI stack) developed at Universidad de Sevilla. Has been implemented with a herarchical design based on local groups that help minimizing control flow and permit the grow of the numbers of participants to a high level

clusterNet provide a usefull framework to easy develop cluster applications. 
clsuterNet is been used to implement a Scada Multicast Cluster for SmartGrids. At present testing developments are using IEC 60870-5-104 protocol and IEC 61850.

#Tools - Examples
cFtp - a ftp cluster tool 

Use:  java -cp clusterNet_v1.0.jar org.clusternet.ftp.cFtp
	  or launch .jnlp file 


cChat - a chat cluster tool

Use: java -cp clusterNet_v1.0.jar org.clusternet.chat.cChat

cPingSender / cPingReceiver Added mutils. A simple ping tool for send and receive multicast packets to test multicast communications on your cluster network. 

Use:java -cp clusterNet_v1.0.jar org.clusternet.ping.mPingSender  <multicast address> <port> <ttl>
Use:java -cp clusterNet_v1.0.jar org.clusternet.ping.mPingReceiver <multicast address> <port>


Example:
java -cp clusterNet_v1.0.jar org.clusternet.ping.mPingReceiver 224.2.2.2 2000

java -cp clusterNet_v1.0.jar org.clusternet.ping.mPingSender 224.2.2.2 2000 16



#Licence
clusterNet use licence Apache 2.0. Work is in progress.

#Articles-Documents 
M. Alejandro García, Antonio Berrocal, Verónica Medina, Francisco Pérez "Diseño e Implementación de un Protocolo de Transporte Multicast Fiable (PTMF), SIT 2002, ISBN 84-699-9417-4

Enabling Scada Cluster and Cloud for Smart Grid using Hierarchical Multicast; the PTMF Framework Industrial Technology (ICIT), 2015 IEEE International Conference on; March 2015. Available at IEEE Explorer.


