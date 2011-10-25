
class DasException extends Exception
{
    public DasException() {}
    public DasException(String msg)
    {
	super(msg);
	this.printStackTrace();
    }
}

