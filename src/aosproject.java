/*
 * Filename    : aosproject.java
 * Author      : Shruthi Deshpande
 * Description : This file contains the implementation of distrubuted databse simulation mechanism using Amazon Dynamo and Google Filesystem concepts.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.net.InetAddress;

/*
 * ClassName   : SampleServer
 * Description : This class creates the TCP Socket Server and Listen on client messages.
 */
class SampleServer extends Thread
{
	private Map<String, Object> ObjectMap = new HashMap<String, Object>();
	public void run()
	{
		nodeInfo ninfo=nodeInfo.getInstance();
		Object obj;
		System.out.println("Server Started: Waiting For Client Messages");
		/*Establish node reliable socket connections (TCP) between each pair of nodes. The algorithm messages are sent over
		these connections*/
		try
		{
			String fileName="Server_"+ninfo.getServerNode()+".log";

			Logger logger = Logger.getLogger(fileName);  
			FileHandler fh; 

			// This block configure the logger with handler and formatter  
			fh = new FileHandler(fileName);  
			logger.addHandler(fh);

			LogFormatter formatter = new LogFormatter();  
			//formatter.
			fh.setFormatter(formatter);
			logger.setUseParentHandlers(false);

			File file = new File(fileName);
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter writer = new PrintWriter(bw);
			List<String> serverdetails = ninfo.getNodeDetails(ninfo.getServerNode());
			ServerSocket serverSock = new ServerSocket(Integer.parseInt(serverdetails.get(1)));
			Socket sock;
			//Server goes into a permanent loop accepting connections from clients until exitStatus is true			
			while(true)
			{
				//Listens for a connection to be made to this socket and accepts it
				//The method blocks until a connection is made
				sock = serverSock.accept();
				ObjectInputStream objectreader = new ObjectInputStream(sock.getInputStream());
				obj= (Object)objectreader.readObject();
				if(obj.equals(null))
				{
					System.out.println("Read Emptry Object");
				}else{

					if(obj.getAction().equals("insert"))
					{
						System.out.print("[INSERT]::");
						System.out.println(obj.getEmployeeId()+" "+obj.getEmployeeName()+" "+obj.getEmployeeDesignation()+" "+obj.getEmployeeSalary());
						logger.info("[INSERT]::"+obj.getEmployeeId()+" "+obj.getEmployeeName()+" "+obj.getEmployeeDesignation()+" "+obj.getEmployeeSalary());
						ObjectMap.put(obj.getEmployeeId(), obj);

					}else if(obj.getAction().equals("update"))
					{
						System.out.print("[UPDATE]::");
						System.out.println(obj.getEmployeeId()+" "+obj.getEmployeeName()+" "+obj.getEmployeeDesignation()+" "+obj.getEmployeeSalary());
						logger.info("[UPDATE]::"+obj.getEmployeeId()+" "+obj.getEmployeeName()+" "+obj.getEmployeeDesignation()+" "+obj.getEmployeeSalary());
						Object updateObj=new Object();
						Object existingObj = new Object();
						updateObj.setEmployeeId(obj.getEmployeeId());
						existingObj = ObjectMap.get(obj.getEmployeeId());
						if(!obj.getEmployeeName().isEmpty())
						{
							updateObj.setEmployeeName(obj.getEmployeeName());
						}else{
							updateObj.setEmployeeName(existingObj.getEmployeeName());
						}
						if(!obj.getEmployeeDesignation().isEmpty())
						{
							updateObj.setEmployeeDesignation(obj.getEmployeeDesignation());
						}else{
							updateObj.setEmployeeName(existingObj.getEmployeeDesignation());
						}
						if(obj.getEmployeeSalary() !=-1 )
						{
							updateObj.setEmployeeSalary(obj.getEmployeeSalary());
						}else{
							updateObj.setEmployeeSalary(existingObj.getEmployeeSalary());
						}
						ObjectMap.remove(obj.getEmployeeId());
						ObjectMap.put(obj.getEmployeeId(), updateObj);
					}else if(obj.getAction().equals("delete"))
					{
						System.out.println("[DELETE]::");
						System.out.println(obj.getEmployeeId());
						logger.info("[DELETE]::"+obj.getEmployeeId());
						ObjectMap.remove(obj.getEmployeeId());
					}else if(obj.getAction().equals("read"))
					{
						//System.out.print("Read Request Received For EmpId= :"+obj.getEmployeeId()+" ::");
						SampleClient SC = new SampleClient();
						for (Map.Entry<String, Object> entry : ObjectMap.entrySet()) {
							String EmployeeId = entry.getKey();
							Object EmpRecord = entry.getValue();
							if(EmployeeId.equals(obj.getEmployeeId()))
							{
								//return EmpRecord; Write on to Client Socket
								List<String> clientdetails = ninfo.getNodeDetails(obj.getClientNodeId());
								//System.out.println("Read Response Sent To "+clientdetails.get(0)+" "+clientdetails.get(1));
								while(true)
								{
									try{
										SC.sendMessage(EmpRecord,clientdetails.get(0),Integer.parseInt(clientdetails.get(1)));
										break;
									}catch(ServerUnAvailableException SUA)
									{
										try{
											Thread.sleep(10);
										}catch(Exception e)
										{

										}
									}
								}
							}
						}

					}else{

					}


				}


				objectreader.close();
			}
		}
		catch(Exception ex)
		{
			System.out.println(" Exception in Server"+ex.getMessage());
		}

	}
}

class HeartBeat extends Thread
{
	public void run()
	{
		nodeInfo ninfo=nodeInfo.getInstance();
		try
		{
			System.out.println("HeartBeat Thread Listenining on port "+ninfo.getHeartBeatPort());
			ServerSocket serverSock = new ServerSocket(ninfo.getHeartBeatPort());
			Socket sock;
			//Server goes into a permanent loop accepting connections from clients until exitStatus is true	
			Object obj;
			SampleClient SC = new SampleClient();
			int NumberOfReplicas=0;
			int AcceptsReceived=0;
			boolean AlreadyRespondedToOtherMaster=false;
			List<Integer> Replicas=new ArrayList<Integer>();
			Object prevObj;
			Vector<Object> buffer= new Vector<Object>();
			while(true)
			{
				//Listens for a connection to be made to this socket and accepts it
				//The method blocks until a connection is made
				sock = serverSock.accept();
				ObjectInputStream objectreader = new ObjectInputStream(sock.getInputStream());
				obj= (Object)objectreader.readObject();
				if(obj.getAction().equals("CHECKACCEPTANCE"))
				{
					Replicas = obj.getReplicaList();
					int i=0;
					ninfo.setHbeatClient(obj.getClientNodeId());
						NumberOfReplicas=Replicas.size();
						while(i < Replicas.size())
						{
							Object hbeatObject = new Object();
							hbeatObject.setAction("HEARTBEAT");
							hbeatObject.setClientNodeId(ninfo.getServerNode());
							List<String> Replicadetails = ninfo.getNodeDetails(Replicas.get(i));
							if(Replicas.get(i) != ninfo.getServerNode())
							{
								SC.sendMessage(hbeatObject,Replicadetails.get(0),Integer.parseInt(Replicadetails.get(1))+1000);
							}
							i++;
						}

				}else if(obj.getAction().equals("HEARTBEAT"))
				{
					Object hbeatObject = new Object();
					hbeatObject.setClientNodeId(ninfo.getServerNode());
					if(!AlreadyRespondedToOtherMaster)
					{
						hbeatObject.setAction("ACCEPT");
						AlreadyRespondedToOtherMaster=true;
					}else{
						hbeatObject.setAction("REJECT");
					}
					List<String> Replicadetails = ninfo.getNodeDetails(obj.getClientNodeId());
					SC.sendMessage(hbeatObject,Replicadetails.get(0),(Integer.parseInt(Replicadetails.get(1))+1000));

				}else if(obj.getAction().equals("ACCEPT"))
				{
					AcceptsReceived++;
					if(AcceptsReceived == (NumberOfReplicas-1))
					{
						List<String> Replicadetails = ninfo.getNodeDetails(ninfo.getHbeatClient());
						Object hbeatObject = new Object();
						hbeatObject.setAction("GOAHEAD");
						SC.sendMessage(hbeatObject,Replicadetails.get(0),Integer.parseInt(Replicadetails.get(1)));
						AcceptsReceived=0;
						int i=0;
							while(i < Replicas.size())
							{
								Object releaseObject = new Object();
								releaseObject.setAction("RELEASE");
							
								List<String> serverdetails = ninfo.getNodeDetails(Replicas.get(i));
								//System.out.println("Sending RELEASE to "+serverdetails.get(0)+" "+serverdetails.get(1));
								if(Replicas.get(i) != ninfo.getServerNode())
								{
									SC.sendMessage(releaseObject,serverdetails.get(0),Integer.parseInt(serverdetails.get(1))+1000);
								}
								i++;
							}
						
						
						
						
					}

				}else if(obj.getAction().equals("REJECT"))
				{
					List<String> Replicadetails = ninfo.getNodeDetails(ninfo.getHbeatClient());
					Object hbeatObject = new Object();
					hbeatObject.setAction("DISCARD");
					SC.sendMessage(hbeatObject,Replicadetails.get(0),Integer.parseInt(Replicadetails.get(1)));
					AcceptsReceived=0;
					
					int i=0;
					while(i < Replicas.size())
					{
						Object releaseObject = new Object();
						releaseObject.setAction("RELEASE");
					
						List<String> serverdetails = ninfo.getNodeDetails(Replicas.get(i));
						//System.out.println("Sending RELEASE to "+serverdetails.get(0)+" "+serverdetails.get(1));
						if((Replicas.get(i) != ninfo.getServerNode()) && (Replicas.get(i) != obj.getClientNodeId()))
						{
							SC.sendMessage(releaseObject,serverdetails.get(0),Integer.parseInt(serverdetails.get(1))+1000);
						}
						i++;
					}

				}else if(obj.getAction().equals("RELEASE"))
				{
					AlreadyRespondedToOtherMaster = false;

				}

			}
		}catch(Exception Ex)
		{

		}
	}
}

class ServerUnAvailableException extends Exception {

	public ServerUnAvailableException(String message){
		super(message);
	}

}

class Object implements Serializable{

	/**
	 * 
	 */
	Object(){
		action="";
		employeeName="";
		employeeDesignation="";
		employeeId="";
		employeeSalary=-1;
		clientNodeId=0;
		Replicas=new ArrayList<Integer>();
	}

	public List<Integer> getReplicaList()
	{
		return Replicas;
	}

	public void insertReplica(int id)
	{
		Replicas.add(id);
	}
	public void setAction(String iAction)
	{
		action=iAction;
	}

	public int getClientNodeId()
	{
		return clientNodeId;
	}
	public void setClientNodeId(int inodeId)
	{
		clientNodeId=inodeId;
	}
	public void setEmployeeName(String iename)
	{
		employeeName=iename;
	}

	public void setEmployeeDesignation(String idesignation)
	{
		employeeDesignation=idesignation;
	}

	public void setEmployeeSalary(int isalary)
	{
		employeeSalary=isalary;
	}
	public void setEmployeeId(String iempId)
	{
		employeeId=iempId;
	}

	public String getAction()
	{
		return action;
	}

	public String getEmployeeName()
	{
		return employeeName;
	}

	public String getEmployeeDesignation()
	{
		return employeeDesignation;
	}

	public int getEmployeeSalary()
	{
		return employeeSalary;
	}
	public String getEmployeeId()
	{
		return employeeId;
	}

	private String action;
	private String employeeName;
	private String employeeDesignation;
	private String employeeId;
	private int employeeSalary;
	private int clientNodeId;
	private List<Integer> Replicas;


}

/*
 * ClassName   : SampleClient
 * Description : This class Send a Messages to Socket Server
 */
class SampleClient extends Thread
{

	public void checkServerAvailability(String nodeAddress, int nodeport) throws ServerUnAvailableException
	{
		Object obj = new Object();
		obj.setAction("CHECK");
		try
		{
			Socket clientSocket = new Socket(nodeAddress,nodeport);
			ObjectOutputStream objectwriter = new ObjectOutputStream(clientSocket.getOutputStream());
			objectwriter.writeObject(obj);
			objectwriter.close();
			clientSocket.close();
		}catch(Exception ex)
		{
			throw new ServerUnAvailableException("Server UnAvailable");
		}
	}

	public void checkAcceptanceOfServers(Object obj,String nodeAddress, int nodeport) throws ServerUnAvailableException
	{
		try
		{
			Socket clientSocket = new Socket(nodeAddress,nodeport);
			ObjectOutputStream objectwriter = new ObjectOutputStream(clientSocket.getOutputStream());
			objectwriter.writeObject(obj);
			objectwriter.close();
			clientSocket.close();
		}catch(Exception ex)
		{
			throw new ServerUnAvailableException("Server is Not Ready to Accept");
		}
	}


	public void sendMessage(Object obj, String nodeAddress, int nodeport) throws ServerUnAvailableException
	{
		//String message=msg;
		try
		{
			Socket clientSocket = new Socket(nodeAddress,nodeport);
			ObjectOutputStream objectwriter = new ObjectOutputStream(clientSocket.getOutputStream());
			objectwriter.writeObject(obj);
			objectwriter.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			throw new ServerUnAvailableException("Server UnAvailable");
		}
	}

	public void run()
	{
		String message ="";
		int i=0;
		int firstReplica=0;
		int secondReplica=0;
		int thirdReplica=0;

		List<String> firstReplicaDetails;
		List<String> secondReplicaDetails;
		List<String> thirdReplicaDetails;

		nodeInfo ninfo=nodeInfo.getInstance();

		int AvailableReplicas;
		int index=0;
		int empId=100;
		while(true)
		{
			Object obj= new Object();
			boolean firstReplicaAvailability=true;
			boolean secondReplicaAvailability=true;
			boolean thirdReplicaAvailability=true;
			AvailableReplicas=3;
			empId++;
			if(ninfo.getCommand().equals("insert"))
			{
				String line = "insert "+empId+" Shruthi "+"SoftwareEngineer "+"120000";

				String[] parts=line.split(" ");

				obj.setAction(parts[0]);
				obj.setEmployeeId(parts[1]);
				obj.setEmployeeName(parts[2]);
				obj.setEmployeeDesignation(parts[3]);
				obj.setEmployeeSalary(Integer.parseInt(parts[4]));

				firstReplica=(obj.getEmployeeId().hashCode())%7;
				firstReplicaDetails = ninfo.getNodeDetails(firstReplica);
				try{
					checkServerAvailability(firstReplicaDetails.get(0),Integer.parseInt(firstReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					firstReplicaAvailability=false;
				}
				//Checking whether 2nd replica's availability
				secondReplica=(obj.getEmployeeId().hashCode()+1)%7;
				secondReplicaDetails = ninfo.getNodeDetails(secondReplica);
				try{
					checkServerAvailability(secondReplicaDetails.get(0),Integer.parseInt(secondReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					secondReplicaAvailability=false;
				}
				//Checking whether 3rd replica's availability
				thirdReplica=(obj.getEmployeeId().hashCode()+2)%7;
				thirdReplicaDetails = ninfo.getNodeDetails(thirdReplica);
				try{
					checkServerAvailability(thirdReplicaDetails.get(0),Integer.parseInt(thirdReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					thirdReplicaAvailability=false;
				}

				System.out.println("AvailableReplicas ="+AvailableReplicas);
				if(AvailableReplicas>=2)
				{
					if(firstReplicaAvailability)
					{
						try{
							sendMessage(obj,firstReplicaDetails.get(0),Integer.parseInt(firstReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+firstReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+firstReplicaDetails.get(0)+" Failed");
						}
					}

					if(secondReplicaAvailability)
					{
						try{
							sendMessage(obj,secondReplicaDetails.get(0),Integer.parseInt(secondReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+secondReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+secondReplicaDetails.get(0)+" Failed");
						}
					}

					if(thirdReplicaAvailability)
					{
						try{
							sendMessage(obj,thirdReplicaDetails.get(0),Integer.parseInt(thirdReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+thirdReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+thirdReplicaDetails.get(0)+" Failed");
						}
					}

				}else{
					System.out.println("Cannot Perform Update because lessthan 2 replicas available");
				}

				try{
					Thread.sleep(2000);
				}catch(Exception exx)
				{

				}
			}else if(ninfo.getCommand().equals("update"))
			{
				String line = "update "+empId+" Shruthi "+ninfo.getUpdateString()+" 200000";

				String[] parts=line.split(" ");
				Object updateObj=new Object();
				obj.setAction(parts[0]);
				obj.setEmployeeId(parts[1]);
				if(!parts[2].isEmpty())
				{
					obj.setEmployeeName(parts[2]);
				}if(!parts[3].isEmpty())
				{
					obj.setEmployeeDesignation(parts[3]);
				}if(!parts[4].isEmpty())
				{
					obj.setEmployeeSalary(Integer.parseInt(parts[4]));
				}
				firstReplica=(obj.getEmployeeId().hashCode())%7;
				firstReplicaDetails = ninfo.getNodeDetails(firstReplica);
				try{
					checkServerAvailability(firstReplicaDetails.get(0),Integer.parseInt(firstReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					firstReplicaAvailability=false;
				}
				//Checking whether 2nd replica's availability
				secondReplica=(obj.getEmployeeId().hashCode()+1)%7;
				secondReplicaDetails = ninfo.getNodeDetails(secondReplica);
				try{
					checkServerAvailability(secondReplicaDetails.get(0),Integer.parseInt(secondReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					secondReplicaAvailability=false;
				}
				//Checking whether 3rd replica's availability
				thirdReplica=(obj.getEmployeeId().hashCode()+2)%7;
				thirdReplicaDetails = ninfo.getNodeDetails(thirdReplica);
				try{
					checkServerAvailability(thirdReplicaDetails.get(0),Integer.parseInt(thirdReplicaDetails.get(1)));
				}catch(ServerUnAvailableException SUA)
				{
					AvailableReplicas--;
					thirdReplicaAvailability=false;
				}

				System.out.println("AvailableReplicas ="+AvailableReplicas);
				if(AvailableReplicas>=2)
				{
					Object checkObj=new Object();
					checkObj.setAction("CHECKACCEPTANCE");
					int MasterNode=-1;
					if(firstReplicaAvailability)
					{
						checkObj.insertReplica(firstReplica);
						if(MasterNode == -1)
						{
							MasterNode=firstReplica;
						}
					}
					if(secondReplicaAvailability)
					{
						checkObj.insertReplica(secondReplica);
						if(MasterNode == -1)
						{
							MasterNode=secondReplica;
						}
					}

					if(thirdReplicaAvailability)
					{
						checkObj.insertReplica(thirdReplica);
						if(MasterNode == -1)
						{
							MasterNode=thirdReplica;
						}
					}
					checkObj.setClientNodeId(ninfo.getClientNodeId());
					List<String> MasterNodeDetails = ninfo.getNodeDetails(MasterNode);
					try
					{
						List<String> clientdetails = ninfo.getNodeDetails(ninfo.getClientNodeId());
						ServerSocket serverSock = new ServerSocket(Integer.parseInt(clientdetails.get(1)));
						Socket sock;
						checkAcceptanceOfServers(checkObj,MasterNodeDetails.get(0),(Integer.parseInt(MasterNodeDetails.get(1))+1000));
						sock = serverSock.accept();
						ObjectInputStream objectreader = new ObjectInputStream(sock.getInputStream());
						checkObj= (Object)objectreader.readObject();
						System.out.println(checkObj.getAction()+"Received From Master Replica");
						serverSock.close();

					}catch(Exception e)
					{
						System.out.println("Exception in checkAcceptanceOfServers "+e.getMessage());
					}
					if(firstReplicaAvailability)
					{

						try{
							sendMessage(obj,firstReplicaDetails.get(0),Integer.parseInt(firstReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+firstReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+firstReplicaDetails.get(0)+" Failed");
						}
						try{
							Thread.sleep(ninfo.getFrequency());
						}catch(Exception exx)
						{

						}
					}

					if(secondReplicaAvailability)
					{
						try{
							sendMessage(obj,secondReplicaDetails.get(0),Integer.parseInt(secondReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+secondReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+secondReplicaDetails.get(0)+" Failed");
						}
						try{
							Thread.sleep(ninfo.getFrequency());
						}catch(Exception exx)
						{

						}
					}

					if(thirdReplicaAvailability)
					{
						try{
							sendMessage(obj,thirdReplicaDetails.get(0),Integer.parseInt(thirdReplicaDetails.get(1)));
							System.out.println("[SENT] "+line+" in Replica "+thirdReplica);
						}catch(ServerUnAvailableException SUA)
						{
							System.out.println("Writting in Replica "+thirdReplicaDetails.get(0)+" Failed");
						}
					}
					try{
						Thread.sleep(ninfo.getFrequency());
					}catch(Exception exx)
					{

					}
				}else{
					System.out.println("Cannot Perform Update because lessthan 2 replicas available");
				}

				try{
					Thread.sleep(500);
				}catch(Exception exx)
				{

				}
			}else if(ninfo.getCommand().equals("read"))
			{
				String line = "read "+empId+" record";

				String[] parts=line.split(" ");
				obj.setAction(parts[0]);
				obj.setEmployeeId(parts[1]);
				obj.setClientNodeId(ninfo.getClientNodeId());

				Random rand = new Random();

				int Replica =(rand.nextInt(((obj.getEmployeeId().hashCode()+2)-(obj.getEmployeeId().hashCode())) + 1) + (obj.getEmployeeId().hashCode()))%7;

				System.out.println("[READING]"+obj.getEmployeeId()+" record from "+Replica);
				List<String> replicaDetails = ninfo.getNodeDetails(Replica);
				try{
					List<String> clientdetails = ninfo.getNodeDetails(ninfo.getClientNodeId());
					ServerSocket serverSock = new ServerSocket(Integer.parseInt(clientdetails.get(1)));
					Socket sock;
					//Listens for a connection to be made to this socket and accepts it
					//The method blocks until a connection is made
					sendMessage(obj,replicaDetails.get(0),Integer.parseInt(replicaDetails.get(1)));
					sock = serverSock.accept();
					ObjectInputStream objectreader = new ObjectInputStream(sock.getInputStream());
					obj= (Object)objectreader.readObject();
					System.out.println("[RECORD] "+obj.getEmployeeId()+" "+obj.getEmployeeName()+" "+obj.getEmployeeDesignation()+" "+obj.getEmployeeSalary());
					serverSock.close();
				}catch(Exception e)
				{
					System.out.println("Client Socket Failed");
				}

				try{
					Thread.sleep(1000);
				}catch(Exception exx)
				{

				}
			}



		}		
	}
}


class nodeInfo {

	private static nodeInfo instance = null;
	protected nodeInfo() {
		// Exists only to defeat instantiation.
	}
	public static nodeInfo getInstance() {
		if(instance == null) {
			instance = new nodeInfo();
		}
		return instance;
	}

	List<String> getNodeDetails(int nodeId)
	{
		List<String> values =new ArrayList<String>();

		for (Map.Entry<Integer, List<String>> entry : nodeIn.entrySet()) {
			int key = entry.getKey();
			if(key == nodeId)
			{
				values = entry.getValue();
				return values;

			}
		}
		return values;
	}

	public int getHbeatClient()
	{
		return HBclientNodeId;
	}

	public void setHbeatClient(int inode)
	{
		HBclientNodeId=inode;
	}

	int getClientNodeId()
	{
		return clientNodeId;
	}
	void setClientNodeId(int inodeId)
	{
		clientNodeId=inodeId;
	}
	String getAddress(){
		return address;
	}
	void setAddress(String iaddress)
	{
		address=iaddress;
	}
	String getCommand(){
		return command;
	}
	void setCommand(String icommand){
		command=icommand;
	}
	int getHeartBeatPort(){
		return HBport;
	}
	void setHeartBeatPort(int iport){
		HBport=iport;
	}
	int getServerNode(){
		return serverNode;
	}
	void setServerNode(int iport){
		serverNode=iport;
	}
	int gettotalNodes()
	{
		return totalnodes;
	}
	void setTotalNodes(int nodes)
	{
		totalnodes=nodes;
	}
	String getUpdateString()
	{
		return updateText;
	}

	void setUpdateString(String iUpdateText)
	{
		updateText=iUpdateText;
	}
	int getFrequency()
	{
		return frequency;
	}

	void setFrequency(int iFrequency)
	{
		frequency=iFrequency;
	}
	Map<Integer,List<String>> getMap()
	{
		return nodeIn;
	}

	void setInfo(Integer nodeId, String address, String port )
	{
		List<String> infoList = new ArrayList<String>();
		infoList.add(address);
		infoList.add(port);
		nodeIn.put(nodeId, infoList);
		nodeIn.keySet();
	}
	private String address;
	private int serverNode;
	private int HBport;
	private int totalnodes;
	private int frequency;
	private int clientNodeId;
	private int HBclientNodeId;
	private String command;
	private String updateText;
	private Map<Integer, List<String>> nodeIn = new HashMap<Integer, List<String>>();
}



/*
 * ClassName   : RAmutualexclusion
 * Description : This is the main class of the program, which is responsible to start the socket threads, and algorithm.
 */
public class aosproject
{
	public static void main(String[] args)
	{
		nodeInfo ninfo = nodeInfo.getInstance();
		try{
			BufferedReader reader = new BufferedReader(new FileReader("aos.conf"));
			String line = null;
			String[] parts;
			boolean first=true;

			//nodeInfo.port=Integer.parseInt(args[0]);
			while ((line = reader.readLine()) != null) {
				//System.out.println(line);
				parts = line.split(" ");
				for (String part : parts) {
					if(part.equals("#"))
					{
						break;
					}
					if(first)
					{
						ninfo.setTotalNodes(Integer.parseInt(part));
						first=false;
					}else{
						ninfo.setInfo(Integer.parseInt(parts[0]),parts[1],parts[2]);
						break;
					}
					//System.out.println(part);
				}
			}
		}catch(Exception e)
		{
			System.out.println(e.getMessage());
		}


		try{
			Thread.sleep(5);
		}catch(Exception e)
		{
		}
		if(Integer.parseInt(args[0])==0)
		{
			ninfo.setServerNode(Integer.parseInt(args[1]));
			List<String> HBServerdetails = ninfo.getNodeDetails(Integer.parseInt(args[1]));
			ninfo.setHeartBeatPort(Integer.parseInt(HBServerdetails.get(1))+1000);
			HeartBeat HeartBeatObj= new HeartBeat();
			HeartBeatObj.start();
			SampleServer SampleServerObj = new SampleServer();
			SampleServerObj.start();

		}else{
			ninfo.setCommand(args[1]);
			if(args[1].equals("read"))
			{
				ninfo.setClientNodeId(Integer.parseInt(args[2]));
			}else if(args[1].equals("update"))
			{
				ninfo.setUpdateString(args[2]);	
				ninfo.setFrequency(Integer.parseInt(args[3]));
				ninfo.setClientNodeId(Integer.parseInt(args[4]));
			}else
			{

			}



			SampleClient SampleClientObj = new SampleClient();
			SampleClientObj.start();
		}


	}
}

class LogFormatter extends Formatter 
{   
	public LogFormatter() { super(); }

	@Override 
	public String format(final LogRecord record) 
	{
		return record.getMessage()+"\n";
	}   
}
