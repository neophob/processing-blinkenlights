package processing.lib.blinken;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

import processing.lib.blinken.jaxb.Blm;
import processing.lib.blinken.jaxb.Frame;
import processing.lib.blinken.jaxb.Row;

/**
 * some blinkenlights helper functions
 * @author michael vogt / neophob.com (c) 2010
 *
 */
public class BlinkenHelper {

	private BlinkenHelper() {
		//no instance allowed
	}
	
	/**
	 * return a converted frame
	 * @param frameNr which frame to convert
	 * @param blm to marshalled object, our source
	 * @return an image out of the frame
	 */
	public static BufferedImage grabFrame(int frameNr, Blm blm, int color) throws NumberFormatException {
		int frames = blm.getFrame().size();
		
		//some sanity checks
		if (frameNr > frames || frameNr < 0) {
			return null;
		}
		
		int width = Integer.parseInt(blm.getWidth());
		int height = Integer.parseInt(blm.getHeight());
		int bits = Integer.parseInt(blm.getBits());
		int channels = Integer.parseInt(blm.getChannels());
		
		Frame f = blm.getFrame().get(frameNr);
		List<Row> rows = f.getRow();
		
		//BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] dest = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
		
		/**
		 * Structure of row data (http://blinkenlights.net/project/bml)
		 * Each single row describes the pixel colour values in hexadecimal notation. 
		 * If the colour depth is between 1 and 4, one hexadecimal digit is used per 
		 * colour value (0-f). If the colour depth is between 5 and 8, two hexadecimal 
		 * digits are used per colour value (00-ff).
		 * 
		 * The is one value for each pixel, one after the other. In an RGB picture with 
		 * channels="3" there are three colour values in sequence.
		 * 
		 */
		float rCol = (float)((color>>16)& 255)/255.f;
		float gCol = (float)((color>>8) & 255)/255.f;
		float bCol = (float)( color     & 255)/255.f;

		int ofs = 0;
		for (Row r: rows) {
			String s = r.getvalue();
			int[] data;
			if (bits>0 && bits<5) {
				//one char per color value
				data = getDataFromOneCharRow(s, rCol, gCol, bCol);
				if (data.length != width) {
					System.out.println("Ooops: looks like here is an error: "+width+"!="+data.length);
				}
			} else {
				//two char per color value
				data = getDataFromTwoCharRow(s);
//				if (data.length != width) {
//					System.out.println("Ooops: looks like here is an error: "+width+"!="+data.length);
//				}
			}
			

			//TODO channels/RGB
			System.arraycopy(data, 0, dest, ofs, width);
			ofs += width;			
		}
		
		return im;
	}
	
	/**
	 * convert string data to int[]
	 * @param data
	 * @param r colorize the pixel
	 * @param g colorize the pixel
	 * @param b colorize the pixel
	 * @return 
	 */
	private static int[] getDataFromOneCharRow(String data, float r, float g, float b) {
		int[] ret = new int[data.length()];
		int ri,gi,bi;
		int ofs=0;
		for (char c: data.toCharArray()) {
			int i=Integer.parseInt(c+"", 16);
			i*=16;
			ri = (int)(i*r);
			gi = (int)(i*g);
			bi = (int)(i*b);
			ret[ofs++] = ri<<16 | gi<<8 | bi;
		}
		return ret;
	}
	
	/**
	 * convert string data to int[]
	 * @param data
	 * @return
	 */
	private static int[] getDataFromTwoCharRow(String data) {
		int[] ret = new int[data.length()];
		int ofs=0;
		String tmp="";
		for (char c: data.toCharArray()) {
			//first char
			if (tmp.isEmpty()) {
				tmp+=c;
			} else {
				//second char
				tmp+=c;
				int i=Integer.parseInt(c+"", 16);
				ret[ofs++]=i;
				tmp="";
			}
		}
		return ret;
	}
}

