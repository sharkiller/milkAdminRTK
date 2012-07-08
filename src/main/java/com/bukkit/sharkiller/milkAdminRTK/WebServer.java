package com.bukkit.sharkiller.milkAdminRTK;


import java.net.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import com.bukkit.sharkiller.milkAdminRTK.Config.*;
import com.bukkit.sharkiller.milkAdminRTK.RTK.*;

/**
 * Simple <code>WebServer</code> All-In-One Class.
 * 
 * @author Sharkiller
 */
public class WebServer extends Thread {
	static Logger LOG = Logger.getLogger("milkAdmin");
    int WebServerMode;
    MilkAdminRTK milkAdminRTKInstance;
    Socket WebServerSocket = null;
	ServerSocket rootSocket = null;
    public WebServer(MilkAdminRTK i, Logger log)
    {
    	LOG = log;
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
			LOG.info(text);
	}
    
    public String readFileAsString(String filePath)
    throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(65536);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        
        char[] buf = new char[65536];
        int length;
        while((length=reader.read(buf)) != -1){
        	fileData.append(String.valueOf(buf, 0, length).replaceAll("_ExternalUrl_", ExternalUrl));
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
	
	public boolean copyFolder(File src, File dest) throws IOException{
		boolean copying;
		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}

			if(!src.exists()){
				debug("[milkAdminRTK] Directory does not exist.");
				return false;
			}
			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				copying = copyFolder(srcFile,destFile);
				if(!copying)
					debug("[milkAdminRTK] Failing when copy: "+srcFile);
			}
		}else{
			
			if(!dest.exists()) {
				dest.createNewFile();
			}
			
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(src).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			}
			finally {
				if(source != null) {
					source.close();
				}
				if(destination != null) {
					destination.close();
				}
			}
			
			/*InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 
			
			// byte fileData[] = new byte[8192];
			byte[] buffer = new byte[8192];

			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();*/
		}
		return true;
	}
	
	static public boolean deleteDirectory(File path) {
		if(path.exists()) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}else{
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}
	
	public void readFileAsBinary(String path, String type)
	throws java.io.IOException{
		readFileAsBinary(path, type, false);
	}

	public void readFileAsBinary(String path, String type, boolean replace)
	throws java.io.IOException{
		try{
			File archivo = new File(path);
			String StringData = new String("");
			long lengthData;
			if(archivo.exists()){
				FileInputStream file = new FileInputStream(archivo);
				byte[] fileData = new byte[65536];
				int length;
			
				if(replace){
					while ((length = file.read(fileData)) > 0){
						String aux = new String(fileData, 0, length);
						StringData = StringData + aux.replaceAll("_ExternalUrl_", ExternalUrl);
					}
					lengthData = StringData.length();
				}else{
					lengthData = archivo.length();
				}
				
				DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
				out.writeBytes("HTTP/1.1 200 OK\r\n");
				if(type != null)
					out.writeBytes("Content-Type: "+type+"; charset=utf-8\r\n");
				out.writeBytes("Content-Length: "+lengthData+"\r\n");
				out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
				out.writeBytes("Server: milkAdmin Webserver\r\n");
				out.writeBytes("Connection: Close\r\n\r\n");
				
				if(replace){
					out.writeBytes(StringData);
				}else{
					while ((length = file.read(fileData)) > 0)
						out.write(fileData, 0, length);
				}
				out.flush();
	
	            file.close();
	            out.close();
			}else{
				httperror("404 Not Found");
			}
		}
		catch (Exception e) {
			debug("[milkAdmin] ERROR in readFileAsBinary(): " + e.getMessage());
		}
	}

	public void print(String data, String MimeType){
		try{ 
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: "+MimeType+"; charset=utf-8\r\n");
			out.writeBytes("Content-Length: "+data.length()+"\r\n");
			out.writeBytes("Cache-Control: no-cache\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(data);
			out.close();
		} catch (Exception e) {
			httperror("500 Internal Server Error");
			debug("[milkAdminRTK] ERROR in print: " + e.getMessage());
		}
	}
	
	public String getParam(String param, String URL)
	{
		Pattern regex = Pattern.compile("[\\?&]"+param+"=([^&#]*)");
		Matcher result = regex.matcher(URL);
		if(result.find()){
			try{
				String resdec = URLDecoder.decode(result.group(1),"UTF-8");
				if(param != "password") debug("getParam: "+param+" - Value: "+resdec);
				return resdec;
			}catch (UnsupportedEncodingException e){
				debug("[milkAdminRTK] ERROR in getParam: " + e.getMessage());
				return "";
			}
		}else
			return "";
	}

	boolean Debug = false, isBackup = false, BackupPlugins = true, BackupAllWorlds = true;
	int Port;
	static InetAddress Ip = null;
	String levelname;
	String PluginDir = "plugins/milkAdmin/";
	String BackupPath = "Backups [milkAdmin]";
	String ExternalUrl = "http://www.sharkale.com.ar/milkAdmin";
	String BackupProgress, BackupStatus;
	List<String> WorldsNames = new ArrayList<String>();
	Configuration Settings = new Configuration(new File(PluginDir+"settings.yml"));
	Configuration Worlds = new Configuration(new File(PluginDir+"worlds.yml"));
	PropertiesFile BukkitProperties = new PropertiesFile("server.properties");
	NoSavePropertiesFile adminList = new NoSavePropertiesFile(PluginDir+"admins.ini");
	PropertiesFile saveAdminList = new PropertiesFile(PluginDir+"admins.ini");
	NoSavePropertiesFile noSaveLoggedIn = new NoSavePropertiesFile(PluginDir+"loggedin.ini");
	PropertiesFile LoggedIn = new PropertiesFile(PluginDir+"loggedin.ini");

	public void load_settings(){
		Settings.load();
		Worlds.load();
		Debug = Settings.getBoolean("Settings.Debug", false);
		String ipaux = Settings.getString("Settings.Ip", null);
		if(ipaux != null && !ipaux.equals("")){
			try {
				Ip = InetAddress.getByName(ipaux);
				debug("Ip: "+ipaux+" - Ip: "+Ip);
			} catch (UnknownHostException e) {}
		}
		Port = Settings.getInt("Settings.Port", 25000);
		BackupPath = Settings.getString("Backup.Path", "Backups [milkAdmin]");
		ExternalUrl = Settings.getString("Settings.ExternalUrl", "http://www.sharkale.com.ar/milkAdmin");
		BackupPlugins = Settings.getBoolean("Backup.Plugins", true);
		BackupAllWorlds = Settings.getBoolean("Backup.AllWorlds", true);
		WorldsNames = Worlds.getStringList("Worlds", new ArrayList<String>());
		NoSavePropertiesFile serverProperties = new NoSavePropertiesFile("server.properties");
		levelname = serverProperties.getString("level-name");
	}
	
	public void backup(){
		boolean copying;
		BackupProgress = "0%";
		isBackup = true;
		DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH-mm-ss");
		Date date = new Date();
		String datez = dateFormat.format(date);
		new File(BackupPath).mkdir();
		new File(BackupPath+"/"+datez).mkdir();
		BackupProgress = "33%";
		if(BackupAllWorlds){
			for(String world: WorldsNames){
				BackupStatus = world;
				File srcFolder = new File(world);
				File destFolder = new File(BackupPath+"/" + datez + "/" + world);
				try{
					copying = copyFolder(srcFolder,destFolder);
					if(!copying)
						debug("[milkAdminRTK] Failed to backup world: "+world);
				}catch(IOException e){
					debug("[milkAdminRTK]  Failed to backup world: "+world);
					debug(e.getMessage());
				}
			}
		}else{
			BackupStatus = levelname;
			File srcFolder = new File(levelname);
			File destFolder = new File(BackupPath+"/" + datez + "/" + levelname);
			try{
				copying = copyFolder(srcFolder,destFolder);
				if(!copying)
					debug("[milkAdminRTK] Failed to backup world: "+levelname);
			}catch(IOException e){
				debug("[milkAdminRTK]  Failed to backup world: "+levelname);
				debug(e.getMessage());
			}
		}
		BackupProgress = "66%";
		if(BackupPlugins){
			BackupStatus = "Plugins";
			File srcFolder = new File("plugins");
			File destFolder = new File(BackupPath+"/" + datez + "/plugins");
			try{
				copying = copyFolder(srcFolder,destFolder);
				if(!copying)
					debug("[milkAdminRTK] Failed to backup plugins folder.");
			}catch(IOException e){
				debug("[milkAdminRTK]  Failed to backup plugins folder.");
				debug(e.getMessage());
			}
		}
		BackupProgress = "100%";
		isBackup = false;
	}
	
	public void httperror(String error){
		try{ 
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 "+error+"\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.close();
		} catch (Exception e) { 
			debug("[milkAdmin] ERROR in httperror(): " + e.getMessage());
		}
	}
		  
	public void run(){
		load_settings();
		try{
	        if ( WebServerMode == 0 ){
	        	if(Ip == null){
					rootSocket = new ServerSocket(Port);
					LOG.info("[milkAdminRTK] WebServer listening on port "+Port);
				}else{
					rootSocket = new ServerSocket(Port, 50, Ip);
					LOG.info("[milkAdminRTK] WebServer listening on "+Ip+":"+Port);
				}
				while(!rootSocket.isClosed())
	                new WebServer(milkAdminRTKInstance, rootSocket.accept());
	        } else {
	            BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
	            try{
	                String l, g, url="", param="", json, htmlDir = "./plugins/milkAdmin/html";
	                while ( (l = in.readLine()) != null ){
	                    if (l.startsWith("GET") ){
	                        g = (l.split(" "))[1];
	                        Pattern regex = Pattern.compile("([^\\?]*)([^#]*)");
							Matcher result = regex.matcher(g);
							if(result.find()){
								url = result.group(1);
								param = result.group(2);
							}
							String HostAddress = WebServerSocket.getInetAddress().getHostAddress();
							debug("[milkAdmin] "+HostAddress+" - "+url);
							if ( url.contains("./")  ){
								httperror("403 Access Denied");
							}
							else if ( url.startsWith("/server/login") ){
	                        	String username = getParam("username", param);
								String password = getParam("password", param);
	                        	if(username.length() > 0 && password.length() > 0){
									if(adminList.containsKey(username)){
										String login = adminList.getString(username, password);
										if(login.contentEquals(sha512me(password))){
											LoggedIn.setString(HostAddress, username);
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
	                        else if (!noSaveLoggedIn.containsKey(HostAddress)){
	                        	debug("[milkAdmin] No logged.");
	                        	if(url.equals("/")){
									readFileAsBinary(htmlDir+"/login.html", "text/html", true);
								}
	                        	else if(url.startsWith("/js/")){
									readFileAsBinary(htmlDir + url, "text/javascript", true);
								}
								else if(url.startsWith("/css/")){
									readFileAsBinary(htmlDir + url, "text/css", true);
								}
								else if( url.startsWith("/images/")){
									readFileAsBinary(htmlDir + url, null);
								}
	                            //OTHERWISE ACCESS DENIED
	                            else{
	                            	httperror("403 Access Denied");
	                            }
	                        }else{
	                        	if(adminList.containsKey("admin")){
	                        		if( url.equals("/register.html")){
										readFileAsBinary(htmlDir+"/register.html", "text/html");
									}
	                        		else if(url.startsWith("/js/")){
										readFileAsBinary(htmlDir + url, "text/javascript", true);
									}
									else if(url.startsWith("/css/")){
										readFileAsBinary(htmlDir + url, "text/css", true);
									}
									else if( url.startsWith("/images/")){
										readFileAsBinary(htmlDir + url, null);
									}
									else if ( url.startsWith("/server/account_create") ){
										String username = getParam("username", param);
										String password = getParam("password", param);
			                        	if(username.length() > 0 && password.length() > 0){
											saveAdminList.setString(username, password);
											saveAdminList.removeKey("admin");
			                        	}
										json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
										print(json, "text/html");
									}else{
										readFileAsBinary(htmlDir+"/register.html", "text/html", true);
									}
								}else{
			                        if ( url.contains("/start") ){
			                        	json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
										json += "<head><script type=\"text/javascript\">tourl = './';</script>"+readFileAsString(htmlDir+"/wait.html");
			                            print(json, "text/html");
			                            try {
											Thread.sleep(1000);
										} catch (InterruptedException e) {
											debug("[milkAdminRTK] ERROR in Start: " + e.getMessage());
										}
										milkAdminRTKInstance.api.executeCommand(RTKInterface.CommandType.UNHOLD_SERVER,null);
			                        }
			                        else if( url.equals("/backup")){
										readFileAsBinary(htmlDir+"/backup.html", "text/html", true);
										try {Thread.sleep(2000);} catch (InterruptedException e) {
											debug("[milkAdminRTK] ERROR in Backup: " + e.getMessage());
										}
										backup();
									}
			                        else if( url.equals("/backup_info.json")){
			                        	if(isBackup){
			                        		json = "{\"isBackup\":\"on\"}";
			                        	}else{
			                        		json = "{\"isBackup\":\"off\"}";
			                        	}
			                        	print(json, "application/json");
			                        }
			                        else if(url.startsWith("/restore")){
			                        	String id = getParam("id", param);
			                        	boolean clear = Boolean.getBoolean(getParam("clear", param));
			                        	if(id.length() > 0){
			                        		new File(BackupPath).mkdir();
			                        		new File(BackupPath+"/"+id).mkdir();
			                        		File destFolder = new File("");
			                        		File srcFolder = new File(BackupPath+"/"+id);
			                        		if(clear){
			                        			String[] folders = srcFolder.list();
			                        			for(String folder: folders){
			                        				if(new File(BackupPath+"/"+id+"/"+folder).isDirectory() && new File(folder).exists()){
			                        					deleteDirectory(new File(folder));
			                        				}
			                        			}
			                        		}
			                        		try{
			                        			copyFolder(srcFolder,destFolder);
			                        			json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
												json += "<head><script type=\"text/javascript\">tourl = './';</script>"+readFileAsString(htmlDir+"/wait.html");
					                            print(json, "text/html");
			                        			try {Thread.sleep(1000);} catch (InterruptedException e) {
													debug("[milkAdminRTK] ERROR in Restore: " + e.getMessage());
												}
												milkAdminRTKInstance.api.executeCommand(RTKInterface.CommandType.UNHOLD_SERVER,null);
			                        		}catch(IOException e){
			                        			debug("[milkAdminRTK] ERROR in Restore: " + e.getMessage());
			                        			return;
			                        		}
			                        	}else
			                        		readFileAsBinary(htmlDir+"/actions.html", "text/html", true);
			                        }
			                        else if(url.startsWith("/js/")){
										readFileAsBinary(htmlDir + url, "text/javascript", true);
									}
									else if(url.startsWith("/css/")){
										readFileAsBinary(htmlDir + url, "text/css", true);
									}
									else if( url.startsWith("/images/")){
										readFileAsBinary(htmlDir + url, null);
									}
			                        else{
			                         	readFileAsBinary(htmlDir+"/actions.html", "text/html", true);
			                        }
			                    }
	                        }
	                    }
	                }
	            } catch (IOException e){
	            } catch (Exception e) {
	            	debug("[milkAdminRTK] ERROR: " + e.getMessage());
	            }
	        }
		} catch (Exception e){
			debug("[milkAdminRTK] ERROR: " + e.getMessage());
		}
	}
	public void stopServer()throws IOException{
		if(rootSocket != null)
			rootSocket.close();
	}
}