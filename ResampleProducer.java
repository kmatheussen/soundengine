




public class ResampleProducer extends SoundProducer{

    GlideVar srate=new GlideVar(1.0f,0.1f);
    private double srate_raw=1.0;

    float prevVal;
    double currReadPos;
    float nextVal;

    float buf[]=null;
    int bufPos;
    int bufLen;

    SoundProducer sourceProducer;
    AudioBuffers audioBuffers;

    MySincSrc mySincSrc=null;
    int srcWidth;

    boolean lastResult1;
    boolean lastResult2;

    // Turn this variable on/off to immeadiately hear the difference between SINC and linear interpolation.
    public static boolean forceLinearInterpolation=false;

    // If true, it will override the value of forceLinearInterpolation (forceLinearInterpolation is static!)
    private boolean forceSincResampler=false;

    private static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    private static float scale(double x,double x1,double x2,double y1,double y2){
	return (float)(y1+( ((x-x1)*(y2-y1))/(x2-x1)));
    }

    public void setForceSincResampler(boolean val){
	forceSincResampler=val;
    }

    public void fillBuf(){
	//fillBufObject.fillBuf(fillBufPos,buf,bufLen,audioBuffers);
	if(lastResult1==false){
	    audioBuffers.clearBuffer(buf,bufLen);
	    lastResult2=false;
	}else{
	    lastResult1=sourceProducer.produceMonoSound(buf,bufLen,audioBuffers);
	}
    }


    public float getNextSample(){
	if(bufPos==bufLen){
	    fillBuf();
	    bufPos=0;
	}
	float ret=buf[bufPos];
	bufPos++;
	return ret;
    }

    private float getLinearlyInterpolatedSample(){
	while(currReadPos > 1.0){
	    currReadPos-=1.0;
	    prevVal=nextVal;
	    nextVal=getNextSample();
	}

	//float ret=scale( currReadPos, 0, 1, prevVal, nextVal);
	// is probably a slower way to do:
	float ret=prevVal + ((float)currReadPos * (nextVal-prevVal));

	currReadPos+=srate_raw;

	return ret;
    }

    public boolean produceResampled(float[] buf,int nFrames){
	if( false
	    || (mySincSrc!=null && forceLinearInterpolation==false)
	    || (mySincSrc!=null && forceSincResampler==true)
	    ){
	    float srate=(float)srate_raw;
	    for(int i=0;i<nFrames;i++){
		buf[i]=mySincSrc.getOutSample(srate);
	    }
	}else
	    for(int i=0;i<nFrames;i++){
		buf[i]=getLinearlyInterpolatedSample();
	    }
	return lastResult2;
    }

    public boolean produceNonResampled(float[] buf,int nFrames){
	if(bufPos==bufLen)
	    return sourceProducer.produceMonoSound(buf,nFrames,audioBuffers);

	for(int i=0;;i++){

	    buf[i]=getNextSample();

	    if(bufPos==bufLen && i<nFrames){
		float[][] tempBuf=audioBuffers.getBuffer();
		boolean ret=sourceProducer.produceMonoSound(tempBuf[0],nFrames-i,audioBuffers);
		audioBuffers.copyBuffer(tempBuf[0],0,buf,i,nFrames-i);
		audioBuffers.returnBuffer(tempBuf);
		return ret;
	    }

	    if(i==nFrames)
		return lastResult1;

	}
    }

    public boolean produceMonoSound(float[] buf,int nFrames,AudioBuffers audioBuffers){
	this.audioBuffers=audioBuffers;
	srate_raw=srate.get();

	if(srate_raw==1.0){
	    return produceNonResampled(buf,nFrames);
	}

	return produceResampled(buf,nFrames);
    }


    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	System.out.println("Error. ResampleProducer.produceSound() is not supposed to be called. You must use StereoResampleProducer instead.");
	return false;
    }

    public synchronized void setSRate_now(double srate){
	this.srate.set_now((float)srate);
	setSRate(srate);
    }

    public synchronized void setSRate(double srate){
	this.srate.set((float)srate);
	if(srate!=1.0 && buf==null){
	    buf=new float[bufLen];
	}
    }


    public void reset(){
	bufPos=bufLen;

	prevVal=0.0f;
	nextVal=0.0f;

	currReadPos=3.0;

	lastResult1=true;
	lastResult2=true;

	if(mySincSrc!=null)
	    mySincSrc.reset();
    }

    public boolean setPlayPos(int newPos){

	reset();

	int start=sourceProducer.startFrame;

	if(newPos <= start)
	    return sourceProducer.setPlayPos(newPos);
	
	return sourceProducer.setPlayPos(start + (int)((newPos - start)*srate_raw));
    }

    class MySincSrc extends SincSrc{
	public float getInSample(int direction){
	    return getNextSample();
	}
	public MySincSrc(int width){
	    super(0.0f,width);
	}
    }

    // srcWidth==0: linear resampling;
    public ResampleProducer(SoundProducer sourceProducer,int srcWidth,AudioBuffers audioBuffers){
	this.sourceProducer=sourceProducer;
	this.srcWidth=srcWidth;
	if(srcWidth>0)
	    mySincSrc=new MySincSrc(srcWidth);
	bufLen=audioBuffers.bufferSize;
	reset();
    }

}
