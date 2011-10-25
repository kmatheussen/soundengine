
import java.util.*;
import java.awt.geom.*;


// Pan12 (mono(1)->stereo(2)) panner

class Pan12 extends Envelope{

    private final float sqrt2=(float)java.lang.Math.sqrt(2.0f);
    private final float pan_scaleval=2.0f-(2*sqrt2);
    private final float one_minus_panscale=1.0f-pan_scaleval;

    public void apply(float[][] buf,int start,int end,int envelopeLength,AudioBuffers audioBuffers){
	int len=end-start;
	float[][] bufs=audioBuffers.getBuffer();
	float[] envData=bufs[0];

	getEnvelopeData(envData,start,end,envelopeLength);

	for(int i=0;i<len;i++){
	    float x           = scale(envData[i],-1,1,0,1);

            if(x<0.0f || x>1.0f){
                System.out.println("Pannegakk!!!: "+envData[i]);
            }

	    float one_minus_x = 1.0f-x;
	    float right       = one_minus_x*((pan_scaleval*one_minus_x)+one_minus_panscale);
	    float left        = x * ( (pan_scaleval*x) + one_minus_panscale);

	    buf[0][i]*=left;
	    buf[1][i]*=right;
	}

	audioBuffers.returnBuffer(bufs);
    }

    public void resetBreaks(){
        super.resetBreaks();
        breakVol[0]=0.0f;
        breakVol[1]=0.0f;
    }
    
    public Pan12(){
	super();
    }
}

