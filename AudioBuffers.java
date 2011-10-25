
import java.util.*;



class AudioBuffers{

    LinkedList<float[][]> buffers=new LinkedList<float[][]>();
    public byte[] byteTempBuf;
    private float[] dasClearBuffer;
    public int bufferSize=0;

    public float[][] getBuffer(){
	float[][] ret;
	if(buffers.size()>0)
	    ret=buffers.removeFirst();
	else
	    ret=new float[2][bufferSize];
	//clearBuffer(ret,bufferSize);
	return ret;
    }
    public void returnBuffer(float[][] buf){
	buffers.addFirst(buf);
    }


    public void copyBuffer(float[] from, float[] to,int nFrames){
	System.arraycopy(from,0,to,0,nFrames);
    }

    public void copyBuffer(float[] from, int fromPos, float[] to, int toPos, int nFrames){
	System.arraycopy(from,fromPos,to,toPos,nFrames);
    }

    public void copyBuffer(float[][] from, float[][] to, int nFrames){
	System.arraycopy(from[0],0,to[0],0,nFrames);
	System.arraycopy(from[1],0,to[1],0,nFrames);
    }

    public void copyBuffer(float[][] from, int fromPos, float[][] to, int toPos, int nFrames){
	System.arraycopy(from[0],fromPos,to[0],toPos,nFrames);
	System.arraycopy(from[1],fromPos,to[1],toPos,nFrames);
    }

    public void clearBuffer(float[][] buf,int nFrames){
	System.arraycopy(dasClearBuffer,0,buf[0],0,nFrames);
	System.arraycopy(dasClearBuffer,0,buf[1],0,nFrames);
    }

    public void clearBuffer(float[] buf,int nFrames){
	System.arraycopy(dasClearBuffer,0,buf,0,nFrames);
    }

    public void clearBuffer(float[] buf,int startPos,int nFrames){
	System.arraycopy(dasClearBuffer,0,buf,startPos,nFrames);
    }

    public void clearBuffer(float[][] buf,int startPos,int nFrames){
	System.arraycopy(dasClearBuffer,0,buf[0],startPos,nFrames);
	System.arraycopy(dasClearBuffer,0,buf[1],startPos,nFrames);
    }


    public void reverseBuffer(float[] buf, int nFrames){
	for(int i=0;i<nFrames/2;i++){
	    float temp=buf[i];
	    buf[i]=buf[nFrames-i-1];
	    buf[nFrames-i-1]=temp;
	}
    }

    public void reverseBuffer(float[][] buf, int nFrames){
	reverseBuffer(buf[0],nFrames);
	reverseBuffer(buf[1],nFrames);
    }

    public void mulBuffer(float[] buf,int nFrames,float vol){
	for(int i=0;i<nFrames;i++){
	    buf[i]*=vol;
	}
    }

    public void mulBuffer(float[] toBuf,float[] volBuf,int nFrames){
	for(int i=0;i<nFrames;i++){
	    toBuf[i]*=volBuf[i];
	}
    }

    public void mixBuffers(float[][] toBuf,float [][] fromBuf,int nFrames){
	float buf00[]=fromBuf[0];
	float buf01[]=fromBuf[1];
	float buf10[]=toBuf[0];
	float buf11[]=toBuf[1];

	for(int i=0;i<nFrames;i++){
	    buf10[i]+=buf00[i];
	}
	for(int i=0;i<nFrames;i++){
	    buf11[i]+=buf01[i];
	}
    }

    public void mixBuffers(float[][] toBuf,float [][] fromBuf,int startToFrame,int nFrames){
	float buf00[]=fromBuf[0];
	float buf01[]=fromBuf[1];
	float buf10[]=toBuf[0];
	float buf11[]=toBuf[1];

	for(int i=0;i<nFrames;i++){
	    buf10[i+startToFrame]+=buf00[i];
	}
	for(int i=0;i<nFrames;i++){
	    buf11[i+startToFrame]+=buf01[i];
	}
    }

    public void mixBuffers(float[] toBuf,float [] fromBuf,int nFrames,float fromBufVol){
	for(int i=0;i<nFrames;i++){
	    toBuf[i]+=fromBuf[i]*fromBufVol;
	}
    }

    public void mixBuffers(float[][] toBuf,float [][] fromBuf,int nFrames,float fromBufVol){
	mixBuffers(toBuf[0],fromBuf[0],nFrames,fromBufVol);
	mixBuffers(toBuf[1],fromBuf[1],nFrames,fromBufVol);
    }

    public float getMin(float[][] buf,int nFrames){
	float newMin=0.0f;
	for(int ch=0;ch<2;ch++){
	    float[] fromBuf=buf[ch];
	    for(int i=0;i<nFrames;i++){
		float val=fromBuf[i];
		if(val < newMin)
		    newMin=val;
	    }
	}
	return newMin;
    }

    public float getMax(float[][] buf,int nFrames){
	float newMax=0.0f;
	for(int ch=0;ch<2;ch++){
	    float[] fromBuf=buf[ch];
	    for(int i=0;i<nFrames;i++){
		float val=fromBuf[i];
		if(val > newMax)
		    newMax=val;
	    }
	}
	return newMax;
    }



    private static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }


    AudioBuffers(int bufferSize){
	this.bufferSize=bufferSize;
	byteTempBuf=new byte[bufferSize*2];

	dasClearBuffer=new float[bufferSize];
	for(int i=0;i<bufferSize;i++){
	    dasClearBuffer[i]=0.0f;
	}

    }
    
}

