import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.awt.Frame; 
import java.awt.BorderLayout; 
import controlP5.*; 
import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class BasicRealtimePlotter extends PApplet {

// import libraries


 // http://www.sojamo.de/libraries/controlP5/


/* SETTINGS BEGIN */

// Serial port to connect to
String serialPortName = "/dev/tty.usbmodem14643401";

// If you want to debug the plotter without using a real serial port set this to true
boolean mockupSerial = false;

/* SETTINGS END */

Serial serialPort; // Serial port object

// interface stuff
ControlP5 cp5;

// Settings for the plotter are saved in this file
JSONObject plotterConfigJSON;

// plots
Graph BarChart = new Graph(225, 70, 600, 150, color(20, 20, 200));
Graph LineGraph = new Graph(225, 350, 600, 200, color (20, 20, 200));
float[] barChartValues = new float[6];

float[][] lineGraphValues = new float[6][400];
float[] lineGraphSampleNumbers = new float[400];

int[] graphColors = new int[6];

// helper for saving the executing path
String topSketchPath = "";

public void setup() {
  surface.setTitle("Realtime plotter");
  

  // set line graph colors
  graphColors[0] = color(131, 255, 20);
  graphColors[1] = color(232, 158, 12);
  graphColors[2] = color(255, 0, 0);
  graphColors[3] = color(62, 12, 232);
  graphColors[4] = color(13, 255, 243);
  graphColors[5] = color(200, 46, 232);

  // settings save file
  topSketchPath = sketchPath();
  plotterConfigJSON = loadJSONObject(topSketchPath+"/plotter_config.json");

  // gui
  cp5 = new ControlP5(this);
  
  // init charts
  setChartSettings();
  for (int i=0; i<barChartValues.length; i++) {
    barChartValues[i] = 0;
  }
  // build x axis values for the line graph
  for (int i=0; i<lineGraphValues.length; i++) {
    for (int k=0; k<lineGraphValues[0].length; k++) {
      lineGraphValues[i][k] = 0;
      if (i==0)
        lineGraphSampleNumbers[k] = k;
    }
  }
  
  // start serial communication
  if (!mockupSerial) {
    //String serialPortName = Serial.list()[3];
    serialPort = new Serial(this, serialPortName, 115200);
  }
  else
    serialPort = null;

  // build the gui
  int x = 170;
  int y = 60;
  cp5.addTextfield("bcMaxY").setPosition(x, y).setText(getPlotterConfigString("bcMaxY")).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMinY").setPosition(x, y=y+150).setText(getPlotterConfigString("bcMinY")).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMaxY").setPosition(x, y=y+130).setText(getPlotterConfigString("lgMaxY")).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMinY").setPosition(x, y=y+200).setText(getPlotterConfigString("lgMinY")).setWidth(40).setAutoClear(false);

  cp5.addTextlabel("on/off2").setText("on/off").setPosition(x=13, y=20).setColor(0);
  cp5.addTextlabel("multipliers2").setText("multipliers").setPosition(x=55, y).setColor(0);
  cp5.addTextfield("bcMultiplier1").setPosition(x=60, y=30).setText(getPlotterConfigString("bcMultiplier1")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMultiplier2").setPosition(x, y=y+40).setText(getPlotterConfigString("bcMultiplier2")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMultiplier3").setPosition(x, y=y+40).setText(getPlotterConfigString("bcMultiplier3")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMultiplier4").setPosition(x, y=y+40).setText(getPlotterConfigString("bcMultiplier4")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMultiplier5").setPosition(x, y=y+40).setText(getPlotterConfigString("bcMultiplier5")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("bcMultiplier6").setPosition(x, y=y+40).setText(getPlotterConfigString("bcMultiplier6")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addToggle("bcVisible1").setPosition(x=x-50, y=30).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible1"))).setMode(ControlP5.SWITCH);
  cp5.addToggle("bcVisible2").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible2"))).setMode(ControlP5.SWITCH);
  cp5.addToggle("bcVisible3").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible3"))).setMode(ControlP5.SWITCH);
  cp5.addToggle("bcVisible4").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible4"))).setMode(ControlP5.SWITCH);
  cp5.addToggle("bcVisible5").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible5"))).setMode(ControlP5.SWITCH);
  cp5.addToggle("bcVisible6").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("bcVisible6"))).setMode(ControlP5.SWITCH);

  cp5.addTextlabel("label").setText("on/off").setPosition(x=13, y=y+90).setColor(0);
  cp5.addTextlabel("multipliers").setText("multipliers").setPosition(x=55, y).setColor(0);
  cp5.addTextfield("lgMultiplier1").setPosition(x=60, y=y+10).setText(getPlotterConfigString("lgMultiplier1")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMultiplier2").setPosition(x, y=y+40).setText(getPlotterConfigString("lgMultiplier2")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMultiplier3").setPosition(x, y=y+40).setText(getPlotterConfigString("lgMultiplier3")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMultiplier4").setPosition(x, y=y+40).setText(getPlotterConfigString("lgMultiplier4")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMultiplier5").setPosition(x, y=y+40).setText(getPlotterConfigString("lgMultiplier5")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addTextfield("lgMultiplier6").setPosition(x, y=y+40).setText(getPlotterConfigString("lgMultiplier6")).setColorCaptionLabel(0).setWidth(40).setAutoClear(false);
  cp5.addToggle("lgVisible1").setPosition(x=x-50, y=330).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible1"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[0]);
  cp5.addToggle("lgVisible2").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible2"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[1]);
  cp5.addToggle("lgVisible3").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible3"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[2]);
  cp5.addToggle("lgVisible4").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible4"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[3]);
  cp5.addToggle("lgVisible5").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible5"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[4]);
  cp5.addToggle("lgVisible6").setPosition(x, y=y+40).setValue(PApplet.parseInt(getPlotterConfigString("lgVisible6"))).setMode(ControlP5.SWITCH).setColorActive(graphColors[5]);
}

byte[] inBuffer = new byte[100]; // holds serial message
int i = 0; // loop variable
public void draw() {
  /* Read serial and update values */
  if (mockupSerial || serialPort.available() > 0) {
    String myString = "";
    if (!mockupSerial) {
      try {
        serialPort.readBytesUntil('\r', inBuffer);
      }
      catch (Exception e) {
      }
      myString = new String(inBuffer);
    }
    else {
      myString = mockupSerialFunction();
    }

    //println(myString);

    // split the string at delimiter (space)
    String[] nums = split(myString, ' ');
    
    // count number of bars and line graphs to hide
    int numberOfInvisibleBars = 0;
    for (i=0; i<6; i++) {
      if (PApplet.parseInt(getPlotterConfigString("bcVisible"+(i+1))) == 0) {
        numberOfInvisibleBars++;
      }
    }
    int numberOfInvisibleLineGraphs = 0;
    for (i=0; i<6; i++) {
      if (PApplet.parseInt(getPlotterConfigString("lgVisible"+(i+1))) == 0) {
        numberOfInvisibleLineGraphs++;
      }
    }
    // build a new array to fit the data to show
    barChartValues = new float[6-numberOfInvisibleBars];

    // build the arrays for bar charts and line graphs
    int barchartIndex = 0;
    for (i=0; i<nums.length; i++) {

      // update barchart
      try {
        if (PApplet.parseInt(getPlotterConfigString("bcVisible"+(i+1))) == 1) {
          if (barchartIndex < barChartValues.length)
            barChartValues[barchartIndex++] = PApplet.parseFloat(nums[i])*PApplet.parseFloat(getPlotterConfigString("bcMultiplier"+(i+1)));
        }
        else {
        }
      }
      catch (Exception e) {
      }

      // update line graph
      try {
        if (i<lineGraphValues.length) {
          for (int k=0; k<lineGraphValues[i].length-1; k++) {
            lineGraphValues[i][k] = lineGraphValues[i][k+1];
          }

          lineGraphValues[i][lineGraphValues[i].length-1] = PApplet.parseFloat(nums[i])*PApplet.parseFloat(getPlotterConfigString("lgMultiplier"+(i+1)));
        }
      }
      catch (Exception e) {
      }
    }
  }

  // draw the bar chart
  background(255); 
  BarChart.DrawAxis();              
  BarChart.Bar(barChartValues); // This draws a bar graph of Array4

  // draw the line graphs
  LineGraph.DrawAxis();
  for (int i=0;i<lineGraphValues.length; i++) {
    LineGraph.GraphColor = graphColors[i];
    if (PApplet.parseInt(getPlotterConfigString("lgVisible"+(i+1))) == 1)
      LineGraph.LineGraph(lineGraphSampleNumbers, lineGraphValues[i]);
  }
}

// called each time the chart settings are changed by the user 
public void setChartSettings() {
  BarChart.xLabel=" Readings ";
  BarChart.yLabel="Value";
  BarChart.Title="";  
  BarChart.xDiv=1;  
  BarChart.yMax=PApplet.parseInt(getPlotterConfigString("bcMaxY")); 
  BarChart.yMin=PApplet.parseInt(getPlotterConfigString("bcMinY"));

  LineGraph.xLabel=" Samples ";
  LineGraph.yLabel="Value";
  LineGraph.Title="";  
  LineGraph.xDiv=20;  
  LineGraph.xMax=0; 
  LineGraph.xMin=-100;  
  LineGraph.yMax=PApplet.parseInt(getPlotterConfigString("lgMaxY")); 
  LineGraph.yMin=PApplet.parseInt(getPlotterConfigString("lgMinY"));
}

// handle gui actions
public void controlEvent(ControlEvent theEvent) {
  if (theEvent.isAssignableFrom(Textfield.class) || theEvent.isAssignableFrom(Toggle.class) || theEvent.isAssignableFrom(Button.class)) {
    String parameter = theEvent.getName();
    String value = "";
    if (theEvent.isAssignableFrom(Textfield.class))
      value = theEvent.getStringValue();
    else if (theEvent.isAssignableFrom(Toggle.class) || theEvent.isAssignableFrom(Button.class))
      value = theEvent.getValue()+"";

    plotterConfigJSON.setString(parameter, value);
    saveJSONObject(plotterConfigJSON, topSketchPath+"/plotter_config.json");
  }
  setChartSettings();
}

// get gui settings from settings file
public String getPlotterConfigString(String id) {
  String r = "";
  try {
    r = plotterConfigJSON.getString(id);
  } 
  catch (Exception e) {
    r = "";
  }
  return r;
}

  
/*   =================================================================================       
     The Graph class contains functions and variables that have been created to draw 
     graphs. Here is a quick list of functions within the graph class:
          
       Graph(int x, int y, int w, int h,color k)
       DrawAxis()
       Bar([])
       smoothLine([][])
       DotGraph([][])
       LineGraph([][]) 
     
     =================================================================================*/   

    
    class Graph 
    {
      
      boolean Dot=true;            // Draw dots at each data point if true
      boolean RightAxis;            // Draw the next graph using the right axis if true
      boolean ErrorFlag=false;      // If the time array isn't in ascending order, make true  
      boolean ShowMouseLines=true;  // Draw lines and give values of the mouse position
    
      int     xDiv=5,yDiv=5;            // Number of sub divisions
      int     xPos,yPos;            // location of the top left corner of the graph  
      int     Width,Height;         // Width and height of the graph
    

      int   GraphColor;
      int   BackgroundColor=color(255);  
      int   StrokeColor=color(180);     
      
      String  Title="Title";          // Default titles
      String  xLabel="x - Label";
      String  yLabel="y - Label";

      float   yMax=1024, yMin=0;      // Default axis dimensions
      float   xMax=10, xMin=0;
      float   yMaxRight=1024,yMinRight=0;
  
      PFont   Font;                   // Selected font used for text 
      
  //    int Peakcounter=0,nPeakcounter=0;
     
      Graph(int x, int y, int w, int h,int k) {  // The main declaration function
        xPos = x;
        yPos = y;
        Width = w;
        Height = h;
        GraphColor = k;
        
      }
    
     
       public void DrawAxis(){
       
   /*  =========================================================================================
        Main axes Lines, Graph Labels, Graph Background
       ==========================================================================================  */
    
        fill(BackgroundColor); color(0);stroke(StrokeColor);strokeWeight(1);
        int t=60;
        
        rect(xPos-t*1.6f,yPos-t,Width+t*2.5f,Height+t*2);            // outline
        textAlign(CENTER);textSize(18);
        float c=textWidth(Title);
        fill(BackgroundColor); color(0);stroke(0);strokeWeight(1);
        rect(xPos+Width/2-c/2,yPos-35,c,0);                         // Heading Rectangle  
        
        fill(0);
        text(Title,xPos+Width/2,yPos-37);                            // Heading Title
        textAlign(CENTER);textSize(14);
        text(xLabel,xPos+Width/2,yPos+Height+t/1.5f);                     // x-axis Label 
        
        rotate(-PI/2);                                               // rotate -90 degrees
        text(yLabel,-yPos-Height/2,xPos-t*1.6f+20);                   // y-axis Label  
        rotate(PI/2);                                                // rotate back
        
        textSize(10); noFill(); stroke(0); smooth();strokeWeight(1);
          //Edges
          line(xPos-3,yPos+Height,xPos-3,yPos);                        // y-axis line 
          line(xPos-3,yPos+Height,xPos+Width+5,yPos+Height);           // x-axis line 
          
           stroke(200);
          if(yMin<0){
                    line(xPos-7,                                       // zero line 
                         yPos+Height-(abs(yMin)/(yMax-yMin))*Height,   // 
                         xPos+Width,
                         yPos+Height-(abs(yMin)/(yMax-yMin))*Height
                         );
          
                    
          }
          
          if(RightAxis){                                       // Right-axis line   
              stroke(0);
              line(xPos+Width+3,yPos+Height,xPos+Width+3,yPos);
            }
            
           /*  =========================================================================================
                Sub-devisions for both axes, left and right
               ==========================================================================================  */
            
            stroke(0);
            
           for(int x=0; x<=xDiv; x++){
       
            /*  =========================================================================================
                  x-axis
                ==========================================================================================  */
             
            line(PApplet.parseFloat(x)/xDiv*Width+xPos-3,yPos+Height,       //  x-axis Sub devisions    
                 PApplet.parseFloat(x)/xDiv*Width+xPos-3,yPos+Height+5);     
                 
            textSize(10);                                      // x-axis Labels
            String xAxis=str(xMin+PApplet.parseFloat(x)/xDiv*(xMax-xMin));  // the only way to get a specific number of decimals 
            String[] xAxisMS=split(xAxis,'.');                 // is to split the float into strings 
            text(xAxisMS[0]+"."+xAxisMS[1].charAt(0),          // ...
                 PApplet.parseFloat(x)/xDiv*Width+xPos-3,yPos+Height+15);   // x-axis Labels
          }
          
          
           /*  =========================================================================================
                 left y-axis
               ==========================================================================================  */
          
          for(int y=0; y<=yDiv; y++){
            line(xPos-3,PApplet.parseFloat(y)/yDiv*Height+yPos,                // ...
                  xPos-7,PApplet.parseFloat(y)/yDiv*Height+yPos);              // y-axis lines 
            
            textAlign(RIGHT);fill(20);
            
            String yAxis=str(yMin+PApplet.parseFloat(y)/yDiv*(yMax-yMin));     // Make y Label a string
            String[] yAxisMS=split(yAxis,'.');                    // Split string
           
            text(yAxisMS[0]+"."+yAxisMS[1].charAt(0),             // ... 
                 xPos-15,PApplet.parseFloat(yDiv-y)/yDiv*Height+yPos+3);       // y-axis Labels 
                        
                        
            /*  =========================================================================================
                 right y-axis
                ==========================================================================================  */
            
            if(RightAxis){
             
              color(GraphColor); stroke(GraphColor);fill(20);
            
              line(xPos+Width+3,PApplet.parseFloat(y)/yDiv*Height+yPos,             // ...
                   xPos+Width+7,PApplet.parseFloat(y)/yDiv*Height+yPos);            // Right Y axis sub devisions
                   
              textAlign(LEFT); 
            
              String yAxisRight=str(yMinRight+PApplet.parseFloat(y)/                // ...
                                yDiv*(yMaxRight-yMinRight));           // convert axis values into string
              String[] yAxisRightMS=split(yAxisRight,'.');             // 
           
               text(yAxisRightMS[0]+"."+yAxisRightMS[1].charAt(0),     // Right Y axis text
                    xPos+Width+15,PApplet.parseFloat(yDiv-y)/yDiv*Height+yPos+3);   // it's x,y location
            
              noFill();
            }stroke(0);
            
          
          }
          
 
      }
      
      
   /*  =========================================================================================
       Bar graph
       ==========================================================================================  */   
      
      public void Bar(float[] a ,int from, int to) {
        
         
          stroke(GraphColor);
          fill(GraphColor);
          
          if(from<0){                                      // If the From or To value is out of bounds 
           for (int x=0; x<a.length; x++){                 // of the array, adjust them 
               rect(PApplet.parseInt(xPos+x*PApplet.parseFloat(Width)/(a.length)),
                    yPos+Height-2,
                    Width/a.length-2,
                    -a[x]/(yMax-yMin)*Height);
                 }
          }
          
          else {
          for (int x=from; x<to; x++){
            
            rect(PApplet.parseInt(xPos+(x-from)*PApplet.parseFloat(Width)/(to-from)),
                     yPos+Height-2,
                     Width/(to-from)-2,
                     -a[x]/(yMax-yMin)*Height);
                     
    
          }
          }
          
      }
  public void Bar(float[] a ) {
  
              stroke(GraphColor);
          fill(GraphColor);
    
  for (int x=0; x<a.length; x++){                 // of the array, adjust them 
               rect(PApplet.parseInt(xPos+x*PApplet.parseFloat(Width)/(a.length)),
                    yPos+Height-2,
                    Width/a.length-2,
                    -a[x]/(yMax-yMin)*Height);
                 }
          }
  
  
   /*  =========================================================================================
       Dot graph
       ==========================================================================================  */   
       
        public void DotGraph(float[] x ,float[] y) {
          
         for (int i=0; i<x.length; i++){
                    strokeWeight(2);stroke(GraphColor);noFill();smooth();
           ellipse(
                   xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width,
                   yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height,
                   2,2
                   );
         }
                             
      }
      
   /*  =========================================================================================
       Streight line graph 
       ==========================================================================================  */
       
      public void LineGraph(float[] x ,float[] y) {
          
         for (int i=0; i<(x.length-1); i++){
                    strokeWeight(2);stroke(GraphColor);noFill();smooth();
           line(xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width,
                                            yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height,
                                            xPos+(x[i+1]-x[0])/(x[x.length-1]-x[0])*Width,
                                            yPos+Height-(y[i+1]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height);
         }
                             
      }
      
      /*  =========================================================================================
             smoothLine
          ==========================================================================================  */
    
      public void smoothLine(float[] x ,float[] y) {
         
        float tempyMax=yMax, tempyMin=yMin;
        
        if(RightAxis){yMax=yMaxRight;yMin=yMinRight;} 
         
        int counter=0;
        int xlocation=0,ylocation=0;
         
//         if(!ErrorFlag |true ){    // sort out later!
          
          beginShape(); strokeWeight(2);stroke(GraphColor);noFill();smooth();
         
            for (int i=0; i<x.length; i++){
              
           /* ===========================================================================
               Check for errors-> Make sure time array doesn't decrease (go back in time) 
              ===========================================================================*/
              if(i<x.length-1){
                if(x[i]>x[i+1]){
                   
                  ErrorFlag=true;
                
                }
              }
         
         /* =================================================================================       
             First and last bits can't be part of the curve, no points before first bit, 
             none after last bit. So a streight line is drawn instead   
            ================================================================================= */ 

              if(i==0 || i==x.length-2)line(xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width,
                                            yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height,
                                            xPos+(x[i+1]-x[0])/(x[x.length-1]-x[0])*Width,
                                            yPos+Height-(y[i+1]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height);
                                            
          /* =================================================================================       
              For the rest of the array a curve (spline curve) can be created making the graph 
              smooth.     
             ================================================================================= */ 
                            
              curveVertex( xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width,
                           yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height);
                           
           /* =================================================================================       
              If the Dot option is true, Place a dot at each data point.  
             ================================================================================= */    
           
             if(Dot)ellipse(
                             xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width,
                             yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height,
                             2,2
                             );
                             
         /* =================================================================================       
             Highlights points closest to Mouse X position   
            =================================================================================*/ 
                          
              if( abs(mouseX-(xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width))<5 ){
                
                 
                  float yLinePosition = yPos+Height-(y[i]/(yMax-yMin)*Height)+(yMin)/(yMax-yMin)*Height;
                  float xLinePosition = xPos+(x[i]-x[0])/(x[x.length-1]-x[0])*Width;
                  strokeWeight(1);stroke(240);
                 // line(xPos,yLinePosition,xPos+Width,yLinePosition);
                  strokeWeight(2);stroke(GraphColor);
                  
                  ellipse(xLinePosition,yLinePosition,4,4);
              }
              
     
              
            }  
       
          endShape(); 
          yMax=tempyMax; yMin=tempyMin;
                float xAxisTitleWidth=textWidth(str(map(xlocation,xPos,xPos+Width,x[0],x[x.length-1])));
          
           
       if((mouseX>xPos&mouseX<(xPos+Width))&(mouseY>yPos&mouseY<(yPos+Height))){   
        if(ShowMouseLines){
              // if(mouseX<xPos)xlocation=xPos;
            if(mouseX>xPos+Width)xlocation=xPos+Width;
            else xlocation=mouseX;
            stroke(200); strokeWeight(0.5f);fill(255);color(50);
            // Rectangle and x position
            line(xlocation,yPos,xlocation,yPos+Height);
            rect(xlocation-xAxisTitleWidth/2-10,yPos+Height-16,xAxisTitleWidth+20,12);
            
            textAlign(CENTER); fill(160);
            text(map(xlocation,xPos,xPos+Width,x[0],x[x.length-1]),xlocation,yPos+Height-6);
            
           // if(mouseY<yPos)ylocation=yPos;
             if(mouseY>yPos+Height)ylocation=yPos+Height;
            else ylocation=mouseY;
          
           // Rectangle and y position
            stroke(200); strokeWeight(0.5f);fill(255);color(50);
            
            line(xPos,ylocation,xPos+Width,ylocation);
             int yAxisTitleWidth=PApplet.parseInt(textWidth(str(map(ylocation,yPos,yPos+Height,y[0],y[y.length-1]))) );
            rect(xPos-15+3,ylocation-6, -60 ,12);
            
            textAlign(RIGHT); fill(GraphColor);//StrokeColor
          //    text(map(ylocation,yPos+Height,yPos,yMin,yMax),xPos+Width+3,yPos+Height+4);
            text(map(ylocation,yPos+Height,yPos,yMin,yMax),xPos -15,ylocation+4);
           if(RightAxis){ 
                          
                           stroke(200); strokeWeight(0.5f);fill(255);color(50);
                           
                           rect(xPos+Width+15-3,ylocation-6, 60 ,12);  
                            textAlign(LEFT); fill(160);
                           text(map(ylocation,yPos+Height,yPos,yMinRight,yMaxRight),xPos+Width+15,ylocation+4);
           }
            noStroke();noFill();
         }
       }
            
   
      }

       
          public void smoothLine(float[] x ,float[] y, float[] z, float[] a ) {
           GraphColor=color(188,53,53);
            smoothLine(x ,y);
           GraphColor=color(193-100,216-100,16);
           smoothLine(z ,a);
   
       }
       
       
       
    }
    
 
// If you want to debug the plotter without using a real serial port

int mockupValue = 0;
int mockupDirection = 10;
public String mockupSerialFunction() {
  mockupValue = (mockupValue + mockupDirection);
  if (mockupValue > 100)
    mockupDirection = -10;
  else if (mockupValue < -100)
    mockupDirection = 10;
  String r = "";
  for (int i = 0; i<6; i++) {
    switch (i) {
    case 0:
      r += mockupValue+" ";
      break;
    case 1:
      r += 100*cos(mockupValue*(2*3.14f)/1000)+" ";
      break;
    case 2:
      r += mockupValue/4+" ";
      break;
    case 3:
      r += mockupValue/8+" ";
      break;
    case 4:
      r += mockupValue/16+" ";
      break;
    case 5:
      r += mockupValue/32+" ";
      break;
    }
    if (i < 7)
      r += '\r';
  }
  delay(10);
  return r;
}
  public void settings() {  size(890, 620); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "BasicRealtimePlotter" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
