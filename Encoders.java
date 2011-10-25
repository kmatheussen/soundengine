
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;


public class Encoders extends Thread{

    static String os_name;

    static boolean encodingAvailable=true;
	
    static boolean isRunningLinux;
    static boolean isRunningWindows;
    static boolean isRunningSolaris;
    static boolean isRunningMacOSX;

    static boolean showOggDownloadProgressBar=false;
    static boolean showMp3DownloadProgressBar=false;

    static String libDirectory="lib"+File.separator;
    static String tempDirectory="";

    //static String encoderURL="http://www.notam02.no/hurtigmikser/";
    static String encoderURL="http://folk.uio.no/ksvalast/hurtigmikser/";
    static String oggencName;
    static String mp3encName;

    static String oggencFile="";
    static String mp3encFile="";

    static boolean oggencDownloaded=false;
    static boolean mp3encDownloaded=false;
    public static boolean oggencAvailable=false;
    public static boolean mp3encAvailable=false;

    public static Process exec(String command) throws IOException{
	return Runtime.getRuntime().exec(command);
    }
    public static Process exec(String[] command) throws IOException{
	return Runtime.getRuntime().exec(command);
    }

    public static void encodeAndDeleteWav(final String fileType,final File fromFile, final File toFile,final String errorMsg) throws DasException{
	String fileName_temp = toFile.getAbsolutePath();
	if (!fileName_temp.endsWith("."+fileType))
	    fileName_temp = fileName_temp + "." + fileType;
	final String fileName = fileName_temp;

	if (!(new File(fileName)).getParentFile().canWrite())
	    throw new DasException(errorMsg+" "+fileName);


	final ProgressMonitor monitor=new ProgressMonitor(null,"Vennligst vent. Konverterer fil.\nPlease wait, converting file.","",0,100);
	monitor.setMillisToPopup(10);
	monitor.setMillisToDecideToPopup(10);
	monitor.setProgress(50);

	//ProgressMonitor monitor=new ProgressMonitor(null,"converting file","hello",0,100);

	Thread t = new Thread (new Runnable() { 
		public void run() { 
		    

		    try{

			if(fileType.equals("ogg"))
			    showOggDownloadProgressBar=true;
			else
			    showMp3DownloadProgressBar=true;

			while(true){
			    if(fileType.equals("ogg") && oggencDownloaded==true)
				break;
			    if(fileType.equals("mp3") && mp3encDownloaded==true)
				break;
			    if(fileType.equals("ogg") && oggencAvailable==false)
				break;
			    if(fileType.equals("mp3") && mp3encAvailable==false)
				break;
			    System.out.println("venter");
			    try{
				sleep(500);
			    }catch(Exception e){
			    }
			}

			{
			    if(fileType.equals("ogg") && oggencAvailable==false){
				JOptionPane.showMessageDialog(null,errorMsg+" "+fileName);
				return;
			    }
			    if(fileType.equals("mp3") && mp3encAvailable==false){
				JOptionPane.showMessageDialog(null,errorMsg+" "+fileName);
				return;
			    }
			}


			{
			    Process process;
			    String command;
			    String[] commands;
			    {
				//String to=(new File(fileName)).getAbsolutePath().replace(" ","\\ ");
				String to=(new File(fileName)).getAbsolutePath();
				String from=fromFile.getAbsolutePath();
				if(fileType=="ogg"){
				    commands=new String[5];
				    commands[0]=oggencFile;
				    commands[1]="--quiet";
				    commands[2]="-o";
				    commands[3]=to;
				    commands[4]=from;
				    //command="sh -c \""+oggencFile+" --quiet -o " + to + " " + from +"\"";
				    //command="sh -c \""+oggencFile+" --quiet -o " + to + " " + from +"\"";
				}else{
				    commands=new String[4];
				    commands[0]=mp3encFile;
				    commands[1]="--nohist";
				    commands[2]=from;
				    commands[3]=to;
				    //command=mp3encFile+" --nohist " + from + " " + to;
				}
			    }

			    System.out.println("Executing command -"+commands+"- exists?:"+fromFile.exists());
			    process=exec(commands);

			    InputStream stream=process.getErrorStream();
			    
			    int b;
			    do{
				b=stream.read();
				monitor.setProgress(b<100?b:99);
				System.out.println("ai: "+((char)b));
			    }while(b!=-1);

			    System.out.println("Executed command -"+commands);

			    process.waitFor();

			    if((new File(fileName)).exists()==false){
				JOptionPane.showMessageDialog(null,errorMsg+" "+fileName+" (enkoding feilet / encoding failed)");
			    }
			}
			
		    }catch(Exception e){
			JOptionPane.showMessageDialog(null,errorMsg+" "+fileName+" - \n\n "+e);
			try{
			    sleep(50000);
			}catch(Exception e2){
			}
		    }

		    monitor.close();
		    fromFile.delete();
		}
	    }
	    ); 
	t.start();
	    

	//monitor.close();

    }

    public static boolean downloadFileFromURL(String fetchUrl, File saveFile, String fileType)
    	throws IOException,FileNotFoundException {
	
	HttpURLConnection c;

	//save file    	
	URL url = new URL(fetchUrl);
	c = (HttpURLConnection)url.openConnection();
	
	//connect
	c.connect();

	//input stream
	BufferedInputStream in = new BufferedInputStream(c.getInputStream());
	
	//save the file
	OutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile));
	byte[] buf = new byte[256];
	int n = 0;

	{
	    int length=c.getContentLength();
	    ProgressMonitor progressMonitor=null;

	    if(false){
	    }

	    int pos=0;
	    
	    while ((n=in.read(buf))>=0) {
		out.write(buf, 0, n);
		pos+=n;
		/*
    <jar href="lib/common/jl1.0.jar"/>
    <jar href="lib/common/mp3spi1.9.4.jar"/>
    <jar href="lib/common/jogg-0.0.7.jar"/>
    <jar href="lib/common/jorbis-0.0.15.jar"/>
    <jar href="lib/common/vorbisspi1.0.2.jar"/>
    <jar href="lib/common/jflac-1.2.jar"/>
    <jar href="lib/common/tritonus-vorbis.jar"/>
    <jar href="lib/common/tritonus_remaining.jar"/>
    <jar href="lib/common/tritonus_share.jar"/>

    <jar href="lib/common/jl1.0.jar"/>
    <jar href="lib/common/jorbis-0.0.15.jar"/>
		*/

		//System.out.println("gakk6 "+pos);

		if( (   fileType.equals("ogg") && showOggDownloadProgressBar==true)
		    || (fileType.equals("mp3") && showMp3DownloadProgressBar==true)
		    )
		    {
			if(progressMonitor==null){
			    progressMonitor=new ProgressMonitor(null,"Please wait, dowloading "+fileType+" encoder.","",0,length==-1?2500000:length);
			    progressMonitor.setMillisToPopup(10);
			    progressMonitor.setMillisToDecideToPopup(10);
			    progressMonitor.setProgress(50);
			}
			progressMonitor.setProgress(pos);
		    }
	    }
	    if(progressMonitor!=null)
		progressMonitor.close();
	}
	
	out.flush();
	out.close();
	
	return true;	    
    }

    public File downloadFileFromURL2(String fetchUrl,String filename, String fileType){
	File file=new File(filename);
	try{
	    downloadFileFromURL(fetchUrl,file,fileType);
	}catch(IOException e){
	    file=null;
	}
	return file;
    }

    static public boolean makeFileExecutable(String file){
	try{
	    exec("chmod a+rx "+file);
	}catch(Exception e){
	    return false;
	}
	return true;
    }

    public void run(){

	Thread t = new Thread (new Runnable() { 
		public void run() {
		    System.out.println("Starting to download oggenc "+oggencFile);
		    oggencAvailable=true;
		    File oggFile=downloadFileFromURL2(encoderURL+oggencName,oggencFile,"ogg");
		    System.out.println("Finished downloading oggenc "+oggencFile);
		    if(oggFile!=null){
			if(isRunningLinux || isRunningMacOSX)
			    if(makeFileExecutable(oggencFile)==false)
				oggencAvailable=false;
			oggFile.deleteOnExit();
			oggencDownloaded=true;
		    }else{
			/*
			JOptionPane.showMessageDialog(null,
						      "Kunne ikke laste ned ogg-enkoder. Enkoding av ogg-filer er ikke mulig\n"+
						      "Could not download ogg encoder. Encoding of ogg files is not possible.");
			oggencAvailable=false;
			*/
		    }
		}
	    });

	if(oggencAvailable==false)
	    t.start();

	if(mp3encAvailable==false){
	    mp3encAvailable=true;
	    System.out.println("Starting to download mp3enc "+mp3encFile);
	    File mp3File=downloadFileFromURL2(encoderURL+mp3encName,mp3encFile,"mp3");
	    System.out.println("Finished downloading mp3enc "+mp3encFile);
	    if(mp3File!=null){
		if(isRunningLinux || isRunningMacOSX)
		    if(makeFileExecutable(mp3encFile)==false)
			mp3encAvailable=false;
		
		mp3File.deleteOnExit();
		mp3encDownloaded=true;
	    }else{
		/*
		JOptionPane.showMessageDialog(null,
					      "Kunne ikke laste ned mp3-enkoder. Enkoding av mp3-filer er ikke mulig\n"+
					      "Could not download mp3 encoder. Encoding of mp3 files is not possible.");
		*/
		mp3encAvailable=false;
	    }
	}

	if(oggencAvailable==false){
	    try{
		t.join();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}
    }
	    
    static void init(){
	File tempFile=null;

	try{
	    tempFile=File.createTempFile("DSP",".tmp");
	}catch(IOException e){
	    JOptionPane.showMessageDialog(null,"IOException while creating tempfile.");
	}

	if(tempFile!=null)
	    tempDirectory=tempFile.getParentFile().toString()+File.separator;

	os_name=java.lang.System.getProperty("os.name");

	isRunningLinux=os_name.equals("Linux");
	isRunningWindows=java.io.File.separator.equals("\\");
	isRunningSolaris=os_name.equals("Solaris");
	
	isRunningMacOSX
	    =  java.io.File.separator.equals("/")
	    && java.lang.System.getProperty("path.separator").equals(":")
	    && !isRunningLinux
	    && !isRunningWindows
	    && !isRunningSolaris;
	

	if(false){
	}else if(isRunningLinux){
	    oggencName="oggenc_linux";
	    mp3encName="lame_linux";
	}else if(isRunningWindows){
	    oggencName="oggenc.exe";
	    mp3encName="lame.exe";
	}else if(isRunningMacOSX){
	    oggencName="oggenc_macosx";
	    mp3encName="lame_macosx";
	}else{
	    encodingAvailable=false;
	}

	if(encodingAvailable){
	    if((new File(libDirectory+oggencName)).exists()){
		oggencAvailable=true;
		oggencDownloaded=true;
		oggencFile=libDirectory+oggencName;
		makeFileExecutable(oggencFile);
		//JOptionPane.showMessageDialog(null,"ogg i lib");
	    }else{
		oggencFile=tempDirectory+oggencName;
	    }
	    if((new File(libDirectory+mp3encName)).exists()){
		mp3encAvailable=true;
		mp3encDownloaded=true;
		mp3encFile=libDirectory+mp3encName;
		makeFileExecutable(mp3encFile);
		//JOptionPane.showMessageDialog(null,"lame i lib");
	    }else{
		mp3encFile=tempDirectory+mp3encName;
	    }
	    Encoders encoders=new Encoders();
	    encoders.start();
	}
    }
}


