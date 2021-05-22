import ithakimodem.*;
import java.util.*;
import java.io.*;
import java.lang.Math;

public class userApplication {

	public static void main(String[] args )throws IOException {
		
		String echoRequest = "E2540";
		String clearImageRequest = "M7426";
		String errorImageRequest ="G4796";
		String gpsRequest ="P4886";
		String ACK ="Q6061";
		String NACK = "R2534";
	
		Modem modem=new Modem(80000);
		modem.setTimeout(2000);
		modem.open("ithaki");
		for (;;) {
			try {
				int k=modem.read();
				if (k==-1) break;
				System.out.print((char)k);
			 } catch (Exception x) {
			 		break;
			 	}
			}
		
		
		getClearImage(modem,clearImageRequest);
		
		getErrorImage(modem,errorImageRequest);
		
		getEchoPackets(modem,echoRequest,300000);
		
		getGPS(modem,gpsRequest);
		
		ARQ(modem,ACK,NACK,300000);
		
		
	}
	
	
	//END OF MAIN!
	
	//Methods
	
	
	public static void getClearImage (Modem modem,String clearImageRequest)throws IOException { //Outputs an Image without errors from Egnatia
		System.out.println("Clear image loading...");
		modem.write((clearImageRequest + "\r").getBytes());
		OutputStream cImageout = new FileOutputStream("E1.jpeg");
		
		for (;;) {
		try {
			int k=modem.read();
			if (k==-1) break;
			cImageout.write(k);
		 } finally {
			 
		 	}
		 }
		cImageout.close();
		System.out.println("Clear image loaded!\n");
	}
	
	public static void getErrorImage (Modem modem,String errorImageRequest)throws IOException { //Outputs an Image with errors from Egnatia
		System.out.println("Error image loading...");
		modem.write((errorImageRequest + "\r").getBytes());
		
		OutputStream eImageout = new FileOutputStream("E2.jpeg");
		for (;;) {
		try {
			int k=modem.read();
			if (k==-1) break;
			eImageout.write(k);
		 } finally {
			 
		 	}
		 }
		eImageout.close();
		System.out.println("Error image loaded!\n");
	}
	
	public static void getGPS (Modem modem,String gpsRequest)throws IOException { //Outputs an image with 5 traces 15 seconds apart. WARNING! Have to define the number of traces with traceNumber and traceNumberString 
		
		System.out.println("GPS loading...");
		
		int traceNumber = 70;
		String traceNumberString = "R=1010070";
		
		modem.write((gpsRequest+ traceNumberString + "\r").getBytes());
		String response="";
		OutputStream out = new FileOutputStream("gps.txt");
		for (;;) {
		try {
			int k=modem.read();
			response += (char)k;
			out.write((char)k);
			if ((response.indexOf("STOP ITHAKI GPS TRACKING")) > 0) break;
		 } finally {
			 
		 	}
		 }
		out.close();
		
		System.out.println("GPS loaded!\n");
		
		
		//Automatic selection of traces
		
		
		System.out.println("GPS Image info loading...");
		int commaCounter= 0;
		int dotCounter = 0;
		int unitCounter = 0;
		int[] gpsTime = new int[traceNumber];
		String[] gpsLat = new String[traceNumber];
		String[] gpsLong = new String[traceNumber];
		
		modem.write((gpsRequest + traceNumberString + "\r").getBytes());
		
		int timeResponse=0;
		String latResponse="";
		String longResponse="";
		
		for (;;) {
		try {
			int k=modem.read();
			
			if((char)k == '$') { //Initialize all counters for each GPS unit
				unitCounter++;
				timeResponse++;
				latResponse="";
				longResponse="";
				commaCounter = 0;
				dotCounter=0;
			}
			
			if((char)k == '.') dotCounter++;
			
			if((char)k == ',') commaCounter++;
			
			
			if(commaCounter == 1 && dotCounter == 1) { //Write time down when it's done. Since every trace has 1 sec difference, it just counts from one.
				gpsTime[unitCounter - 1]=timeResponse;
			}
			
			if(commaCounter==2 && (char)k != '.' && (char)k != ',' ) {
				latResponse += (char)k;
			}
			if(commaCounter == 3) {
				int secs = (int)(Integer.parseInt(latResponse.substring(4, 8)) * 0.006);
				gpsLat[unitCounter-1] = latResponse.substring(0,4) + String.valueOf(secs);
			}
			if(commaCounter==4 && (char)k != '.' && (char)k != ',') {
				longResponse += (char)k;
			}
			if(commaCounter == 5) {
				int secs = (int)(Integer.parseInt(longResponse.substring(5, 9)) * 0.006);
				gpsLong[unitCounter-1] = longResponse.substring(1,5) + String.valueOf(secs);
			}
			
			
			
			if (k == -1) break;
		 } finally {
			 
		 	}
		 }
		
		
		String Tpar = "T=" + gpsLong[0] + gpsLat[0];
		int Tcounter = 1;
		
		for(int i = 0; i<traceNumber;i++) {
			for(int j = 0; j<traceNumber ; j++) {
				if(gpsTime[j]-gpsTime[i] == 15 ) {
					Tpar += "T=" +gpsLong[j]+gpsLat[j];
					Tcounter++;
					i=j-1;
					break;
				}
			}
			if(Tcounter == 4) break;
		}
		System.out.println("GPS Image info loaded!\n");
		
		
		System.out.println("GPS image loading...");
		modem.write((gpsRequest+Tpar+"\r").getBytes());
		OutputStream gpsImageout = new FileOutputStream("M1.jpeg");
		for (;;) {
		try {
			int k=modem.read();
			if (k==-1) break;
			gpsImageout.write(k);
		 } finally {
			 
		 	}
		 }
		gpsImageout.close();
		System.out.println("GPS image loaded!\n");
	}
	
	public static void getSingleEchoPacket(Modem modem,String echoRequest)throws IOException { //Requests a single echo packet, but doesn't save it anywhere. Used purely for timing purposes.
		modem.write((echoRequest + "\r").getBytes());
		String packet = "";
		for (;;) {
		try {
			int k=modem.read();
			packet += (char)k;
			if (packet.indexOf("PSTOP")>=0) break;
		 } finally {
			 
		 	}
		 }
	}
	
	public static void getEchoPackets(Modem modem,String echoRequest,int time)throws IOException { //Requests and times echo packets for time specified, outputs a text file in format of "time,response time" for each packet.
		long startFiveMins = System.currentTimeMillis();
		String timetxt="";
		System.out.println("Packets loading...");
		while(System.currentTimeMillis()-startFiveMins<time){
			long startTimer=System.currentTimeMillis();
			getSingleEchoPacket(modem,echoRequest);
			long totalTime = System.currentTimeMillis() - startTimer;
			timetxt += ((System.currentTimeMillis()-startFiveMins)/1000)+", "+ totalTime + "\n";
		}
		stringToTextFile("forG1",timetxt);
		System.out.println("Packets loaded!\n");
	}
	
	public static void stringToTextFile(String name,String input)throws IOException { //Outputs input String as a text file with the specified name.
		BufferedWriter writer = new BufferedWriter(new FileWriter(name +".txt"));
	    writer.write(input);
	     
	    writer.close();
	}
	
	public static String XOR(String one,String two) { //Applies XOR on two Strings and returns the result.
		String result="";
		for(int i=0;i<7;i++) {
			if(one.charAt(i)==two.charAt(i)) {
				result += "0";
			}else {
				result +="1";
			}
		}
		return result;
	}
	
	public static int binaryToDecimal(String binary) { //Converts a binary string to int decimal. Outputs int decimal.
		int result=0;
		for(int i =0; i<binary.length();i++) {
			if(binary.charAt(i)=='1') {
				result += (int)Math.pow(2, binary.length()-i-1);
			}
		}
		return result;
	}
	
	public static void ARQ(Modem modem,String ACK,String NACK,int time)throws IOException { //Gets package and checks FCS code and accepts it or rejects it
		
		long startFiveMins = System.currentTimeMillis();
		System.out.println("ARQ working...");
		boolean accepted = true;
		int success = 0; 
		int failure =0;
		int failurePerPackage =0;
		Map<Integer,Integer> failureMap = new HashMap<Integer,Integer>();
		
		String timeTxtFile="Total Time, Response Time, Retransmissions\n"; // For G2 diagram
		String errorBitsFile ="";
		String retransmissionFile=""; // For G3 diagram
		
		long startResponseTime = 0;  //To measure the time to run.
		
		while(System.currentTimeMillis()-startFiveMins<time || accepted == false ) {
			
			String packet="";
			int startChar =0;
			int stopChar = 0;
			int counter =0;
			int FCS=0;
			String FCString="";
			int FCSCounter =0;
			String[] binaryTable = new String[16];

			 
			if(accepted == true) {
				modem.write((ACK+"\r").getBytes());
				startResponseTime = System.currentTimeMillis(); // To measure the response time of each packet
			}else {
				modem.write((NACK+"\r").getBytes());
			}
			for (;;) {
				try {
					int k=modem.read();
					packet += (char)k;
					
					if((char)k == '>') stopChar++;
					
					if(startChar == 1 && stopChar ==0 ) {
						String binary = Integer.toBinaryString((char)k);
						binaryTable[counter] = binary;
						counter++;
					}
					if((char)k == '<') startChar++;
					
					if(stopChar==1) FCSCounter++;
					
					
					if(FCSCounter > 2 && FCSCounter < 6 ) {
						FCString += (char)k;
					}
					if (packet.indexOf("PSTOP") >= 0) {
						break;
					}
				 } catch (Exception x) {
				 		break;
				 	}
				}
			
			String result = XOR(binaryTable[0],binaryTable[1]);
			for(int i =2;i<16;i++) {
				result = XOR(result,binaryTable[i]);
			}
			FCS = Integer.parseInt(FCString);
			int decResult = binaryToDecimal(result);
			if(decResult == FCS) {
				accepted = true;
				success++;
				
				long totalResponseTime = System.currentTimeMillis() - startResponseTime;  //To measure response time of packet.
				long totalTime = (System.currentTimeMillis()- startFiveMins)/1000;		//To measure time running up to this point. 
				timeTxtFile += (totalTime+", "+ totalResponseTime + "\n");
				
				if(failureMap.containsKey(failurePerPackage) && failurePerPackage != 0) {
					failureMap.replace(failurePerPackage, failureMap.get(failurePerPackage)+1);
				}else if(failurePerPackage != 0) {
					failureMap.put(failurePerPackage, 1);
				}
				
				failurePerPackage = 0;
				
			}else {
				accepted = false;
				failure++;
				failurePerPackage++;
			}

		}
		
		System.out.println("ARQ finished!\n");
		
		System.out.println("Packets successfully loaded: " + success);
		errorBitsFile += "Packets successfully loaded: " + success + "\n";
		
		System.out.println("Packets rejected and repeated: " + failure);
		errorBitsFile += "Packets rejected and repeated: " + failure + "\n";
		
		System.out.println("Total requests: " + (success + failure));
		errorBitsFile += "Total requests: " + (success + failure) + "\n";
		
		retransmissionFile = "Retransmissions number, Times occured\n";
		retransmissionFile += "0, " + success+"\n";
		for(int i=0;i<failureMap.size();i++) {
			if(failureMap.containsKey(i+1)) {
				retransmissionFile += (i+1) + ", " + failureMap.get(i+1)+"\n";
			}else continue;
		}
		
		stringToTextFile("forG2",timeTxtFile);
		stringToTextFile("forBER",errorBitsFile);
		stringToTextFile("forG3",retransmissionFile);
		
		
	}
	
}


