
class Message{
    int type;
    int something_int;
    float something_float;
    SoundProducer something_SoundProducer;
    Object something_Object;

    // So that stuff can be garbage collected.
    public void clear(){
	something_SoundProducer=null;
	something_Object=null;
    }

}
