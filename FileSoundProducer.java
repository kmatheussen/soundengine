/*

class OverlappingBuffersProcess{
    void apply(float[] buf1, float[] buf2, int nFrames){
	System.out.println("I am not supposed to be called.");
    }
}


class FadeIn extends OverlappingBuffersProcess{
    void apply(float[] buf1, float[] buf2, int nFrames){
    }
}

class FadeOut extends OverlappingBuffersProcess{
    void apply(float[] buf1, float[] buf2, int nFrames){
    }
}



class OverlappingBuffers extends SoundProducer{
    public int startPos1;
    public int endPos1;

    public int startPos2;
    public int endPos2;

    OverlappingBuffersProcess olbp;

    public boolean produceMonoSound(float[] buf,int nFrames,AudioBuffers audioBuffers){
    }

    public OverlappingBuffers(OverLappingBuffersProcess olbp){
	this.olbp=olbp;
    }

}

*/


public class FileSoundProducer extends SoundProducer{
    public MyAudioFileBuffer fileReader;
    int fileChannel;
    public boolean reverse=false;

    public int startRead=0;
    public int endRead=0;

    public int readPos=0;

    public int fadeInLen=0;
    public int fadeOutLen=0;

    public Envelope envelope=null;
    public Pan12 panner=null;

    public int getNumPlayFrames(){
	return (int)fileReader.nFrames-startRead-endRead;
    }

    public int getStartFrame(){
	return (int)startRead; 
   }

    public int getEndFrame(){
	return (int)fileReader.nFrames-endRead;
    }

    /*
    public void setStartFrame(int newStartFrame){
	startFrame=newStartFrame;
    }
    */

    public static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    public boolean setPlayPos(int newPlayPos){

	// Note that startFrame is set to startRead by the SoundObject. (hmm, thats confusing)
	if( (newPlayPos < startFrame+getNumPlayFrames())
	    && newPlayPos >= startFrame
	    )
	    {
		readPos=newPlayPos-startFrame + getStartFrame();
		//System.out.println("setPlayPos returned true");
		return true;
	    }

	readPos=getStartFrame();
	//System.out.println("setPlayPos returned false");
	return false;
    }



    public void readFromFile(float[] buf,int nFrames, AudioBuffers audioBuffers){
	
	if(reverse){
	    int fileFrames=(int)fileReader.nFrames;
	    fileReader.putBuf(buf,audioBuffers.byteTempBuf,fileChannel,fileFrames-readPos-nFrames,nFrames,true);
	}else
	    fileReader.putBuf(buf,audioBuffers.byteTempBuf,fileChannel,readPos,nFrames,false);

    }

    float[] rightChannelBuf;

    public boolean produceMonoSound(float[] buf,int nFrames,AudioBuffers audioBuffers){

	int framesToRead=nFrames;

	int numPlayFrames=getNumPlayFrames();

	if(readPos+framesToRead > getEndFrame()){
	    framesToRead=getEndFrame()-readPos;
	    audioBuffers.clearBuffer(buf,framesToRead,nFrames-framesToRead);
	}

	readFromFile(buf,framesToRead,audioBuffers);


	// fade-in
	{
	    int bufStartPos=readPos-getStartFrame();
	    if( bufStartPos < fadeInLen){
		for(int i=0 ; i<framesToRead; i++){
		    if( i+bufStartPos < fadeInLen) {
			buf[i] *= (float)((float)(i+bufStartPos) / (float)fadeInLen);
			//buf[i]*=scale( i+readPos , 0, fadeInLen, 0.0f, 1.0f);
		    }
		}
	    }
	}

	// Envelope.
	if(envelope!=null){
	    envelope.apply(buf,readPos,readPos+framesToRead,getEndFrame(),audioBuffers);
	}

	// fade-out
	{
	    int endFrame=getEndFrame();
	    int fadeOutStart=endFrame - fadeOutLen;
	    if( readPos+framesToRead >= fadeOutStart){
		for(int i=0 ; i<framesToRead; i++){
		    if( i+readPos >= fadeOutStart) {
			buf[i]*=scale( i+readPos , fadeOutStart, endFrame, 1.0f, 0.0f);
		    }
		}
	    }
	}

        // Panner
        if(panner!=null){
            float[][] stereo=new float[2][];
            stereo[0]=buf;
            stereo[1]=rightChannelBuf;
            audioBuffers.copyBuffer(stereo[0],stereo[1],nFrames);            
	    panner.apply(stereo,readPos,readPos+framesToRead,getEndFrame(),audioBuffers);
        }

	readPos+=framesToRead;
	if(readPos==getEndFrame()){
	    readPos=0;
	    return false;
	}

	return true;
    }


    // Stereo panned
    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
        if(panner!=null){
            rightChannelBuf=buf[1];
            return produceMonoSound(buf[0],nFrames,audioBuffers);
        }else{
            boolean ret=produceMonoSound(buf[0],nFrames,audioBuffers);
            audioBuffers.copyBuffer(buf[0],buf[1],nFrames);
            return ret;
        }
    }

    public FileSoundProducer(MyAudioFileBuffer fileReader,int fileChannel){
	this.fileReader=fileReader;
	this.fileChannel=fileChannel;
    }

}



