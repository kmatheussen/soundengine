
import java.io.File;
import jass.render.*;
import de.gulden.framework.jjack.*;
import java.nio.FloatBuffer;

// All this runnable/threadgroup/thread-verbosity is there because I hoped creating my own threadgroup would let me set max priority of a thread in a signed applet. Apparently it does not.


public class MyPlayer implements Runnable,JJackAudioProcessor {
    //extends Thread{

    ThreadGroup threadGroup;
    Thread thread;

    SourcePlayer sourcePlayer;

    boolean usingJack = false;

    private boolean useRTPlay;
    RTPlay rtPlayer;

    public float srate;
    int nFrames;
    private float[][] srate_buf;
    private float[][] buf;
    private float[] interleavedBuf;
    AudioBuffers audioBuffers;

    private boolean pleaseExit=false;
    public boolean player_has_started=false;

    private final int channels=2;
    private final int bits=16;
   
    public void getData(float[][] buf,int nFrames,AudioBuffers audioBuffers){
    	System.out.println("MyPlayer.java/getData: Error. I am not supposed to be called. Subclasses of MyPlayer must implement getData.");
    }

    /*
    private int currSampleChannel=0;
    private int currSampleBufLength=0;
    private int currSampleBufPos=0;
    public float getSample(){
	if(currSampleBufPos>=currSampleBufLength){
	    getData(currSampleBufLength,buf);
	    currSampleBufPos=0;
	}
	currSampleBufPos++;
	return buf[currSampleChannel,currSampleBufPos-1];
    }
    */


    // Pull. Used when using jack.
    public void process(JJackAudioEvent e) {
	FloatBuffer out0 = e.getOutput(0);
	FloatBuffer out1 = e.getOutput(1);
	int nFrames=de.gulden.framework.jjack.JJackSystem.getBufferSize();

	getData(buf,nFrames,audioBuffers);

	for(int i=0;i<nFrames;i++){
	    out0.put(i,buf[0][i]);
	    out1.put(i,buf[1][i]);
	}

    }


    // Push. Not used when using jack.
    public void run(){
	thread.setPriority(Thread.MAX_PRIORITY);
	//setPriority(MAX_PRIORITY);
	System.out.println("Sound thread has priority:"+thread.getPriority()+" (max is:"+Thread.MAX_PRIORITY+", and max in threadGroup is:"+threadGroup.getMaxPriority()+")");
	//setPriority(MAX_PRIORITY);

	while(pleaseExit==false){
	    try{
		getData(buf,nFrames,audioBuffers);
	    }catch(Exception e){
		e.printStackTrace();
		System.out.println("hepp "+e+", "+e.getStackTrace());
		Thread.dumpStack();
	    }

	    for(int i=0;i<nFrames;i++){
		interleavedBuf[i*channels]=buf[0][i];
	    }
	    for(int i=0;i<nFrames;i++){
		interleavedBuf[i*channels+1]=buf[1][i];
	    }
	    if(useRTPlay){
		rtPlayer.write(interleavedBuf,nFrames*channels);
	    }else{
		sourcePlayer.push(interleavedBuf);
	    }
	}
    }

    public void startPlayer(){
	if(player_has_started==false){
	    player_has_started=true;
	    if(useRTPlay){
		rtPlayer = new RTPlay(nFrames*bits/8,srate,bits,channels,true);
	    }

	    thread = new Thread(threadGroup,this);
	    thread.start();

	    //start();
	    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	    System.out.println("Sys priority is:"+Thread.currentThread().getPriority()+" (max is:"+Thread.MAX_PRIORITY+", and min is:"+Thread.MIN_PRIORITY+")");

	}
    }

    public void exitPlayer(){
	pleaseExit=true;
    }

    boolean checkIfJackIsAvailable(){
	try{
	    //System.setProperty("java.library.path", (new File (".")).getCanonicalPath()+"/lib/linux");
	    //System.setProperty("java.library.path","lib/linux");
	    //System.loadLibrary("jjack");
	    //System.out.println("-"+(new File (".")).getCanonicalPath()+"/lib/linux/libjjack.so-");
	    //System.load((new File (".")).getCanonicalPath()+"/lib/linux/libjjack.so");
	    String lib = "lib/"+System.getProperty("os.arch")+"/"+System.getProperty("os.name")+"/libjjack.so";
	    try{
		File file = new File(lib);
		String libJJackFileName = file.getAbsolutePath();
		System.load(libJJackFileName);
	    }catch(Throwable e3) {
		if(Encoders.downloadFileFromURL("http://archive.notam02.no/DSP02/program/stable/"+lib,new File("/tmp/libjjack.so"),"so")==true)
		    System.load("/tmp/libjjack.so");
	    }
	}catch(Throwable e) {
	    System.err.println("MyPlayer: Could not load jjack native library");
	    e.printStackTrace();
	    return false;
	}
	return de.gulden.framework.jjack.JJackSystem.isInitialized();
    }

    void startJack(){
	System.out.println(de.gulden.framework.jjack.JJackSystem.VERSION);
	System.out.println("isrunning: "+de.gulden.framework.jjack.JJackSystem.isInitialized());
	de.gulden.framework.jjack.JJackSystem.setProcessor(this);
	player_has_started=true;
	this.srate = de.gulden.framework.jjack.JJackSystem.getSampleRate();
	this.nFrames = de.gulden.framework.jjack.JJackSystem.getBufferSize();
    }


    // NOTE! The parameters 'das_srate' and 'das_nFrames' are only hints.
    // 
    // The player will use das_srate and das_nFrames if possible, but it could
    // very well use something else instead. For jack, for instance, the das_srate
    // and das_nFrames arguments doesn't matter.
    public MyPlayer(int das_nFrames,float das_srate,boolean useRTPlay){
	this.useRTPlay=useRTPlay;
	nFrames=das_nFrames;
	srate=das_srate;

	if(checkIfJackIsAvailable()){
	    usingJack = true;
	    startJack();
	}else if(useRTPlay==true){
	    //rtPlayer = new RTPlay(nFrames*bits/8,srate,bits,channels,true);
	}else{
	    sourcePlayer=new SourcePlayer(nFrames*bits/8,srate);
	    sourcePlayer.setNChannels(channels);
	    //System.out.println("    NUMBUFFERS: "+sourcePlayer.getNumRtAudioBuffersNative());
	    //sourcePlayer.initPush();
	    //sourcePlayer.setUseNativeSound(true);
	}

	this.audioBuffers=new AudioBuffers(nFrames);

	buf=audioBuffers.getBuffer();

	if(usingJack==false){
	    interleavedBuf=new float[nFrames*channels];

	    threadGroup=new ThreadGroup("Speedmixerthreadgroup");
	    threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
	}
    }

}

