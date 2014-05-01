Files:
-------
Report.txt  : This File contains, my observations and Explanation
src/Makefile: Make file of the Project
src/aos.conf: Configuration file of the project
src/aosproject.java: This file contains source code of the project.
src/Server_Port.log: This file contains the logs of each server.
log : This folder contains project log files.

Instructions To Compile the Project:
------------------------------------
1)Go to the src folder
2)Compile the program by typing "make" command

Instruction To Run Servers(Replicas):
------------------------------------
Syntax: java <javabinary> <Server=0/Client=1> <NodeId>
Javabinary: Compiled binary file for the project
Server/Client: input the value as '0', to run program as Server
NodeId: Node Id of the System ( Node Id and Ipaddresses menstioned in aos.conf file).
Example: java aosproject 0 0

Instruction To Run Client which will responsible to Insert the data in servers:
-------------------------------------------------------------------------------
Syntax: java <javabinary> <Server=0/Client=1> <insert>
Javabinary: Compiled binary file for the project
Server/Client: input the value as '1', to run program as Client
insert: Its an instruction to the program to insert the data into servers
Example: java aosproject 1 insert

Instruction To Run Client which will responsible to Update the data in servers:
-------------------------------------------------------------------------------
Syntax: java <javabinary> <Server=0/Client=1> <update> <employeeDesignation> <DelayBetweenEachReplicaUpdate> <NodeId>
Javabinary: Compiled binary file for the project
Server/Client: input the value as '1', to run program as Client
update: Its an instruction to the program to update the data into servers
employeeRole: Provide the value(TeamLead/Manager/ProjectLead) to update the existing employee designation.
DelayBetweenEachReplicaUpdate: Its a delay between updating the 2 replicas
NodeId: Node Id of the System ( Node Id and Ipaddresses mentioned in aos.conf file).
Example: java aosproject 1 update ProjectHead 8 7

Instruction To Run Client which will responsible to Read the data in servers.
-----------------------------------------------------------------------------
Syntax: java <javabinary> <Server=0/Client=1> <update> <NodeId> 
Javabinary: Compiled binary file for the project
Server/Client: input the value as '1', to run program as Client
read: Its an instruction to the program to update the data into servers
NodeId: Node Id of the System ( Node Id and Ipaddresses menstioned in aos.conf file).
Example: java aosproject 1 read 7
