package jp.classmethod.sample.audiomixer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.R.integer;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;

import com.todoroo.aacenc.AACEncoder;

public class AudioMixer {

	public static final int FREQUENCY = 44100;

	Context context;
	
	public AudioMixer(Context context)
	{
		this.context = context;
	}
	
	
	
	
	
	
	public class WAVFile
	{
		public String assetPath;
		public double startTime;
		
		public WAVFile()
		{
			
		}
	}
	
	
	
	public WAVFile createWAVFile()
	{
		return new WAVFile();
	}
	

	
	public void mixWAVFilesArray(List<WAVFile> wavFiles, double totalTime) {
		try {

			int dataLength = (int)(44100 * 16 * 2 * totalTime / 8);
			short[] output = new short[dataLength];
			
			for (int i = 0; i < output.length; i++)
			{
				output[i] = 0;
			}
			
			for (WAVFile wavFile : wavFiles)
			{
				InputStream is = context.getAssets().open(wavFile.assetPath);
				short[] isArray = bytetoshort(convertStreamToByteArray(is));
				
				int startTime = (int)(44100 * 16 * 2 * (wavFile.startTime) / 8);
				int mod = startTime % 16;
				if (mod > 0)
				{
					startTime = startTime - mod;
				}
				int endTime = startTime + isArray.length;
				
				for (int i = startTime, j = 0; i < endTime - 44 && i < output.length - 44; i++, j++)
				{
					float sample = isArray[j + 44];/// / 32768.0f;
					
					float input = (float) (output[i]);/// / 32768.0f);
					
					float mixed = (float)(sample * 0.5) + (float)(input * 0.5);
					
					if (mixed > 32768.0f)
					{
						mixed = 32768.0f;
					}
					else if (mixed < -32768.0f)
					{
						mixed = -32768.0f;
					}
					
					short result = (short) (mixed * 1.0f);
					
					output[i] = result; 
				}
			}
			
			
			// LOW PASS FILTER
//			short smoothFirstValue = output[0];
//			for (int i = 1, len = output.length; i < len; i++)
//			{
//				short currentValue = output[i];
//				smoothFirstValue += (currentValue - smoothFirstValue) / 10;
//				output[i] = smoothFirstValue;
//			}
			
			
			
			byte[] byteOutput = shorttobyte(output);
			
			
		    AACEncoder encoder = new AACEncoder();
	        String AAC_FILE = Environment.getExternalStorageDirectory()
					.getPath() + "/mixed.aac";

	        long delta = 5000;

	        int sampleRate = (int) (byteOutput.length * 1000 / delta);
	        sampleRate = 8000; // THIS IS A MAGIC NUMBER@?!!?!?!
	        // can i has calculate?

	        System.err.println("computed sample rate: " + sampleRate);

	        encoder.init(768000, 2, 44100, 16, AAC_FILE);

	        encoder.encode(byteOutput);

	        System.err.println("end");

	        encoder.uninit();
	        
	        
	        
	        			
			
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	// ==============================================================

	public short[] bytetoshort(byte[] bite) {
		// Grab size of the byte array, create an array of shorts of the same
		// size
		int size = bite.length;
		short[] shortArray = new short[size];

		for (int index = 0; index < size; index++)
			shortArray[index] = (short) bite[index];

		return shortArray;

	}

	// ==============================================================

	public byte[] shorttobyte(short[] shot) {
		// Grab size of the byte array, create an array of shorts of the same
		// size
		int size = shot.length;
		byte[] byteArray = new byte[size];

		for (int index = 0; index < size; index++)
			byteArray[index] = (byte) shot[index];

		return byteArray;

	}

	// =======================================================

	public byte[] convertStreamToByteArray(InputStream is) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[10240];
		int i = Integer.MAX_VALUE;
		while ((i = is.read(buff, 0, buff.length)) > 0) {
			baos.write(buff, 0, i);
		}

		return baos.toByteArray(); // be sure to close InputStream in calling
									// function

	}
	
	
	
	
	public static byte[] createMusicArray(InputStream is) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[10240];
		int i = Integer.MAX_VALUE;
		while ((i = is.read(buff, 0, buff.length)) > 0) {
			baos.write(buff, 0, i);
		}

		return baos.toByteArray(); // be sure to close InputStream in calling
									// function

	}

	public static void convertByteToFile(byte[] fileBytes)
			throws FileNotFoundException {

		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(Environment.getExternalStorageDirectory()
						.getPath() + "/mixed.mp3"));
		try {
			bos.write(fileBytes);
			bos.flush();
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
