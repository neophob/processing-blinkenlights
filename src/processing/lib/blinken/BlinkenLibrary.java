/**
 * blinkenlights processing lib.
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
 * @author		Michael Vogt
 * @modified	16.12.2010
 * @version		v0.5
 */

package processing.lib.blinken;


import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
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

	private static Logger log = Logger.getLogger(BlinkenLibrary.class.getName());

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
	private Thread runner;

	private int defaultDelay = 150;
	// if the animation is currently playing
	private boolean play;
	// if the animation is currently looping
	private boolean loop;
	// ignore blm delay time
	private boolean ignoreFileDelay = false;
	// last time the frame changed
	private int lastJumpTime;

	private boolean threadRunning = false;

	private String filename;
	private int color;

	public final static String NAME = "blinkenlights";
	public final static String VERSION = "v0.7";


	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * @param theParent your papplet
	 * @param filename - the .blm file to load
	 * @throws JAXBException
	 */
	public BlinkenLibrary(PApplet theParent) {
		this(theParent, 255, 255, 255);
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
	public BlinkenLibrary(PApplet parent, int r, int g, int b) {
		super(1, 1, RGB);
		this.color = r<<16 | g<<8 | b;
		this.parent = parent;
		log.log(Level.INFO, "{0} {1}", new Object[] { NAME, VERSION });

		this.parent.registerDispose(this);	
	}
	

	public void loadFile(String filename) {
		this.loadFile(filename, Integer.MAX_VALUE); 
		//yeah, pretty ugly stuff here...
	}
	
	/**
	 * load a new bml file
	 * @param filename
	 */
	public void loadFile(String filename, int maximalSize) {
		InputStream input = null;

		try {
		  //make sure input file exist
			JAXBContext context = JAXBContext.newInstance("processing.lib.blinken.jaxb");
			Unmarshaller unmarshaller = context.createUnmarshaller();			
			input = this.parent.createInput(filename);
		  if (input ==null) {
        //we failed to find file
        log.log(Level.WARNING, "Failed to load {0}, File not found", new Object[] { filename });
        return;
		  }

      this.filename = filename;
      
      //signal to stop
			this.threadRunning = false;

      blm = (Blm) unmarshaller.unmarshal(input);

      //load images
      int wi = Integer.parseInt(blm.getWidth());
      int he = Integer.parseInt(blm.getHeight());

		  
			//wait until thread is stopped
			if (this.runner != null) {				
				try {				
					this.runner.join(100);
				} catch (InterruptedException e) {
					//ignored
				}
			} 

			boolean oldPlay = play;
			//stop thread
			play=false;
			
			//if the image is larger than maximalSize, reduce size
			if (wi > maximalSize || he > maximalSize) {
				//we need to shrink the image!
				log.log(Level.INFO, "Shrink image to {0} pixels, raw size: {1} {2}", new Object[] { maximalSize, wi, he });
				this.frames = extractFrames(color, maximalSize);
			} else {
				this.frames = extractFrames(color, 0);				
			}

			//load delays
			this.delays = extractDelays();
			//init applet
			super.init(frames[0].width, frames[0].height, RGB);

			//Select frame 0
			this.currentFrame=0;
			this.loop = true;
			this.play=oldPlay;

			// and now, make the magic happen
			this.runner = new Thread(this);				
			this.runner.setName("Blinkenlights BML Animator "+filename);
			this.runner.start();

			this.jump(currentFrame);

			log.log(Level.INFO, "Loaded file {0}, contains {1} frames", new Object[] { filename, frames.length });

		} catch (Exception e) {
			//e.printStackTrace();
			log.log(Level.WARNING, "Failed to load {0}, Error: {1}", new Object[] { filename, e });
		} finally {
			try {
				if (input!=null) {
					input.close();
					input = null;
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Failed to close file {0}, Error: {1}" , new Object[] { filename, e });
			}
		}
	}

	/**
	 * clean up thread
	 */
	public void dispose() {
		stop();
		this.threadRunning = false;
		play = false;
		loop = false;
	}

	/**
	 * the thread's run method
	 */
	public void run() {
		threadRunning = true;
		while (threadRunning) {
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
						if (currentFrame >= frames.length-1) {
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
		frames = null;
		delays = null;
		//blm = null;
		log.log(Level.INFO, "Thread {0} stopped", filename);
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

		if (where+1 > frames.length) {
			log.log(Level.WARNING, "Invalid jump frame: {0}", where);
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
		int[] delaysTmp = new int[n];
		for (int i = 0; i < n; i++) {
			int delay = defaultDelay;
			try {
				delay = Integer.parseInt(blm.getFrame().get(i).getDuration());				
			} catch (Exception e) {	}
			delaysTmp[i] = delay; // display duration of frame in milliseconds
		}
		return delaysTmp;
	}

	/**
	 * creates a PImage-array of gif frames in a GifDecoder object 
	 * @return 
	 */
	private PImage[] extractFrames(int color, int maximalSize) {
		int n = blm.getFrame().size();
		PImage[] framesTmp = new PImage[n];

		for (int i = 0; i < n; i++) {
			PImage pi = BlinkenHelper.grabFrame(i, blm, color);
			if (maximalSize>0) {
				pi.resize(maximalSize, maximalSize);
			}
			framesTmp[i] = pi;
		}						
		return framesTmp;
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

