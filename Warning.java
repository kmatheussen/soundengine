import javax.swing.*;

class Warning{
    public static void print(String message,Exception e){
	if(e!=null){
	    e.printStackTrace();
	    message=message+".\n(exception: "+e.toString()+")\n"+"getMessage():"+e.getMessage();
	}
	System.out.println(message);
	JOptionPane.showMessageDialog(null,"Warning: "+message);
	//JOptionPane.showMessageDialog(MikserGUI.uberThis,"Warning: "+message);
    }
    public static void print(String message){
	print(message,null);
    }
}
