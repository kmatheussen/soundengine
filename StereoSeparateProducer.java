



// Note that this one is not a SoundProducer subclass. (That's not an error, but perhaps the class should have a different name)


public class StereoSeparateProducer{

    public RingMonoBuffer[] ringMonoBuffers=new RingMonoBuffer[2];


    public boolean produceMonoSound(float[] buf,int channel,int nFrames,SoundProducer stereoProducer,AudioBuffers audioBuffers){
	boolean ret=true;
	
	if(ringMonoBuffers[channel].getCurrSize()<nFrames){
	    float[][] stereoBuf=audioBuffers.getBuffer();
	    ret=stereoProducer.produceSound(stereoBuf,nFrames,audioBuffers);
	    ringMonoBuffers[0].push(stereoBuf[0],nFrames);
	    ringMonoBuffers[1].push(stereoBuf[1],nFrames);
	    audioBuffers.returnBuffer(stereoBuf);
	}
	
	ringMonoBuffers[channel].get(buf,nFrames);

	return ret;
    }

    public StereoSeparateProducer(AudioBuffers audioBuffers){
	ringMonoBuffers[0]=new RingMonoBuffer(audioBuffers.bufferSize*2);
	ringMonoBuffers[1]=new RingMonoBuffer(audioBuffers.bufferSize*2);
    }
    
}

