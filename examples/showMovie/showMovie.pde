import processing.lib.blinken.*;

BlinkenLibrary blink;
int x,y,totFrames;
PFont font;

void setup() {
  font = loadFont("Ziggurat-HTF-Black-32.vlw"); 
  textFont(font); 
  //support for gzip'ed files
  blink = new BlinkenLibrary(this, "rings_0.bml.gz", 255, 155, 66);
  x = blink.width;
  y = blink.height;
  totFrames = blink.getNrOfFrames();
//  blink.setIgnoreFileDelay(true);
  blink.loop();    
  frameRate(10);
  size(x*8, y*8);
  background(0);
}

void draw() { 
    image(blink, 0, 0, x*8, y*8); 
    text(blink.getCurrentFrame()+"/"+totFrames, 15, 30); 
    //random select one frame
    blink.jump(int(random(totFrames)));
}
  
