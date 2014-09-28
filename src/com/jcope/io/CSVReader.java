package com.jcope.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

class MalformedCSVException extends Exception
{
	/**
	 * DEFAULT serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private String msg;
	
	@Override
	public String getMessage()
	{
		return msg;
	}
	
	public void setMessage(String msg)
	{
		this.msg = msg;
	}
}

class InvalidConfigException extends Exception
{
	/**
	 * DEFAULT serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private String msg;
	
	@Override
	public String getMessage()
	{
		return msg;
	}
	
	public void setMessage(String msg)
	{
		this.msg = msg;
	}
}

public class CSVReader extends BufferedReader
{
	private static final MalformedCSVException malformedCSVException = new MalformedCSVException();
	private static final InvalidConfigException invalidConfigException = new InvalidConfigException();
	
	private final Reader[] ReaderPtr = new Reader[]{null};
	private String[] columnNames = null;
	private boolean expectColumnNames;
	
	public CSVReader(Reader reader, boolean expectColumnNames)
	{
		super(reader);
		ReaderPtr[0] = reader;
		this.expectColumnNames = expectColumnNames;
	}
	
	public static CSVReader getInstance(File srcFile, boolean expectColumnNames) throws FileNotFoundException
	{
		return getInstance(srcFile.getAbsolutePath(), expectColumnNames);
	}
	
	public static CSVReader getInstance(String filePath, boolean expectColumnNames) throws FileNotFoundException
	{
		FileReader fileReader = new FileReader(filePath);
		return new CSVReader(fileReader, expectColumnNames);
	}
	
	public String[] getColumnNames() throws InvalidConfigException
	{
		if (!expectColumnNames)
		{
			throw invalidConfigException;
		}
		// TODO complete
		return null;
	}
	
	String[] nextRow() throws MalformedCSVException
	{
		// TODO complete
		return null;
	}
}