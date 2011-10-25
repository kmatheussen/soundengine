
import java.util.*;

import javax.sound.sampled.*;
import java.io.*;
import jass.render.*;
import jass.generators.*;


// Note that a lot of the variables below are public only for debugging purposes.


public class SoundObject extends SoundProducer{

    final private int reqtype_setPos=0;
    final private int reqtype_setStartReadPos=1;
    final private int reqtype_setEndReadPos=2;
    final private int reqtype_setPosAndPan=3;
    final private int reqtype_setSRate=4;
    final private int reqtype_addEnvelope=5;
    final private int reqtype_removeEnvelope=6;

    private int sincWidth = 10;

    private DasMixer mixer;

    public FileSoundProducer fileSoundProducers[];
    
    /*
    ArrayList<Envelope> envelopes=new ArrayList<Envelope>();

    public Envelope fadeIn=null;
    public Envelope fadeOut=null;
    */

    Envelope envelope=null;
    Pan12 panner=null;

    ResampleProducer resampleProducers[];

    public float pan=0.0f;
    private float[][] panvals;
    public float vol=1.0f;
    public double srate_change=1.0;
    private final int channels=2;
    private int fileChannels; // = min(2,fileBuffer.nChannels)
    
    public boolean reverse=false;

    public boolean forDSP=false;


    /*
    public int getStart_inSamples(){
	return x0;
    }
    public int getStartPlay_inSamples(){
	return x1;
    }
    public int getEndPlay_inSamples(){
	return x2;
    }
    */


    public void handleMessage(Message message){
	switch(message.something_int){
	case reqtype_setPos:
	    setPos(message.something_float);
	    break;
	case reqtype_setSRate:
	    setSRate(message.something_float);
	    break;
	case reqtype_setStartReadPos:
	    setStartReadPos(message.something_float);
	    break;
	case reqtype_setEndReadPos:
	    setEndReadPos(message.something_float);
	    break;
	    /*
	      case reqtype_addEnvelope:
	      envelopes.add((Envelope)message.something_Object);
	      break;
	      case reqtype_removeEnvelope:
	      envelopes.remove((Envelope)message.something_Object);
	      break;
	    */
	}
	
    }
    

    public static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    private final float sqrt2=(float)java.lang.Math.sqrt(2.0f);
    private final float pan_scaleval=2.0f-(2*sqrt2);

    // The pan parameter goes from -1 to 1. 
    // For stereo files, at least one of the channels are allways either -1 or 1. Ie. both channels are allways heard.
    public void setPan(float pan){
	this.pan=pan;

	if(fileChannels==1){
	    float x=scale(pan,-1,1,0,1);
	    panvals[0][0] = ((1.0f-x)*((pan_scaleval*(1.0f-x))+(1.0f-pan_scaleval)));
	    panvals[0][1] = x * ( (pan_scaleval*x) + (1.0f-pan_scaleval));
	}else{
	    if(pan<=0){
		panvals[0][0] = 1.0f;
		panvals[0][1] = 0.0f;

		pan=scale(pan,-1,0,-1,1);
		float x=scale(pan,-1,1,0,1);
		panvals[1][0] = ((1.0f-x)*((pan_scaleval*(1.0f-x))+(1.0f-pan_scaleval)));
		panvals[1][1] = x * ( (pan_scaleval*x) + (1.0f-pan_scaleval));
		//System.out.println("l/r/pan "+panvals[1][0]+" "+panvals[1][1]+" "+pan);
	    }else{
		pan=scale(pan,0,1,-1,1);
		float x=scale(pan,-1,1,0,1);
		panvals[0][0] = ((1.0f-x)*((pan_scaleval*(1.0f-x))+(1.0f-pan_scaleval)));
		panvals[0][1] = x * ( (pan_scaleval*x) + (1.0f-pan_scaleval));

		panvals[1][0] = 0.0f;
		panvals[1][1] = 1.0f;
		//System.out.println("l/r/pan "+panvals[0][0]+" "+panvals[0][1]+" "+pan);
	    }
	}

    }

    public float getPan(){
	return pan;
    }


    private void setPos(float newx1){
	//System.out.println("SoundObject.setPos "+newx1);
	mixer.scheduler.move(this,(int)(newx1*mixer.srate));

	/*
	for(int i=0;i<envelopes.size();i++){
	    Envelope envelope=envelopes.get(i);
	    envelope.move(dx);
	}
	*/
    }

    public void requestTo_setPos(float newx1){
	if(mixer.isPlayerRunningQuestionmark()==false){
	    setPos(newx1);
	}else{
	    Message message=mixer.getMessage();
	    message.something_int=reqtype_setPos;
	    message.something_float=newx1;
	    mixer.requestTo_addSoundObjectMessage(this,message);
	}
    }

    public void requestTo_setPosAndPan(float newx1,float newpan){
	//System.out.println("new time/pan: "+newx1+"/"+newpan);
	requestTo_setPos(newx1);
	setPan(newpan);
    }


    private void setSRate(float newSRate){
	srate_change=newSRate;
	for(int i=0;i<fileChannels;i++){
	    resampleProducers[i].setSRate_now(srate_change);
	}
    }

    public void requestTo_setSRate(float newSRate){
	srate_change=newSRate;

	Message message=mixer.getMessage();
	message.something_int=reqtype_setSRate;
	message.something_float=newSRate;
	mixer.requestTo_addSoundObjectMessage(this,message);
    }

    // newpos=relative to start of file.
    private void setStartReadPos(float newPos){
	int newPosInFrames=(int)(newPos*(float)mixer.srate * srate_change);

	int absStartFrame=startFrame-fileSoundProducers[0].startRead;

	for(int i=0;i<fileChannels;i++){
	    fileSoundProducers[i].startRead=newPosInFrames;
	}

	//System.out.println("NewPlace: "+(absStartFrame+newPosInFrames)/44100.0);
	mixer.scheduler.move(this, absStartFrame + newPosInFrames);

	/*
	if(fadeIn!=null)
	    fadeIn.move(x1-old_x1);
	*/

	//System.out.println("ssrp "+start_org+" "+x1);
    }

    public void requestTo_setStartReadPos(float newpos){
	Message message=mixer.getMessage();
	message.something_int=reqtype_setStartReadPos;
	message.something_float=newpos;
	mixer.requestTo_addSoundObjectMessage(this,message);
    }

    // das_newpos=relative to start of file.
    private void setEndReadPos(float newPos){

	int newPosInFrames=(int)(newPos*(float)mixer.srate * srate_change);

	if(newPosInFrames < 0)
	    newPosInFrames = 0;
	if(newPosInFrames >= fileSoundProducers[0].fileReader.nFrames - 1)
	    newPosInFrames = (int)fileSoundProducers[0].fileReader.nFrames - 1;

	for(int i=0;i<fileChannels;i++){	    
	    fileSoundProducers[i].endRead=(int)fileSoundProducers[i].fileReader.nFrames-newPosInFrames;
	    //System.out.println("hmm "+fileSoundProducers[i].endRead/44100f);
	}

	mixer.scheduler.move(this,startFrame);

	/*
	if(fadeOut!=null)
	    fadeOut.move(x2-old_x2);
	*/
    }

    public void requestTo_setEndReadPos(float newpos){
	Message message=mixer.getMessage();
	message.something_int=reqtype_setEndReadPos;
	message.something_float=newpos;
	mixer.requestTo_addSoundObjectMessage(this,message);
    }
     


    public void requestTo_addEnvelope(Envelope envelope){
	Message message=mixer.getMessage();
	message.something_int=reqtype_addEnvelope;
	message.something_Object=envelope;
	mixer.requestTo_addSoundObjectMessage(this,message);
    }

    public void requestTo_removeEnvelope(Envelope envelope){
	Message message=mixer.getMessage();
	message.something_int=reqtype_removeEnvelope;
	message.something_Object=envelope;
	mixer.requestTo_addSoundObjectMessage(this,message);
    }
    
    public void requestTo_setFadeIn(float length){
	//System.out.println("setFadeIn "+length);
	for(int i=0;i<fileChannels;i++){
	    fileSoundProducers[i].fadeInLen=(int)(length*mixer.srate*srate_change);
	}

	/*
	if(fadeIn!=null)
	    requestTo_removeEnvelope(fadeIn);
	fadeIn=new Envelope(x1,0.0f,x1+(int)(length*mixer.srate),1.0f);
	requestTo_addEnvelope(fadeIn);
	*/
    }

    public void requestTo_setFadeOut(float length){
	//System.out.println("setFadeOut "+length);
	for(int i=0;i<fileChannels;i++){
	    fileSoundProducers[i].fadeOutLen=(int)(length*mixer.srate*srate_change);
	}

	/*
	if(fadeOut!=null)
	    requestTo_removeEnvelope(fadeOut);
	fadeOut=new Envelope(x2-(int)(length*mixer.srate),1.0f,x2,0.0f);
	requestTo_addEnvelope(fadeOut);
	*/
    }




    public boolean setPlayPos(int newPos){
	boolean ret=true;
	for(int i=0;i<fileChannels;i++){
	    fileSoundProducers[i].startFrame=startFrame;
	    ret=resampleProducers[i].setPlayPos(newPos);
	}
	return ret;
    }



    public boolean produceSound_quickmixer(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	boolean ret=false;


	float[][] tempBuf=audioBuffers.getBuffer();

	for(int i=0;i<fileChannels;i++){
	    fileSoundProducers[i].reverse=this.reverse;
	    //ret=fileSoundProducers[i].produceMonoSound(tempBuf[i],nFrames,audioBuffers);
	    ret=resampleProducers[i].produceMonoSound(tempBuf[i],nFrames,audioBuffers);
	}


	/* Apply envelopes */
	/*
	for(int i=0;i<envelopes.size();i++){ // Do not allocate an iterator, in fear of a GC.
	    Envelope envelope=envelopes.get(i);
	    envelope.apply(y1,y2,tempBuf,fileChannels);
	}
	*/

	for(int ch=0;ch<channels;ch++){
	    float panvol = panvals[0][ch] * vol;
	    audioBuffers.copyBuffer(tempBuf[0],buf[ch],nFrames);
	    audioBuffers.mulBuffer(buf[ch],nFrames,panvol);
	}

	if(fileChannels==2){
	    for(int ch=0;ch<channels;ch++){
		float panvol = panvals[1][ch] * vol;
		audioBuffers.mixBuffers(buf[ch],tempBuf[1],nFrames,panvol);
	    }
	}

	audioBuffers.returnBuffer(tempBuf);
	return ret;
    }

    public boolean produceSound_DSP(float[][] buf,int nFrames,AudioBuffers audioBuffers){
        return fileSoundProducers[0].produceSound(buf,nFrames,audioBuffers);
    }

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	//System.out.println("produceSound called. nFrames: "+nFrames+", vol: "+vol);
        if(forDSP)
            return produceSound_DSP(buf,nFrames,audioBuffers);
        else
            return produceSound_quickmixer(buf,nFrames,audioBuffers);
    }

    public Envelope getEnvelope(){
	if(envelope==null){
	    envelope=new Envelope();
	    for(int ch=0;ch<fileChannels;ch++){
		fileSoundProducers[ch].envelope=envelope;
	    }
	}
	return envelope;
    }

    public Pan12 getPanner(){
        if(panner==null){
            panner=new Pan12();
	    for(int ch=0;ch<fileChannels;ch++){
                fileSoundProducers[ch].panner=panner;
            }
        }
        return panner;
    }
    
    public String getFileName(){
	return fileSoundProducers[0].fileReader.fileName;
    }

    // Better not use this one.
    public float getPlayLength(){
	//System.out.println("soundobject.getLength()");
	return((float)fileSoundProducers[0].getNumPlayFrames()/(float)(mixer.srate*srate_change));
    }

    // Better not use this one.
    public float getAbsLength(){
	//System.out.println("soundobject.getLength()");
	return((float)fileSoundProducers[0].fileReader.nFrames/(float)(mixer.srate*srate_change));
    }


    public SoundObject(MyAudioFileBuffer fileBuffer,float pan,float vol,DasMixer mixer,boolean forDSP){

	this.mixer=mixer;
	this.vol=vol;
        this.forDSP=forDSP;

	fileChannels=java.lang.Math.min(channels,fileBuffer.nChannels);
	panvals=new float[2][2];
	setPan(pan);

	resampleProducers=new ResampleProducer[fileChannels];
	fileSoundProducers=new FileSoundProducer[fileChannels];
	for(int ch=0;ch<fileChannels;ch++){
	    fileSoundProducers[ch]=new FileSoundProducer(fileBuffer,ch);
	    resampleProducers[ch]=new ResampleProducer(fileSoundProducers[ch],sincWidth,mixer.audioBuffers);
	}

	//System.out.println("Srate: "+this.srate+" "+x1+" "+x2);
    }

}

