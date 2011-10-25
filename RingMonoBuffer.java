

// Note that get and push are not thread safe. RingMonoBuffer is not supposed (in the current state) to be called from different threads.

public class RingMonoBuffer{

    public float[] buf;
    int maxSize;
    int currSize;
    public int readPos=0;
    public int writePos=0;


    public void get(float[] buf,int nFrames){

	//if(nFrames+currSize > maxSize){
	if(nFrames > currSize){
	    System.out.println("Error. RingMonoBuffer.java/get: Ringbuffer not full enough. This is not supposed to happen. nFrames: "+nFrames+", currSize: "+currSize+", maxSize: "+maxSize);
	    return;
	}

	if(readPos+nFrames <= maxSize){

	    System.arraycopy(this.buf,readPos,buf,0,nFrames);
	    readPos+=nFrames;

	}else{

	    int firstLength=maxSize-readPos;
	    int secondLength=nFrames-firstLength;

	    System.arraycopy(this.buf,readPos, buf,0,           firstLength);
	    System.arraycopy(this.buf,0,       buf,firstLength, secondLength);

	    readPos=secondLength;
	}

	currSize-=nFrames;
	if(readPos==maxSize)
	    readPos=0;
    }



    public void push(float[] buf,int nFrames){
	if(nFrames+currSize > maxSize){
	    System.out.println("Error. RingMonoBuffer.java/push: Ringbuffer full. This is not supposed to happen. nFrames: "+nFrames+", currSize: "+currSize+", maxSize: "+maxSize);
	    return;
	}

	if(writePos+nFrames <= maxSize){

	    System.arraycopy(buf,0,this.buf,writePos,nFrames);
	    writePos+=nFrames;

	}else{

	    int firstLength=maxSize-writePos;
	    int secondLength=nFrames-firstLength;

	    System.arraycopy(buf,0,           this.buf,writePos, firstLength);
	    System.arraycopy(buf,firstLength, this.buf,0,        secondLength);

	    writePos=secondLength;
	}

	currSize+=nFrames;
	if(writePos==maxSize)
	    writePos=0;
	
    }

    public int getCurrSize(){
	return currSize;
    }

    public RingMonoBuffer(int maxSize){
	this.maxSize=maxSize;
	buf=new float[maxSize];
    }


}

