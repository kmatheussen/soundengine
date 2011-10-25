



public class SoundProducer{

    public int startFrame=0;

    public boolean setPlayPos(int newPos){
	System.out.println("Warning, setPlayPos is supposed to be overridden by subclasses.");
	return false;
    }

    public boolean produceMonoSound(float[] buf,int nFrames,AudioBuffers audioBuffers){
	System.out.println("Warning, produceMonoSound is supposed to be overridden by subclasses.");
	return false;
    }
    

    public boolean produceSound(float[][] buf,int nFrames,AudioBuffers audioBuffers){
	System.out.println("Warning, produceSound is supposed to be overridden by subclasses.");
	return false;
	/*
	// Nah....
	produceMonoSound(buf[0],nFrames,audioBuffers);
	return produceMonoSound(buf[1],nFrames,audioBuffers);
	*/
    }

}

