
import java.util.*;
import java.awt.geom.*;

public class Envelope implements BreakPoints{

    // breaks[a][b]. a=0: X value. a=1: Y value.
    //float[][] breaks;
    int breakLen;
    float[] breakPos;
    float[] breakVol;

    public static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    public void printBreaks(){
        for(int i=0;i<breakLen;i++){
            System.out.print("("+breakPos[i]+","+breakVol[i]+") ");
        }
        System.out.println();
    }

    public void resetBreaks(){
	int breakLen=2;
	float[] breakPos=new float[breakLen];
	float[] breakVol=new float[breakLen];
	breakPos[0]=0.0f;
	breakPos[1]=1.0f;
	breakVol[0]=1.0f;
	breakVol[1]=1.0f;

	synchronized(this){
	    this.breakLen=breakLen;
	    this.breakPos=breakPos;
	    this.breakVol=breakVol;
	}
    }

    // 'breakList' list of Point2D elements.
    // The x values are scaled to 0 - 1 after reading.
    public void setBreaks(LinkedList breakList){
	if(breakList==null){
	    breakList=new LinkedList();
	}

	int breakLen=breakList.toArray().length;

	if(breakLen<2){
	    
	    resetBreaks();

	}else{

	    float[] breakPos=new float[breakLen];
	    float[] breakVol=new float[breakLen];
	    
	    Iterator itr = breakList.iterator(); 
	    int i=0;

	    while(itr.hasNext()) {
		Point2D point = (Point2D)itr.next(); 
		breakPos[i]=(float)point.getX();
		breakVol[i]=(float)point.getY();
		i++;
	    }
	    
	    // scale x values to 0-1
	    {
		float minX=breakPos[0];
		float maxX=breakPos[breakLen-1];
		
		for(i=0;i<breakLen;i++){
		    breakPos[i]=scale(breakPos[i],minX,maxX,0.0f,1.0f);
		}
	    } 

	    synchronized(this){
		this.breakLen=breakLen;
		this.breakPos=breakPos;
		this.breakVol=breakVol;
	    }
	}

        printBreaks();
    }

    
    synchronized void getEnvelopeData(float[] buf,int start,int end,int envelopeLength){
	int len=end-start;
	int nextBreak=1;

	double scaledI=scale(start,0,envelopeLength,0.0f,1.0f);
	double scaledInc=1.0/(double)envelopeLength;

        while(scaledI >= breakPos[nextBreak])
            nextBreak++;
	float prevX=breakPos[nextBreak-1];
	float nextX=breakPos[nextBreak];
	float prevY=breakVol[nextBreak-1];
	float nextY=breakVol[nextBreak];


        /*
	//System.out.println("scaledI: "+scaledI+", scaledInc: "+scaledInc+", breaks[0].length: "+breaks[0].length);
	//System.out.println("breaks[0][1]: "+breaks[0][1]);
        System.out.println("scaledI/start/end/length: "+scaledI+", "+start+", "+end+", "+envelopeLength);
        printBreaks();
        System.out.println("nextBreak: "+nextBreak+". "+breaks[1][nextBreak-1]+"->"+breaks[1][nextBreak]);
        */

	for(int i=0;i<len;i++){

	    if(scaledI >= nextX){
                while(scaledI >= breakPos[nextBreak])
                    nextBreak++;
                //System.out.println("scaledI: "+scaledI);
		prevX=breakPos[nextBreak-1];
		nextX=breakPos[nextBreak];
		prevY=breakVol[nextBreak-1];
		nextY=breakVol[nextBreak];
	    }
            //scaledI=scale(start+i,0,envelopeLength,0.0f,1.0f);
            //System.out.println(breaks[1][0]+", "+breaks[1][1]+", "+scale((float)scaledI,prevX,nextX,prevY,nextY));
	    buf[i]=scale((float)scaledI,prevX,nextX,prevY,nextY);
	    
	    scaledI+=scaledInc;
	}
    }

    public void apply(float buf[],int start,int end,int envelopeLength,AudioBuffers audioBuffers){
	int len=end-start;
	float[][] bufs=audioBuffers.getBuffer();
	float[] envData=bufs[0];

	getEnvelopeData(envData,start,end,envelopeLength);

	for(int i=0;i<len;i++)
	    buf[i]*=envData[i];

	audioBuffers.returnBuffer(bufs);
    }

    public Envelope(){
	resetBreaks();
    }

}



