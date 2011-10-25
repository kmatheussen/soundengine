
/*
  Simple interpolation where set and get can be called at any time.
  Furthermore, the difference between two returned (i.e. 'get()') values will
  never be larger than 'largest_diff'.

  Note that the implementation is the simplest possible. There are certainly
  better ways to do this.

  For volume variables, use GlideVar2 instead.
*/


public class GlideVar{  
    float largest_diff;
    float current;
    float to;

    public float get(){
	if(current==to){
	    //
	}else if(to > current){
	    current += largest_diff;
	    if(current>to)
		current = to;
	}else if(to < current){
	    current -= largest_diff;
	    if(current<to)
		current = to;
	}

	return current;
    }

    public void get(float[] data,int nFrames){	
	for(int i=0;i<nFrames;i++){
	    data[i] = get();
	}
    }

    public float get_goal(){	
	return to;
    }

    public void set_now(float new_val){
	to = new_val;
	current = new_val;
    }

    public void set(float new_val){
	to = new_val;
    }

    public GlideVar(float init_val,float largest_diff){
	this.largest_diff=largest_diff;
	current = init_val;
	to = init_val;
    }
}

