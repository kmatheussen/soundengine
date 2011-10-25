
import java.io.*;

public class AudioFileInfo{

    String fileName=null;
    int nChannels;
    int nFrames;
    float srate;
    String tempFileNames[]=null;
    RandomAccessFile tempFiles[] = null;
    Peaks peaks[]=null;


    public void close(){
	for(int ch=0;ch<nChannels;ch++){
	    try{
		tempFiles[ch].close();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    peaks[ch]=null;
	}
    }
    public void delete(){
	for(int ch=0;ch<nChannels;ch++){
	    try{
		(new File(tempFileNames[ch])).delete();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    peaks[ch]=null;
	}
    }

    protected void finalize() throws Throwable
    {
        try{
            if(tempFiles!=null)
                for(int ch=0;ch<nChannels;ch++){
                    tempFiles[ch].close();
                }
        } finally {
            //super.finalize(); (no super)
        }
    }

    // Make new
    public AudioFileInfo(String fileName,int nChannels,int nFrames,float srate,String tempFileNames[],Peaks[] peaks){
	this.fileName=fileName;
	this.nChannels=nChannels;
	this.nFrames=nFrames;
	this.srate=srate;
	this.tempFileNames=tempFileNames;
	this.peaks=peaks;
    }

    // Make new, but includes tempfiles
    public AudioFileInfo(String fileName,int nChannels,int nFrames,float srate,String tempFileNames[],RandomAccessFile tempFiles[], Peaks[] peaks){
	this.fileName=fileName;
	this.nChannels=nChannels;
	this.nFrames=nFrames;
	this.srate=srate;
	this.tempFileNames=tempFileNames;
	this.tempFiles=tempFiles;
	this.peaks=peaks;
    }

    // Make new info object, and create new tempfiles and peaks as well.
    public AudioFileInfo(int nChannels,int nFrames,float srate){
	this.nChannels=nChannels;
	this.nFrames=nFrames;
	this.srate=srate;
	this.tempFiles=new RandomAccessFile[nChannels];
	this.tempFileNames=new String[nChannels];
	this.peaks=new Peaks[nChannels];
	try{
	    {
		for(int ch=0;ch<nChannels;ch++){
		    TempFile tempFile=TempFile.create("DSP2","sounddata"); //new RandomAccessFile(tempFileNames[ch],"rw");
		    this.tempFiles[ch]=tempFile;
		    this.tempFileNames[ch]=tempFile.name;
		    this.peaks[ch]=new Peaks();
		}
	    }
	}catch(IOException e){
	    e.printStackTrace();
	}
    }

    // Make copy
    public AudioFileInfo(AudioFileInfo baseInfo){
	this.fileName=baseInfo.fileName;
	this.nChannels=baseInfo.nChannels;
	this.nFrames=baseInfo.nFrames;
	this.srate=baseInfo.srate;
	this.tempFileNames=baseInfo.tempFileNames;
	this.peaks=baseInfo.peaks;
	this.tempFiles=new RandomAccessFile[nChannels];
	try{
	    {
		for(int ch=0;ch<nChannels;ch++){
		    tempFiles[ch]=new RandomAccessFile(tempFileNames[ch],"rw");
		}
	    }
	}catch(IOException e){
	    e.printStackTrace();
	}
    }
}



