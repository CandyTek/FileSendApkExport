package com.xiaoming.desktop.filereceiver.file_receiver;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileItem implements Comparable<FileItem> {

	private final String filepath;
	private final long fileSize;
	private final File file;
	public FileItem(String filepath) {

		this.filepath = filepath;
		this.file = new File(filepath);
		this.fileSize = file.length();
	}
	public FileItem(File filepath) {

		this.filepath = filepath.getAbsolutePath();
		this.file = filepath;
		this.fileSize = file.length();
	}
	@Override
	public int compareTo(@NotNull FileItem fileItem) {
		return 0;
	}
	public String getName() {

		return filepath;
	}
	public long length() {
		return fileSize;
	}
	public String getPath() {
		return filepath;
	}
	public void delete() {
		file.delete();
	}
}
