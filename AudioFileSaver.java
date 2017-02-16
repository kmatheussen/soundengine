
// Code taken from Hurtigmikser. -Kjetil.



import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import javazoom.spi.vorbis.sampled.file.*;
import javax.sound.sampled.AudioFormat.*;

import javax.swing.*;
import java.awt.*;

import org.tritonus.share.sampled.*;



public class AudioFileSaver{

    float[] interleavedBuf;
    AudioBuffers audioBuffers;
    WavFileSaver wfs;

    Callbacks progressFunc;

    int bits;
    int channels;
    long srate;

    int maxProgress;

    DasMixer dasMixer;
    float[][] buf;


    ProgressMonitor progressMonitor=null;

    public boolean writeIt(String filename){
	File file=new File(filename);
	int nFrames=buf[0].length;

	try{
	    wfs=new WavFileSaver(file,channels,bits,srate);

	    while(true){
		dasMixer.getData_common(buf,nFrames,audioBuffers);
		wfs.write(buf,nFrames);

		if(progressMonitor!=null){

		    if(progressMonitor.isCanceled()){
			wfs.file.close();
			file.delete();
			return false;
		    }
		    progressMonitor.setProgress(dasMixer.scheduler.playPos);
		}

		if(dasMixer.scheduler.playPos==0 || dasMixer.scheduler.playPos>maxProgress)
		    break;
	    }
	    wfs.close();
	}catch(IOException e){
	    JOptionPane.showMessageDialog(null,"Kunne ikke lagre fila. ("+e.getMessage()+")");
	    e.printStackTrace();
	    file.delete();
	    return false;
	}

	//file.close();
	boolean ret=true;
	
	if(progressMonitor!=null){
	    ret=progressMonitor.isCanceled()?false:true;
	    progressMonitor.close();
	}

	return(ret);
    }


    public AudioFileSaver(final String filename, int channels,float srate,int bits, float seconds, DasMixer dasMixer,final Callbacks progressFunc,final Callbacks returnFunc,AudioBuffers audioBuffers,Container container){

	this.audioBuffers=audioBuffers;
	this.channels=channels;
	this.bits=bits;
	this.srate=(long)srate;

	buf=audioBuffers.getBuffer();

	this.dasMixer=dasMixer;
	this.progressFunc=progressFunc;

	maxProgress=(int)(seconds*srate + dasMixer.nFrames+2);

	progressMonitor=new ProgressMonitor(container,"Please wait, saving "+filename,"",0,maxProgress);
	progressMonitor.setMillisToPopup(10);
	progressMonitor.setMillisToDecideToPopup(10);
	progressMonitor.setProgress(50);

	Thread thread=new Thread() {
		public void run() {
		    System.out.println("About to save");
		    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		    boolean ret=writeIt(filename);
		    System.out.println("Finished saving");
		    if(returnFunc!=null)
			returnFunc.callBoolean(ret);
		}
	    };
	thread.start();

    }

}



