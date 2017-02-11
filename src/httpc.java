import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
* 445 networking
*
* Jia Jun Chen   27804954
* Erin Benderoff 27768478
* Lab Assignment 1
*/


public class httpc {
	
	boolean verbose = false;																				
	boolean hasInlineData = false;
	boolean hasFile = false;
	boolean redirect = false;
	boolean toFile;
	int argsLength;
	String contentLength = "0";
	String contentType = "";
	String inlineData ="";
	String redirectURL ="";
	String urlString ="";
	String inFile ="";
	String outFile = "";
	String requestURI ="";
	StringBuilder headerFields;
	Map <String, String> headerPairs = new HashMap <String,String>();
	
	
	
	public void parse(String [] args){															// parse all options 
		argsLength = args.length;
		requestURI = args[argsLength-1];
			
		for(int i = 0;i < argsLength; i++){	
			switch (args[i]){
				case "-v":																		// verbose
					verbose = true;
					break;
				case "-h":																		// header
					String pairs = args[i+1];
					int colon = pairs.indexOf(':');
					headerPairs.put(pairs.substring(0, colon), pairs.substring(colon+1));
					break;
				case "-d":																		// inlineData
					hasInlineData = true;
					int e = i+1;
					inlineData += args[e];	
					while(!args[e].endsWith("'")){
						inlineData += " " + args[++e];
					}
					inlineData = inlineData.substring(1, inlineData.length()-1);
					contentLength = "";
				    contentLength += inlineData.length();
					headerPairs.put("Content-Length", contentLength);
					break;
				case "-f":																		// post file
					hasFile = true;
					inFile = args[i+1];
					File fileContent = new File (inFile);
					contentLength += fileContent.length();
					headerPairs.put("Content-Length", contentLength);
					break;	
				case "-o":																		// print response to file
					toFile = true;
					outFile = args[i+1];
					break;
			}
		}
		headerPairs.put("Connection", "close");													//always add close connection
	}
	
	public void process(String [] args){
		System.out.println();
		redirect = false;																		//redirect resets to false
		//input Error
		
		if(hasFile && hasInlineData){															//exits if both -d and -f 
			System.out.println("Either [-d] or [-f] can be used but not both.");
			System.exit(0);
		}
		
		if(argsLength == 0 ){																	//notify if no arguments								
			System.out.println("Arguments required.");
			System.exit(0);
		}
		
		switch (args[0]){
		case "help":																			// help info
			if(args.length == 1 && args.length !=0){
			System.out.println("httpc is a curl-like application but supports HTTP protocol only.");
			System.out.println("Usage:");
			System.out.println("\t httpc command [arguments]");
			System.out.println("The commands are:");
			System.out.println("\t get executes a HTTP GET request and prints the response.");
			System.out.println("\t post executes a HTTP POST request and prints the response.");
			System.out.println("\t help prints this screen.");
			System.out.println("");
			System.out.println("Use \"httpc help [command]\" for more information about a command.");
			}else if(args.length == 2){
				switch(args[1]){																							// help get info
				case "GET":
				case "get":
					System.out.println("get executes a HTTP GET request and prints the response.");
					System.out.println("\t -v Prints the detail of the response such as protocol,status, and headers.");
					System.out.println("\t -h key:value Associates headers to HTTP Request with the format 'key:value'");

					break;
				case "POST":																							    // help post info
				case "post":
					System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL");
					System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.");
					System.out.println("\t -v Prints the detail of the response such as protocol, status, and headers.");
					System.out.println("\t -h key:value Associates headers to HTTP Request with the format  \'key:value\'");
					System.out.println("\t -d string Associates an inline data to the body HTTP POST request.");
					System.out.println("\t -f file Associates the content of a file to the body HTTP POST request");
					break;
				}
			}
			
			break;
		case "get":																						// get and post request
		case "GET":
		case "post":
		case "POST":
			
			Socket clientSocket;																		// create socket
			
			BufferedReader input;																		// input stream reader
			PrintWriter output;																			// output stream writer
		
			try {
				
				URI url = new URI(requestURI);															//URI
				String host = url.getHost();
				
				if(host == null && headerPairs.containsKey("Host")){									//uses host in url parameter if no provided in URI
					host = headerPairs.get("Host");
				}
							
				int port;																				//port number (default:80)
				if((port = url.getPort()) == -1){
				port = 80;
				}
				clientSocket = new Socket(host,port);
				input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));		//initialize in & output streams
				output = new PrintWriter(clientSocket.getOutputStream(), true);			
				String request = args[0].toUpperCase()+ " " + requestURI + " HTTP/1.1";					//formulate request
				boolean quit = true;
				StringBuilder sb = new StringBuilder();
				
		
	///////////////////////////////////////////////send the request
				
					output.println(request);															//Request line
					output.println("Host: " + host);													//header fields
					if(!headerPairs.isEmpty()){															
							for (Map.Entry<String, String> entry: headerPairs.entrySet()){
								output.println(entry.getKey()+ ": " + entry.getValue());
							}
					}
					output.println("");
					if(hasInlineData){																	//inline data
						output.println(inlineData);
					}
					
					
					if(hasFile){																		//file data
						BufferedReader br = new BufferedReader(new FileReader (inFile));
						String line;
						String lineContent ="";
						while((line = br.readLine()) != null){
							lineContent += line + "\n";
						}
						output.println(lineContent);
						br.close();
					}
					output.flush();																		

	///////////////////////////////////////////////////print response
					
					if(!toFile){																		//to not print to file
					
					if(!verbose){																											
						boolean vb= false;																//non-verbose
						for(String line= input.readLine(); line != null; line = input.readLine()){
							if(line.matches("^.*\\s3\\d\\d\\s.*$")) redirect = true;
							if(line.contains("Location")){redirectURL = line.substring(line.indexOf(':')+2,line.length());}
							if(vb||line.contains("{")||line.isEmpty())vb = true;
							if(vb)System.out.println(line);
						}
					}else{																				//verbose
							for(String line= input.readLine(); line != null; line = input.readLine()){
								
								if(line.matches("^.*\\s3\\d\\d\\s.*$")) redirect = true;
								if(line.contains("Location")){redirectURL = line.substring(line.indexOf(':')+2,line.length());}
								System.out.println(line);
							}
					}
					
					}else{																				//print to file
						
						PrintWriter pw = new PrintWriter(outFile,"utf-8");								//initialize PrintWriter
						
							if(!verbose){																//non-verbose
								boolean vb= false;														
								for(String line= input.readLine(); line != null; line = input.readLine()){
									if(line.matches("^.*\\s3\\d\\d\\s.*$")) redirect = true;
									if(line.contains("Location")){redirectURL = line.substring(line.indexOf(':')+2,line.length());}
									if(vb||line.contains("{")||line.isEmpty())vb = true;
									if(vb)pw.println(line);
								}
								
							}else{																			//verbose											
								for(String line= input.readLine(); line != null; line = input.readLine()){
									
									if(line.matches("^.*\\s3\\d\\d\\s.*$")) redirect = true;
									if(line.contains("Location")){redirectURL = line.substring(line.indexOf(':')+2,line.length());}
									pw.println(line);
								}
						}
						pw.close();																			//close writer
					}

				input.close();																				//close in, out stream & socket
				output.close();
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
	}
	
	public void redirect(int renum, String []args){															//redirects renum amount of times
		for(int i = 0; (i < renum)&& redirect; i++){
		requestURI = redirectURL;
		process(args);
		}
	}
		
	public static void main (String [] args){

		httpc client = new httpc();
		client.parse(args);								//checks options
		client.process(args);							//process request
		client.redirect(5,args);                      //redirects will repeat 5 times then stop

}
}
