
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Webserver
{

   //serverSocket declared
   public ServerSocket serverSocket;
   //Threadpool to implement a queue
   //java documentation
   //Source: http://docs.oracle.com/javase/tutorial/essential/
   public final ExecutorService pool;
   //HashMaps Used to record a particular IP for preventing DOS
   private static HashMap IPaddr_time=new HashMap();
   private static HashMap IPaddr_num=new HashMap();
   
   //construct the server Socket with the port no.
   public Webserver(int port,int poolSize) throws IOException
   {
      serverSocket = new ServerSocket(port);
      pool=  Executors.newFixedThreadPool(poolSize);
   }
   //function to insert or Update elements into the HashTables
   public void insertIP(String IP,long time)
   {
	   if(IPaddr_num.get(IP)==null)
	   {
		   IPaddr_time.put(IP , time);
	   	   IPaddr_num.put(IP,1);
	   }
	   else{
		   int num=(Integer) IPaddr_num.get(IP);
		   IPaddr_time.remove(IP);
	   	   IPaddr_num.remove(IP);
	   	   IPaddr_time.put(IP , time);
	   	   IPaddr_num.put(IP,num+1);
	   }
   }
   //function to get the previous Used time for a particular IP
   public  long getIpTime(String IP){
	   if(IPaddr_time.get(IP)==null)return -1;
	   long num=(Long) IPaddr_time.get(IP);
	   return num;
   }
   //function to get the count of the Visits of a particular IP
   public  int getIpCount(String IP){
	   if(IPaddr_num.get(IP)==null)return -1;
	   int num=(Integer) IPaddr_num.get(IP);
	   return num;
   }
   //Function to Reset the counts for an IP
   public void updateCount(String IP){
   	   IPaddr_num.remove(IP);
   	   IPaddr_num.put(IP,0);
   }
   //Function to Update the time for an IP
   public void updateTime(String IP,long time){
   	   IPaddr_time.remove(IP);
   	   IPaddr_time.put(IP,time);
   }
}