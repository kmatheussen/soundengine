
class ReverbBus extends Bus{

    public FreeVerb freeVerb;

    public void read(float[][] buf,int nFrames,float vol){
	freeVerb.compute(nFrames,buffer,buffer);
	mix(buf,nFrames,vol);
	clear(nFrames);
    }

    public void reset(){
	freeVerb.reset();
    }

    public ReverbBus(int channels,AudioBuffers audioBuffers,float sampleRate){
	super(channels,audioBuffers);
	freeVerb = new FreeVerb((int)sampleRate);
	freeVerb.setWet(1.0f);
    }
}

