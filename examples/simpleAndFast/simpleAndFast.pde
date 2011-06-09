import processing.lib.blinken.*;

BlinkenLibrary blink;

void setup() {
  //colorize the movie
  blink = new BlinkenLibrary(this, "bb-frogskin1.bml", 255, 192,255);
  blink.loop();
  frameRate(40);
  background(0);
  size(170, 140);
  //ignore the delay of the movie, use our fps!
  blink.setIgnoreFileDelay(true);
}

void draw() { 
    image(blink, 0, 0, 170, 140); 
}
  
