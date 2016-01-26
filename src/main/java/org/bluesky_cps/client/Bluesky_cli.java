/**
 * The Bluesky_cli.class is a connector
 * library of bluesky for java programmer.
 * 
 * Author: Praween AMONTAMAVUT (Hayakawa Laboratory)
 * E-mail: praween@hykwlab.org
 * Create date: 2015-12-02
 */

package org.bluesky_cps.client;

//import org.apache.commons.lang3.*;
//import com.ning.http.client.*;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.*;
import java.io.*;
//import com.google.gson.*;
import org.json.*;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicHeader;
//import org.apache.http.*;

import java.net.Socket;
import java.net.UnknownHostException;

public class Bluesky_cli{
    private String blueskyGateway;
    private String username;
    private String password;
    private String[][] resHeader;
    private BlueskyHandler bh;

    public Bluesky_cli(String blueskyGateway, String username, String password){
	this.blueskyGateway = blueskyGateway.trim().replace("http://", "");
	this.username = username;
	this.password = password;
	this.resHeader = null;
	this.bh = null;
    }

    private void test(){
	//Test login.
	boolean isLoginSuccess = this.login();
	System.out.println("Login result: " + isLoginSuccess);
	System.out.println("\r\n");

	//Test logout.
	boolean isLogoutSuccess = this.logout();
	System.out.println("Logout result: " + isLogoutSuccess);
	System.out.println("\r\n");

	//Test createBlueskyParam.
	String[] opts = {"noneFix", "edconnected"};
	String blueskyParamStr = this.createBlueskyParam("ls", opts);
	System.out.println("createBlueskyParam: " + blueskyParamStr);
	System.out.println("\r\n");

	//Test blueskyGet.
	String[] opts2 = {"172.16.4.109", "gpio", "set", "21", "1"};
	blueskyParamStr = this.createBlueskyParam("sensornetwork", opts2);
	System.out.println(blueskyParamStr);
	isLoginSuccess = isLoginSuccess = this.login();
	System.out.println("Login result: " + isLoginSuccess);
	System.out.println("blueskyGet: " + this.blueskyGet(blueskyParamStr) + " \r\n");

	opts2[4] = "0";
	blueskyParamStr = this.createBlueskyParam("sensornetwork", opts2);
	System.out.println("blueskyGet: " + this.blueskyGet(blueskyParamStr) + " \r\n");
	isLogoutSuccess = this.logout();
	System.out.println("Logout result: " + isLogoutSuccess);

	//Test list_ed.
	System.out.println(this.list_ed());

	//Test search header.
	System.out.println("\r\nserver: " + this.getResponseHeaderOf("server"));
	System.out.println("\r\n");

	//Test getSensorDatByAdc
	this.login();
	String edip = "172.16.4.80";
	JSONObject sensingDat = this.getSensorDatByAdc(edip, "mcp3208");
	if(sensingDat != null){
	    System.out.println(sensingDat);
	    System.out.println("\r\n");
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
	JSONArray ret = null;
	if(!listEd.equals(" ") && !listEd.equals("")){
	    JSONObject jsonObj = new JSONObject(listEd);
	    ret = jsonObj.getJSONObject("ETLog").getJSONArray("EDConnStatement");
	}else{
	    JSONObject jsonObj = new JSONObject("{\"EDConnStatement\":[\"null\"]}");
	    ret = jsonObj.getJSONArray("EDConnStatement");
	}
	
	
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
	    /*AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
	    Future<Response> f = asyncHttpClient.prepareGet("http://" + this.blueskyGateway + blueskyParam).execute();
	    Response r = f.get();
	    data = r.getResponseBody();*/
	    this.bh = new BlueskyHandler(this.blueskyGateway, this.username, this.password);
	    this.bh.setup("get", blueskyParam);
	    this.bh.fetch();
	    data = this.bh.getResponseBody();
	    this.resHeader = this.bh.getResponseHeader();
	}catch(Exception e){
	    System.out.println("in blueskyGet: " + e);
	    e.printStackTrace();
	}	
	return data;
    }

    /**
     * get response header by key.
     * @return the header field value.
     */
    public String getResponseHeaderOf(String key){
	String ret = " ";
	if(this.bh != null){
	    if(this.resHeader != null){
		ret = this.bh.searchValueOfHeader(this.resHeader, key);
	    }
	}
	return ret;
    }

     /**
     * get response header by key from handler.
     * @return the header field value.
     */
    public String getResponseHeaderOf(String key, BlueskyHandler bh){
	String ret = " ";
	if(bh != null){
	    if(this.resHeader != null){
		ret = bh.searchValueOfHeader(this.resHeader, key);
	    }
	}
	return ret;
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
		/*AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.preparePost("http://" + this.blueskyGateway + "/doLogin.ins").setBody(param.getBytes()).execute();
		Response r = f.get();
		String result = r.getResponseBody();*/

		BlueskyHandler bh = new BlueskyHandler(this.blueskyGateway, this.username, this.password);
		bh.setup("post", "/doLogin.ins", param);
		bh.fetch();
		String result = bh.getResponseBody();
		if(!result.equals(" ")){
		    JSONObject jsonObj = new JSONObject(result);
		    String isSuccess = jsonObj.getJSONObject("ETLog").getJSONObject("login").getString("result");
		    if(isSuccess.equalsIgnoreCase("true")){
			ret = true;
			break;
		    }
		}
	    }catch(Exception e){
		System.out.println("login:" + e);
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
		/*AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.preparePost("http://" + this.blueskyGateway + "/doLogout.ins").setBody(param.getBytes()).execute();
		Response r = f.get();
		String result = r.getResponseBody();*/

		BlueskyHandler bh = new BlueskyHandler(this.blueskyGateway, this.username, this.password);
                bh.setup("post", "/doLogout.ins", param);
                bh.fetch();
                String result = bh.getResponseBody();
		if(!result.equals(" ")){
		    JSONObject jsonObj = new JSONObject(result);
		    String isSuccess = jsonObj.getJSONObject("ETLog").getJSONObject("logout").getString("result");
		    if(isSuccess.equalsIgnoreCase("true")){
			ret = true;
			break;
		    }
		}
	    }catch(Exception e){
		System.out.println("logout: " + e);
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
	BlueskyHandler bh = new BlueskyHandler("http://127.0.0.1:8189", "guest", "guest");
	
	System.out.println(bh.test());
	bh.setup("get", "/ETLog?instruction=ls&opt1=noneFix&opt2=edconnected", " ");
	bh.fetch();
	System.out.println(bh.getResponseBody());

	String param = "username=guest&password=guest&mode=signin";
	bh.setup("post", "/doLogin.ins", param);
        bh.fetch();
        System.out.println(bh.getResponseBody());

	param = "username=guest&mode=signout";
        bh.setup("post", "/doLogout.ins", param);
        bh.fetch();
        System.out.println(bh.getResponseBody());

    }
}


