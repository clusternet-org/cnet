#clusterNet is a FREE cluster framework for develop java cluster applications.

#Description
clusterNet is initialy based on a novel reliable multicast transport protocol (level 4 of the OSI stack) developed at Universidad de Sevilla. Has been implemented with a herarchical design based on local groups that help minimizing control flow and permit the grow of the numbers of participants to a high level

Tools for testing multicast communications has been developed to assist network administer. 

clusterNet provide a usefull framework to easy develop cluster applications. 
clsuterNet is been used to implement a Scada Multicast Cluster for SmartGrids. At present testing developments are using IEC 60870-5-104 protocol and IEC 61850.


#Tools

#mCopy - a multi host file copy cluster tool 
This tool let transmit a file simultaneously to a number of receivers without replicate the information send over the network. 
Use:  java -cp ./cNetv1.0.jar cnet.mcopy.runGUI

	  
	  
#mChat - a multi chat cluster tool
This tool let chat between different users using a decentralized system 
Use: java -cp ./cNetv1.0.jar cnet.mchat.runGUI


#mPing - a simple multicast ping tool
mPing is a simple multicast ping to test multicast capabilities in your network. It send and receive packets to/from a multicast IPv4 address and port. TTL scope could be set and also different payloads to increase packet local. 
Use: java -cp ./cNetv1.0.jar cnet.util.mping 

Example:
java -cp ./cNetv1.0.jar cnet.util.mping 224.1.1.100 2020 32 32


#Multicast ping tool cPingSender / cPingReceiver
cPingSender / cPingReceiver  are anothers simple ping tools for send and receive multicast packets to test multicast communications on your cluster network. 
Use:java -cp ./cNetv1.0.jar cnet.util.mPingReceiver <multicast address> <port>
Use:java -cp ./cNetv1.0.jar cnet.util.mPingSender  <multicast address> <port> <ttl>


Example:
java -cp -cp ./cNetv1.0.jar cnet.util.mPingReceiver 224.2.2.2 2000
java -cp ./cNetv1.0.jar cnet.util.mPingSender 224.2.2.2 2000 16


#Licence
clusterNet use licence Apache 2.0. Work is in progress.

#Articles-Documents 
M. Alejandro García, Antonio Berrocal, Verónica Medina, Francisco Pérez "Diseño e Implementación de un Protocolo de Transporte Multicast Fiable (PTMF), SIT 2002, ISBN 84-699-9417-4

Alejandro García, "Enabling Scada Cluster and Cloud for Smart Grid using Hierarchical Multicast; the PTMF Framework" Industrial Technology (ICIT), 2015 IEEE International Conference on; March 2015. Available at IEEE Explorer.


