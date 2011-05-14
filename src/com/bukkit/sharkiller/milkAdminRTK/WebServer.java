package com.bukkit.sharkiller.milkAdminRTK;


import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import com.bukkit.sharkiller.milkAdminRTK.NoSavePropertiesFile;
import com.bukkit.sharkiller.milkAdminRTK.PropertiesFile;
import com.bukkit.sharkiller.milkAdminRTK.Config.*;
import com.bukkit.sharkiller.milkAdminRTK.RTKToolkit.*;

public class WebServer extends Thread {
    int WebServerMode;
    MilkAdminRTK milkAdminRTKInstance;
    Socket WebServerSocket = null;
	ServerSocket rootSocket = null;
    public WebServer(MilkAdminRTK i)
    {
        WebServerMode = 0;
        milkAdminRTKInstance = i;
        start();
    }
    public WebServer(MilkAdminRTK i, Socket s)
    {
        WebServerMode = 1;
        milkAdminRTKInstance = i;
        WebServerSocket = s;
        start();
    }
    
	public void debug(String text){
		if(Debug)
			System.out.println(text);
	}
    
    private static String readFileAsString(String filePath)
    throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

	public String sha512me(String message){
		  MessageDigest md;
		  try {
		      md= MessageDigest.getInstance("SHA-512");

		      md.update(message.getBytes());
		      byte[] mb = md.digest();
		      String out = "";
		      for (int i = 0; i < mb.length; i++) {
		          byte temp = mb[i];
		          String s = Integer.toHexString(new Byte(temp));
		          while (s.length() < 2) {
		              s = "0" + s;
		          }
		          s = s.substring(s.length() - 2);
		          out += s;
		      }
		      message = out;

		  } catch (NoSuchAlgorithmException e) {
			  debug("[milkAdminRTK] ERROR in sha512me: " + e.getMessage());
		  }
			return message;
			}
	
	public void copyFolder(File src, File dest)
	throws IOException{

		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}

			if(!src.exists()){
				debug("Directory does not exist.");
				return;
			}
			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				copyFolder(srcFile,destFile);
			}
		}else{
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 
			
			// byte fileData[] = new byte[8192];
			byte[] buffer = new byte[8192];

			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
		}
	}

	public void readFileAsBinary(String path)
	throws java.io.IOException{
		try{
			File archivo = new File(path);
			
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Length: "+archivo.length()+"\r\n");
			out.writeBytes("Cache-Control: no-cache\r\n");
			out.writeBytes("Server: milkAdmin Webserver\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			
            FileInputStream file = new FileInputStream(archivo);
            byte[] fileData = new byte[8192];
            for(int i = 0; (long)i < archivo.length(); i += 8192){
                int bytesRead = file.read(fileData);
                out.write(fileData, 0, bytesRead);
            }
            file.close();
            out.close();
		}
		catch (Exception e) {
			debug("[milkAdminRTK] ERROR in readFileAsBinary: " + e.getMessage());
		}
	}

	public void print(String data, String MimeType){
		try{ 
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: "+MimeType+"\r\n");
			out.writeBytes("Cache-Control: no-cache\r\n");
			out.writeBytes("Content-Length: "+data.length()+"\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(data);
			out.close();
		} catch (Exception e) { 
			debug("[milkAdminRTK] ERROR in print: " + e.getMessage());
		}
	}
	
	public String getParam(String param, String URL)
	{
		Pattern regex = Pattern.compile("[\\?&]"+param+"=([^&#]*)");
		Matcher result = regex.matcher(URL);
		if(result.find()){
			try{
				debug(result.group(0));
				String resdec = URLDecoder.decode(result.group(1),"UTF-8");
				debug(resdec);
				return resdec;
			}catch (UnsupportedEncodingException e){
				debug("[milkAdminRTK] ERROR in getParam: " + e.getMessage());
				return "";
			}
		}else
			return "";
	}

	boolean Debug;
	int Port;
	String levelname;
	Configuration Settings = new Configuration(new File("milkAdmin/settings.yml"));
	PropertiesFile BukkitProperties = new PropertiesFile("server.properties");
	NoSavePropertiesFile adminList = new NoSavePropertiesFile("milkAdmin/admins.ini");
	PropertiesFile saveAdminList = new PropertiesFile("milkAdmin/admins.ini");
	NoSavePropertiesFile noSaveLoggedIn = new NoSavePropertiesFile("milkAdmin/loggedin.ini");
	PropertiesFile LoggedIn = new PropertiesFile("milkAdmin/loggedin.ini");

	public void load_settings(){
		Settings.load();
		Debug = Settings.getBoolean("Settings.Debug", false);
		Port = Settings.getInt("Settings.Port", 64712);
		NoSavePropertiesFile serverProperties = new NoSavePropertiesFile("server.properties");
		levelname = serverProperties.getString("level-name");
	}
		  
	public void run(){
		load_settings();
		try{
	        if ( WebServerMode == 0 ){
	            rootSocket = new ServerSocket(Port);
	            for (;;)
	                new WebServer(milkAdminRTKInstance, rootSocket.accept());
	        } else {
	            BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
	            try{
	                String l, g, json;
	                while ( (l = in.readLine()).length() > 0 ){
	                    if ( l.startsWith("GET") ){
	                        g = (l.split(" "))[1];
	                        if ( g.startsWith("/server/login") ){
	                        	String username = getParam("username", g);
								String password = getParam("password", g);
	                        	if(username.length() > 0 && password.length() > 0){
									if(adminList.containsKey(username)){
										String login = adminList.getString(username, password);
										if(login.contentEquals(sha512me(password))){
											LoggedIn.setString(WebServerSocket.getInetAddress().getHostAddress(), WebServerSocket.getInetAddress().getCanonicalHostName());
											LoggedIn.setString(WebServerSocket.getInetAddress().getCanonicalHostName(), WebServerSocket.getInetAddress().getHostAddress());
											json = "ok";
										}else{
											json = "error";
										}
									}else{
										json = "error";
									}
								}else{
									json = "error";
								}
								print(json, "text/plain");
							}
	                        else if (!noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getCanonicalHostName()) || !noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getHostAddress())){
	                        	if( g.equals("/")){
									readFileAsBinary("./milkAdmin/html/login.html");
								}
								else if( g.equals("/invalidlogin.html")){
									readFileAsBinary("./milkAdmin/html/invalidlogin.html");
								}
								else if( g.startsWith("/images/") || g.startsWith("/js/") || g.startsWith("/css/")){
									readFileAsBinary("./milkAdmin/html" + g);
								}
	                            //OTHERWISE LOAD PAGES
	                            else{
	                            	json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
									print(json, "text/html");
	                            }
	                        }else{
	                        	if(adminList.containsKey("admin")){
	                        		if( g.equals("/register.html")){
										readFileAsBinary("./milkAdmin/html/register.html");
									}
	                        		else if( g.startsWith("/images/") || g.startsWith("/js/") || g.startsWith("/css/")){
										readFileAsBinary("./milkAdmin/html" + g);
									}
									else if ( g.startsWith("/server/account_create") ){
										String username = getParam("username", g);
										String password = getParam("password", g);
			                        	if(username.length() > 0 && password.length() > 0){
											saveAdminList.setString(username, password);
											saveAdminList.removeKey("admin");
			                        	}
										json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
										print(json, "text/html");
									}else{
										readFileAsBinary("./milkAdmin/html/register.html");
									}
								}else{
			                        if ( g.contains("/start") ){
			                        	json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">"+readFileAsString("./milkAdmin/html/wait.html");
			                            print(json, "text/html");
			                            try {
											Thread.sleep(1000);
										} catch (InterruptedException e) {
											debug("[milkAdminRTK] ERROR in Start: " + e.getMessage());
										}
										milkAdminRTKInstance.api.executeCommand(RTKInterface.CommandType.UNHOLD_SERVER);
			                        }
			                        else if(g.startsWith("/restore")){
			                        	String id = getParam("id", g);
			                        	if(id.length() > 0){
			                        		new File("backups").mkdir();
			                        		new File("backups/"+id).mkdir();
			                        		File destFolder = new File(levelname);
			                        		File srcFolder = new File("backups/"+id+"/"+levelname);
			                        		try{
			                        			copyFolder(srcFolder,destFolder);
			                        			json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0\"; url=\"/\"></head></html>";
			                        			print(json, "text/html");
			                        			try {
													Thread.sleep(1000);
												} catch (InterruptedException e) {
													debug("[milkAdminRTK] ERROR in Restore: " + e.getMessage());
												}
												milkAdminRTKInstance.api.executeCommand(RTKInterface.CommandType.UNHOLD_SERVER);
			                        		}catch(IOException e){
			                        			debug("[milkAdminRTK] ERROR in Restore: " + e.getMessage());
			                        			return;
			                        		}
			                        	}
			                        }
			                        else if( g.startsWith("/images/") || g.startsWith("/js/") || g.startsWith("/css/")){
										readFileAsBinary("./milkAdmin/html" + g);
									}
			                        else{
			                         	readFileAsBinary("./milkAdmin/html/startServer.html");
			                        }
			                    }
	                        }
	                    }
	                }
	            }
	            catch (Exception e) {
	            	debug("[milkAdminRTK] ERROR: " + e.getMessage());
	            }
	        }
		} catch (Exception e){
			debug("[milkAdminRTK] ERROR: " + e.getMessage());
		}
	}
	public void stopServer()throws IOException{
		rootSocket.close();
	}
}