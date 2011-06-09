import processing.lib.blinken.*;

BlinkenLibrary blink;

void setup() {
  blink = new BlinkenLibrary(this, "bnf_auge.bml");
  blink.loop();    
  frameRate(20);
  background(0);
  size(170, 140);
}

void draw() { 
    image(blink, 0, 0, 170, 140); 
}
  
