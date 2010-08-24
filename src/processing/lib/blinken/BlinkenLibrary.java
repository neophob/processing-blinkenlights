/**
 * you can put a one sentence description of your library here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package processing.lib.blinken;


import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import processing.core.PApplet;
import processing.core.PImage;
import processing.lib.blinken.jaxb.Blm;
import processing.lib.blinken.jaxb.Header;

/**
 * Blinkenlight processing library
 * 
 * by michu / neophob.com 2010 
 * some code is ripped of the animated gif lib by extrapixel.ch 
 *
 */
public class BlinkenLibrary extends PImage implements Runnable {
	
	static Logger log = Logger.getLogger(BlinkenLibrary.class.getName());

	// myParent is a reference to the parent sketch
	private PApplet parent;
	// the marshalled .blm file
	private Blm blm;	
	// array containing the frames as PImages
	private PImage[] frames;
	// array containing the delay in ms of every frame
	private int[] delays;
	// the current frame number
	private int currentFrame;
	// displaing thread
	Thread runner;
	private int defaultDelay = 150;
	// if the animation is currently playing
	private boolean play;
	// if the animation is currently looping
	private boolean loop;
	// ignore blm delay time
	private boolean ignoreFileDelay = false;
	// last time the frame changed
	private int lastJumpTime;

	private String filename;
	private int color;
	
	public final static String NAME = "blinkenlights";
	public final static String VERSION = "v0.41";


	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * @param theParent your papplet
	 * @param filename - the .blm file to load
	 * @throws JAXBException
	 */
	public BlinkenLibrary(PApplet theParent, String filename) {
		this(theParent, filename, 255, 255, 255);
	}
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * @param parent your papplet
	 * @param filename - the .blm file to load
	 * @param r colorize the movie
	 * @param g colorize the movie
	 * @param b colorize the movie
	 */
	public BlinkenLibrary(PApplet parent, String filename, int r, int g, int b) {
		super(1, 1, RGB); 		
		this.parent = parent;
		log.log(Level.INFO, "{0} {1}", new Object[] { NAME, VERSION });

		// fill up the PImage and the delay arrays
		this.color = r<<16 | g<<8 | b;
		
		this.parent.registerDispose(this);	
		
		this.loadFile(filename);
		// and now, make the magic happen
		this.runner = new Thread(this);
		this.runner.setName("Blinkenlights BML Animator");
		this.runner.start(); 		
	}
	
	/**
	 * load a new file
	 * @param filename
	 */
	public void loadFile(String filename) {
		boolean oldPlay = play;
		//stop thread
		play=false;

		try {
			JAXBContext context = JAXBContext.newInstance("processing.lib.blinken.jaxb");
			Unmarshaller unmarshaller = context.createUnmarshaller();
			this.filename = filename;
			InputStream input = this.parent.createInput(filename);
			blm = (Blm) unmarshaller.unmarshal(input);
			input.close();
			
			//load images
			this.frames = extractFrames(color);
			
			//load delays
			this.delays = extractDelays();
			//reinit applet
			super.init(frames[0].width, frames[0].height, RGB);
			//Select frame 0
			this.currentFrame=0;
			this.jump(0);
			log.log(Level.INFO,
					"Loaded file {0}, contains {1} frames"
					, new Object[] { filename, frames.length });
		} catch (Exception e) {
			log.log(Level.WARNING,
					"Failed to load {0}, Error: {1}"
					, new Object[] { filename, e });
		}
		this.loop = true;
		this.play=oldPlay;
		// re-init our PImage with the new size	
	}

	/**
	 * clean up thread
	 */
	public void dispose() {
		stop();
		runner = null;
		play = false;
		loop = false;
	}

	/**
	 * the thread's run method
	 */
	public void run() {
		while (Thread.currentThread() == runner) {
			try {
				if (ignoreFileDelay) {
					int delay = (int)(1000.0f/this.parent.frameRate);
					Thread.sleep(delay);
				} else {
					//wait per default 5ms - check below if next frame should taken
					Thread.sleep(5);	
				}							
			} catch (InterruptedException e) {
			}

			if (play) { 
				// if playing, check if we need to go to next frame
				if (ignoreFileDelay || (parent.millis() - lastJumpTime >= delays[currentFrame])) {
					// we need to jump

					try {
						if (currentFrame == frames.length - 1) {
							// its the last frame
							if (loop) {
								jump(0); // loop is on, so rewind
							} 
						} else {
							// go to the next frame
							jump(currentFrame + 1);
						}					

					} catch (Exception e) {
						//jump can fail if we load a new file!
					}
				}
			}
		}
		log.log(Level.INFO, "Thread {0} stopped", filename);
		frames = null;
		delays = null;
		System.gc();
	}

	/**
	 * Jump to a specific location (in frames). if the frame does not exist, go
	 * to last frame
	 * @param where the frame nr
	 */
	public void jump(int where) {
		if (where < 0) {
			where = 0;
		}
		
		if (where > frames.length) {
			//System.out.println("where > frames.length:"+where+" > "+frames.length);
			return;
		}
		currentFrame = where;
		
		// update the pixel-array			
		loadPixels();
		System.arraycopy(frames[currentFrame].pixels, 0, pixels, 0, width*height);
		updatePixels();

		// set the jump time
		lastJumpTime = parent.millis();
	} 

	/**
	 * creates an int-array of frame delays in the gifDecoder object
	 * @return
	 */
	private int[] extractDelays() {
		int n = blm.getFrame().size();
		int[] delays = new int[n];
		for (int i = 0; i < n; i++) {
			int delay = defaultDelay;
			try {
				delay = Integer.parseInt(blm.getFrame().get(i).getDuration());				
			} catch (Exception e) {	}
			delays[i] = delay; // display duration of frame in milliseconds
		}
		return delays;
	}

	/**
	 * creates a PImage-array of gif frames in a GifDecoder object 
	 * @return 
	 */
	private PImage[] extractFrames(int color) {
		int n = blm.getFrame().size();
		PImage[] frames = new PImage[n];

		for (int i = 0; i < n; i++) {
			BufferedImage blinkFrame = BlinkenHelper.grabFrame(i, blm, color);
			frames[i] = new PImage(blinkFrame.getWidth(), blinkFrame.getHeight(), RGB);
			frames[i].loadPixels();
			int[] pixels = blinkFrame.getRGB(0, 0, blinkFrame.getWidth(), blinkFrame.getHeight(), null, 0, blinkFrame.getWidth());
			System.arraycopy(pixels, 0, frames[i].pixels, 0, blinkFrame.getWidth() * blinkFrame.getHeight());
			frames[i].updatePixels();
		}
		return frames;
	}

	/**
	 * Begin playing the animation
	 */
	public void play() {
		play = true;
	}
	/**
	 * Begin playing the animation, with repeat.
	 */
	public void loop() {
		play = true;
		loop = true;
	}
	/**
	 * Shut off the repeating loop (enabled by default).
	 */
	public void noLoop() {
		loop = false;
	}
	/**
	 * Pause the animation at its current frame.
	 */
	public void pause() {
		play = false;
	}
	/**
	 * Stop the animation, and rewind.
	 */
	public void stop() {
		play = false;
		currentFrame = 0;
	}
	/**
	 * total frame numbers of current movie
	 * @return how many frames this movie contains
	 */
	public int getNrOfFrames() {
		return blm.getFrame().size();
	}

	/**
	 * get meta information (title, duration...) about the loaded file
	 * @return the header object
	 */
	public Header getHeader() {
		return blm.getHeader();
	}

	/**
	 * get the marshalled object
	 * @return the marshalled blinkenlights file
	 */
	public Blm getRawObject() {
		return blm;
	}
	/**
	 * return current frame
	 * @return current frame nr
	 */
	public int getCurrentFrame() {
		return currentFrame;
	}
	/**
	 * is the internal (frame) delay used or an external?
	 * @return true if processing framerate is used, else
	 * the file's delay is used
	 */
	public boolean isIgnoreFileDelay() {
		return ignoreFileDelay;
	}
	/**
	 * ignore the source file delay time
	 * @param ignoreFileDelay
	 */
	public void setIgnoreFileDelay(boolean ignoreFileDelay) {
		this.ignoreFileDelay = ignoreFileDelay;
	}

	/**
	 * return the version of the library.
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

}

