
// This is not pretty, but that's what happens when a language doesn't support higher order functions
// (even C does that to a certain degree), and one has to resort to OO programming.


class StereoResampleProducerInternal extends SoundProducer{

    public SoundProducer sourceProducer;
    public StereoSeparateProducer stereoSeparateProducer;
    public ResampleProducer converter0;
    public ResampleProducer converter1;
    public DivideStereoProducer divideStereoProducer;
    //public StereoResampleProducer parent;
    
    int currChannel;
    public double srate=1.0;

    public boolean produceMonoSound(float[] buf,int nFrames,AudioBuffers audioBuffers){
	return stereoSeparateProducer.produceMonoSound(buf,currChannel,nFrames,sourceProducer,audioBuffers);
    }

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	if(srate==1.0)
	    return sourceProducer.produceSound(buf,nFrames,audioBuffers);

	//System.out.println("nFrames: "+nFrames);

	currChannel=0;
	converter0.produceMonoSound(buf[0],nFrames,audioBuffers);

	currChannel=1;
	return converter1.produceMonoSound(buf[1],nFrames,audioBuffers);
    }

    public void setSRate_now(double srate){
	this.srate=srate;
	converter0.setSRate_now(srate);
	converter1.setSRate_now(srate);	
    }

    public void setSRate(double srate){
	this.srate=srate;
	converter0.setSRate(srate);
	converter1.setSRate(srate);
    }

    public void reset(){
	converter0.reset();
	converter1.reset();
    }

    public boolean setPlayPos(int newPos){
	reset();
	return sourceProducer.setPlayPos((int)(newPos*srate));
    }

    public StereoResampleProducerInternal(SoundProducer sourceProducer, int sincWidth, AudioBuffers audioBuffers){
	//this.parent=parent;
	this.sourceProducer=sourceProducer;
	converter0=new ResampleProducer(this,sincWidth,audioBuffers);
	converter1=new ResampleProducer(this,sincWidth,audioBuffers);

	stereoSeparateProducer=new StereoSeparateProducer(audioBuffers);
    }
}



public class StereoResampleProducer extends SoundProducer{
    public DivideStereoProducer divideStereoProducer;
    public StereoResampleProducerInternal internal;

    // Only for reading.
    public double srate=1.0;

    public void setSRate_now(double srate){
	this.srate=srate;
	internal.setSRate_now(srate);
    }

    public void setSRate(double srate){
	this.srate=srate;
	internal.setSRate(srate);
    }

    public void setForceSincResampler(boolean val){
	internal.converter0.setForceSincResampler(val);
	internal.converter1.setForceSincResampler(val);
    }

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){

	if(srate<=1.0)
	    return internal.produceSound(buf,nFrames,audioBuffers);


	// Why are we doing the things below? Good question, and there is a perfectionably rational answer to it.

	divideStereoProducer.maxNFrames=nFrames/2;
	while(divideStereoProducer.maxNFrames >= (int)(nFrames-2)/srate)
	    divideStereoProducer.maxNFrames/=2;

	return divideStereoProducer.produceSound(buf,nFrames,audioBuffers);
    }

    public void reset(){
	internal.reset();
    }

    public boolean setPlayPos(int newPos){
	return internal.setPlayPos(newPos);
    }

    public StereoResampleProducer(SoundProducer sourceProducer, int sincWidth, AudioBuffers audioBuffers){
	internal=new StereoResampleProducerInternal(sourceProducer,sincWidth,audioBuffers);
	divideStereoProducer=new DivideStereoProducer(internal,audioBuffers.bufferSize);
    }
}


