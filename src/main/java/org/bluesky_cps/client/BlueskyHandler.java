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


public class BlueskyHandler{
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
		String contentLenStr = this.searchValueOfHeader(this.readHeader, "content-length").replace("\r", "").replace("\n", "").trim();
		int contentLength = -1;
		if(!(contentLenStr.equals("") || contentLenStr.equals(" "))){
		    contentLength = Integer.parseInt(contentLenStr);
		}
		
		// if Content-Length field was not sepecified.
		//if(contentLength == -1){
		    while((readed = this.reader.readLine()) != null){
			if(readed.equalsIgnoreCase("")){
			    break;
			}
			ret += readed + this.NEWLINE;
		    }
		    /*}else{
		    byte[] bread = new byte[contentLength];
		    reader2.read(bread, 0, contentLength);
		    //reader2.readFully(bread);
		    //bread[contentLength + 0] = 0x00;
		    //bread[contentLength + 1] = 0x0a;
		    //bread[contentLength + 2] = 0x0d;
		
		    readed = new String(bread) + this.NEWLINE;*/
		    
		    //readed = this.readLine(this.reader2);
		    /*byte[] eof = new byte[3];
		    eof[0] = 0x00;
		    eof[1] = 0x0a;
		    eof[2] = 0x0d;
		    readed = this.readOffset(this.reader2, contentLength) + new String(eof);*/
		    //readed = this.readUTF();
		    /*ret = readed;
		      }*/
		    
		    //System.out.println("readContent: " + ret);
		
	    } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ret = " ";
	    } 
        }else{
            ret = " ";
        }
        return ret.trim();
    }
    private String readUTF(){
	String ret = "";
	String val = "";
	try{
	    while(reader2.available() > 0 && (val = reader2.readUTF()) != null){
		ret += val;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	//System.out.println(ret);
	return ret;
    }
    private String readOffset(DataInputStream reader2, int offset){
	String ret = "";
	//System.out.println("readOffset: ");
	
	    for(int i = 0; i < offset; i++){
		byte b;
		try {
		    b = reader2.readByte();
		    if(b > 0){			
			ret += new String(new byte[]{b});
		    }else{
			break;
		    }

		} catch (EOFException e) {
		    this.clearConnection();
		    //System.out.println("EOFException readed byte: " + ret);
		    break;
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	
	return ret;
    }
    private String readLine(DataInputStream reader2){
	String ret = "";
	
	    while(true){
		byte b;
		try {
		    b = reader2.readByte();
		    if(b == '\n' || b == 0xff || b == 0x00 || b == '\r'){
			break;
		    }else{
			ret += new String(new byte[]{b});
		    }
		} catch (EOFException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		    this.clearConnection();
		    //System.out.println("readLine readed byte: " + ret);
		    break;
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		    //System.out.println("readLine readed byte: " + ret);
		    break;
		} 
		
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
