
/* High quality sample rate conversion from CLM.
   Written in C by Bill Schottstaedt. (taken from code written by Perry Cook)
   Translated from C to Java by Kjetil Matheussen.

   To use it, make a subclass of SincSrc and override getInSample(int dir);
   dir==1: forward direction
   dir==-1: backward direction

   Original comments by Bill below:
*/


/* ---------------- src ---------------- */

/* sampling rate conversion */
/* taken from sweep_srate.c of Perry Cook.  To quote Perry:
 *
 * 'The conversion is performed by sinc interpolation.
 *    J. O. Smith and P. Gossett, "A Flexible Sampling-Rate Conversion Method," 
 *    Proc. of the IEEE Conference on Acoustics, Speech, and Signal Processing, San Diego, CA, March, 1984.
 * There are essentially two cases, one where the conversion factor
 * is less than one, and the sinc table is used as is yielding a sound
 * which is band limited to the 1/2 the new sampling rate (we don't
 * want to create bandwidth where there was none).  The other case
 * is where the conversion factor is greater than one and we 'warp'
 * the sinc table to make the final cutoff equal to the original sampling
 * rate /2.  Warping the sinc table is based on the similarity theorem
 * of the time and frequency domain, stretching the time domain (sinc
 * table) causes shrinking in the frequency domain.'
 *
 * we also scale the amplitude if interpolating to take into account the broadened sinc 
 *   this means that isolated pulses get scaled by 1/src, but that's a dumb special case
 */



import java.util.*;


class SincTableHolder{
    static class Table{
	float[] table;
	int width;
    }

    final static int SRC_SINC_DENSITY=1000;

    static ArrayList<Table> tables=new ArrayList<Table>();

    private static float[] create_new_sinc_table(int width){
	Table table=new Table();
	int i, size, padded_size, loc;
	float sinc_freq, win_freq, sinc_phase, win_phase;

	size = width * SRC_SINC_DENSITY;
	padded_size = size + 4;
	sinc_freq = (float) (Math.PI / (float)SRC_SINC_DENSITY);
	win_freq = (float) (Math.PI / (float)size);

	float[] sinc_table=new float[padded_size];
	table.table=sinc_table;
	table.width=width;
	sinc_table[0] = 1.0f;

	for (i = 1, sinc_phase = sinc_freq, win_phase = win_freq; i < padded_size; i++, sinc_phase += sinc_freq, win_phase += win_freq)
	    sinc_table[i] = (float) (Math.sin(sinc_phase) * (0.5f + 0.5f * Math.cos(win_phase)) / sinc_phase);

	tables.add(table);

	return table.table;
    }

    static public float[] getTable(int width){
	for(int i=0;i<tables.size();i++)
	    if(tables.get(i).width==width)
		return tables.get(i).table;

	return create_new_sinc_table(width);
    }
}

class SincSrc{

    final int MUS_MAX_CLM_SINC_WIDTH=65536;
    final float MUS_MAX_CLM_SRC=65536.0f;
    final int SRC_SINC_WIDTH=10;

    private float x;
    private float incr, width_1;
    private int width, lim;
    private int len;
    private float[] data,ks;
    private float[] sinc_table = null;

    
    public float getInSample(int direction){
	return 0.0f;
    }

    public SincSrc(float srate, int width){

	if ((width < 0) || (width > MUS_MAX_CLM_SINC_WIDTH))
	    System.out.println("width arg invalid: "+width);


	int wid;
	if (width <= 0)
	    width = SRC_SINC_WIDTH;
	if (width < (int)(Math.abs(srate) * 2))
	    wid = (int)(Math.ceil(Math.abs(srate)) * 2); 
	else
	    wid = width;

	x = 0.0f;
	incr = srate;
	this.width = wid;
	lim = 2 * wid;
	len = wid * SincTableHolder.SRC_SINC_DENSITY;
	data = new float[lim+1];
	ks = new float[lim];

	sinc_table=SincTableHolder.getTable(wid);

	width_1 = 1.0f - wid;
    }

    void reset(){
	this.x = 0;
	Arrays.fill(data,0.0f);
    }

    float getOutSample(){
	return getOutSample(0.0f);
    }

    float getOutSample(float sr_change){
	float sum = 0.0f, zf, srx, factor;
	int fsx, i, k, loc;
	int xi, xs;
	boolean int_ok = false;

	int lim=this.lim;
	float[] data=this.data;
	float[] ks=this.ks;
	float[] sinc_table=this.sinc_table;

	if (Float.isNaN(sr_change) || Float.isInfinite(sr_change))
	    sr_change = 0.0f;

	if (sr_change > MUS_MAX_CLM_SRC) 
	    sr_change = MUS_MAX_CLM_SRC;
	else{
	    if (sr_change < -MUS_MAX_CLM_SRC) 
		sr_change = -MUS_MAX_CLM_SRC;
	}

	srx = incr + sr_change;

	if (this.x >= 1.0f){
	    fsx = (int)(this.x);
	    this.x -= fsx;
	    /* realign data, reset srp->x */
	    if (fsx > lim){
		int dir = 1;
		if (srx < 0.0f) dir = -1;
		/* if sr_change is so extreme that the new index falls outside the data table, we need to
		 *   read forward until we reach the new data bounds
		 */
		for (i = lim; i < fsx; i++)
		    getInSample(dir);
		fsx = lim;
	    }

	    loc = lim - fsx;

	    if (loc > 0)
		System.arraycopy(this.data,fsx,this.data,0,loc);

	    for (i = loc; i < lim; i++) 
		this.data[i] = getInSample((srx >= 0.0) ? 1 : -1);
	}

	/* if (srx == 0.0) srx = 0.01; */ /* can't decide about this ... */
	if (srx < 0.0f)
	    srx = -srx;

	/* tedious timing tests indicate that precalculating this block in the sr_change=0 case saves no time at all */

	if (srx > 1.0f){
	    factor = 1.0f / srx;
	    /* this is not exact since we're sampling the sinc and so on, but it's close over a wide range */
	    zf = factor * (float)SincTableHolder.SRC_SINC_DENSITY; 
	    xi = (int)zf;
	    int_ok = ((zf - xi) < .001f);
	}else{
	    factor = 1.0f;
	    zf = (float)SincTableHolder.SRC_SINC_DENSITY;
	    xi = SincTableHolder.SRC_SINC_DENSITY;
	    int_ok = true;
	}

	if (int_ok){
	    xs = (int)(zf * (this.width_1 - this.x));
	    i = 0;
	    if (xs < 0)
		for (; (i < lim) && (xs < 0); i++, xs += xi)
		    sum += (data[i] * sinc_table[-xs]); /* fma? */
	    for (; i < lim; i++, xs += xi)
		sum += (data[i] * sinc_table[xs]);

	}else{
	    float x = zf * (width_1 - this.x);

	    if(false){
		// Tried to make it run faster by turning the code into something which is easier to vectorize. Not faster, but almost. -Kjetil
		if(true){
		    i = 0;
		    if(x<0.0f)
			for (; (i < ks.length) && (x<0.0f); i++, x += zf)
			    ks[i] = sinc_table[(int)(-x)];		
		    for (; i < ks.length ; i++, x += zf)
			if(x>sinc_table.length)
			    ks[i] = sinc_table[(int)x];
		}else{
		    for (i = 0; i < ks.length; i++, x += zf)
			if (x < 0)
			    ks[i] = sinc_table[(int)(-x)];
			else
			    ks[i] = sinc_table[(int)x];
		}

		if(false){
		    for (i = 0; i < ks.length && i<data.length; i++)
			ks[i] *= data[i];
		    for (i = 0; i < lim; i++)
			sum += ks[i];
		}else{
		    double sum2=0.0;
		    for (i = 0; i<ks.length; i++)
			sum2 += data[i] * ks[i];
		    sum=(float)sum2;
		}

	    }else if(false){
		// Just an experiment, result could be wrong. Turns out it's not faster anyway, so it's probably useless to continue this branch. -Kjetil
		int int_x=(int)(x*1024.0f);
		int int_zf=(int)(zf*1024.0f);
		double sum2=0.0;
		for (i = 0; i < lim; i++, int_x += int_zf){
		    /* we're moving backwards in the data array, so the sr->x field has to mimic that (hence the '1.0 - this.x') */
		    if (int_x < 0)
			k = -int_x;
		    else
			k = int_x;
		    sum2 += (data[i] * sinc_table[k>>10]);
		    /* rather than do a bounds check here, we just padded the sinc_table above with 2 extra 0's */
		}
		sum=(float)sum2;
	    }else{
		double sum2=0.0; // Java sometimes works faster with doubles than floats. -Kjetil
		/* this form twice as slow because of float->int conversions */
		for (i = 0; i < data.length-1; i++, x += zf){
		    /* we're moving backwards in the data array, so the sr->x field has to mimic that (hence the '1.0 - this.x') */
		    if (x < 0)
			k = (int)(-x);
		    else
			k = (int)x;

		    sum2 += (data[i] * sinc_table[k]);
		    /* rather than do a bounds check here, we just padded the sinc_table above with 2 extra 0's */
		    /* Well, java checks bounds anyway, and there's currently no way to turn it off. -Kjetil*/
		}
		sum=(float)sum2;
	    }
	}

	this.x += srx;
	
	return(sum * factor);
    }


    // For benchmarking
    public static void main(String args[]){
	if(args.length==0)
	    System.out.println("Usage: SincSrc srate width iterations");
	SincSrc sincSrc=new SincSrc(Float.valueOf(args[0]),Integer.valueOf(args[1]));
	int len=Integer.valueOf(args[2]);
	for(int i=0;i<len;i++)
	    sincSrc.getOutSample();
    }
    /*
      4.170
      3.164
     */
}


