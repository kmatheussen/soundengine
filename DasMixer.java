


import java.util.*;
    
import javax.sound.sampled.*;
import java.io.File;
import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.awt.*;

import java.util.concurrent.atomic.AtomicBoolean;

class AudioFileSaverParameters{
    String filename;
    int channels;
    float srate;
    int bits;
    float seconds;
    Callbacks progressFunc;
    Callbacks returnFunc;
    Container container;
}

public class DasMixer extends MyPlayer{

    public static DasMixer uberThis=null;

    final private int reqtype_newplaypos=0;
    final private int reqtype_add=1;
    final private int reqtype_remove=2;
    final private int reqtype_removeAll=3;
    final private int request_soundObjectMessage=4;
    final private int reqtype_save=5;

    private int sincWidth = 20;

    public Scheduler scheduler=new Scheduler();

    public Cursor cursor=null;

    private RingBuffer messages=new RingBuffer();

    private AtomicBoolean isPlaying=new AtomicBoolean(false);
    private AtomicBoolean isPlayerRunning=new AtomicBoolean(false);

    private AtomicBoolean isLooping=new AtomicBoolean(true);
    private int request_newplaypos=-1;
    private int length;
    private final int channels=2;
    private int bits;

    public float maxVolume=0.5f;
    public GlideVar volume=new GlideVar2(0.5f,0.999f);

    public float maxVal=0.0f;
    public double srate_change=1.0f;
    public double srate_change_rate_mismatch; // In case srate and requested_srate differs significantly, everything needs to be resampled. This is the mismatch resample factor.
    public float requested_srate;

    private float[][] tempBuf;
    private byte[] byteTempBuf;

    public StereoResampleProducer stereoResampleProducer;

    public float reverbAmount = 0.5f;
    public FreeVerb freeVerb = null;
    public ReverbBus reverbBus = null;

    public static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }

    public void setForceSincResampler(boolean val){
	stereoResampleProducer.setForceSincResampler(val);
    }
    
    //////////////////////////////////////////////////////////////////////
    // Messages
    //////////////////////////////////////////////////////////////////////

    LinkedList<Message> readyMessages=new LinkedList<Message>();
    int freedMessagesLength=0;
    LinkedList<Message> freedMessages=new LinkedList<Message>();
    
    public Message getMessage(){
	Message ret;
	synchronized(this){
	    if(readyMessages.size()==0)
		ret=new Message();
	    else
		ret=readyMessages.removeFirst();
	}
	return ret;
    }

    public void drainFreedMessages(){
	synchronized(this){
	    if(freedMessagesLength>0){
		readyMessages.addAll(freedMessages);
		freedMessages=new LinkedList<Message>();
		freedMessagesLength=0;
	    }
	}
    }

    void reset(){
	stereoResampleProducer.reset();
	if(freeVerb!=null)
	    freeVerb = freeVerb.reset();	
    }

    void setPlayPos(int pos){
	reset();
	scheduler.setPlayPos(pos);
    }

    private void drainRingBuffer(){
	//System.out.println("drainRingBuffer");
	Message message=(Message)messages.read();
	while(message!=null){
	    switch(message.type){
	    case reqtype_newplaypos:
		setPlayPos(message.something_int);
		//notify();
		break;
	    case reqtype_add:
		//if(message.something_SoundProducer==null)
		//    System.out.println("Yes, it's really null");
		scheduler.add(message.something_SoundProducer,message.something_int);
		// seems like message.something_SoundProducer are null sometimes.
		break;
	    case reqtype_remove:
		scheduler.delete(message.something_SoundProducer);
		break;
	    case reqtype_removeAll:
		scheduler.deleteAll();
		break;
	    case request_soundObjectMessage:
		SoundObject so=(SoundObject)message.something_SoundProducer;
		so.handleMessage(message);
		break;
	    case reqtype_save:
		AudioFileSaverParameters afsp=(AudioFileSaverParameters)message.something_Object;
		save(afsp);
		break;
	    }
	    message.clear();
	    if(freedMessagesLength<1000){
		freedMessages.addFirst(message);
		freedMessagesLength++;
	    }
	    message=(Message)messages.read();
	}
    }



    //////////////////////////////////////////////////////////////////////
    // Produce sound
    //////////////////////////////////////////////////////////////////////


    /*
    public void fillBuf(int pos,float[][] buf,int nFrames,AudioBuffers audioBuffers){
	System.out.println("dasMixer.fillBuf, nFrames:"+nFrames);

	for(int i=0;i<soundProducers.size();i++){
	    SoundProducer soundProducer=soundProducers.get(i);  // We do not allocate an iterator, in fear of a GC.
	    audioBuffers.addBuf(pos,buf,nFrames,soundProducer); // Calling soundProducer.fillBuf()
	}


	// Set maxVal (for the vu meter)
	maxVal=java.lang.Math.max(audioBuffers.getMax(buf,nFrames),-audioBuffers.getMin(buf,nFrames))*volume;

	// Updating playPos here (instead of at the top of this function) causes the cursor to be slightly behind the sound. I'm not sure what's the least worst way to fix that.
	playPos=pos+nFrames;
	if(playPos>length)
	    playPos=0;

	cursor.request_paint();
	
    }
    */

	
    // can also be called from AudioFileSaver
    public void getData_common(float[][] buf,int nFrames,AudioBuffers audioBuffers){	
	stereoResampleProducer.setSRate(srate_change*srate_change_rate_mismatch);
	stereoResampleProducer.produceSound(buf,nFrames,audioBuffers);
	//scheduler.produceSound(buf,nFrames,audioBuffers);

	if(reverbBus!=null)
	    reverbBus.write(buf,nFrames,reverbAmount);

	if(freeVerb!=null)
	    freeVerb.compute(nFrames,buf,buf);

	float[][] volume = audioBuffers.getBuffer();{
	    this.volume.get(volume[0],nFrames);

	    audioBuffers.mulBuffer(buf[0],volume[0],nFrames);
	    audioBuffers.mulBuffer(buf[1],volume[0],nFrames);
	}audioBuffers.returnBuffer(volume);

	maxVal=java.lang.Math.max(audioBuffers.getMax(buf,nFrames),-audioBuffers.getMin(buf,nFrames));

	//audioBuffers.resampleBuf(pos,buf,nFrames,(int)(srate_change*nFrames),sampleRateConverters,this,true);
	if(scheduler.playPos>length){
	    setPlayPos(0);
	}

	if(cursor!=null)
	    cursor.request_paint();
    }

    
    // Sound Playing
    public void getData(float[][] buf,int nFrames,AudioBuffers audioBuffers){

	try{
	    drainRingBuffer();
	}catch(NullPointerException e){
	    e.printStackTrace();
	    System.out.println("hepp2 "+e);//+", "+e.getStackTrace());
	    Thread.dumpStack();
	}
	if(isPlaying.get()==true){
	    getData_common(buf,nFrames,audioBuffers);
	    if(scheduler.playPos==0 && isLooping.get()==false){
		isPlaying.set(false);
	    }
	}else{
	    audioBuffers.clearBuffer(buf,nFrames);
	    if(isPlayerRunning.get()==true){
		isPlayerRunning.set(false);
		maxVal=0.0f;
		if(cursor!=null)
		    cursor.request_paint();
		drainFreedMessages();
	    }
	}

        {
            ResampleProducer player=singlefileplayer;
            if(player!=null){
		//player.setSRate(srate_change*srate_change_rate_mismatch);
                float[][] tempBuf=audioBuffers.getBuffer();
                
                if(player.produceMonoSound(tempBuf[0],nFrames,audioBuffers)==false){
                    singlefileplayer=null;
                    System.out.println("finished");
                }
                audioBuffers.mulBuffer(tempBuf[0],nFrames,singlefileplayer_volume*volume.get_goal());
                
                audioBuffers.copyBuffer(tempBuf[0],buf[0],nFrames);
                audioBuffers.copyBuffer(tempBuf[0],buf[1],nFrames);

                audioBuffers.returnBuffer(tempBuf);
            }
        }
    }




    //////////////////////////////////////////////////////////////////////
    // Single file player
    //////////////////////////////////////////////////////////////////////

    FileSoundProducer singlefileplayerplayer=null;
    ResampleProducer singlefileplayer=null;
    ResampleProducer singlefileplayer_paused=null;
    float singlefileplayer_volume=1.0f;

    public void addSingleFilePlayer(MyAudioFileBuffer fileReader,int ch,double srate,float volume){
	singlefileplayerplayer=new FileSoundProducer(fileReader,ch);
        singlefileplayer=new ResampleProducer(singlefileplayerplayer,sincWidth,audioBuffers);
	singlefileplayer.setSRate_now(srate);
        singlefileplayer_volume=volume;
	startPlayer();
    }

    public void addSingleFilePlayer(MyAudioFileBuffer fileReader,int ch){
        addSingleFilePlayer(fileReader,ch,srate_change*srate_change_rate_mismatch,1.0f);
    }

    public void pauseSingleFilePlayer(){
        singlefileplayer_paused=singlefileplayer;
        singlefileplayer=null;
    }
    public void resumeSingleFilePlayer(){
        singlefileplayer=singlefileplayer_paused;
    }

    public void stopSingleFilePlayer(){
        singlefileplayer=null;
    }

    public float getSingleFilePlayerTime(boolean compensateLatency){
        if(singlefileplayer==null){
	    System.out.println("singlefileplayer==null");
            return 0.0f;
	}
	if(compensateLatency)
	    return Math.max(0.0f,((float)singlefileplayerplayer.readPos-(0.5f*nFrames))/requested_srate);
	else
	    return singlefileplayerplayer.readPos/requested_srate;
    }

    public float getSingleFilePlayerTime(){
	return getSingleFilePlayerTime(true);
    }
    public boolean isSingleFilePlayerPlaying(){
        return singlefileplayer!=null;
    }




    //////////////////////////////////////////////////////////////////////
    // User Interface
    //////////////////////////////////////////////////////////////////////


    private void finalizeRequest(Message message){
	if(messages.write(message)==false){
	    int before=messages.getWriteSpace();
	    //stopPlaying();
	    //JOptionPane.showMessageDialog(null,"Error. Ringbuffer is full. Stopping player");
	    //System.out.println("           BEFORE WAITING");
	    try{
		Thread.currentThread().sleep(100);
	    }catch(InterruptedException ie){

	    }
	    //System.out.println("           AFTER WAITING");
	    int after=messages.getWriteSpace();
	    System.out.println("Ringbuffer was full. size before: "+before+". After: "+after);
	    finalizeRequest(message);
	    return;
	}
	if(isPlayerRunning.get()==false){
	    drainRingBuffer();
	    drainFreedMessages();
	}
    }
    
    public void requestTo_addSoundObjectMessage(SoundObject soundObject, Message message){
	message.type=request_soundObjectMessage;
	message.something_SoundProducer=soundObject;
	finalizeRequest(message);
    }

    public void requestTo_add(SoundProducer soundProducer,int startFrame){
	if(isPlayerRunning.get()==false){
	    scheduler.add(soundProducer,startFrame);
	}else{
	    Message message=getMessage();
	    message.type=reqtype_add;
	    message.something_SoundProducer=soundProducer;
	    message.something_int=startFrame;
	    finalizeRequest(message);
	}
    }

    public void requestTo_remove(SoundProducer soundProducer){
	Message message=getMessage();
	message.type=reqtype_remove;
	message.something_SoundProducer=soundProducer;
	finalizeRequest(message);
    }    

    public void requestTo_removeAll(){
	Message message=getMessage();
	message.type=reqtype_removeAll;
	finalizeRequest(message);
    }
    
    public void requestTo_setPlayPos(float newPos,boolean wait){
	int dasNewPos=(int)(newPos*requested_srate);

	if(dasNewPos<0)
	    dasNewPos=0;
	if(dasNewPos>=length)
	    dasNewPos=0;

	if(isPlayerRunning.get()==false){
	    setPlayPos(dasNewPos);
	}else{
	    Message message=getMessage();
	    message.type=reqtype_newplaypos;
	    message.something_int=dasNewPos;
	    finalizeRequest(message);
	    /*
	    if(wait)
		try{
		    wait();
		}catch(InterruptedException e){
		    System.out.println("Got interrupted "+e);
		}
	    */
	}
    }

    public void requestTo_setPlayPos(float newPos){
	requestTo_setPlayPos(newPos,false);
    }

    private void save(AudioFileSaverParameters afsp){
	stopPlaying();
	setPlayPos(0);
	new AudioFileSaver(afsp.filename,afsp.channels,afsp.srate,afsp.bits,afsp.seconds,this,afsp.progressFunc,afsp.returnFunc,audioBuffers,afsp.container);
    }
	
    public void requestTo_save(String filename, int channels,float srate,int bits, float seconds, Callbacks progressFunc, Callbacks returnFunc,Container container){
	AudioFileSaverParameters afsp=new AudioFileSaverParameters();
	afsp.filename=filename;
	afsp.channels=channels;
	afsp.srate=srate;
	afsp.bits=bits;
	afsp.seconds=seconds;
	afsp.returnFunc=returnFunc;
	afsp.progressFunc=progressFunc;
        afsp.container=container;

	Message message=getMessage();
	message.type=reqtype_save;
	message.something_Object=afsp;
	finalizeRequest(message);
    }

    public void requestTo_save(String filename, int channels,float srate,int bits, float seconds, Callbacks progressFunc, Callbacks returnFunc){
	requestTo_save(filename,channels,srate,bits,seconds,progressFunc,returnFunc,null);
    }

    public float getPlayPos(boolean compensateLatency){
	if(compensateLatency)
	    return Math.max(0.0f,((float)scheduler.playPos-(0.5f*nFrames))/requested_srate);
	else
	    return ((float)scheduler.playPos)/requested_srate;
    }

    public float getPlayPos(){
	return getPlayPos(true);
    }

    public void setLooping(boolean isLooping){
	this.isLooping.set(isLooping);
    }
    public boolean getLooping(){
	return isLooping.get();
    }

    public void startPlaying(){
	isPlaying.set(true);
	isPlayerRunning.set(true);
	startPlayer();
    }
    public void stopPlaying(){
	isPlaying.set(false);
    }

    public boolean isPlayingQuestionmark(){
	return isPlaying.get();
    }

    public boolean isPlayerRunningQuestionmark(){
        return isPlayerRunning.get();
    }

    public void setLength(float length){
	this.length=(int)(requested_srate*length);
    }

    public void setCursor(Cursor cursor){
	this.cursor=cursor;
    }

    public void enableFreeVerb(){
	if(freeVerb != null){
	    freeVerb = freeVerb.reset();
	}else
	    freeVerb = new FreeVerb((int)srate);
    }

    public DasMixer(int das_nFrames, float das_srate, boolean useRTPlay, float length){
	super(das_nFrames,das_srate,useRTPlay);

	if(Math.abs(das_srate-srate)<10)
	    this.requested_srate = srate;
	else
	    this.requested_srate = das_srate;

	srate_change_rate_mismatch = requested_srate / srate;
	
	System.out.println(nFrames+" "+srate+" "+useRTPlay+", mismatch: "+srate_change_rate_mismatch);
	this.tempBuf=new float[channels][nFrames];
	this.byteTempBuf=new byte[nFrames*2];
	this.length=(int)(requested_srate*length);
	//cursor=new Cursor(this);

        DasMixer.uberThis=this;

	stereoResampleProducer=new StereoResampleProducer(scheduler,sincWidth,audioBuffers);
	stereoResampleProducer.setSRate_now(srate_change*srate_change_rate_mismatch);
	//freeVerb.setWet(1.0f);
	//freeVerb.setRoomSize(1.0f);
    }
}

