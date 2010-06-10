import processing.lib.blinken.*;

BlinkenLibrary blink;

void setup() {
  //colorize the movie
  blink = new BlinkenLibrary(this, "torus.bml", 255, 192,255);
  blink.loop();
  frameRate(40);
  background(0);
  size(260, 200);
  //ignore the delay of the movie, use our fps!
  blink.setIgnoreFileDelay(true);
}

void draw() { 
    image(blink, 0, 0, 260, 200); 
}
  
