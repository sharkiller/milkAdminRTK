package com.bukkit.sharkiller.milkAdminRTK.RTK;
/**
 * Abstract interface that defines an object capable of processing inbound API events.
 * 
 * @author <a href="mailto:nick@drdanick.com">Nick Stones-Havas</a>
 * @version 1, 09/02/2011
 */
public interface RTKListener{
	/**
	 * Called when either a String is received from the wrapper, or the API call times out.
	 * @param message The message received from the Wrapper.
	 */
	public void onRTKStringReceived(String message);
}