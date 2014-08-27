package com.risevision.riseplayer;

public class DisplayError {

	public int code;
	public int occourances;
	
	public DisplayError(int code, int occourances) {
		this.code = code;
		this.occourances = occourances;
	}
	public boolean compareTo(DisplayError e) { return (this.code == e.code ?  true: false); }

}
