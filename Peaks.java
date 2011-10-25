
import java.lang.*;
import java.awt.*;
import java.util.*;

public class Peaks{
    public int nPeaks;
    public int framesPerPixel;
    public byte[][] peaks=new byte[2][128];

    public ArrayList<byte[][]> peaksPeaks=new ArrayList<byte[][]>();


    private void setNewPeakLength(int newLength){
	byte[] new_peaks=new byte[newLength];
	java.lang.System.arraycopy(peaks[0],0,new_peaks,0,nPeaks);
	peaks[0]=new_peaks;
	
	new_peaks=new byte[newLength];
	java.lang.System.arraycopy(peaks[1],0,new_peaks,0,nPeaks);
	peaks[1]=new_peaks;
    }


    private static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    private void updateMinMax(short minmax[],short[] buf,int x1,int x2){
	short min=minmax[0];
	short max=minmax[1];

	for(int i=x1;i<x2;i++){
	    short val;
	    val=buf[i];
	    if(val<min)
		min=val;
	    if(val>max)
		max=val;
	}

	minmax[0]=min;
	minmax[1]=max;
    }


    int counter=0;
    short minmax_temp[];

    private void addPeak(){
	counter=0;
	if(peaks[0].length==nPeaks)
	    setNewPeakLength(nPeaks*2);

	peaks[0][nPeaks]=(byte)(minmax_temp[0]/256);
	peaks[1][nPeaks]=(byte)(minmax_temp[1]/256);
	nPeaks++;

	minmax_temp[0]=32767;
	minmax_temp[1]=-32768;
    }

    public void addData(short[] buf,int nFrames){
	//if(nFrames>0)
	//    return;

	if(nFrames+counter<=framesPerPixel){
	    updateMinMax(minmax_temp,buf,0,nFrames);
	    counter+=nFrames;
	    if(counter==framesPerPixel)
		addPeak();
	    return;
	}

	int pos=0;
	while(true){
	    int nFrames2=framesPerPixel-counter;

	    if(pos+nFrames2>nFrames)
		nFrames2=nFrames-pos;

	    updateMinMax(minmax_temp,buf,pos,pos+nFrames2);
	    pos+=nFrames2;
	    counter+=nFrames2;

	    if(counter==framesPerPixel)
		addPeak();

	    if(pos==nFrames)
		break;
	}
    }


    private void addPeaksPeaks(byte[][] peaks){
	int nFrames=peaks[0].length;
	if(nFrames<=2)
	    return;

	int nSubFrames=nFrames/2;
	if((nSubFrames % 2)==1)
	    nSubFrames--;

	byte[][] subPeaks=new byte[2][nSubFrames];

	for(int i=0;i<nSubFrames;i++){
	    subPeaks[0][i] = (peaks[0][i*2+1] < peaks[0][i*2]) ? peaks[0][i*2+1] : peaks[0][i*2];
	    subPeaks[1][i]=  (peaks[1][i*2+1] > peaks[1][i*2]) ? peaks[1][i*2+1] : peaks[1][i*2];
	}


	peaksPeaks.add(subPeaks);
	addPeaksPeaks(subPeaks);
    }

    public void close(){
	if(counter>0)
	    addPeak();

	if(peaks[0].length>=nPeaks)
	    setNewPeakLength(nPeaks);

	peaksPeaks.add(peaks);
	addPeaksPeaks(peaks);
    }

    private byte findMin(final byte[] p,final int x1,final int x2){
	byte min=p[x1];

	for(int i=x1+1;i<x2;i++){
	    final byte val=p[i];
	    if(val<min)
		min=val;
	}
	return min;
    }

    private byte findMax(final byte[] p,final int x1,final int x2){
	byte max=p[x1];

	for(int i=x1+1;i<x2;i++){
	    final byte val=p[i];
	    if(val>max)
		max=val;
	}
	return max;
    }


    /*
    private float paintWave(Graphics2D g,Color c1, Color c2,int x1,int y1,int x2,int y2,boolean playWave,
			    float pixelPos,float peaksPerPixel,byte[][] peaks,AlphaComposite alphaComposite
			    ){
	byte min;
	byte max;
	byte[] minp=peaks[0];
	byte[] maxp=peaks[1];
    
	if(playWave==false){
	    g.setComposite(alphaComposite);
	    g.setColor(c1);
	}
	for(int x=x1;x<x2;x++){
	    int px1=(int)pixelPos;
	    int px2=(int)(pixelPos+peaksPerPixel);
	    if(px2-px1==1){
		min = minp[px1] < minp[px2] ? minp[px1] : minp[px2];
		max = maxp[px1] > maxp[px2] ? maxp[px1] : maxp[px2];
	    }else{
		min=findMin(minp,px1,px2);
		max=findMax(maxp,px1,px2);
	    }
	    pixelPos+=peaksPerPixel;
	    int py1=(int)scale(min,-128,127,y1,y2);
	    int py2=(int)scale(max,-128,127,y1,y2);
	    if(playWave){
		g.setColor(c1);
		g.drawLine(x,py1,x,py2-1);
		g.setColor(c2);
		g.drawLine(x,py2,x,py2+1);
	    }else{
		g.drawLine(x,py1,x,py2-1);
	    }
	}

	if(playWave==false){
	    g.setComposite(AlphaComposite.SrcOver);
	}

	return pixelPos;
    }


    public void paintWave_quite_old(Graphics2D g,Color c1, Color c2,int x1,int y1,int x2,int y2,
			  Color nonPlayColor,int startPlay,int endPlay,AlphaComposite alphaComposite
			  ){
	int width=x2-x1-1;

	byte[][] peaks=peaksPeaks.get(0);
	for(int i=1;i<peaksPeaks.size();i++){
	    byte[][] newpeaks=peaksPeaks.get(i);
	    if(newpeaks[0].length*2 < width*3)
		break;
	    peaks=newpeaks;
	}

	int nPeaks=peaks[0].length;
	float peaksPerPixel=(nPeaks-2)/(float)width;
	float pixelPos=0.0f;

	//System.out.println("///////// nPeaks:"+nPeaks+",width: "+width+", peaksPerPixel:"+peaksPerPixel);

	pixelPos=paintWave(g,nonPlayColor,c2,x1,y1,startPlay,y2,false,
			   pixelPos,peaksPerPixel,peaks,alphaComposite);
	pixelPos=paintWave(g,c1,c2,startPlay,y1,endPlay-1,y2,true,
			   pixelPos,peaksPerPixel,peaks,alphaComposite);
	pixelPos=paintWave(g,nonPlayColor,c2,endPlay-1,y1,x2-1,y2,false,
			   pixelPos,peaksPerPixel,peaks,alphaComposite);

    }
    */


    private static int[] xPoly=new int[1024];
    private static int[] yPoly=new int[1024];


    // Note that nPeaks is relatively small, and the polygon shouldn't take much time to calculate. Therefore, I don't
    // think there's anything to gain by precalculating the polygon. (it will use memory too). (blitting takes a lot more time than painting the waveform anyway)
    public void paintWave(Graphics2D g,Color c1, Color c2,int x1,int y1,int x2,int y2,
			  int startPaint,int endPaint,int quality, boolean paintShade,boolean backwards,float vol
			  ){


	if(peaksPeaks.size()==0)
	    return;

	int width=x2-x1-1;

	byte[][] peaks=peaksPeaks.get(0);
	for(int i=1;i<peaksPeaks.size();i++){
	    byte[][] newpeaks=peaksPeaks.get(i);
	    if(newpeaks[0].length*quality < width)
		break;
	    peaks=newpeaks;
	}


	double pixelsPerPeak=((float)width+2)/((float)peaks[0].length);

	int startPeak;
	int endPeak;
	if(backwards){
	    int d=endPaint-startPaint;
	    int s=startPaint-x1;
	    startPeak=(int)scale(x2-d-s      ,x1,x2, 0,peaks[0].length);
	    endPeak  =(int)scale(x2-s        ,x1,x2, 0,peaks[0].length);
	}else{
	    startPeak=(int)scale(startPaint,x1,x2,0,peaks[0].length);
	    endPeak=(int)scale(endPaint,x1,x2,0,peaks[0].length);
	}
	int nPeaks=endPeak-startPeak;

	if(endPeak>peaks[0].length)
	    endPeak=peaks[0].length;

	nPeaks=endPeak-startPeak;
	if(nPeaks<=0)
	    return;


	{
	    byte[] minp=peaks[0];
	    byte[] maxp=peaks[1];
	    
	    while(nPeaks*2 > xPoly.length){
		xPoly=new int[xPoly.length*2];
		yPoly=new int[yPoly.length*2];
	    }

	    double x;
	    if(backwards)
		x=scale(endPeak,0,peaks[0].length,x2,x1);
	    else
		x=scale(startPeak,0,peaks[0].length,x1,x2);

	    //System.out.println("wavegen length:"+xs.length+", "+minp.length+", width: "+width+", peaksperpixel:"+peaksPerPixel);
	    int i=0;
	    try{
		if(backwards){
		    for(i=0;i<nPeaks;i++){
			xPoly[i]=(int)x;
			xPoly[nPeaks*2-i-1]=xPoly[i];
			x+=pixelsPerPeak;
			
			yPoly[i]=(int)scale(minp[endPeak-i-1]*vol,-128,127,y1,y2);
			yPoly[nPeaks*2-i-1]=(int)scale(maxp[endPeak-i-1]*vol,-128,127,y1,y2);
		    }
		}else{
		    for(i=0;i<nPeaks;i++){
			xPoly[i]=(int)x;
			xPoly[nPeaks*2-i-1]=xPoly[i];
			x+=pixelsPerPeak;
			
			yPoly[i]=(int)scale(minp[i+startPeak]*vol,-128,127,y1,y2);
			yPoly[nPeaks*2-i-1]=(int)scale(maxp[i+startPeak]*vol,-128,127,y1,y2);
		    }
		}
	    }catch(Exception e){
		e.printStackTrace();
		System.out.println("i:"+i+", x:"+x);
	    }

	    // Make sure it starts and ends at the right places
	    xPoly[0]=startPaint;
	    xPoly[nPeaks*2-1]=startPaint;

	    // The y values must be scaled as well.
	    /*
	    if(nPeaks>=2){
		yPoly[nPeaks-1]=(int)scale(endPaint,xPoly[nPeaks-2],xPoly[nPeaks-1],yPoly[nPeaks-2],yPoly[nPeaks-1]);
		yPoly[nPeaks]=(int)scale(endPaint,xPoly[nPeaks+1],xPoly[nPeaks],yPoly[nPeaks+1],yPoly[nPeaks]);
	    }
	    */

	    xPoly[nPeaks-1]=endPaint;
	    xPoly[nPeaks]=endPaint;


	    g.setColor(c1);
	    g.fillPolygon(xPoly,yPoly,nPeaks*2);

	    if(paintShade){
		g.setColor(c2);
		g.drawPolyline(xPoly,yPoly,nPeaks*2);
	    }
	}


	/*
	for(int x=x1;x<x2-1;x++){
	    g.setColor(c1);
	    
	    g.drawLine(x,py1,x,py2-1);
	    g.setColor(c2);
	    g.drawLine(x,py2,x,py2+1);
	}
	*/
    }

    /*
    public void paintWave_old(Graphics g,int x1,int y1,int x2,int y2){
	int width=x2-x1-1;
	float peaksPerPixel=nPeaks/(float)width;
	float pixelPos=0.0f;
	
	for(int x=x1;x<x2-1;x++){
	    byte min=findMin((int)pixelPos,(int)(pixelPos+peaksPerPixel));
	    byte max=findMax((int)pixelPos,(int)(pixelPos+peaksPerPixel));
	    pixelPos+=peaksPerPixel;
	    g.drawLine(x,(int)scale(min,127,-128,y1,y2),
		       x,(int)scale(max,127,-128,y1,y2));
	}
    }
    */

    private void init(int framesPerPixel){
	this.framesPerPixel=framesPerPixel;
	minmax_temp=new short[2];
	minmax_temp[0]=32767;
	minmax_temp[1]=-32768;
    }

    public Peaks(int framesPerPixel){
	init(framesPerPixel);
    }


    public Peaks(){
	init(512);
    }
}

