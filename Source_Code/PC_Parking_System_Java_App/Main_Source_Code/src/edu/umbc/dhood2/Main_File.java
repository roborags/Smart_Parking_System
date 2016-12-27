package edu.umbc.dhood2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.ejml.equation.Equation; 
import org.ejml.equation.Sequence; 
import org.ejml.simple.SimpleMatrix; 

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
 

public class Main_File {
	
	 private enum STATE { 
		  X_POS, 
		  X_POS_VELOCITY, 
		  Y_POS, 
		  Y_POS_VELOCITY, 
		  Z_POS, 
		  Z_POS_VELOCITY, 
		  X_AXIS_ROTATION, 
		  X_AXIS_ROTATION_RATE, 
		  Y_AXIS_ROTATION, 
		  Y_AXIS_ROTATION_RATE, 
		  Z_AXIS_ROTATION, 
		  Z_AXIS_ROTATION_RATE 
		 }; 
		 
	private SimpleMatrix xSimpleMatrix; 
	
	private SimpleMatrix AccSimpleMatrix; 
	  
	private SimpleMatrix rSimpleMatrix = new SimpleMatrix(12, 1); 
	  
	private SimpleMatrix aSimpleMatrix = SimpleMatrix.identity(12); 
	 
	private SimpleMatrix bSimpleMatrix = SimpleMatrix.identity(12); 
	   
	private SimpleMatrix cSimpleMatrix = new SimpleMatrix(12, 6); 
	 
	private SimpleMatrix zSimpleMatrix = new SimpleMatrix(12, 1); 
	
	private long measUpateTime; 
	 
	private double alpha = 1.0; 
	
	private Kalman_Filter_Distance Axis_Calc_Distance; 
	 
	private Equation equations = new Equation(); 
	
	private Sequence projectStateEquation; 
	 
	private Sequence updateEquation; 
	
	private static final String COMMA_DELIMITER = ",";
	
    private static final String NEW_LINE_SEPARATOR = "\n";
    
    /*********  work only for Dedicated IP ***********/
    static String FTP_HOST= "153.92.11.11";

    static String FTP_HOST_LINK= "files.000webhost.com";

    /*********  FTP USERNAME ***********/
    static String FTP_USER = "roborags";

    /*********  FTP PASSWORD ***********/
    static String FTP_PASS ="Rags@420";
    
    static String FileToDownload = "Saved_Sensor_Values.csv";
    
    private String File_Path;
    
    private static final int CSV_Sensor_ID = 0;
    private static final int CSV_Time_HM = 1;
    private static final int CSV_Time_MS = 2;
    private static final int CSV_X_Axis_Val = 3;
    private static final int CSV_Y_Axis_Val = 4;
    private static final int CSV_Z_Axis_Val = 5;
    
    private double Straight_distance = 0;
    private int Turn_Occurred = 0;
    private int turn_count = 1;
    
    public double Gps_LAT = 0; 
	public double Gps_LONG = 0; 
	public double Gps_ALT = 0; 
    
	public static void main(String[] args) 
	{
		Main_File Run_Prog = new Main_File();
		
		File fileIn = null;
		Run_Prog.File_Path = System.getProperty("user.dir");
		try
		{
	        fileIn = new File(Run_Prog.File_Path, FileToDownload);
	        if (!fileIn.exists()) 
	        {
	            fileIn.createNewFile();
	        }
		}
		catch (IOException e) 
		{
			System.out.print("File Open Error : "+e+"\n");
		}
	    
		Run_Prog.DownloadFile(fileIn);
		
		System.out.println("File Downloaded ");
		
		Run_Prog.KalmanFilter();
		
		System.out.println("Kalman filter intiated and ready to start ");
		
		Run_Prog.ReadFromFile(fileIn);
		
		System.out.println("Kalman filter calculation finished ");
		
		System.out.println("X distance is = "+Run_Prog.Axis_Calc_Distance.xDistance);
		System.out.println("Y distance is = "+Run_Prog.Axis_Calc_Distance.yDistance);
		System.out.println("Z distance is = "+Run_Prog.Axis_Calc_Distance.zDistance);
		
		if(Objects.equals(Run_Prog.Turn_Occurred,1))
		{
			System.out.println("Right Turn Occurred");
			if(Run_Prog.Straight_distance > 5)
			{
				System.out.println("Position R2");
				System.out.println("R2 GPS location is Lat = "+Run_Prog.Gps_LAT+" Long = "+Run_Prog.Gps_LONG+" Alt = "+Run_Prog.Gps_ALT);
			}
			else
			{
				System.out.println("Position R1");
				System.out.println("R1 GPS location is Lat = "+Run_Prog.Gps_LAT+" Long = "+Run_Prog.Gps_LONG+" Alt = "+Run_Prog.Gps_ALT);
			}
		}
		else if(Objects.equals(Run_Prog.Turn_Occurred,2))
		{
			System.out.println("Left Turn Occurred");
			if(Run_Prog.Straight_distance > 5)
			{
				System.out.println("Position L2");
				System.out.println("L2 GPS location is Lat = "+Run_Prog.Gps_LAT+" Long = "+Run_Prog.Gps_LONG+" Alt = "+Run_Prog.Gps_ALT);
			}
			else
			{
				System.out.println("Position L1");
				System.out.println("L1 GPS location is Lat = "+Run_Prog.Gps_LAT+" Long = "+Run_Prog.Gps_LONG+" Alt = "+Run_Prog.Gps_ALT);
			}
		}
		else
			System.out.println("Error finding Position : No turn occurred");

		
		
	}
	
    public void CalculateDistance(Kalman_Filter_Val CurrMeasureVal)
    {
    	Axis_Calc_Distance.xVelocity += ((Math.abs(Math.abs(xSimpleMatrix.get(0, 0)) - Math.abs(AccSimpleMatrix.get(0,0)))) * (CurrMeasureVal.CTime * 0.001)) ;
    	Axis_Calc_Distance.yVelocity += ((Math.abs(Math.abs(xSimpleMatrix.get(2, 0)) - Math.abs(AccSimpleMatrix.get(1,0)))) * (CurrMeasureVal.CTime * 0.001));
    	Axis_Calc_Distance.zVelocity += ((Math.abs(Math.abs(xSimpleMatrix.get(4, 0)) - Math.abs(AccSimpleMatrix.get(2,0)))) * (CurrMeasureVal.CTime * 0.001));
    	
    	Axis_Calc_Distance.xDistance += (Axis_Calc_Distance.xVelocity * (CurrMeasureVal.CTime * 0.001)) ;
    	Axis_Calc_Distance.yDistance += (Axis_Calc_Distance.yVelocity * (CurrMeasureVal.CTime * 0.001)) ;
    	Axis_Calc_Distance.zDistance += (Axis_Calc_Distance.zVelocity * (CurrMeasureVal.CTime * 0.001)) ;
    	
    	if((xSimpleMatrix.get(10,0) < -2.0) && (turn_count != 0))
    	{
    		turn_count++;
    		if(turn_count>9)
    		{
    			Turn_Occurred = 1; // Right turn
    			turn_count = 0;
    			
        		Straight_distance = Axis_Calc_Distance.zDistance;
        		System.out.println("Right turn Straight_Dist = "+Straight_distance);
    		}
    	}
    	else if((xSimpleMatrix.get(10,0) > 2.0) && (turn_count != 0))
    	{
    		turn_count++;
    		if(turn_count>9)
    		{
    			Turn_Occurred = 2; // Left turn
    			turn_count = 0;
    			
        		Straight_distance = Axis_Calc_Distance.zDistance;
        		System.out.println("Left turn Straight_Dist = "+Straight_distance);
    		}
    	}
    }
	
    public void DownloadFile(File FileToSave)
    {
        FTPClient client = new FTPClient();
        try
        {
            client.connect(FTP_HOST,21);
            //client.connect(FTP_HOST_LINK);
            client.login(FTP_USER, FTP_PASS);
            client.setType(FTPClient.TYPE_BINARY);
            client.changeDirectory("/upload/");
            client.download(FileToDownload, FileToSave);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            try 
            {
                client.disconnect(true);
            } 
            catch (Exception e2) 
            {
                e2.printStackTrace();
            }
        }
    }
	
	public void ReadFromFile(File InputFile)
	{
		BufferedReader File_Read = null;
		try
		{
			String line = "";
			File_Read = new BufferedReader(new FileReader(InputFile));
			
			while ((line = File_Read.readLine()) != null) 
			{
				String[] tokens = line.split(COMMA_DELIMITER);

				if(Objects.equals(tokens[CSV_Sensor_ID],"G"))
				{
					Gps_LAT = Double.parseDouble(tokens[CSV_X_Axis_Val]);
					Gps_LONG = Double.parseDouble(tokens[CSV_Y_Axis_Val]);
					Gps_ALT = Double.parseDouble(tokens[CSV_Z_Axis_Val]);						
				}
				else if(Objects.equals(tokens[CSV_Sensor_ID],"A"))
				{
					line = "";
					if((line = File_Read.readLine()) != null)
					{
						String[] tokens1 = line.split(COMMA_DELIMITER);
						if(Objects.equals(tokens1[CSV_Sensor_ID],"Y"))
						{
							Kalman_Filter_Val CurrMeasureVal = new Kalman_Filter_Val();
							
							CurrMeasureVal.MTime = Long.parseLong(tokens[CSV_Time_MS]);
							CurrMeasureVal.x = Float.parseFloat(tokens[CSV_X_Axis_Val]);
							CurrMeasureVal.y = Float.parseFloat(tokens[CSV_Y_Axis_Val]);
							CurrMeasureVal.z = Float.parseFloat(tokens[CSV_Z_Axis_Val]);
							
							CurrMeasureVal.xRotation = Float.parseFloat(tokens1[CSV_X_Axis_Val]);
							CurrMeasureVal.yRotation = Float.parseFloat(tokens1[CSV_Y_Axis_Val]);
							CurrMeasureVal.zRotation = Float.parseFloat(tokens1[CSV_Z_Axis_Val]);
							measurementUpdate(CurrMeasureVal);
						}
						
						else if(Objects.equals(tokens1[CSV_Sensor_ID],"G"))
						{
							Gps_LAT = Double.parseDouble(tokens1[CSV_X_Axis_Val]);
							Gps_LONG = Double.parseDouble(tokens1[CSV_Y_Axis_Val]);
							Gps_ALT = Double.parseDouble(tokens1[CSV_Z_Axis_Val]);
						}
						
					}
					else
					{
						System.out.println("End of CSV File");
						break;
					}
				}

			}

		}
		catch (Exception e) 
		{
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } 

	}

	public void KalmanFilter() 
	{ 
		cSimpleMatrix.zero(); 
		
		Axis_Calc_Distance = new Kalman_Filter_Distance();
	
		Axis_Calc_Distance.xVelocity = 0;
		Axis_Calc_Distance.yVelocity = 0;
		Axis_Calc_Distance.zVelocity = 0;
		
		Axis_Calc_Distance.xDistance = 0;
		Axis_Calc_Distance.yDistance = 0;
		Axis_Calc_Distance.zDistance = 0;
		
		Axis_Calc_Distance.CTime = 0;
		
	} 
    
	public void measurementUpdate(Kalman_Filter_Val CurrMeasureVal) 
	{ 
		if(CurrMeasureVal == null) 
			return; 

		if(xSimpleMatrix == null) 
		{ 
			xSimpleMatrix = new SimpleMatrix(12, 1, true, new double [] 
			{ 
					CurrMeasureVal.x, 
					0, 
					CurrMeasureVal.y, 
					0, 
					CurrMeasureVal.z, 
					0, 
					CurrMeasureVal.xRotation, 
					0, 
					CurrMeasureVal.yRotation, 
					0, 
	     			CurrMeasureVal.zRotation, 
	     			0 
	     	} );
			
			AccSimpleMatrix =  new SimpleMatrix(3, 1, true, new double [] 
			{ 
					CurrMeasureVal.x, 
					CurrMeasureVal.y, 
					CurrMeasureVal.z 
	     	} );

			measUpateTime = CurrMeasureVal.MTime;
			return; 
		} 
		
	  	AccSimpleMatrix.set(0,0,(xSimpleMatrix.get(0, 0)));
	  	AccSimpleMatrix.set(1,0,(xSimpleMatrix.get(2, 0)));
	  	AccSimpleMatrix.set(2,0,(xSimpleMatrix.get(4, 0)));
	  	
		long tau = CurrMeasureVal.MTime - measUpateTime; 
		CurrMeasureVal.CTime = tau;

		SimpleMatrix uSimpleMatrix = new SimpleMatrix(6, 1, true, new double [] 
		{ 
		    CurrMeasureVal.x,  
		    CurrMeasureVal.y,  
		    CurrMeasureVal.z,  
		    CurrMeasureVal.xRotation,  
		    CurrMeasureVal.yRotation,  
		    CurrMeasureVal.zRotation
	    } ); 
	   
		equations.alias( uSimpleMatrix, "u");  
	   
		aSimpleMatrix.set(0, 0,  1.0 - alpha); 
		aSimpleMatrix.set(0, 1, (1.0 - alpha) * tau); 
		aSimpleMatrix.set(1, 0, -1.0 * alpha / tau); 
		aSimpleMatrix.set(1, 1,  1.0 - alpha); 
		aSimpleMatrix.set(2, 2,  1.0 - alpha); 
		aSimpleMatrix.set(2, 3, (1.0 - alpha) * tau); 
		aSimpleMatrix.set(3, 2, -1.0 * alpha / tau); 
		aSimpleMatrix.set(3, 3,  1.0 - alpha); 
		aSimpleMatrix.set(4, 4,  1.0 - alpha); 
		aSimpleMatrix.set(4, 5, (1.0 - alpha) * tau); 
		aSimpleMatrix.set(5, 4, -1.0 * alpha / tau); 
		aSimpleMatrix.set(5, 5,  1.0 - alpha); 
		aSimpleMatrix.set(6, 6,  1.0 - alpha); 
		aSimpleMatrix.set(6, 7, (1.0 - alpha) * tau); 
		aSimpleMatrix.set(7, 6, -1.0 * alpha / tau); 
		aSimpleMatrix.set(7, 7,  1.0 - alpha); 
		aSimpleMatrix.set(8, 8,  1.0 - alpha); 
		aSimpleMatrix.set(8, 9, (1.0 - alpha) * tau); 
		aSimpleMatrix.set(9, 8, -1.0 * alpha / tau); 
		aSimpleMatrix.set(9, 9,  1.0 - alpha); 
		aSimpleMatrix.set(10, 10,  1.0 - alpha); 
	  	aSimpleMatrix.set(10, 11, (1.0 - alpha) * tau); 
	  	aSimpleMatrix.set(11, 10, -1.0 * alpha / tau); 
	  	aSimpleMatrix.set(11, 11,  1.0 - alpha); 
	  	
	  	cSimpleMatrix.set(0, 0, alpha); 
	  	cSimpleMatrix.set(1, 0, alpha/tau); 
	  	cSimpleMatrix.set(2, 1, alpha); 
	  	cSimpleMatrix.set(3, 1, alpha/tau); 
	  	cSimpleMatrix.set(4, 2, alpha); 
	  	cSimpleMatrix.set(5, 2, alpha/tau); 
	  	cSimpleMatrix.set(6, 3, alpha); 
	  	cSimpleMatrix.set(7, 3, alpha/tau); 
	  	cSimpleMatrix.set(8, 4, alpha); 
	  	cSimpleMatrix.set(9, 4, alpha/tau); 
	  	cSimpleMatrix.set(10, 5, alpha); 
	  	cSimpleMatrix.set(11, 5, alpha/tau); 
	  	
	  	
	  	if(updateEquation == null) 
	  	{ 
	  		equations.alias( 
	  				rSimpleMatrix, "r", 
	  				aSimpleMatrix, "A", 
	  				xSimpleMatrix, "x", 
	  				cSimpleMatrix, "C", 
	  				uSimpleMatrix, "u"); 
	  		
	  		updateEquation = equations.compile("r = A*x + C*u"); 
	  	} 
	 
	  	updateEquation.perform();
 
	  	xSimpleMatrix = rSimpleMatrix; 

	  	equations.alias( xSimpleMatrix, "x"); 
 
	  	measUpateTime = CurrMeasureVal.MTime; 

	  	if(alpha > 0.5) 
	  		alpha = 0.5; 
	  	else if(alpha > 0.25) 
	  		alpha = 0.25; 
	  	
	  	CalculateDistance(CurrMeasureVal);
 	} 
	
	public Kalman_Filter_Val projectState(long time) 
	{ 
		long tau = time - measUpateTime; 

		if(xSimpleMatrix == null) 
			return null; 
		
		bSimpleMatrix.set(0, 1, tau); 
		bSimpleMatrix.set(2, 3, tau); 
		bSimpleMatrix.set(4, 5, tau); 
		bSimpleMatrix.set(6, 7, tau); 
		bSimpleMatrix.set(8, 9, tau); 
		
		if(projectStateEquation == null) 
		{  
			equations.alias( xSimpleMatrix, "x", bSimpleMatrix, "B", zSimpleMatrix, "z"); 
			
			projectStateEquation = equations.compile("z = B*x"); 
		} 

		projectStateEquation.perform(); 

		Kalman_Filter_Val FilteredMeas = new Kalman_Filter_Val(); 
		FilteredMeas.x = (float) zSimpleMatrix.get( STATE.X_POS.ordinal()); 
		FilteredMeas.y = (float) zSimpleMatrix.get( STATE.Y_POS.ordinal()); 
		FilteredMeas.z = (float) zSimpleMatrix.get( STATE.Z_POS.ordinal()); 
		FilteredMeas.xRotation = zSimpleMatrix.get( STATE.X_AXIS_ROTATION.ordinal()); 
		FilteredMeas.yRotation = zSimpleMatrix.get( STATE.Y_AXIS_ROTATION.ordinal()); 
		FilteredMeas.zRotation = zSimpleMatrix.get( STATE.Z_AXIS_ROTATION.ordinal()); 
		
		return FilteredMeas; 
 	} 
	
}


