
// For synchronizing two producers which have different block frames (i.e. different nFrames)

import java.lang.*;


public class DivideStereoProducer{

    public SoundProducer sourceProducer;

    public int maxNFrames;

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	if(nFrames<=maxNFrames){
	    return sourceProducer.produceSound(buf,nFrames,audioBuffers);
	}

	float[][] tempBuf = audioBuffers.getBuffer();
	int framesLeft    = nFrames;
	int pos           = 0;

	while(framesLeft>0){
	    int newNFrames = java.lang.Math.min(framesLeft,maxNFrames);

	    framesLeft -= newNFrames;

	    boolean ret = sourceProducer.produceSound(tempBuf,newNFrames,audioBuffers);
	    audioBuffers.copyBuffer(tempBuf,0,buf,pos,newNFrames);

	    if(ret==false){
		if(framesLeft>0){
		    audioBuffers.clearBuffer(buf,pos,framesLeft);
		}
		audioBuffers.returnBuffer(tempBuf);
		return false;
	    }
	    
	    pos+=newNFrames;
	}

	audioBuffers.returnBuffer(tempBuf);
	return true;
    }

    public DivideStereoProducer(SoundProducer sourceProducer, int maxNFrames){
	this.sourceProducer=sourceProducer;
	this.maxNFrames=maxNFrames;
    }
}

