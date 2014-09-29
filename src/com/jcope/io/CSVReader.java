package com.jcope.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class CSVReader extends BufferedReader
{
	public static class MalformedCSVException extends Exception
	{
		/**
		 * Generated serialVersionUID
		 */
		private static final long serialVersionUID = 3371632447430259689L;
		
		private String msg;
		
		@Override
		public String getMessage()
		{
			return msg;
		}
		
		public MalformedCSVException setMessage(String msg)
		{
			this.msg = msg;
			return this;
		}
	}

	public static class InvalidConfigException extends Exception
	{
		/**
		 * Generated serialVersionUID
		 */
		private static final long serialVersionUID = -4400346896764490501L;
		
		private String msg;
		
		@Override
		public String getMessage()
		{
			return msg;
		}
		
		public InvalidConfigException setMessage(String msg)
		{
			this.msg = msg;
			return this;
		}
	}
	
	private static enum PARSE_STATE {
		FIELD_START,
		IN_FIELD,
		IN_QUOTED_FIELD,
		IN_QUOTED_FIELD_MAYBE_END
	};
	
	private PARSE_STATE state = PARSE_STATE.FIELD_START;
	private final Reader[] readerPtr = new Reader[]{null};
	private String[] columnNames = null;
	private boolean expectColumnNames;
	private boolean handledColumnNames = false;
	private char[] rBuff = new char[]{'\n'};
	private StringBuffer sb = new StringBuffer();
	private int colIdx;
	private int expectNumColumns;
	private int lineNumber = 1;
	private int rowNumber = 1;
	private boolean lastCharHandled = true;
	private boolean atEOF = false;
	private boolean open = true;
	private boolean lastCharWasEOL = true;
	
	private String getErrStrHeader()
	{
		return String.format("CSV Row %d starting on line %d, in column %d", rowNumber, lineNumber, colIdx + 1);
	}
	
	private void pushSB(ArrayList<String> rvalBuff) throws MalformedCSVException
	{
		String str = sb.toString();
		sb.setLength(0);
		colIdx++;
		if (expectNumColumns > 0 && !(colIdx < expectNumColumns))
		{
			throw new MalformedCSVException().setMessage(String.format("%s: more than %d columns", getErrStrHeader(), expectNumColumns));
		}
		rvalBuff.add(str);
	}
	
	public String[] _nextRow() throws MalformedCSVException, IOException
	{
		if (!open)
		{
			throw new IOException("File already closed");
		}
		if (atEOF)
		{
			return null;
		}
		boolean isCR;
		char c;
		int nr = 0;
		ArrayList<String> rvalBuff;
		
		colIdx = 0;
		rvalBuff = (expectNumColumns > 0) ? (new ArrayList<String>(expectNumColumns)) : (new ArrayList<String>());
		
		END_OF_ROW:
		while ((!lastCharHandled) || (nr = read(rBuff)) > 0)
		{
			while ((isCR = ((c = rBuff[0]) == '\r')) || (c == '\n'))
			{
				boolean _lastCharWasEOL = lastCharWasEOL;
				lastCharWasEOL = true;
				lineNumber++;
				if (isCR)
				{
					if ((nr = read(rBuff)) > 0)
					{
						c = rBuff[0];
						lastCharHandled = (c == '\n');
					}
					else
					{
						lastCharHandled = true;
						atEOF = true;
					}
				}
				else
				{
					lastCharHandled = true;
				}
				switch (state)
				{
				case FIELD_START:
					{
						boolean doPush = true;
						do
						{
							if (_lastCharWasEOL || rvalBuff.size() > 0)
							{
								break;
							}
							if (!isCR)
							{
								lastCharHandled = false;
								if ((nr = read(rBuff)) <= 0)
								{
									atEOF = true;
								}
								else
								{
									break;
								}
							}
							if (!atEOF)
							{
								break;
							}
							doPush = false;
						} while (false);
						if (doPush)
						{
							pushSB(rvalBuff);
						}
						break END_OF_ROW;
					}
				case IN_FIELD:
					pushSB(rvalBuff);
					state = PARSE_STATE.FIELD_START;
					break END_OF_ROW;
				case IN_QUOTED_FIELD:
					if (isCR)
					{
						sb.append('\r');
						if (c == '\n')
						{
							sb.append(c);
						}
					}
					else
					{
						sb.append('\n');
					}
					break;
				case IN_QUOTED_FIELD_MAYBE_END:
					pushSB(rvalBuff);
					state = PARSE_STATE.FIELD_START;
					break END_OF_ROW;
				}
				if (atEOF)
				{
					break END_OF_ROW;
				}
				else if (!lastCharHandled && (c == '\r' || c == '\n'))
				{
					continue;
				}
				else if (lastCharHandled)
				{
					if ((nr = read(rBuff)) > 0)
					{
						continue;
					}
					atEOF = true;
					break END_OF_ROW;
				}
				break;
			}
			lastCharHandled = true;
			lastCharWasEOL = false;
			switch (state)
			{
			case FIELD_START:
				switch (c)
				{
				case '"':
					state = PARSE_STATE.IN_QUOTED_FIELD;
					break;
				case ',':
					pushSB(rvalBuff);
					break;
				default:
					sb.append(c);
					state = PARSE_STATE.IN_FIELD;
					break;
				}
				break;
			case IN_FIELD:
				switch (c)
				{
				case '"':
					throw new MalformedCSVException().setMessage(String.format("%s: Got start of quoted field in the middle of unquoted field", getErrStrHeader()));
					//break;
				case ',':
					pushSB(rvalBuff);
					state = PARSE_STATE.FIELD_START;
					break;
				default:
					sb.append(c);
					break;
				}
				break;
			case IN_QUOTED_FIELD:
				switch (c)
				{
				case '"':
					state = PARSE_STATE.IN_QUOTED_FIELD_MAYBE_END;
					break;
				default:
					sb.append(c);
					break;
				}
				break;
			case IN_QUOTED_FIELD_MAYBE_END:
				switch (c)
				{
				case '"':
					state = PARSE_STATE.IN_QUOTED_FIELD;
					sb.append(c);
					break;
				case ',':
					pushSB(rvalBuff);
					state = PARSE_STATE.FIELD_START;
					break;
				default:
					throw new MalformedCSVException().setMessage(String.format("%s: Expected continuation of quoted field, field delimiter, or end of row, got '%c'", getErrStrHeader(), c));
				}
				break;
			}
		}
		
		if (nr <= 0)
		{
			atEOF = true;
			// at End of File (EOF)
			switch(state)
			{
			case IN_QUOTED_FIELD:
				throw new MalformedCSVException().setMessage(String.format("%s: Unexpected end of file while parsing quoted field", getErrStrHeader()));
				//break;
			case FIELD_START:
				c = rBuff[0];
				if (!(c == '\r' || c == '\n'))
				{
					pushSB(rvalBuff);
				}
				break;
			case IN_FIELD:
			case IN_QUOTED_FIELD_MAYBE_END:
				pushSB(rvalBuff);
				break;
			default:
				break;
			}
			destroyBuffers();
		}
		
		if (expectNumColumns > 0 && expectNumColumns != (colIdx + 1))
		{
			throw new MalformedCSVException().setMessage(String.format("%s: Less than %d columns", getErrStrHeader(), expectNumColumns));
		}
		
		rowNumber++;
		
		int rvalSize = rvalBuff.size();
		if (rvalSize == 0 && atEOF)
		{
			return null;
		}
		
		String[] rval = new String[rvalSize];
		for (int i=0; i<rval.length; i++)
		{
			rval[i] = rvalBuff.get(i);
		}
		return rval;
	}
	
	public CSVReader(Reader reader, boolean expectColumnNames, int expectNumColumns)
	{
		super(reader);
		readerPtr[0] = reader;
		this.expectColumnNames = expectColumnNames;
		this.expectNumColumns = expectNumColumns;
	}
	
	public static CSVReader getInstance(String filePath, boolean expectColumnNames, int expectNumColumns) throws FileNotFoundException
	{
		FileReader fileReader = new FileReader(filePath);
		return new CSVReader(fileReader, expectColumnNames, expectNumColumns);
	}
	
	public static CSVReader getInstance(String filePath, boolean expectColumnNames) throws FileNotFoundException
	{
		return getInstance(filePath, expectColumnNames, 0);
	}
	
	public static CSVReader getInstance(String filePath) throws FileNotFoundException
	{
		return getInstance(filePath, false);
	}
	
	public String[] getColumnNames() throws InvalidConfigException, MalformedCSVException, IOException
	{
		if (!expectColumnNames)
		{
			throw (new InvalidConfigException()).setMessage("Not expecting headers");
		}
		if (handledColumnNames)
		{
			return columnNames;
		}
		handledColumnNames = true;
		columnNames = _nextRow();
		return columnNames;
	}
	
	public String[] nextRow() throws MalformedCSVException, IOException
	{
		if (!handledColumnNames)
		{
			if (expectColumnNames)
			{
				try {
					getColumnNames();
				}
				catch (InvalidConfigException e) {
					// this block not possible to reach
					throw new RuntimeException(e);
				}
			}
			handledColumnNames = true;
		}
		return _nextRow();
	}
	
	@Override
	public void close() throws IOException
	{
		Reader reader = null;
		synchronized (readerPtr)
		{
			reader = readerPtr[0];
			readerPtr[0] = null;
		}
		if (reader == null)
		{
			return;
		}
		open = false;
		try
		{
			try
			{
				super.close();
			}
			finally {
				try {
					reader.close();
				} catch (IOException e) {
					// Do Nothing
				}
			}
		}
		finally {
			destroyBuffers();
		}
	}
	
	private void destroyBuffers()
	{
		rBuff = null;
		sb = null;
	}
}