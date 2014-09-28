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
	public static final Pattern regex_hasMultiLines = Pattern.compile("[\\r\\n]");
	private final Writer[] writerPtr = new Writer[]{null};
	
	public static final String toCSV(String str)
	{
		boolean needsEscape = (str.indexOf('"') > -1);
		String rval;
		rval = (needsEscape || regex_hasMultiLines.matcher(str).find()) ? String.format("\"%s\"", (needsEscape ? str.replaceAll("\"", "\"\"") : str)) : str;
		return rval;
	}
	
	private void writeField(String str) throws IOException
	{
		int doubleQuoteIdx = str.indexOf('"');
		
		if (!(doubleQuoteIdx > -1 || regex_hasMultiLines.matcher(str).find()))
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
				int maxIdx = str.length() - 1;
				int lastDoubleQuoteIdx = -1;
				
				do
				{
					if (doubleQuoteIdx - 1 > lastDoubleQuoteIdx + 1)
					{
						// a substring can be written
						write(str.substring(lastDoubleQuoteIdx + 1, doubleQuoteIdx - 1));
					}
					write("\"\"");
					lastDoubleQuoteIdx = doubleQuoteIdx;
					doubleQuoteIdx = str.indexOf('"', lastDoubleQuoteIdx + 1);
				} while (doubleQuoteIdx > -1);
				if (doubleQuoteIdx == -1 && lastDoubleQuoteIdx != maxIdx)
				{
					// a substring can be written
					write(str.substring(lastDoubleQuoteIdx + 1, maxIdx));
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
	
	public void writeRow(String[] rows) throws IOException
	{
		boolean isNotFirstCol = false;
		for (String str : rows)
		{
			if (str != null && !str.equals(""))
			{
				writeField(str);
			}
			if (isNotFirstCol)
			{
				write(',');
			}
			else
			{
				isNotFirstCol = true;
			}
		}
		write(END_OF_ROW);
	}
}