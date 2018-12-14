package com.zhouqing.chatproject.realtimeindoorlocation.model;


public class ComparableSensorEvent {

	public float[] values ;
	public int type;
	public long timestamp;
	//public int accuracy;

	public ComparableSensorEvent(float[] values, long timeString, int type) {
		this.values  = values.clone();
		this.type = type;
//		this.timestamp = se.timestamp;
		this.timestamp = timeString;
		//this.accuracy = se.accuracy;
	}

//	@Override
//	public boolean equals(Object o) {
//		// TODO Auto-generated method stub
//		return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o, false);
//	}
//
//	@Override
//	public int hashCode() {
//		// TODO Auto-generated method stub
//		return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this, false);
//	}
	
	

}
