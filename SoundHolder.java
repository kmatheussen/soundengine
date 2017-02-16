
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import java.util.*;

import java.awt.*;
import javax.swing.*;

import org.tritonus.share.sampled.file.TAudioFileFormat;


public class SoundHolder{

    // There would usually only be one SoundHolder object loaded.
    // Therefore "uberthis" holds last created SoundHolder for convenience.
    public static SoundHolder uberthis=null;
    

    Hashtable audioFileInfos=null;
    Vector<AudioFileInfo> audioFileInfos_nonbase=null;
    Hashtable audiosBeingDownloaded=null;

    short byteToShort( final byte[] data, final int start ) {
        return (short) (
			( ( data[start + 1] & 0xff ) << 8 )
			|
			( data[start] & 0xff ) );
    }

    short byteToShort_be( final byte[] data, final int start ) {
        return (short) (
			( ( data[start] & 0xff ) << 8 )
			|
			( data[start + 1] & 0xff ) );
    }



    boolean ret=false;
    public void addSound(final String fileName,final float srate,final Callbacks returnFunc,final Callbacks progressFunc){

        Thread callBooleanTrue=new Thread() {
                public void run() {
                    returnFunc.callBoolean(true);
                }
            };
        Thread callBooleanFalse=new Thread() {
                public void run() {
                    returnFunc.callBoolean(false);
                }
            };

        synchronized(audiosBeingDownloaded){
            if(audiosBeingDownloaded.get(fileName)!=null){
                if(returnFunc!=null)
                    callBooleanFalse.start();
                return;
            }
        }
        
        AudioFileInfo audioFileInfo;
        synchronized(audioFileInfos){
            audioFileInfo=(AudioFileInfo)audioFileInfos.get(fileName);
        }
        
        if(audioFileInfo!=null){
            if(returnFunc!=null)
                callBooleanTrue.start();
            return;
        }
        
        synchronized(audiosBeingDownloaded){
            audiosBeingDownloaded.put(fileName,this);
        }
                    

        Thread thread=new Thread() {
                public void run() {                    
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    ret=addSound_doit(fileName,srate,progressFunc);
                    if(returnFunc!=null)
                        returnFunc.callBoolean(true);
                }
            };
        thread.start();
        
    }

    public void cancelLoading(String fileName){
	synchronized(audiosBeingDownloaded){
	    audiosBeingDownloaded.remove(fileName);
	}
    }

    private boolean addSound_failed(String fileName,String message,Exception e){
	synchronized(audiosBeingDownloaded){
	    audiosBeingDownloaded.remove(fileName);
	}
	//JOptionPane.showMessageDialog(null,message);
	Warning.print(message,e);
	return false;
    }

    private boolean addSound_doit(String fileName,float srate,Callbacks progressFunc){

	URL url;
	{
	    try {
		url = new URL(fileName);
	    } catch(java.net.MalformedURLException e1) {
		// No protocol prefix, assume local file
		try{
		    url = new URL("file:" + fileName);
		} catch(java.net.MalformedURLException e2) {
		    e2.printStackTrace();
		    System.out.println("SoundHolder.addSound: Unable to open url or file \""+fileName+"\".");
		    return addSound_failed(fileName,"Ikke i stand til aa aapne \""+fileName+"\".",e2);
		}
	    }
	}


	// Create stream.
	int nChannels;
	AudioInputStream org_in=null;
	AudioInputStream din = null;
	{
	    AudioFormat baseFormat;
	    try{
		org_in = AudioSystem.getAudioInputStream(url);
	    }catch(javax.sound.sampled.UnsupportedAudioFileException e){
		System.out.println("SoundHolder.addSound: Unsupported sound file \""+fileName+"\".");
		return addSound_failed(fileName,"Kan ikke aapne \""+fileName+"\". Ukjent filformat",e);
       	    }catch(java.io.IOException e){
		return addSound_failed(fileName,"Klarer ikke aa aapne \""+fileName+"\", eller filen er ikke funnet.",e);
	    }

	    baseFormat = org_in.getFormat();
	    AudioFormat decodedFormat = new AudioFormat(
							baseFormat.getSampleRate(),
							16,
							baseFormat.getChannels(),
							true,false);
	    if (!AudioSystem.isConversionSupported(decodedFormat, baseFormat)) {
		return addSound_failed(fileName,"Klarer ikke aa aapne \""+fileName+"\". Ukjent filformat.",null);
	    }

	    nChannels=baseFormat.getChannels();

	    din = AudioSystem.getAudioInputStream(decodedFormat, org_in);
	}


	// Estimate the number of frames to download for the progress monitor. If the length can't be estimated, set length_unknown to true.
	boolean length_unknown=false;
	long length=din.getFrameLength();
	{



	    // All of this and everything else always fails :((((( (din.getFrameLength() seems to be the only thing that works)
	    /*
	    AudioFileFormat fileFormat=null;
	    try{
		fileFormat=AudioSystem.getAudioFileFormat(org_in);
	    }catch(Exception e){
		e.printStackTrace();
	    }

	    Map props = fileFormat.properties();


	    long seconds=0;

	    if (props.containsKey("duration"))
		seconds = (Long)props.get("duration");
	    //seconds = (long) Math.round((((Long)props.get("duration")).longValue())/1000000);
	    System.out.println("length:"+length+", secodns:"+seconds+" "+props+", format:"+din.getFormat().properties());

	    System.out.println("bytelength: "+fileFormat.getByteLength());
	    */

	    if(length<=0){
		//progressMonitor.setNote("Length Unknown for file "+fileName);
		length=1024;
		length_unknown=true;
	    }

	}


	// Write audio data to temporary wav files
	int nFrames=0;
	String[] tempNames=new String[nChannels];
	TempFile tempFiles[]=new TempFile[nChannels];
	Peaks[] peaks=new Peaks[nChannels];
	{
            //System.out.println("*** Part 1\n");
	    for(int ch=0;ch<nChannels;ch++){

		try {
		    tempFiles[ch] = TempFile.create("Hurtigmixer-"+ch+"-", ".data");
		} catch (Exception e2) {
		    // Cannot write temporary audio file, must report to user
		    //Warning.print("Could not make soundfile.",e2);
		    return addSound_failed(fileName,
					   "Klarer ikke lage lydfil (\""+
                                           tempFiles[ch].name+
					   "\")(1). Sjekk at du har skriverettighet og at ikke harddisken er full. ("+
					   e2.getMessage()+")",e2);
		}
		tempNames[ch]=tempFiles[ch].name;
		//System.out.println("SoundHolder.addLyd, temporary audio file : " + tempNames[ch]);

		peaks[ch]=new Peaks();
	    }
	    

            //System.out.println("*** Part 2\n");

	    int bufferSize=1024;
	    int bytestoread=2*nChannels*bufferSize;
	    int bytestowrite=2*bufferSize;
	    byte b[]=new byte[bytestoread];
	    byte s[][]=new byte[nChannels][bytestowrite];
	    short p[]=new short[bytestowrite*2];
	    int lastsecond=-1;
	    //System.out.println("nChannels: "+nChannels+", bytestoread: "+bytestoread+", bytestowrite:"+bytestowrite);

	    try{
		while(true){
		    int numread=din.read(b,0,bytestoread);
                    //System.out.println("*** Part 3\n"+numread);

		    if(numread==-1)
			break;

		    synchronized(audiosBeingDownloaded){
			if(audiosBeingDownloaded.get(fileName)==null){
			    din.close();
			    org_in.close();
			    return false;
			}
		    }

		    /*

		    if(progressMonitor.isCanceled()){
			for(int ch=0;ch<nChannels;ch++){
			    tempAudioFiles[ch].delete();
			}
			synchronized(audiosBeingDownloaded){
			    audiosBeingDownloaded.remove(fileName);
			}
			return false;
		    }
		    */

		    int framesread=numread/(2*nChannels);
		    //System.out.println("numread:"+numread+", framesread:"+framesread);
		    for(int ch=0;ch<nChannels;ch++) {
			for(int i=0;i<framesread;i++){
			    s[ch][(i*2) ]   = b[(i*2*nChannels) + (ch*nChannels)];
			}
			for(int i=0;i<framesread;i++){
			    s[ch][(i*2)+ 1] = b[(i*2*nChannels) + (ch*nChannels) +1];
			}
			for(int i=0;i<framesread;i++){
			    p[i]=byteToShort(s[ch],i*2);
			}
			tempFiles[ch].write(s[ch],0,numread/nChannels);
			peaks[ch].addData(p,framesread);
		    }

		    nFrames+=framesread;

		    int second=nFrames/(int)srate;
		    if(second!=lastsecond){
			if(progressFunc!=null)
                            progressFunc.callProgress(nFrames/(int)srate,length_unknown?-1:(int)length/(int)srate);
			/*
			if(length_unknown==true){
			    progressMonitor.setNote(""+nFrames/(int)srate+" seconds.");
			    progressMonitor.setMaximum(nFrames+1024);
			}
			progressMonitor.setProgress(nFrames);
			*/
			lastsecond=second;
		    }

		}
                //System.out.println("*** Part 4\n");
		for(int ch=0;ch<nChannels;ch++){
		    peaks[ch].close();
		}
	    }catch(IOException e){		
		//Warning.print("Could not create soundfile.",e);
		return addSound_failed(fileName,"Klarer ikke lage lydfil (2). Er harddisken full? ("+e.getMessage()+")",e);
	    }catch(Exception e){
		return addSound_failed(fileName,"Soundfile seems corrupt.\n Lydfila virker korrupt.\n ("+e.getMessage()+")",e);
            }
	}

        //System.out.println("*** Part 5\n");
	
	synchronized(audioFileInfos){
	    audioFileInfos.put(fileName,new AudioFileInfo(fileName,nChannels,nFrames,srate,tempNames,tempFiles,peaks));
	}

        //System.out.println("*** Part 6\n");

	synchronized(audiosBeingDownloaded){
	    audiosBeingDownloaded.remove(fileName);
	}

        //System.out.println("*** Part 7\n");

	try{
	    din.close();
	    org_in.close();
	}catch(java.io.IOException e){
	    Warning.print("Could not close file",e);
	}

        //System.out.println("*** Part 8\n");

	return true;
    }


    public boolean waitForSound(String fileName){
	AudioFileInfo audioFileInfo=null;
	while(true){
            synchronized(audioFileInfos){
                audioFileInfo=(AudioFileInfo)audioFileInfos.get(fileName);
            }
	    if(audioFileInfo!=null)
		return true;
            Object gotIt;
            synchronized(audiosBeingDownloaded){
                gotIt=audiosBeingDownloaded.get(fileName);
            }
	    if(gotIt==null)
		return false;
	    try{
		Thread.sleep(100);
	    }catch(Exception e){
	    }		
	    System.out.println("Waiting for "+fileName+" to be converted...");
	}
    }


    public AudioFileInfo getAudioFileInfo(String fileName,float srate){
	AudioFileInfo audioFileInfo=(AudioFileInfo)audioFileInfos.get(fileName);
	if(audioFileInfo==null){
	    System.out.println(fileName+" not successfully loaded, or not added yet. Can't use sound.");
	    return null;
	}

	audioFileInfo=new AudioFileInfo(audioFileInfo);
	audioFileInfos_nonbase.add(audioFileInfo);

	return audioFileInfo;
    }


    public void reset(){
	if(audioFileInfos_nonbase!=null)
	    for (Iterator it=audioFileInfos_nonbase.iterator();it.hasNext();)
		((AudioFileInfo)it.next()).close();
	if(audioFileInfos!=null)
	    for (Iterator it=audioFileInfos.values().iterator();it.hasNext();)
		((AudioFileInfo)it.next()).delete();
	
	audioFileInfos=new Hashtable();
	audioFileInfos_nonbase=new Vector<AudioFileInfo>();
	audiosBeingDownloaded=new Hashtable();
    }

    public SoundHolder(){
	reset();
	if(uberthis!=null)
	    uberthis.reset();
	uberthis=this;
    }

}
