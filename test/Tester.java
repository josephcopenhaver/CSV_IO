import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.jcope.io.CSVReader;
import com.jcope.io.CSVReader.MalformedCSVException;
import com.jcope.io.CSVWriter;

public class Tester
{
	public static void main(String[] args) throws IOException, MalformedCSVException
	{
		(new Tester()).run();
	}

	File tempFile = null;
	ArrayList<FileReader> fileReaders = new ArrayList<FileReader>();
	ArrayList<FileWriter> fileWriters = new ArrayList<FileWriter>();
	String endStrPadding = null;
	private int testNum = 0;
	
	private void assert_(boolean condition)
	{
		if (!condition)
		{
			throw new AssertionError();
		}
	}
	
	private void performTest() throws IOException, MalformedCSVException
	{
		testNum++;
		int subTestNum = 0;
		String[][][] testSet = new String[][][]{
			new String[][]{
				new String[]{"Hello", "World"}
			},
			new String[][]{
				new String[]{"Hello,", "World"}
			},
			new String[][]{
				new String[]{"Hello", ",World"}
			},
			new String[][]{
				new String[]{"Hello\"", "\"World"}
			},
			new String[][]{
				new String[]{"\"Hello", "World"}
			},
			new String[][]{
				new String[]{"Hello\r", "\nWorld"}
			},
			new String[][]{
				new String[]{"Hello\n", "\rWorld"}
			},
			new String[][]{
				new String[]{""}
			},
			new String[][]{
				new String[]{"RAWR"},
				new String[]{""}
			}
		};
		for (String[][] rowData : testSet)
		{
			boolean broken = true;
			subTestNum++;
			try
			{
				FileWriter w = newFileWriter(tempFile);
				CSVWriter cw = new CSVWriter(w);
				String[] lastRow = null;
				for (String[] row : rowData)
				{
					cw.writeRow(row);
					lastRow = row;
				}
				if (endStrPadding != null && (lastRow.length > 1 || (lastRow.length == 1 && lastRow[0] != null && !lastRow[0].equals(""))))
				{
					cw.write(endStrPadding);
				}
				cw.close();
				cw = null;
				fileWriters.remove(w);
				
				
				
				FileReader r = newFileReader(tempFile);
				CSVReader cr = new CSVReader(r, false, 0);
				String[] row;
				int rowCount = 0;
				while ((row = cr.nextRow()) != null)
				{
					rowCount++;
					assert_(rowCount <= rowData.length);
					assert_(row.length == rowData[rowCount-1].length);
					for (int i=0; i<row.length; i++)
					{
						assert_(row[i].equals(rowData[rowCount-1][i]));
					}
				}
				assert_(rowCount == rowData.length);
				cr.close();
				cr = null;
				fileReaders.remove(r);
				broken = false;
			}
			finally {
				if (broken)
				{
					System.err.println(String.format("Test %d.%d failed!", testNum, subTestNum));
				}
			}
		}
	}
	
	private void performTests() throws IOException, MalformedCSVException
	{
		performTest();
		endStrPadding = "\n";
		performTest();
		endStrPadding = "\r\n";
		performTest();
	}

	public void run() throws IOException, MalformedCSVException {
		allocResources();
		try
		{
			performTests();
		}
		finally
		{
			try
			{
				releaseResources();
			}
			finally {
				destroyTemporaryFiles();
			}
		}
		System.out.println("SUCCESS!");
	}
	
	private FileWriter newFileWriter(File file) throws IOException
	{
		FileWriter rval = new FileWriter(file);
		fileWriters.add(rval);
		return rval;
	}

	private FileReader newFileReader(File file) throws IOException
	{
		FileReader rval = new FileReader(file);
		fileReaders.add(rval);
		return rval;
	}

	private void destroyTemporaryFiles() throws IOException {
		if (!tempFile.delete())
		{
			throw new IOException("Failed to delete file: " + tempFile.getAbsolutePath());
		}
	}

	private void allocResources() throws IOException {
		tempFile = File.createTempFile("TEST_CSV_IO.", ".csv");
	}

	private void releaseResources() throws IOException {
		endStrPadding = null;
		IOException fe = null;
		for (FileReader reader : fileReaders)
		{
			try {
				reader.close();
			} catch (IOException e) {
				if (fe == null)
				{
					fe = e;
				}
			}
		}
		for (FileWriter writer : fileWriters)
		{
			try {
				writer.close();
			} catch (IOException e) {
				if (fe == null)
				{
					fe = e;
				}
			}
		}
		if (fe != null)
		{
			throw fe;
		}
	}
}