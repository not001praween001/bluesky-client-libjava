/**
 * The Bluesky_cli.class on the  package bluesky.client is a connector
 * library of bluesky for java programmer.
 * 
 * Author: Praween AMONTAMAVUT (Hayakawa Laboratory)
 * E-mail: praween@hykwlab.org
 * Create date: 2015-12-02
 */

package bluesky.client;

import org.apache.commons.lang3.*;
import com.ning.http.client.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.io.*;
import com.google.gson.*;
import org.json.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.*;

public class Bluesky_cli{
    private String blueskyGateway;
    private String username;
    private String password;

    public Bluesky_cli(String blueskyGateway, String username, String password){
	this.blueskyGateway = blueskyGateway.trim().replace("http://", "");
	this.username = username;
	this.password = password;
    }

    private void test(){
	//Test login.
	boolean isLoginSuccess = this.login();
	System.out.println("Login result: " + isLoginSuccess);

	//Test logout.
	boolean isLogoutSuccess = this.logout();
	System.out.println("Logout result: " + isLogoutSuccess);

	//Test createBlueskyParam.
	String[] opts = {"noneFix", "edconnected"};
	String blueskyParamStr = this.createBlueskyParam("ls", opts);
	System.out.println("createBlueskyParam: " + blueskyParamStr);

	//Test blueskyGet.
	//System.out.println(this.blueskyGet(blueskyParamStr));

	//Test list_ed.
	System.out.println(this.list_ed());

	//Test getSensorDatByAdc
	this.login();
	String edip = "172.16.4.80";
	JSONObject sensingDat = this.getSensorDatByAdc(edip, "mcp3208");
	if(sensingDat != null){
	    System.out.println(sensingDat);
	}
	this.logout();
    }

    /**
     * Get sensing data by ADC.
     * @param deviceIP
     * @param adcmodule
     */
    public JSONObject getSensorDatByAdc(String deviceIP, String adcmodule){
	String mosi = "10",
	    miso = "9",
	    clk  = "11",
	    ce   = "8";
	JSONObject spiDat = null;
	String sensorDat = " ";
	String[] opts = {deviceIP, "spi", adcmodule, mosi, miso, clk, ce};
	try{
	    sensorDat = this.sensornetwork(opts);
	    JSONObject jsonObj = new JSONObject(sensorDat);
	    spiDat = jsonObj.getJSONObject("ETLog").getJSONObject("logging");
	    
	}catch(Exception e){
	    System.out.println(e);
	}
	return spiDat;
    }

    /**
     * Using sensornetwork with bluesky API.
     * @param opts
     */
    public String sensornetwork(String[] opts){
	String params = this.createBlueskyParam("sensornetwork", opts);
	String doTheAPI = this.blueskyGet(params);
	return doTheAPI;
    }

    /**
     * Listing the connecting embedded devices.
     * @return the list of connecting embedded devices.
     */
    public JSONArray list_ed(){
	String params = this.createBlueskyParam("ls", new String[]{"noneFix", "edconnected"});
        String listEd = this.blueskyGet(params);
	
	JSONObject jsonObj = new JSONObject(listEd);
	JSONArray ret = jsonObj.getJSONObject("ETLog").getJSONArray("EDConnStatement");
	
	return ret;
    }

    /**
     * Convert to parameter of HTTP.
     * @param instruction
     * @param opts
     * @return HTTP query parameters.
     */
    public String createBlueskyParam(String instruction, String[] opts){
	String ret = " ";
	if(!instruction.equals("")){
	    ret = "/etLog?instruction=" + instruction;
	    for(int i = 0, j = 1; i < opts.length; i++, j++){
		ret += "&opt" + j + "=" + opts[i];
	    }
	}
	return ret;
    }

    /**
     * Do something with Bluesky API
     * @param blueskyParam
     * @return API responsed Data
     */
    public String blueskyGet(String blueskyParam){
	String data = " ";
	try{
	    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
	    Future<Response> f = asyncHttpClient.prepareGet("http://" + this.blueskyGateway + blueskyParam).execute();
	    Response r = f.get();
	    data = r.getResponseBody();
	}catch(Exception e){
	    System.out.println(e);
	}	
	return data;
    }

    /**
     * login to the system as the public account.
     * @return login result
     */
    public boolean login(){
	boolean ret = false;
	String param = "username=" + this.username + "&password=" + this.password + "&mode=signin" ;
	for(int i = 0; i < 3; i++){
	    try{
		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.preparePost("http://" + this.blueskyGateway + "/doLogin.ins").setBody(param.getBytes()).execute();
		//Response r = f.get(3, TimeUnit.SECONDS);
		Response r = f.get();
		String result = r.getResponseBody();
		JSONObject jsonObj = new JSONObject(result);
		String isSuccess = jsonObj.getJSONObject("ETLog").getJSONObject("login").getString("result");
		if(isSuccess.equalsIgnoreCase("true")){
		    ret = true;
		    break;
		}
	    }catch(Exception e){
		System.out.println(e);
	    }
	}
	return ret;
    }

    /**
     * logout from the system.
     * @return logout result
     */
    public boolean logout(){
	boolean ret = false;
	String param = "username=" + this.username + "&mode=signout" ;
	for(int i = 0; i < 3; i++){
	    try{
		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.preparePost("http://" + this.blueskyGateway + "/doLogout.ins").setBody(param.getBytes()).execute();
		Response r = f.get();
		String result = r.getResponseBody();
		JSONObject jsonObj = new JSONObject(result);
		String isSuccess = jsonObj.getJSONObject("ETLog").getJSONObject("logout").getString("result");
		if(isSuccess.equalsIgnoreCase("true")){
		    ret = true;
		    break;
		}
	    }catch(Exception e){
		System.out.println(e);
	    }
	}
	return ret;
    }

    /**
     * For test the class only.
     */
    public static void main( String[] args ){
	Bluesky_cli conn = new Bluesky_cli("http://127.0.0.1:8189", "guest", "guest");
	conn.test();
    }
}