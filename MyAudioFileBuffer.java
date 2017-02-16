
import java.lang.*;
import java.net.*;
import java.awt.*;
import java.io.*;

import java.nio.channels.FileChannel;
import java.nio.*;

import javax.sound.sampled.*;
import org.tritonus.sampled.convert.*;

import javax.swing.*;

import jass.render.*;
import jass.generators.*;

public class MyAudioFileBuffer{

    public String fileName;

    public float srate;
    public long nFrames;
    public int nChannels;

    public AudioFileInfo audioFileInfo;

    int writtenFrames=0;

    boolean reverse=false;

    boolean updatePeaks = true;

    static short byteToShort( final byte[] data, final int start ) {
        return (short) (
			( ( data[start + 1] & 0xff ) << 8 )
			|
			( data[start] & 0xff ) );
    }

    static final public  void byteToFloatReverse(float [] dbuf, byte[] bbuf,int bufsz) {
        int ib=(bufsz-1)*2;
	for(int i=0;i<bufsz;i++) {
	    short y=byteToShort(bbuf,ib);
	    ib -= 2;
	    dbuf[i] = y/32768.f;
	}
    }


    static final public void shortToByte(byte[] byteSound, short [] dbuf, int start, int end){
        int ib=0;
        for(int i=start;i<end;i++) {
            short y = dbuf[i];
	    byteSound[ib] = (byte) ( ( y >>> 0 ) & 0xff );
	    byteSound[ib+1] = (byte) ( ( y >>> 8 ) & 0xff );
	    ib+=2;
        }
    }

    public void setChannelDataEmpty(int start,int end){
        if(end>nFrames)
            end=(int)nFrames;
        int len=end-start;

	for(int ch=0;ch<nChannels;ch++){
	    RandomAccessFile file=audioFileInfo.tempFiles[ch];
	    try{
		file.seek(start*2);
		
		int pos=0;
		byte[] buf=new byte[4096];
		
		while(pos<len){
		    int endPos=pos+2048;
		    if(endPos>len)
			endPos=len;
		    file.write(buf,0,(endPos-pos)*2);
		    pos=endPos;
		}
	    }catch(java.io.IOException e){
		Warning.print("Not able to seek or write. Disk full? "+" "+ch+", "+nFrames+", "+audioFileInfo.fileName,e);
		return;
	    }
	}
	writtenFrames=Math.max(end,writtenFrames);
    }

    public void setRemainingDataEmpty(){
	if(writtenFrames<nFrames)
	    setChannelDataEmpty(writtenFrames,(int)nFrames);
    }

    public void generateNewPeaks(int ch){
	Peaks peaks=new Peaks(512);
	short[] temp = new short[4096];
	int start=0;

	do{
	    int end = start+4096;
	    if(end>nFrames)
		end=(int)nFrames;
	    getChannelData(temp,ch,start,end);
	    peaks.addData(temp,end-start);
	    start = end;
	}while(start<nFrames);
	peaks.close();            
	audioFileInfo.peaks[ch]=peaks;
    }

    public void generateNewPeaks(){
	for(int ch=0;ch<nChannels;ch++){
	    generateNewPeaks(ch);
	}
    }

    public static void writeShorts(RandomAccessFile file,short[] samples, int start, int end)  throws java.io.IOException{
        int len=end-start;

	file.seek(start*2);

	int pos=0;
	byte[] buf=new byte[4096];
	
	while(pos<len){
	    int endPos=pos+2048;
	    if(endPos>len)
		endPos=len;
	    shortToByte(buf,samples,pos,endPos);
	    file.write(buf,0,(endPos-pos)*2);
	    pos=endPos;
	}
    }

    public static void writeShorts(RandomAccessFile file,short[] samples) throws java.io.IOException{
	writeShorts(file,samples,0,samples.length);
    }

    public void setChannelData(int ch,short[] samples,int start,int end){
        RandomAccessFile file=audioFileInfo.tempFiles[ch];
        if(end>nFrames)
            end=(int)nFrames;
        int len=end-start;

	if(writtenFrames<start){
	    setChannelDataEmpty(writtenFrames,start);
	}

        // write to disk
	try{
	    writeShorts(file,samples,start,end);
	    writtenFrames=Math.max(end,writtenFrames);
	}catch(java.io.IOException e){
	    Warning.print("Not able to seek or write. Disk full? "+" "+ch+", "+nFrames+", "+audioFileInfo.fileName,e);
	}

        // Create new peaks
        if(updatePeaks){
            Peaks peaks=new Peaks(512);

            if(start>0){
                short[] temp=getChannelData(ch,0,start);
                peaks.addData(temp,temp.length);
            }
            peaks.addData(samples,len);
            if(end<nFrames){
                short[] temp=getChannelData(ch,end,(int)nFrames);
                peaks.addData(temp,temp.length);
            }

	    peaks.close();            
	    audioFileInfo.peaks[ch]=peaks;
        }
    }

    public void setChannelData(int ch,short[] samples,int start){
        setChannelData(ch,samples,start,start+samples.length);
    }

    public void setChannelData(int ch,short[] samples){
        setChannelData(ch,samples,0);
    }

    // memory-mapped data ! (mmap)
    public short[] getMemoryMappedChannel(int ch){
	try{
	    setChannelDataEmpty(0,(int)nFrames);
	    RandomAccessFile file=audioFileInfo.tempFiles[ch];
	    file.seek(0);
	    FileChannel fileChannel = file.getChannel();
	    MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,0,nFrames * 2);
	    buffer.load();
	    ShortBuffer shortBuffer = buffer.asShortBuffer();
	    System.out.println("Hasarray: "+shortBuffer.hasArray()+". Direct: "+shortBuffer.isDirect());
	    return shortBuffer.array();
	}catch(Exception e){
	    e.printStackTrace();
	    System.out.println("Error! "+e);
	    return new short[(int)nFrames*2];
	}
    }

    public static void readShorts(RandomAccessFile file, short[] ret, int retpos, int start, int end)  throws java.io.IOException{
        int len=end-start;
	file.seek(start*2);
	
	int pos=0;
	byte[] temp=new byte[4096];
	
	while(pos<len){
	    int endPos=pos+2048;
	    if(endPos>len)
		endPos=len;
	    file.read(temp,0,(endPos-pos)*2);
	    for(int i=pos;i<endPos;i++){
		ret[i+retpos]=byteToShort(temp,(i-pos)*2);
	    }
	    pos=endPos;
	}
    }

    public static short[] readShorts(RandomAccessFile file, int start, int end)  throws java.io.IOException{
	short[] ret=new short[end-start];
	readShorts(file,ret,0,start,end);
	return ret;
    }

    public void getChannelData(short[] ret,int retpos,int ch,int start,int end){
	try{
            RandomAccessFile file=audioFileInfo.tempFiles[ch];
	    readShorts(file,ret,retpos,start,end);
	}catch(java.io.IOException e){
	    e.printStackTrace();
	    System.out.println("2gasldkjf, buf.lenght "+" "+ch+", "+nFrames+", "+audioFileInfo.fileName);
	}
    }

    public void getChannelData(short[] ret,int ch,int start,int end){
	getChannelData(ret,0,ch,start,end);
    }

    public short[] getChannelData(int ch,int start,int end){
        int len=end-start;
        short ret[]=new short[len];
	getChannelData(ret,ch,start,end);
	return ret;
    }

    public short[] getChannelData(int ch){
        return getChannelData(ch,0,(int)nFrames);
    }

    public short getFrame(int pos,int ch){
        short ret=0;
	try{
            RandomAccessFile file=audioFileInfo.tempFiles[ch];
            file.seek(pos*2);

            ret=file.readShort();
	}catch(java.io.IOException e){
	    Warning.print("Not able to seek or write "+" "+ch+", "+nFrames+", "+audioFileInfo.fileName,e);
	}        
        return ret;
    }
    
    public void putBuf(float buf[],byte byteTempBuf[], int ch,int start,int nFrames, boolean reverse){
	try{
	    int numbytes=nFrames*2;

	    /*
	    if(byteBuf==null || byteBuf.length<numbytes) // || byteBuf.length>numbytes*2)
		byteBuf=new byte[numbytes];
	    byteTempBuf=byteBuf;
	    */

	    int bytesleft=numbytes;
	    int bytesread=0;

	    if(start<0){
		Warning.print("Internal error. start<0 in MyAudioFileBuffer.putBuf",null);
		return;
	    }
	    
	    audioFileInfo.tempFiles[ch].seek(start*2);

	    while(true){
		if(bytesread+bytesleft>byteTempBuf.length){
		    Warning.print("Internal error in MyAudioFileBuffer.putBuf "
				  +"bytesread: "+bytesread
				  +", bytesleft: "+bytesleft+", byteTempBuf.length: "
				  +byteTempBuf.length+", "+buf.length+", nFrames:"+nFrames,
				  null);
		    return;
		}
		int read=audioFileInfo.tempFiles[ch].read(byteTempBuf,bytesread,bytesleft);
		if(read==-1){
		    (new Exception()).printStackTrace();
		    String backtrace=(new Exception()).getMessage();
		    String error="Unless your hardrive is full or there could be other causes of disk failure,\n"+
			"please consider this to be an error in DSP. In case you report this error, please include the following\n"+
			"information.\n"+
			"start:"+start+", bytesread: "+bytesread+", nFrames: "+nFrames+", this.nFrames: "+this.nFrames+", wrttenFrames: "+writtenFrames+",\n"+
			backtrace;
		    Warning.print(error,null);
		    return;
		}
		bytesread+=read;
		bytesleft-=read;
		if(bytesleft==0)
		    break;
	    }

	    if(reverse)
		byteToFloatReverse(buf,byteTempBuf,nFrames);
	    else
                FormatUtils.byteToFloat(buf,byteTempBuf,nFrames);
 
	    /*
	      
	    audioFileInfo.tempFiles[ch].seek(start*2);
	    for(int i=0;i<nFrames;i++){
	    buf[i]=audioFileInfo.tempFiles[ch].readShort()/32768.f;
	    
	    */
	    
	}catch(java.io.IOException e){
	    Warning.print("gasldkjf, buf.lenght "+buf.length+", "+ch+", "+start+", "+nFrames+", "+audioFileInfo.fileName,e);
	}catch(NullPointerException e){
	    Warning.print("Nullpointerexception",e);
	    System.out.println("audiofileinfo: "+audioFileInfo);
	    System.out.println("tempfilenames: "+audioFileInfo.tempFileNames);	    
	}
    }


    private static float scale(float x,float x1,float x2,float y1,float y2){
	return y1+( ((x-x1)*(y2-y1))/(x2-x1));
    }


    public void paintWave(Graphics2D g,Color c1, Color c2,int ch,int x1,int y1,int x2,int y2,
			  int startPaint,int endPaint,int quality, boolean paintShade,boolean backwards,float vol){
	audioFileInfo.peaks[ch].paintWave(g,c1,c2,x1,y1,x2,y2,startPaint,endPaint,quality,paintShade,backwards,vol);
    }

    public long getNumBytes(){
	return nChannels*2*nFrames;
    }



    public MyAudioFileBuffer(AudioFileInfo audioFileInfo){
	this.fileName=audioFileInfo.fileName;
	this.audioFileInfo=audioFileInfo;

	nChannels = audioFileInfo.nChannels;
	srate = audioFileInfo.srate;
	nFrames = audioFileInfo.nFrames;
	
        /*
	if(java.lang.Math.abs(mixer.srate-srate)>1){
	    JOptionPane.showMessageDialog(null,"Warning, samplerate for "+audioFileInfo.fileName+" and the mixer does not match. This situation is currently not handled");
	}
        */

	//System.out.println("Channels: "+nChannels+", srate: "+srate+", nFrames: "+nFrames);
	
    }

}


