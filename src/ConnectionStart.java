import java.io.IOException;
import java.net.Socket;
import java.util.Date;

//class for defining the main function to start a connection
public class ConnectionStart {
	//mainfunction
	public static void main(String [] args)
	{
		int port = 8082;//default port used
		int poolSize= 100;//for queueing purposes
		try
		{
			//Initialising the WebServer
			Webserver myServer=new Webserver(port, poolSize);
			//starting a connection
			while(true){
				//defining a null Socket
				Socket clientSocket = null;
				//System.out.println("NewConnection");
				//serverSocket accepts the connection from client and defines the socket
				clientSocket = myServer.serverSocket.accept();
				//tUse the IP Address information for mapping
				//the Mapping is Used for prevention of DOS
				String IP=clientSocket.getInetAddress().toString();
				//get the current time
				Date date=new Date();
				long current=date.getTime();
				//get the number of visits of this IP
				int count=myServer.getIpCount(IP);
				//System.out.println(IP+"Connection no. "+count);
				//if count becomes greater than a certain limitclose the connection
				//and note the current time
				if(count==5){
					myServer.insertIP(IP,current);
					clientSocket.close();
					continue;
				}
				//then wait till the time gap increases 100 s and then set the 
				//count 0
				else if(count>5){
					long previous=myServer.getIpTime(IP);
					////System.out.println(current-previous);
					if(current-previous>100000){
						myServer.updateTime(IP, current);
						myServer.updateCount(IP);
					}
					else{
						clientSocket.close();
						continue;
					}
				}
				myServer.insertIP(IP,current);
				//and starts a thread : MULTI THREADING
				myServer.pool.execute(new Web_connection(clientSocket));
			}
		}catch(IOException e)
		{
			//e.printStackTrace();
		}
	}

}