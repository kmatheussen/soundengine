
import java.io.*;


// Automatically delete file when object is garbage collected.

class Node{
    Node prev=null;
    Node next=null;
    boolean free=false;
    int start=0;
    int end=0;

    public int size(){
	return end-start;
    }

    public Node(int start,int end){
	this.start=start;
	this.end=end;
    }
}

class TempFileHolder{
    File file;
    int fileSize=0;
    Node nodes=null;

    public synchronized void deleteNode(Node node){
	Node prev=node.prev;
	Node next=node.next;
	if(prev!=null && prev.free==true){
	    prev.end=node.end;
	    prev.next=next;
	    if(next!=null)
		next.prev=prev;
	    deleteNode(prev);
	}else if(next!=null && next.free==true){
	    next.start=node.start;
	    next.prev=prev;
	    if(prev!=null)
		prev.next=next;
	    deleteNode(next);
	}else{
	    node.free=true;
	}
	    
    }

    public synchronized Node newNode(int size){
	Node lastNode=null;
	Node node=nodes;
	Node fit=null;
	while(node!=null){
	    // perfect fit
	    if(node.free=true && node.size()==size){
		return node;
	    }
	    if(node.free==true && node.size()>size){
		if(fit==null || node.size()<fit.size()){
		    fit=node;
		}
	    }
	    lastNode=node;
	    node=node.next;
	}

	// split old node, using best fit
	if(fit!=null){
	    node=fit;
	    int end=node.start+size;
	    Node nextNode=new Node(end,node.end);
	    if(node.next!=null)
		node.next.prev=nextNode;
	    nextNode.prev=node;
	    nextNode.next=node.next;		
	    node.next=nextNode;
	    return node;
	}

	// Make new node and put it at the end of the list.
	node=new Node(fileSize,fileSize+size);

	if(lastNode!=null){
	    lastNode.next=node;
	    node.prev=lastNode;
	}else
	    nodes=node;

	return node;
    }

    public TempFileHolder() throws FileNotFoundException, IOException {
	file=File.createTempFile("DSPTempFile","data");
    }

}

class TempFile extends RandomAccessFile{

    public File file;
    public String name;
    boolean isClosed=false;
    boolean isDeleted=false;

    protected void finalize() throws Throwable{
        try{
            delete();
        } finally {
            super.finalize();
        }
    }

    public void delete(){
        close();
        if(isDeleted==false){
            try{
                System.out.println("    DELETE TEMPFILE "+name);
                file.delete();
            }catch(Exception e){
                System.out.println("grumble2"+e);
            }
            isDeleted=true;
        }
    }

    public void close(){
        if(isClosed==false){
            try{
                super.close();
            }catch(Exception e){
		Warning.print("Unable to close file",e);
            }
            isClosed=true;
        }
    }

    private int shortslen=0;
    public void writeShorts(short[] data){
	try{
	    shortslen=data.length;
	    MyAudioFileBuffer.writeShorts(this,data);
	}catch(Exception e){
	    Warning.print("Could not write to file.",e);
	}
    }

    public short[] readShorts(){
	try{
	    return MyAudioFileBuffer.readShorts(this,0,shortslen);
	}catch(Exception e){
	    Warning.print("Could not read from file.",e);
	    return new short[shortslen];
	}
    }

    TempFile(java.io.File file,int magic) throws FileNotFoundException, IOException {
        super(file,"rw");
        if(magic!=12345)
            System.out.println(" Error, use TempFile.create, not TempFile");
    }
    
    public static TempFile create(String prefix,String suffix)  throws FileNotFoundException, IOException {
        File file=File.createTempFile(prefix,suffix);
        file.deleteOnExit();
        String name=file.getAbsolutePath();
        System.out.println("   CREATE TEMPFILE"+name);
        TempFile ret=new TempFile(file,12345);
        ret.file=file;
        ret.name=name;
        return ret;
    }

    public static TempFile createAndHandleExceptions(String prefix,String suffix){
	try{
	    return create(prefix,suffix);
	}catch(Exception e){
	    Warning.print("Could not create new file.",e);
	    return null;
	}
    }

    public static void main(String []args){
	try{
            for(int i=0;i<500;i++){
                TempFile temp=TempFile.create("test","suff");
                temp=null;
                System.gc();
            }
	}catch(IOException e){
	    e.printStackTrace();
	}
    }
}

