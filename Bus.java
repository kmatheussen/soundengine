

class Bus{
    int channels;
    AudioBuffers audioBuffers;
    float[][] buffer;

    public void write(float[][] buf,int nFrames,float vol){
	audioBuffers.mixBuffers(buffer,buf,nFrames,vol);
    }

    public float[][] getBuffer(){
	return buffer;
    }

    public void mix(float[][] buf,int nFrames,float vol){
	audioBuffers.mixBuffers(buf,buffer,nFrames,vol);
    }

    public void clear(int nFrames){
	audioBuffers.clearBuffer(buffer,nFrames);
    }

    public Bus(int channels,AudioBuffers audioBuffers){
	this.channels = channels;
	this.audioBuffers = audioBuffers;
	buffer = audioBuffers.getBuffer();
    }
}

