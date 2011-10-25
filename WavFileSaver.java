


/*
  Saving a wave file using sun's AudioSystem is complicated beyond belief.
  (The two most important reasons are that it requires _streams_ (which in many situations are nice, but in this case really stupid),
  and that it's not possible to set size after finished writing (ie. you have to know in advance how many bytes you are going to write)).

  Here is the proper way to do it. (only tested with 16 bits/2/44100, and less than 1 minute.)

  Quite funny that it requires less lines of code, and less time to gather the necesarry information to save the bits manually, rather
  than using sun's API trying (and failing) to do the same thing.
*/


import java.io.*;


public class WavFileSaver{

    public RandomAccessFile file;
    int nFrames=0;
    int channels;
    int bits;
    long samplerate;


    void w(String s) throws IOException{
	file.writeBytes(s);
    }
    void w(int b) throws IOException{
	file.write(b);
    }
    void w(int b,int b2)  throws IOException{
	w(b);
	w(b2);
    }
    void w(int b,int b2,int b3,int b4) throws IOException{
	w(b,b2);
	w(b3,b4);
    }

    void w(short s) throws IOException{
	int b0,b1;

	b0=(s>>>0) & 0xff;
	b1=(s>>>8) & 0xff;
	w(b0,b1);
    }

    void w(long das_l) throws IOException{
	int l=(int)das_l;
	int b0,b1,b2,b3;

	b0=(l>>>0) & 0xff;
	b1=(l>>>8) & 0xff;
	b2=(l>>>16) & 0xff;
	b3=(l>>>24) & 0xff;
	w(b0,b1,b2,b3);
    }


    private void floatToByte(byte[] byteSound, float [] dbuf) {
        int bufsz = dbuf.length;
        int ib=0;
	for(int i=0;i<bufsz;i++) {
	    short y = (short)(32767. * dbuf[i]);
	    byteSound[ib] = (byte) ( ( y >>> 0 ) & 0xff );
	    byteSound[ib+1] = (byte) ( ( y >>> 8 ) & 0xff );
	    ib+=2;
	}
    }

    // buf is an array of array of floats. buf[0] contains channel 0, buf[1] contains channel 1, and so on.
    public void write(float[][] buf,int nFrames)  throws IOException{
	float[] interleavedBuf=new float[nFrames*channels];
	byte[] byteBuf=new byte[nFrames*channels*bits/8];

	for(int i=0;i<nFrames;i++){
	    for(int ch=0;ch<channels;ch++){
		interleavedBuf[i*channels + ch]=buf[ch][i];
	    }
	}
	floatToByte(byteBuf,interleavedBuf);
	file.write(byteBuf);

	this.nFrames+=nFrames;
    }

    public void write(short[][] buf,int nFrames) throws IOException{
	byte[] byteBuf=new byte[nFrames*channels*bits/8];

	if(channels>1){
	    short[] interleavedBuf=new short[nFrames*channels];
	    for(int i=0;i<nFrames;i++){
		for(int ch=0;ch<channels;ch++){
		    interleavedBuf[i*channels + ch]=buf[ch][i];
		}
	    }
	    MyAudioFileBuffer.shortToByte(byteBuf,interleavedBuf,0,nFrames*channels);
	}else{
	    MyAudioFileBuffer.shortToByte(byteBuf,buf[0],0,nFrames);
	}

	file.write(byteBuf);

	this.nFrames+=nFrames;
    }

    public void writeHeader() throws IOException{
	file.seek(0);

	{
	    w("RIFF");
	    w((long)(file.length()-8)); // filelength 
	}

	{
	    w("WAVE");
	    {
		w("fmt ");
		w((long)16); // chunklength
		
		w((short)1); // compression code (WAVE_FORMAT_PCM)
		w((short)channels); 
		w((long)samplerate);
		w((long)(samplerate*channels*bits/8)); // Bytes per second
		w((short)(channels*bits/8)); // Block align 
		w((short)bits);
	    }
	    
	    {
		w("data");
		w((long)(nFrames*channels*bits/8)); // data length.
	    }
	}
    }

    public void close() throws IOException{
	writeHeader(); // Have to write header again since we didn't know nFrames the first timee. (this is one of the things Sun fails to do when using their ridiculous API)
	file.close();
    }

    public static TempFile createEmptyTempFile(long nFrames,int channels,int bits, long samplerate) throws IOException{
        TempFile tempFile=TempFile.create("DSP2-empty-temp", ".wav");
        WavFileSaver wfs=new WavFileSaver(tempFile,channels,bits,samplerate);
        float[][] buf=new float[channels][4096];
        int writtenFrames=0;
        while(writtenFrames<nFrames){
            int towrite=Math.min(4096,(int)(nFrames-writtenFrames));
            wfs.write(buf,towrite);
            writtenFrames+=towrite;
        }
        wfs.close();
        return tempFile;
    }

    private void init(RandomAccessFile dasfile,int channels, int bits, long samplerate) throws IOException{
	this.channels=channels;
	this.bits=bits;
	this.samplerate=samplerate;
        file=dasfile;
	file.setLength(0L);
	writeHeader();        
    }

    public WavFileSaver(RandomAccessFile dasfile,int channels, int bits, long samplerate) throws IOException{
        init(dasfile,channels,bits,samplerate);
    }

    public WavFileSaver(File dasfile,int channels, int bits, long samplerate) throws IOException{
        init(new RandomAccessFile(dasfile,"rw"),channels,bits,samplerate);
    }


    public static void main(String []args){
	try{
	    WavFileSaver wfs=new WavFileSaver(new File("/home/kjetil/test2.wav"),2,16,44100L);
	    int size=2432;
	    float[][] buf=new float[2][size];
	    wfs.write(buf,size);
	    wfs.close();
	}catch(IOException e){
	    e.printStackTrace();
	}
    }

}

