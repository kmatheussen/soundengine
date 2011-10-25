
/*
  This one is better for volume sliders since the diff value
  decreases when reaching the goal. For example, when going
  from 0.5 to 0.0, get() returns something like this:
  0.5, 0.25. 0.12, 0.6, 0.3, 0.2, 0.1, 0.05, etc.

  'smoothness' must be between 0.000000001 and 0.999999999
  The closer 'smoothness' is to 1, the smoother the returned
  values from calls to 'get' will be.
 */

public class GlideVar2 extends GlideVar{
    float smallest_diff = 0.0001f;
    float smoothness;
    float last_val;

    public float get(){
	float ret = last_val;
	if(ret == to)
	    return to;
	else if(Math.abs(to-last_val) < smallest_diff)
	    last_val = to;
	else
	    last_val = (float) (to*(1.0-smoothness) + last_val*smoothness);
	return ret;
    }

    public GlideVar2(float init_val,float smoothness){
	super(init_val,0.0f); // last argument not used.
	last_val = init_val;
	this.smoothness = smoothness;
    }
}


