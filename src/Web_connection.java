import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;
public class Web_connection extends Thread{
	//Socket definition and initialised with null
	protected Socket curr_Socket = null;


	//keeping a limit on requests on a socket to prevent flooding(DOS)
	int keepAliveLimit=10;
	int curr_request=0;

	//constructing a connection using the socket and initialising  the input and output stream 
	public Web_connection(Socket socket) throws IOException{
		curr_Socket=socket;
	}

	// similar to http://www.lnaffah.com/aula3/WebServer.java(resource mentioned by Varsha Mam)
	//function to determine the content type field for the http reply  
	private String detectType(String fileName) {

		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		else if(fileName.endsWith(".css")) {
			return "text/css";
		}
		else if(fileName.endsWith(".jpg")){
			return "image/jpg";
		}
		else if(fileName.endsWith(".jpeg")){
			return "image/jpeg";
		}
		return "application/octet-stream" ;
	}

	//similar to http://www.lnaffah.com/aula3/WebServer.java(resource mentioned by Varsha Mam)
	//function to send the file by using the input file stream and output stream
	private static void sendBytes(FileInputStream fis, 
			OutputStream os) throws Exception {
		// Construct a 1Kilobyte buffer to hold bytes on their way to the socket.
		byte[] buffer = new byte[1024];
		int bytes = 0;
		// Copy requested file into the socket's output stream.
		while ((bytes = fis.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
		}
	}
	private void listenAndSend(){
		try{
			BufferedReader myInput=new BufferedReader(new InputStreamReader(curr_Socket.getInputStream()));
			DataOutputStream myOutput=new DataOutputStream(curr_Socket.getOutputStream());
			String fileName="";//at the end will contain the fileName 
			String httpVersion="";//just a dummy for an if else to determine http version
			String httpMessage=null;//request message taken in this variable
			String Username=null;//to extract the username
			Boolean connectionKeepAlive=false;//to check if the connection is alive
			String requestMade;
			StringTokenizer tokenizedLine;
			//A String Tokenizer:Methods related to the tokenizer have been taken from
			//StackOverflow
			//read the message and tokenize it and take care of the null pointer exception
			try{
				httpMessage = myInput.readLine();
				tokenizedLine =new StringTokenizer(httpMessage);
				requestMade=tokenizedLine.nextToken();
			}
			catch(NullPointerException e){
				return;
			}
			//check the request command
			
			//if request Command is GET try to get the fileName and also keep the count of such requests
			if (requestMade.equals("GET")) 
			{
				fileName = tokenizedLine.nextToken();	
				curr_request++;
			}
			//else return as the program works for the GET request only
			else return;
			//the url is considered of the format  http://ip/~user/filepath
			//remove the first forward slash
			fileName =  fileName.substring(1);
			//also need to get the username seperate from fileName
			//default file name is index.html
			if(fileName.startsWith("~") == true )
			{
				if(fileName.contains("/")){
					int pos=fileName.indexOf('/');
					Username = fileName.substring(0, pos);
					fileName = fileName.substring(pos+1);
					if(fileName.equals(""))fileName="index.html";
				}
				else{
					//System.out.println(fileName);
					Username=fileName;
					fileName="index.html";
				}
			}
			else fileName="";
			//if(Username.equals(null))return;
			//add the public_html folder
			fileName="public_html/"+fileName;
			//get the exact path using bash (snippet from stackoverflow)
			fileName=new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"/bin/sh","-c","echo "+Username+"/"+fileName}).getInputStream())).readLine();
			//System.out.println(fileName);

			boolean auth=false;
			//goes through all the message lines
			while( httpMessage != null ) 
			{
				//breaks if empty
				if(httpMessage.equals("")) break;
				//tokenize the line
				tokenizedLine = new StringTokenizer(httpMessage);
				while(tokenizedLine.hasMoreTokens())
				{
					//get the next token
					String token = tokenizedLine.nextToken();
					//check for the version 
					if(token.startsWith("HTTP")){
						if(token.contains("/1.0"))
							httpVersion = "HTTP/1.0";
						else if(token.contains("/1.1")){
							httpVersion = "HTTP/1.1";
							connectionKeepAlive = true;
						}
					}
					//check for if the client wants a keep alive connection
					else if(token.equals("Connection:")){
						if(tokenizedLine.nextToken().equals("keep-alive")) //checking for connection type
							connectionKeepAlive = true;
						else 
							connectionKeepAlive=false;
					}
					else if(token.equals("Authorization:")){
						//check for the username-password equality
						tokenizedLine.nextToken();
						String uspass=tokenizedLine.nextToken();
						if(uspass.equals("ZGVlcGFuamFuOjEyMw==")){
							auth=true;
						}
					}
				}
				try{
					httpMessage = myInput.readLine();
				}
				catch(Exception e)
				{
					break;
				}
			}
			//if Authorization is not successful send a 401 message
			if(!auth){
				//System.out.println("Authentication Send");
				String statusLine = null;
				String entityBody = null;
				String contentByteLength = null;
				statusLine="HTTP/1.1 401 Access Denied"+"\r\n";
				entityBody="WWW-Authenticate: Basic realm=\"deepanjan\""+"\r\n";
				contentByteLength="Content-Length: 0\r\n";
				myOutput.writeBytes(statusLine);
				myOutput.writeBytes(entityBody);
				myOutput.writeBytes(contentByteLength);
				myOutput.writeBytes("\r\n");
				if(connectionKeepAlive)
					curr_Socket.setKeepAlive(true);
				else
					curr_Socket.setKeepAlive(false);
				return;
			}

			////System.out.println("auth");
			//FileInputStream defined and file existence checked 
			//snippet from http://www.lnaffah.com/aula3/WebServer.java(resource mentioned by Varsha Mam)
			FileInputStream inFile = null ;
			boolean fileExists=true;
			try {
				inFile = new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				fileExists = false ;
			}
			//Open the file note the different parameters of the packet as per the http response
			//Similar to http://www.lnaffah.com/aula3/WebServer.java(resource mentioned by Varsha Mam)
			//Has been improved  as per requirement
			File file=new File(fileName);
			//determine file Size
			int Bytes=(int)file.length();
			//declare and initialise different response packet parameters
			String statusLine = null;
			String contentTypeLine = null;
			String entityBody = null;
			String contentByteLength = null;
			if (fileExists) {
				statusLine = "HTTP/1.1 200 OK\r\n";
				contentTypeLine = "Content-Type: " + 
				detectType(fileName) + "\r\n";
				contentByteLength="Content-Length: "+Bytes+"\r\n";
			} 
			//Send Error 404 if file not present
			else {

				statusLine = "HTTP/1.1 404 Not Found" + "\r\n";
				contentTypeLine = "Content-Type: text/html" + "\r\n";
				entityBody = "<HTML>" + 
				"<HEAD><TITLE>File Not Found</TITLE></HEAD>" +
				"<BODY>"+fileName+" Not Found</BODY></HTML>";
				contentByteLength="Content-Length: "+(64+fileName.length())+"\r\n";
				////System.out.println(contentByteLength);
			}
			//write the bytes to the ouput stream
			myOutput.writeBytes(statusLine);

			myOutput.writeBytes(contentTypeLine);

			myOutput.writeBytes(contentByteLength);

			myOutput.writeBytes("\r\n");
			//send the response packet 
			if (fileExists) {
				sendBytes(inFile, myOutput);
				inFile.close();
			} else {
				myOutput.writeBytes(entityBody) ;
			}
			//System.out.println("Persistent"+curr_request);
			//Updating the KeepAlive of the Socket
			if(connectionKeepAlive)
				curr_Socket.setKeepAlive(true);
			else
				curr_Socket.setKeepAlive(false);
			//if the number of requests in the given persistent connection cross the keepAlive Limit then close the connection(DOS)

			if(curr_request==keepAliveLimit){
				//System.out.println("Limit Reached");
				
				curr_Socket.close();
			}
		}
		catch(SocketTimeoutException e){
			//code for timeout 
			//When the persistent connection times out close the connection
			try {
				//System.out.println("Connection TimeOut");
				
				curr_Socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
			}
		}
		catch(IOException e){
			//e.printStackTrace();
		}
		catch(Exception e){
			//e.printStackTrace();
		}

	}
	//the run method used for thread
	public void run(){
		try{
			//attach a timeout with the socket and keep it alive for persistent connection
			curr_Socket.setSoTimeout(20000);
			//set keep alive true 
			curr_Socket.setKeepAlive(true);
			//the while loop ensures that the socket reads the new requests and new connection is not established
			while(!curr_Socket.isClosed()){
				listenAndSend();
			}
		}
		catch(SocketException e){
			e.printStackTrace();
		}
	}
}