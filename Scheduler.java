
import java.util.*;



public class Scheduler extends SoundProducer{

    ArrayList<SoundProducer> runningProducers=new ArrayList<SoundProducer>();
    ArrayList<SoundProducer> waitingProducers=new ArrayList<SoundProducer>();

    public int playPos=0;

    SoundProducer[] tempProducers=new SoundProducer[10];


    public void add(SoundProducer soundProducer,int startFrame){
	//System.out.println("     Before  scheduler.add at " + (double)startFrame/44100.0 + "s. Added soundobject. New length:"+waitingProducers.size()+"/"+runningProducers.size());

	soundProducer.startFrame=startFrame;
	waitingProducers.add(soundProducer);
	/*
	  I don't know why soundProducer is immediately put into the waiting list, but
	  it could be a good reason for doing so. There should have been a comment about it here. -Kjetil
	 */

	if(soundProducer.setPlayPos(playPos)==true){
	    runningProducers.add(soundProducer);
	    waitingProducers.remove(soundProducer);	    
	}


	//System.out.println("     After scheduler.add. Added soundobject. New length:"+waitingProducers.size()+"/"+runningProducers.size());

	if(tempProducers.length < runningProducers.size()+waitingProducers.size()){
	    tempProducers=new SoundProducer[runningProducers.size()+waitingProducers.size()];
	}
    }

    /*
    public void add(SoundProducer soundProducer){
	//soundProducer.startFrame=startFrame;
	System.out.println("Warning, scheduler.add");
	waitingProducers.add(soundProducer);
    }
    */

    public void delete(SoundProducer soundProducer){
	runningProducers.remove(soundProducer);
	waitingProducers.remove(soundProducer);

	// It might be in tempProducers as well and therefore avoid being garbage collected.
	for(int i=0;i<tempProducers.length;i++){
	    tempProducers[i]=null;
	}
    }

    public void deleteAll(){
	tempProducers=new SoundProducer[10];
	runningProducers.clear();
	waitingProducers.clear();
    }

    public void move(SoundProducer soundProducer,int newTime){
	//System.out.println("Moving to "+newTime/44100.0f);
	if(runningProducers.contains(soundProducer)){
	    runningProducers.remove(soundProducer);
	    waitingProducers.add(soundProducer);
	}

	soundProducer.startFrame=newTime;

	if(soundProducer.setPlayPos(playPos)==true){
	    runningProducers.add(soundProducer);
	    waitingProducers.remove(soundProducer);	    
	}
    }
    

    public SoundProducer[] getSoundProducerArray(ArrayList<SoundProducer> list){
	if(tempProducers.length<list.size()){
	    System.out.println("Error. Gakkegakkegakke!!");
	    tempProducers=new SoundProducer[list.size()*2];
	}
	for(int i=0;i<list.size();i++)
	    tempProducers[i]=list.get(i);
	return tempProducers;
    }

    
    public boolean setPlayPos(int newPos){
	waitingProducers.addAll(runningProducers);
	runningProducers.clear();
	playPos=newPos;
	SoundProducer[] producers=getSoundProducerArray(waitingProducers);
	int len=waitingProducers.size();
	for(int i=0;i<len;i++){
	    if(producers[i].setPlayPos(newPos)==true){
		runningProducers.add(producers[i]);
		waitingProducers.remove(producers[i]);
	    }
	}
	return true;
    }


    // This is far from optimally efficiently programmed, but it seems to be more than good enough.

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){

	float[][] produceBuf=audioBuffers.getBuffer();

	audioBuffers.clearBuffer(buf,nFrames);

	// First call all addBufs methods in the runningProducers list
	//////////////////////////////////////////////////////////////
	
	SoundProducer[] producers=getSoundProducerArray(runningProducers);
	int len=runningProducers.size();

	//System.out.println("gakk: "+playPos/44100.0f+", "+producers.length+", "+runningProducers.size()+", "+waitingProducers.size());

	for(int i=0;i<len;i++){
	    if(producers[i].produceSound(produceBuf,nFrames,audioBuffers)==false){
		runningProducers.remove(producers[i]);
		waitingProducers.add(producers[i]);
		//System.out.println("gakk2: "+len+", "+runningProducers.size()+", "+waitingProducers.size());
	    }
	    audioBuffers.mixBuffers(buf,produceBuf,nFrames);
	}



	// Check if any new nodes should be run for the first time.
	///////////////////////////////////////////////////////////

	int endFrame=playPos+nFrames;
	
	producers=getSoundProducerArray(waitingProducers);
	len=waitingProducers.size();
	for(int i=0;i<len;i++){
	    int pStartFrame=producers[i].startFrame;
	    //System.out.println("producers size: "+len+", playPos: "+playPos+", pStartFrame: "+pStartFrame+", endFrame: "+endFrame);
	    if(pStartFrame>=playPos && pStartFrame<endFrame){
		int skew=pStartFrame-playPos;
		int nFrames2=nFrames - skew;
		if(producers[i].produceSound(produceBuf,nFrames2,audioBuffers)==true){
		    runningProducers.add(producers[i]);
		    waitingProducers.remove(producers[i]);
		}
		audioBuffers.mixBuffers(buf,produceBuf,skew,nFrames2);
	    }
	}

	audioBuffers.returnBuffer(produceBuf);

	playPos+=nFrames;

	return true;
    }

}

