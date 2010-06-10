import processing.lib.blinken.jaxb.*;
import processing.lib.blinken.*;

BlinkenLibrary blink;

void setup() {
  blink = new BlinkenLibrary(this, "torus.bml");
  blink.loop();    
  frameRate(20);
  background(0);
  size(260, 200);
}

void draw() { 
    image(blink, 0, 0, 260, 200); 
}
  
