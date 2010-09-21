package processing.lib.blinken;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import processing.core.PImage;
import processing.lib.blinken.jaxb.Blm;
import processing.lib.blinken.jaxb.Frame;
import processing.lib.blinken.jaxb.Row;

/**
 * some blinkenlights helper functions
 * @author michael vogt / neophob.com (c) 2010
 *
 */
public class BlinkenHelper {

	static Logger log = Logger.getLogger(BlinkenHelper.class.getName());
	
	private BlinkenHelper() {
		//no instance allowed
	}
	
	/**
	 * return a converted frame
	 * @param frameNr which frame to convert
	 * @param blm to marshalled object, our source
	 * @return an image out of the frame
	 */
	public static PImage grabFrame(int frameNr, Blm blm, int color) throws NumberFormatException {
		int frames = blm.getFrame().size();
		
		//some sanity checks
		if (frameNr > frames || frameNr < 0) {
			return null;
		}
		
		int width = Integer.parseInt(blm.getWidth());
		int height = Integer.parseInt(blm.getHeight());
		int bits = Integer.parseInt(blm.getBits());
		//int channels = Integer.parseInt(blm.getChannels());
		
		Frame f = blm.getFrame().get(frameNr);
		List<Row> rows = f.getRow();
		
		PImage img = new PImage(width, height, PImage.RGB);
		img.loadPixels();
		
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
					log.log(Level.WARNING,
							"Ooops: looks like here is an error: {0}!={1}"
							, new Object[] { width, data.length });
				}
			} else {
				//two char per color value
				//TODO no rgb files exists yet - so this code is very experimental!
				data = getDataFromTwoCharRow(s);
			}
			
			//TODO channels/RGB
			
			System.arraycopy(data, 0, img.pixels, ofs, width);
			ofs += width;			
		}
		img.updatePixels();
		return img;
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

