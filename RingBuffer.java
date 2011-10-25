

import javax.swing.*;



import java.util.concurrent.atomic.AtomicInteger;

// One thread only to access read(), and one thread only to access write(). (and one thread to nest them all, (not available))
// Ringbuffer.read() will never block.

public class RingBuffer{
    final int bufferSize=(1024*4)/1;
    private int readPos=0;
    private int writePos=0;
    private AtomicInteger unRead=new AtomicInteger();

    private Object buffer[]=new Object[bufferSize];


    public Object read(){
	if(unRead.get()==0)
	    return null;

	Object ret=buffer[readPos];
	readPos++;
	if(readPos==bufferSize)
	    readPos=0;

	unRead.decrementAndGet();

	return ret;
    }

    public int getWriteSpace(){
	return bufferSize - (unRead.get()+3);
    }

    public boolean write(Object object){
	if(unRead.get()+3>bufferSize){
	    return false;
	}
	/*
	if(unRead+3>bufferSize){
	    for(int i=0;i<20;i++){
		try{
		    Thread.currentThread().sleep(100);
		}catch(InterruptedException ie){
		    //If this thread was intrrupted by nother thread 
		}
		if(unRead+3<=bufferSize)
		    break;
	    }
	    if(unRead+3>bufferSize){
		JOptionPane.showMessageDialog(null,"Error. Ringbuffer is full.");
		write(object);
		return;
	    }
	}
	*/
	buffer[writePos]=object;

	unRead.incrementAndGet();

	writePos++;
	if(writePos==bufferSize)
	    writePos=0;

	return true;

    }
    
}

