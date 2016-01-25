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
	//System.out.println(this.blueskyGet(blueskyParamStr));

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
	    System.out.println(e);
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
		//System.out.println(result);

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
		/*AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.preparePost("http://" + this.blueskyGateway + "/doLogout.ins").setBody(param.getBytes()).execute();
		Response r = f.get();
		String result = r.getResponseBody();*/

		BlueskyHandler bh = new BlueskyHandler(this.blueskyGateway, this.username, this.password);
                bh.setup("post", "/doLogout.ins", param);
                bh.fetch();
                String result = bh.getResponseBody();

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



class BlueskyHandler{
    private String blueskyGateway;
    private String username;
    private String password;
    private int port = 8189;

    private  Socket socket;
    private  BufferedReader reader;
    private  DataOutputStream writer;
    private  String line;
    private  DataInputStream reader2;

    private String[][] readHeader = null;
    private String methodLine = " ";
    public boolean isEnable = false;
    public static final String NEWLINE = "\r\n";
    public static final int uriCharMax = 2000;
    public static final int contentMax = 1024;

    private String responseBody = " ";
    private String setupMethod = "get";
    private String setupParam = " ";
    private String setupContent = " ";

    public BlueskyHandler(String blueskyGateway, String username, String password){
        this.blueskyGateway = blueskyGateway;
        this.username = username;
        this.password = password;
        this.init();
    }

    private void init(){
        this.blueskyGateway = this.blueskyGateway.trim().replace("http://", "");
        String tmp[] = this.blueskyGateway.split(":");
        if(tmp.length <= 1){
            this.port = 80;
        }else{
            try{
                this.port = Integer.parseInt(tmp[1]);
                this.blueskyGateway = tmp[0];
            }catch(Exception e){
                this.port = 80;
            }
        }
	this.setup("get", "/", " ");
    }


    public String test(){
        String ret = " ";
        if(this.isEnable){
	    System.out.println("Handler....");
            ret = this.fetchHttpReq("GET", "/ETLog?instruction=ls&opt1=noneFix&opt2=edconnected", " ");
        }
        return ret;
    }

    public String[][] getResponseHeader(){
	if(this.readHeader != null){
	    return this.readHeader; 
	}else{
	    this.readHeader = new String[1][2];
	    this.readHeader[0][0] = " ";
	    this.readHeader[0][1] = " ";
	    return this.readHeader;
	}
    }

    public boolean connectBluesky(){

        try {
            this.socket = new Socket(this.blueskyGateway, this.port);
            this.socket.setReuseAddress(true);
            //socket.setSoTimeout(1000);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            this.isEnable = false;
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            this.isEnable = false;
            return false;
        }
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.reader2 = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            this.writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.isEnable = this.socket.isConnected();
        return this.isEnable;
    }

    public void setup(String method, String param){
	this.setup(method, param, " ");
    }
    public void setup(String method, String param, String content){
	this.clearConnection();
	this.setupMethod = method;
	this.setupParam = param;
	this.setupContent = content;
	this.isEnable = this.connectBluesky();
    }

    private void clearConnection(){
	try{
	    if(this.reader != null)
		this.reader.close();
	} catch(Exception e){
	    e.printStackTrace();
	}
        try{
            if(this.reader2 != null)
                this.reader2.close();
        } catch(Exception e){
	    e.printStackTrace();
        }
        try{
            if(this.writer != null)
                this.writer.close();
        } catch(Exception e){
	    e.printStackTrace();
        }
	try{
	    if(this.socket != null)
		if(this.socket.isConnected())
		    this.socket.close();
	} catch(Exception e){
	    e.printStackTrace();
	}
    }

    public void fetch(){
	this.responseBody = this.fetchHttpReq(this.setupMethod, this.setupParam, this.setupContent);
    }

    public String getResponseBody(){
	return this.responseBody;
    }

    private String fetchHttpReq(String httpMethod, String uriPath, String content){
        String ret = " ";
	int contentLength = content.length();
	Boolean isGet = httpMethod.equalsIgnoreCase("get");
	Boolean isPost = httpMethod.equalsIgnoreCase("post");
	Boolean isPermitFetch = (contentLength <= this.contentMax)?true:false;
	if(isGet || isPost){
	    String userAgent = "Bluesky-cli";
	    String host = this.blueskyGateway + ":" + this.port;
	    if(this.isEnable){
		String reqMes = httpMethod + " " + uriPath + " HTTP/1.1" + this.NEWLINE;
		isPermitFetch &= (reqMes.length() > this.uriCharMax)?false:true;
		if(isPermitFetch){
		    reqMes += "Host: " + this.blueskyGateway + ":" + this.port + this.NEWLINE;
		    reqMes += "User-Agent: " + userAgent + this.NEWLINE;
		    if(!(content.equals("") || content.equals(" ")) && isPost){
			reqMes += "Content-Length: " + contentLength + this.NEWLINE;
			reqMes += this.NEWLINE;
			reqMes += content;
		    }else{
			reqMes += this.NEWLINE;
		    }

		    try {
			this.writer.writeBytes(reqMes);
			this.writer.flush();
			String readed = this.readHeader();
			readed = this.readContent();
			ret = readed;

		    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = " ";
		    }
		}else{
		    ret = "{\"err\":\"Cannot fetching your instruction.\"}";
		}
	    }
	}
	this.clearConnection();
        return ret;
    }
    private String readHeader(){
        String ret = "";
        if(this.isEnable){
            try {
                String readed = "";
                while((readed = this.reader.readLine()) != null){
                    if(readed.trim().equalsIgnoreCase("")){
                        break;
                    }
                    ret += readed + this.NEWLINE;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ret = " ";
            }
        }else{
            ret = " ";
        }
        String h[][] = this.headerAnalysis(ret);
	this.readHeader = h;
        return ret;
    }
    private String readContent(){
        String ret = "";
        if(this.isEnable){
            try {
                String readed = "";
                while((readed = this.reader.readLine()) != null){
                    if(readed.trim().equalsIgnoreCase("")){
                        break;
                    }
                    ret += readed + this.NEWLINE;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ret = " ";
            }
        }else{
            ret = " ";
        }
        return ret;
    }
    private String[][] headerAnalysis(String header){
        header = header.replace("\r", "");
        String[] sptHeader = header.split("\n");
        int len = sptHeader.length;
        String[][] ret = new String[len - 1][2];
        // HTTP Method field.
        this.methodLine = sptHeader[0];
        // HTTP header field.
        if(len > 1){
            for(int i = 1; i < len - 1; i++){
                ret[i - 1] = this.getHeaderField(sptHeader[i].trim());
            }
        }
        this.readHeader = ret;
        return ret;
    }
    private String[] getHeaderField(String headerField){
        String headerFieldName = "";
        String headerFieldData = "";
        String[] ret = new String[2];
        int len = headerField.length();
        char ch = ' ';
        for(int i = 0; i < len; i++){
            ch = headerField.charAt(i);
            if(ch != ':'){
                headerFieldName += new String("" + ch);
            }else{
                do{
                    i++;
                    ch = headerField.charAt(i);
                }while(ch == ' ');
                if(i != 0 && i < len - 1)
                    headerFieldData = headerField.substring(i, len);
                break;
            }
        }
        ret[0] = headerFieldName.equals("")?" ":headerFieldName.replace("\r", "").replace("\n", "").trim();
        ret[1] = headerFieldData.equals("")?" ":headerFieldData.replace("\r", "").replace("\n", "").trim();
        return ret;
    }
    public String searchValueOfHeader(String[][] header, String key){
        String value = " ";
	if(header != null){
	    for(int i = 0; i < header.length; i++){
		if(header[i][0] != null){
		    if(header[i][0].equalsIgnoreCase(key)){
			value = header[i][1];
			break;
		    }
		}
	    }
	}
        return value;
    }
}
