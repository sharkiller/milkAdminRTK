package com.bukkit.sharkiller.milkAdminRTK;

//these must be imported

import com.bukkit.sharkiller.milkAdminRTK.Config.*;
import com.bukkit.sharkiller.milkAdminRTK.RTKToolkit.*;
import com.drdanick.McRKit.module.Module;
import com.drdanick.McRKit.module.ModuleLoader;
import com.drdanick.McRKit.module.ModuleMetadata;
import com.drdanick.McRKit.ToolkitEvent;

import java.io.File;
import java.io.IOException;

//This class defines a skeleton module that will be enabled when the server is stopped, and disabled when the server is started.
@SuppressWarnings("unused")
public class MilkAdminRTK extends Module implements RTKListener{
	private ModuleMetadata meta;
	private ModuleLoader moduleLoader;
	private ClassLoader cLoader;
	RTKInterface api = null;
	String PluginDir = "plugins/milkAdmin";
	Configuration Settings = new Configuration(new File(PluginDir+"/settings.yml"));
	private WebServer server = null;
	public MilkAdminRTK(ModuleMetadata meta, ModuleLoader moduleLoader, ClassLoader cLoader){
		super(meta,moduleLoader,cLoader,ToolkitEvent.ON_SERVER_HOLD,ToolkitEvent.ON_SERVER_RESTART);
		//the last two parameters define the events where the plugin is enabled and disabled respectively.
		try{
			Settings.load();
            String username = Settings.getString("RTK.Username", "user");
            String password = Settings.getString("RTK.Password", "pass");
            int port = Settings.getInt("RTK.Port", 25561);
        	api = RTKInterface.createRTKInterface(port,"localhost",username,password);
		}catch(RTKInterfaceException e){
			e.printStackTrace();
		}catch(Exception e){
			System.err.println("[milkAdminRTK] Error instantiating milkAdmin Module");
			e.printStackTrace();
		}
	}
	protected void onEnable(){
		System.out.println("[milkAdminRTK] Module enabled!");
        api.registerRTKListener(this);
		server = new WebServer(this);
	}

	public void onRTKStringReceived(String s){
		System.out.println("[milkAdminRTK] From RTK: "+s);
	}
	
	protected void onDisable(){
		System.out.println("[milkAdminRTK] Module disabled!");
		try{
			server.stopServer();
		}catch(IOException e){
			System.err.println("[milkAdminRTK] Error closing milkAdmin Server");
			e.printStackTrace();
		}
	}
}
