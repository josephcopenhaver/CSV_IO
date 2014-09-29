package com.jcope.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

public class CSVWriter extends BufferedWriter
{
	public static final String END_OF_ROW = "\n";
	public static final Pattern regex_hasMultipleLinesOrComma = Pattern.compile("[,\\r\\n]");
	private final Writer[] writerPtr = new Writer[]{null};
	private boolean isNotFirstRow = false;
	private Boolean lastRowEmpty = Boolean.FALSE;
	
	public static final String toCSV(String str)
	{
		boolean needsEscape = (str.indexOf('"') > -1);
		String rval;
		rval = (needsEscape || regex_hasMultipleLinesOrComma.matcher(str).find()) ? String.format("\"%s\"", (needsEscape ? str.replaceAll("\"", "\"\"") : str)) : str;
		return rval;
	}
	
	private void writeField(String str) throws IOException
	{
		int doubleQuoteIdx = str.indexOf('"');
		
		if (!(doubleQuoteIdx > -1 || regex_hasMultipleLinesOrComma.matcher(str).find()))
		{
			// data can be directly represented as a row
			write(str);
		}
		else
		{
			// need to wrap output
			write('"');
			
			if (doubleQuoteIdx > -1)
			{
				// need to escape DQ's
				int length = str.length();
				int lastDoubleQuoteIdx = -1;
				
				do
				{
					if (doubleQuoteIdx - 1 > lastDoubleQuoteIdx + 1)
					{
						// a substring can be written
						write(str.substring(lastDoubleQuoteIdx + 1, doubleQuoteIdx));
					}
					write("\"\"");
					lastDoubleQuoteIdx = doubleQuoteIdx;
					doubleQuoteIdx = str.indexOf('"', lastDoubleQuoteIdx + 1);
				} while (doubleQuoteIdx > -1);
				if (doubleQuoteIdx == -1 && lastDoubleQuoteIdx != (length - 1))
				{
					// a substring can be written
					write(str.substring(lastDoubleQuoteIdx + 1, length));
				}
			}
			else
			{
				write(str);
			}
			
			write('"');
		}
	}

	public CSVWriter(Writer writer)
	{
		super(writer);
		writerPtr[0] = writer;
	}
	
	public CSVWriter getInstance(File dstFile) throws IOException
	{
		return getInstance(dstFile.getAbsolutePath());
	}
	
	public CSVWriter getInstance(String filePath) throws IOException
	{
		FileWriter fileWriter = new FileWriter(filePath);
		return new CSVWriter(fileWriter);
	}
	
	public void writeRow(String[] row) throws IOException
	{
		boolean isNotFirstCol = false;
		if (isNotFirstRow)
		{
			write(END_OF_ROW);
		}
		else
		{
			isNotFirstRow = true;
		}
		
		String row0;
		if (row.length == 0 || (row.length == 1 && ((row0 = row[0]) == null || row0.equals(""))))
		{
			lastRowEmpty = Boolean.TRUE;
			return;
		}
		row0 = null;
		lastRowEmpty = Boolean.FALSE;
		
		for (String str : row)
		{
			if (isNotFirstCol)
			{
				write(',');
			}
			else
			{
				isNotFirstCol = true;
			}
			if (str != null && !str.equals(""))
			{
				writeField(str);
			}
		}
	}
	
	@Override
	public void close() throws IOException
	{
		Writer writer = null;
		boolean lastRowEmpty;
		synchronized (writerPtr) {synchronized(this.lastRowEmpty){
			writer = writerPtr[0];
			writerPtr[0] = null;
			lastRowEmpty = this.lastRowEmpty;
		}}
		if (writer == null)
		{
			return;
		}
		try
		{
			try
			{
				if (lastRowEmpty)
				{
					write(END_OF_ROW);
				}
			}
			finally {
				super.close();
			}
		}
		finally {
			try {
				writer.close();
			} catch (IOException e) {
				// Do Nothing
			}
		}
	}
}