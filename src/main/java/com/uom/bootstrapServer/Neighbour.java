package BootstrapServer;

class Neighbour{
	private final String ip;
	private final int port;
	private final String username;	

	public Neighbour(String ip, int port, String username){
		this.ip = ip;
		this.port = port;
		this.username = username;
	}	

	public String getIp(){
		return this.ip;
	}

	public String getUsername(){
		return this.username;
	}

	public int getPort(){
		return this.port;
	}
}
