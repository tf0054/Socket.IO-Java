package com.glines.socketio.sample.echo;

public class Pojo {
    public int x = 1;
    public int y = 2;
    public String clientId = "x";
    
    Pojo(){
    }
    
    Pojo(String c, int x, int y){
    	this.x=x; this.y = y; this.clientId = c;
    }
    	
}

