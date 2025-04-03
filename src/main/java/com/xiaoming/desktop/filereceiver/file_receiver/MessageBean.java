package com.xiaoming.desktop.filereceiver.file_receiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//██ Model类__数据信息 ██
public class MessageBean {
	private final long time;
	private final String message;
	private final int colorIndex;
	//SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());

	public MessageBean(long time,String message,int i) {
		this.time = time;
		this.message = String.valueOf(message);
		this.colorIndex = i;
	}
	public int getColor() {
		return colorIndex;
	}

	String getFormattedTime() {
		//SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date(time));
	}
	public String getMessage() {
		return message;
	}
}
