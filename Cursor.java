// Code taken from Hurtigmikser. -Kjetil.


public class Cursor{
    DasMixer mixer;

    public void request_paint(){
	System.out.println("Cursor.request_paint(). This method should probably be overridden.");
    }

    public Cursor(DasMixer mixer){
	this.mixer=mixer;
    }

}


