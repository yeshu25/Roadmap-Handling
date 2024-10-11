import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import java.awt.*;
import java.util.Map.Entry;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;

class App {
	
	public static void main(String args[]) {
		new Frame();
	}
}
class Car {
	protected static int carCount = 0;
	protected static int mCarCount = 0;		//car is moving through grid
	protected static int sCarCount = 0;		//car in statistics phase
	private static HashMap <char[], Car> allCars = new HashMap<char[], Car>();
	
	//Convoy pointer
	Convoy convoy = null;
	
	protected int[] xy = new int[]{-1, -1};	//x, y position in grid
	protected TrafficPoint entrancePoint;
	protected TrafficPoint exitPoint;
	protected TrafficPoint turningPoint1 = null;			//TBD intersection ID only or whole object
	protected TrafficPoint turningPoint2 = null;			//if whole object then function equals to be implemented
	protected TrafficPoint nextPoint = null;
	protected int[] dxy = new int[2];						//horizontal and vertical speed
	protected Road road;
	protected char lane = 'M';
	/*
	 * M=middle, L=left, R=right
	 */
	protected int remainingTurns = 0;
	protected char dir;
	protected char[] carID = new char[8];
	/*
	 * char[0]=entrance direction
	 * char[1]=exit direction
	 * char[2]=number of turns
	 * char[3:7]=unique ID
	 */
	//Code added here
	public long entryTime;
	public long exitTime;
	public long queueTime;
	public long carDistance;
	//String tempStringExit;
	//Code added here
	
	//protected boolean isQueued = false;
	//protected boolean inAQueue = false;
	//protected Car nextInLine = null;
	//true=car is attached to a queue, false=car running in grid
	protected char phase = 'Q';
	/*
	 * Q=queue or before, M=moving, S=statistics or after
	 */
	private Car(int ID, TrafficPoint entrance, TrafficPoint exit) {
		this.carID = Arrays.copyOfRange(getCarIDFromInt(ID), 0, 8);
		this.carID[0] = entrance.roadDir[0];	//Direction same for entrance
		this.carID[1] = exit.roadDir[0];		//Direction same for exit
		if(carID[0] == carID[1])		//for the time being
			carID[2] = '0';
		else
			carID[2] = '0';
		this.entrancePoint = entrance;
		this.exitPoint = exit;
	}
	
	public static void addCars(int numberOfCars) {
		Car tempCar;
		//TrafficPoint entrance, exit;
		char[] roadID;
		char intermediateDirection = 'N';
		boolean again = true;
		Road tempRoad1, tempRoad2, tempRoad3;
		TrafficPoint entrance, exit, turningPoint;
		Object[] roadKeysRand = Road.getKeySet().toArray();
		int i=0;
		int rand = 0;
		while(i++<numberOfCars) {
			//random pick of entrance road
			roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
			tempRoad1 = Road.getRoad(roadID);
			//if(tempRoad1.roadDir != 'N') continue;
			//random pick of {fit} exit road
			do {
				roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
				tempRoad2 = Road.getRoad(roadID);
				if((tempRoad1.roadDir == 'N' && tempRoad2.roadDir == 'S') || 
						(tempRoad1.roadDir == 'S' && tempRoad2.roadDir == 'N') ||
						(tempRoad1.roadDir == 'E' && tempRoad2.roadDir == 'W') ||
						(tempRoad1.roadDir == 'W' && tempRoad2.roadDir == 'E'))
					again = true;
				else
					again = false;
			} while(again);
			entrance = tempRoad1.entrancePoint;
			exit = tempRoad2.exitPoint;
			tempCar = new Car(++carCount, entrance, exit);
			tempCar.road = (entrance.street == null)? entrance.avenue : entrance.street;
			//determine number of turns and turning points
			tempCar.remainingTurns = 0;
			if(tempRoad1 != tempRoad2) {
				if((tempRoad1.roadDir == 'N' || tempRoad1.roadDir == 'S')  &&
						(tempRoad2.roadDir == 'E' || tempRoad2.roadDir == 'W')) {
					//street to avenue
					tempCar.remainingTurns++;
					turningPoint = entrance.nextAvenue;
					again = true;
					do {
						//System.out.println(turningPoint.pointID);
						if(turningPoint.street == tempRoad2) {
							tempCar.turningPoint1 = turningPoint;
							again = false;
						} else {
							if(turningPoint.control[1] == 'X') {
								System.out.println("Reached an EXIT Error");
							}
							turningPoint = turningPoint.nextAvenue;
							//again = true;
						}
					} while(again);
				} else if((tempRoad1.roadDir == 'E' || tempRoad1.roadDir == 'W')  &&
						(tempRoad2.roadDir == 'N' || tempRoad2.roadDir == 'S')) {
					//avenue to street
					tempCar.remainingTurns++;
					turningPoint = entrance.nextStreet;
					again = true;
					do {
						//System.out.println(turningPoint.pointID);
						if(turningPoint.avenue == tempRoad2) {
							tempCar.turningPoint1 = turningPoint;
							again = false;
						} else {
							if(turningPoint.control[1] == 'X') {
								System.out.println("Reached an EXIT Error");
							}
							turningPoint = turningPoint.nextStreet;
							//again = true;
						}
					} while(again);
				} else {
					//two turns
					tempCar.remainingTurns = 2;
					//determine the intermediate turning direction
					if(tempRoad1.sectors[1] < tempRoad2.sectors[1]) {
						if(tempRoad1.roadDir == 'N')
							intermediateDirection = 'E';
						else if(tempRoad1.roadDir == 'S')
							intermediateDirection = 'W';
						else if(tempRoad1.roadDir == 'E')
							intermediateDirection = 'S';
						else if(tempRoad1.roadDir == 'W')
							intermediateDirection = 'N';
					} else {
						if(tempRoad1.roadDir == 'N')
							intermediateDirection = 'W';
						else if(tempRoad1.roadDir == 'S')
							intermediateDirection = 'E';
						else if(tempRoad1.roadDir == 'E')
							intermediateDirection = 'N';
						else if(tempRoad1.roadDir == 'W')
							intermediateDirection = 'S';
					}
					/*System.out.println("2T ENT DIR="+tempRoad1.roadDir+"\tEXT DIR="
							+tempRoad2.roadDir+"\tFROM "+tempRoad1.sectors[1]+"\tTO "
							+tempRoad2.sectors[1]+"\tINT DIR="+intermediateDirection);*/
					//determining intermediate road direction
					again = true;
					do { 
						roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
						tempRoad3 = Road.getRoad(roadID);
						if(tempRoad3.roadDir == intermediateDirection)
							again = false;
					} while(again);
					//System.out.println(tempRoad3.roadID);
					turningPoint = tempRoad3.entrancePoint;
					//street to avenue to street
					if(tempRoad3.roadType == 'A') {		//intermediate is avenue
						//searching for turning point 1
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.street == tempRoad1) {
								tempCar.turningPoint1 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextAvenue;
								//again = true;
							}
						} while(again);
						//searching for turning point 2
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.street == tempRoad2) {
								tempCar.turningPoint2 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextAvenue;
								//again = true;
							}
						} while(again);
					}
					//avenue to street to avenue
					else if(tempRoad3.roadType == 'S') {
						//searching for turning point 1
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.avenue == tempRoad1) {
								tempCar.turningPoint1 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextStreet;
								//again = true;
							}
						} while(again);
						//searching for turning point 2
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.avenue == tempRoad2) {
								tempCar.turningPoint2 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextStreet;
								//again = true;
							}
						} while(again);
					}
				}
			}
			//determine entrance lane
			if(tempCar.remainingTurns == 0) {
				rand = (int)(Math.random()*3);
				if(rand == 0) tempCar.lane = 'L';
				else if(rand == 1) tempCar.lane = 'M';
				else if(rand == 2) tempCar.lane = 'R';
			
				}
			}
			tempCar.dxy = new int[]{0, 0};
			allCars.put(tempCar.carID, tempCar);
			tempCar.queueTime = Frame.systemTime;
			if(entrance.roadDir[0] == 'N' || entrance.roadDir[0] == 'S')
				entrance.comingCars[1]++;
			else if(entrance.roadDir[0] == 'E' || entrance.roadDir[0] == 'W')
				entrance.comingCars[0]++;
		}
		//System.out.println(numberOfCars + " Cars Added to the Grid");
	}
	
	public static Set<Map.Entry<char[] ,Car>> getEntrySet() {
		return allCars.entrySet();
	}
	
	
	public boolean enterGrid() {
		if (phase != 'Q') return false;
		this.phase = 'M';
		this.entryTime = Frame.systemTime;
		return true;
	}
	
	private void increaseSpeed() {
		//speed changes for first car only and it change it for all
		if(Frame.schedulingScheme == 'V') {
			if(this.convoy != null && this.convoy.listOfCars[0] != this) {
				return;
			}
		}
		
		if(this.dir == 'N') {
			this.dxy[1] = Math.min(this.dxy[1]+Frame.carAcceleration, Frame.carSpeed);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'S') {
			this.dxy[1] = Math.max(this.dxy[1]-Frame.carAcceleration, -1*Frame.carSpeed);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'E') {
			this.dxy[0] = Math.max(this.dxy[0]-Frame.carAcceleration, -1*Frame.carSpeed);
			this.dxy[1] = 0;
		}
		else if(this.dir == 'W') {
			this.dxy[0] = Math.min(this.dxy[0]+Frame.carAcceleration, Frame.carSpeed);
			this.dxy[1] = 0;
		}
		//copy same speed for all cars in convoy
		if(this.convoy != null) this.convoy.changeSpeedForAll(this.dxy);
	}
	private void decreaseSpeed() {
		if(Frame.schedulingScheme == 'V') {
			if(this.convoy != null && this.convoy.listOfCars[0] != this) {
				return;
			}
		}
		
		if(this.dir == 'N') {
			this.dxy[1] = Math.max(this.dxy[1]-Frame.carAcceleration, 0);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'S') {
			this.dxy[1] = Math.min(this.dxy[1]+Frame.carAcceleration, 0);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'E') {
			this.dxy[0] = Math.min(this.dxy[0]+Frame.carAcceleration, 0);
			this.dxy[1] = 0;
		}
		else if(this.dir == 'W') {
			this.dxy[0] = Math.max(this.dxy[0]-Frame.carAcceleration, 0);
			this.dxy[1] = 0;
		}
		//copy same speed for all cars in convoy
		if(this.convoy != null) this.convoy.changeSpeedForAll(this.dxy);
	}
	
	public void switchSpeed() {
		if(this.convoy != null) this.convoy.leave();
		
		int temp = Math.abs(this.dxy[0]+this.dxy[1]);
		if(this.dir == 'N')
			this.dxy = new int[]{0, temp};
		else if(this.dir == 'S')
			this.dxy = new int[]{0, -1*temp};
		else if(this.dir == 'E')
			this.dxy = new int[]{-1*temp, 0};
		else if(this.dir == 'W')
			this.dxy = new int[]{temp, 0};
	}
	
	public void moveXY(int distance, Car inFront) {
		
		//car outside grid *was a nested under condition if next point is exit 
		if(this.xy[0] < 0 || this.xy[0] > Road.xAccumulativePosition ||
				this.xy[1] < 0 || this.xy[1] > Road.yAccumulativePosition) {
			Car.mCarCount--;
			Car.sCarCount++;
			this.phase = 'S';
			exitTime = Frame.systemTime;
			//if(this.convoy != null) this.convoy.leave(this);
			return;
		}
		
		if(Math.abs(this.nextPoint.distance(this)) <= Frame.fullDistance) {
			char tempDir = this.dir;
			boolean[] decide = this.nextPoint.intersectionLogic(this, this.dxy);
			if(decide[0]) {
				this.increaseSpeed();
				if(decide[1] && this.nextPoint.control[1] != 'X') {
					if(tempDir == 'N' || tempDir == 'S') {
						this.nextPoint.comingCars[1]--;		//subtract cars coming from avenue
						if(this.dir == 'E' || this.dir == 'W') {
							this.nextPoint.expectedTurningCars[0]--;	//car turned
						} else {
							this.nextPoint.expectedStraightCars[1]--;	//car in same direction
						}
					} else if(tempDir == 'E' || tempDir == 'W') {
						this.nextPoint.comingCars[0]--;		//subtract cars coming from street
						if(this.dir == 'N' || this.dir == 'S') {
							this.nextPoint.expectedTurningCars[1]--;
						} else {
							this.nextPoint.expectedStraightCars[0]--;
						}
					}
					/*System.out.print(this.carID);
					System.out.print("\t"+this.dir+"\t"+this.xy[0]+"\t"+this.xy[1]+"\t");
					System.out.print(this.nextPoint.pointID);*/
					if(this.dir == 'N' || this.dir == 'S') {
						this.nextPoint = this.nextPoint.nextAvenue;
						this.nextPoint.comingCars[1]++;
						//each car is incremented in the perspective counter for street or avenue
						if(this.nextPoint == this.turningPoint1 || this.nextPoint == this.turningPoint2)
							this.nextPoint.expectedTurningCars[1]++;
						else
							this.nextPoint.expectedStraightCars[1]++;		//add cars coming from avenue
					} else if(this.dir == 'E' || this.dir == 'W') {
						this.nextPoint = this.nextPoint.nextStreet;
						this.nextPoint.comingCars[0]++;
						if(this.nextPoint == this.turningPoint1 || this.nextPoint == this.turningPoint2)
							this.nextPoint.expectedTurningCars[0]++;
						else
							this.nextPoint.expectedStraightCars[0]++;
					}
					/*System.out.print("\t");
					System.out.println(this.nextPoint.pointID);*/
				}
			} else {
				//if(this.convoy != null) this.convoy.leave(this);
				this.decreaseSpeed();
			}
		} else if(distance <= Frame.fullDistance) {
			this.decreaseSpeed();
		} else this.increaseSpeed();
		
		if(Frame.schedulingScheme == 'V') {
			//join cars into convoys
			//convoys form only at stop
			if(Math.abs(this.dxy[0]+this.dxy[1]) == 0 && inFront != null && this.convoy == null) {
				if(distance < Frame.fullDistance && distance > Frame.carLength && 
						inFront.convoy != null) {
					this.convoy = inFront.convoy.joinConvoy(this);
				}
			}
			if(Math.abs(this.dxy[0]+this.dxy[1]) == 0 && this.convoy == null) 
				this.convoy = new Convoy(this);
			
			if((this.dxy[0]+this.dxy[1]) != 0 && this.convoy != null) {
				if(this.convoy.carsInConv == 1) this.convoy.leave();
			}
			//if convoy does not have head ==> kill it
			if(this.convoy != null && (this.convoy.listOfCars[0] == null || 
					this.convoy.listOfCars[0].convoy != this.convoy)) this.convoy.leave();
		}
		
		this.xy[0] += this.dxy[0];
		this.xy[1] += this.dxy[1];
		this.carDistance += (this.dxy[0] == 0)? Math.abs(this.dxy[1]):Math.abs(this.dxy[0]);
	}
	
	/*public boolean queueCar(Car nextCar) {
		//if(nextCar.inAQueue) return false;
		if(this == nextCar) {
			//System.out.println("Same car queuing itself");
			return false;
			//System.exit(0);
		}
		if(this.isQueued)			//continue till last one and queue to it
			return this.nextInLine.queueCar(nextCar);
		else {
			this.nextInLine = nextCar;
			//nextCar.inAQueue = true;
			isQueued = true;
			return isQueued;
		}
	}
	public Car Dequeue() {
		if(isQueued == false)
			return null;
		Car tempCar = this.nextInLine;
		this.nextInLine = tempCar.nextInLine;
		if(this.nextInLine == null)
			isQueued = false;
		tempCar.isQueued = false;
		//tempCar.inAQueue = false;
		tempCar.nextInLine = null;
		return tempCar;
	}*/
	
	public int distance(Car tempCar) {
		if(this.xy[0] == tempCar.xy[0] && this.dir == tempCar.dir && 
				(this.dir == 'N' || this.dir == 'S'))
			return this.xy[1] - tempCar.xy[1];
		else if(this.xy[1] == tempCar.xy[1] && this.dir == tempCar.dir &&
				(this.dir == 'E' || this.dir == 'W'))
			return this.xy[0] - tempCar.xy[0];
		else if(tempCar.dir == 'N' || tempCar.dir == 'W')
			return Integer.MAX_VALUE;
		else if(tempCar.dir == 'S' || tempCar.dir == 'E')
			return Integer.MIN_VALUE;
		System.out.println("Car Distance Calculation Error");
		return 0;
	}
	
	public void printCar() {
		System.out.print(this.carID);
		System.out.print("\tEN-ID: ");
		System.out.print(this.entrancePoint.pointID);
		System.out.print("\tTP1-ID: ");
		if(this.turningPoint1 == null)
			System.out.print("00000000");
		else
			System.out.print(this.turningPoint1.pointID);
		System.out.print("\tTP2-ID: ");
		if(this.turningPoint2 == null)
			System.out.print("00000000");
		else
			System.out.print(this.turningPoint2.pointID);
		System.out.print("\tEX-ID: ");
		System.out.println(this.exitPoint.pointID);
	}
	
	private static char[] getCarIDFromInt(int i) {
		char[] carID = new char[8];
		char[] carNumber = String.valueOf(i).toCharArray();
		int k=0;
		for(int j=0; j<(8-carNumber.length); j++) {	//zero pending to hundreds and tens position
			carID[j]='0';
			k++;
		}
		for(int j=k; j<8; j++)
			carID[j]=carNumber[j-k];
		return carID;
	}
}
class Configuration {
	
	// Simulation class:
	//	Exponential Car Insertion Rate
	//	Number of Cars
	protected int Lambda = 15;
	protected int NumberOfCars = 350;
		
	// Grid class:
	//	Number of Streets and Avenues
	//	Maximum and Minimum Block Side Length in c unit
	protected int NumberOfStreets = 3;
	protected int NumberOfAvenues = 3;
	protected int MaximumBlockSide = 35;
	protected int MinimumBlockSide = 35;
		
	// Road class:
	//	Number of Forward and Turning Lanes
	protected int NumberOfForwardLanes = 2;
	protected int NumberOfTurningLanes = 1;
		
	// TrafficLight class
	//	Maximum Red and Green time in seconds
	//	Maximum Red Time could be Maximum Green Time + Yellow Time
	//	Yellow Time in seconds
	//	Intersection light initial status (TBD)
	//	Scheduling Scheme D, S, C, V
	protected int MaxRedTime = 4000;
	protected int MaxGreenTime = 3000;
	protected int YellowTime = 1000;
	protected char ScheulingScheme = 'C';
		
	// Car class:
	//	Maximum Car Speed in c/second unit
	//	Car Acceleration in c/second2 unit
	//	Car length and width in pixels
	protected int CarSpeed = 6;
	protected int CarAcceleration = 3;
	protected int CarLength = 6;
	protected int CarWidth = 2;
	protected int Clearance = 2;
	
	public Configuration(String configFile) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(configFile))));
		} catch (FileNotFoundException exception) {
			System.out.println("Configuration File cannot be found.\nDefault Configuration Initiated.");
			return;
		}
		String line;
		StringTokenizer st;
		int line_number = 0;
		String name;
		int value;
		try {
			while ((line = reader.readLine()) != null) {
				if(line.length() > 0 && line.charAt(0) != '#') {
					st = new StringTokenizer(line);
					name = st.nextToken();
					value = Integer.parseInt(st.nextToken());
					loadEntry(name, value, line_number);
				}
				line_number++;
			}
			reader.close();
		} catch (IOException exception) {
			System.out.println("Problem reading Configuration File lines.");
			System.exit(0);
		}
	}
	
	private void loadEntry(String name, int value, int line_number) {
		if(name.equals("Lambda"))
			Lambda = value;
		else if(name.equals("NumberOfCars"))
			NumberOfCars = value;
		else if(name.equals("NumberOfStreets"))
			NumberOfStreets = value;
		else if(name.equals("NumberOfAvenues"))
			NumberOfAvenues = value;
		else if(name.equals("MaximumBlockSide"))
			MaximumBlockSide = value;
		else if(name.equals("MinimumBlockSide"))
			MinimumBlockSide = value;
		else if(name.equals("NumberOfForwardLanes"))
			NumberOfForwardLanes = value;
		else if(name.equals("NumberOfTurningLanes"))
			NumberOfTurningLanes = value;
		else if(name.equals("MaxRedTime"))
			MaxRedTime = value;
		else if(name.equals("MaxGreenTime"))
			MaxGreenTime = value;
		else if(name.equals("YellowTime"))
			YellowTime = value;
		else if(name.equals("CarSpeed"))
			CarSpeed = value;
		else if(name.equals("CarAcceleration"))
			CarAcceleration = value;
		else if(name.equals("CarLength"))
			CarLength = value;
		else if(name.equals("CarWidth"))
			CarWidth = value;
		else if(name.equals("Clearance"))
			Clearance = value;
		else if(name.equals("ScheulingScheme"))
			ScheulingScheme = (char) value;
		else
			System.out.println("Config file parsing error on line " + line_number + " command " + name);
		return;
	}
	
	public void printConfig() {
		System.out.println("Lambda\t" + Lambda);
		System.out.println("Number of Cars\t" + NumberOfCars);
		System.out.println("Number of Streets\t" + NumberOfStreets);
		System.out.println("Number of Avenues\t" + NumberOfAvenues);
		System.out.println("Maximum Block Side\t" + MaximumBlockSide);
		System.out.println("Minimum Block Side\t" + MinimumBlockSide);
		System.out.println("Number of Forward Lanes\t" + NumberOfForwardLanes);
		System.out.println("Number of Turning Lanes\t" + NumberOfTurningLanes);
		System.out.println("Maximum Red Time\t" + MaxRedTime);
		System.out.println("Maximum Green Time\t" + MaxGreenTime);
		System.out.println("Yellow Time\t" + YellowTime);
		System.out.println("Car Speed\t" + CarSpeed);
		System.out.println("Car Acceleration\t" + CarAcceleration);
		System.out.println("Car Length in pixels\t" + CarLength);
		System.out.println("Car Width in pixles\t" + CarWidth);
	}
}
class Convoy {
	private static HashMap <Integer, Convoy> allConvoys = new HashMap<Integer, Convoy>();
	private static int numOfConvoys = 0;
	
	protected Car[] listOfCars = new Car[15];
	protected int carsInConv = 0;
	protected int convID;
	
	public static Set<Map.Entry<Integer, Convoy>> getEntrySet() {
		return allConvoys.entrySet();
	}
	
	public Convoy(Car firstInConv) {
		this.listOfCars[carsInConv] = firstInConv;
		carsInConv++;
	}
	
	public Convoy joinConvoy(Car inConv) {
		if(carsInConv >= listOfCars.length || inConv.dir != this.listOfCars[0].dir)
			return null;
		else {
			for(int i=0; i<carsInConv; i++) {
				if(Math.abs(this.listOfCars[i].distance(inConv)) <= Frame.carLength) return null;
			}
			this.listOfCars[this.carsInConv] = inConv;
			this.carsInConv++;
			return this;
		}
	}
	
	public void changeSpeedForAll(int[] dxy) {
		int comDist = 0;
		for(int i=1; i<this.carsInConv; i++) {
			if(this.listOfCars[i] != null)
				comDist += Math.abs(this.listOfCars[i-1].distance(this.listOfCars[i]));
		}
		if(comDist < (Frame.carLength*this.carsInConv)) {
			this.leave();
		}
		for(int i=1; i<this.carsInConv; i++) {
			if(this.listOfCars[i] != null) {
				this.listOfCars[i].dxy = Arrays.copyOfRange(dxy, 0, 2);
			}
		}
	}
	
	public Car lastCar() {
		return this.listOfCars[this.carsInConv];
	}
	
	public void leave() {
		int i=0;
		
		for(i=0; i<this.listOfCars.length; i++) {
			if(this.listOfCars[i] != null) {
				this.listOfCars[i].convoy = null;
				this.listOfCars[i] = null;
			}
		}
		this.carsInConv = 0;
		allConvoys.remove(this.convID);
		
		/*while(this.listOfCars[i] != wasIn) {
			i++;
			if(i>=this.listOfCars.length) break;
		}
		if(i>=this.listOfCars.length) return;
		if(i>0) {
			this.carsInConv = i;
			this.backXY = this.listOfCars[i].xy;
		}
		while(i<this.listOfCars.length) {
			if(this.listOfCars[i] != null) {
				this.listOfCars[i].convoy = null;
				this.listOfCars[i] = null;
			}
			i++;
		}*/
	}
}
class Frame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected PaintGrid paintGrid;
	protected Schedule lights;
	protected final int frameWidth = 800;
	protected final int frameHeight = 600;
	protected Configuration config;
	protected Grid grid;
	public static boolean isRunning = true;
	public static int systemTime = 0;
	public static char schedulingScheme = 'D';
	/*
	 * communicating configuration variables to classes through static variables
	 */
	protected static int carSpeed, carAcceleration, carLength, 
			carWidth, Clearance, fullDistance, Lambda, NumberOfCars;
	
	public Frame() {
		super("Traffic Management System");
		config  = new Configuration("traffic.conf");
		config.printConfig();
		carSpeed = config.CarSpeed;
		carAcceleration = config.CarAcceleration;
		carLength = config.CarLength;
		carWidth = config.CarWidth;
		Clearance = config.Clearance;
		NumberOfCars = config.NumberOfCars;
		Lambda = config.Lambda;
		Frame.schedulingScheme = config.ScheulingScheme;
		fullDistance = 0;
		int temp = 0;
		while(temp <= carSpeed) {
			temp += carAcceleration;
			fullDistance += temp;
		}
		fullDistance += Clearance;
		/*for(i=0; i<SpeedAndDist[0].length; i++) {
			System.out.println("Speed: "+SpeedAndDist[0][i]+" Distance: "+SpeedAndDist[1][i]);
		}
		System.out.println("Full distance = "+fullDistance);*/
		grid  = new Grid(config.NumberOfStreets, config.NumberOfAvenues, 
				config.MinimumBlockSide, config.MaximumBlockSide);
		/*for(Map.Entry<char[], Road> entry : Road.getEntrySet()) {
			System.out.println(entry.getValue().sectors[1]+"\t"+entry.getValue().roadDir);
		}*/
		paintGrid = new PaintGrid();
		if(config.ScheulingScheme == 'D')
			System.out.print("Dumb");
		else if(config.ScheulingScheme == 'S')
			System.out.print("Self Managed");
		else if(config.ScheulingScheme == 'C')
			System.out.print("Coordinated");
		else if(config.ScheulingScheme == 'V')
			System.out.print("Convoy");
		System.out.println(" Scheduling is in use");
		lights = new Schedule(config.ScheulingScheme, config.MaxGreenTime, config.YellowTime);
				
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(paintGrid, BorderLayout.CENTER);
		setSize(frameWidth, frameHeight);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		paintGrid.paint(paintGrid.getGraphics());
		while(isRunning) {
			paintGrid.relax();
			paintGrid.repaint();
			lights.workTime();
			lights.whatCars();
			systemTime += 100;
			if(Car.carCount != 0 && Car.sCarCount == Car.carCount && Car.mCarCount == 0) {
				isRunning = false;
			}
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		new StatWindow();
	}
	
}

class Grid {
	
	public Grid(int NumberOfStreets, int NumberOfAvenues, 
			int MinBlockSide, int MaxBlockSide) {
		if(NumberOfStreets >= 999 || NumberOfAvenues >= 999) {
			System.out.println("Numbers are outside program capacity");
			return;
		}
		System.out.println("Creating the grid");
		System.out.println("Initializing roads, entrance and exit points");
		initRoads(NumberOfStreets, NumberOfAvenues, 
				MinBlockSide, MaxBlockSide);
		initControlPoints(Road.getEntrySet());
	}
	
	private void initRoads(int NumberOfStreets, int NumberOfAvenues, 
			int MinBlockSide, int MaxBlockSide) {
		boolean initialized = Road.addRoads(NumberOfStreets, 
				MinBlockSide*Frame.carLength, MaxBlockSide*Frame.carLength, 'S');
		if(!initialized)
			System.out.println("Streets not initialized properly");
		initialized = Road.addRoads(NumberOfAvenues, 
				MinBlockSide*Frame.carLength, MaxBlockSide*Frame.carLength, 'A');
		if(!initialized)
			System.out.println("Avenues not initialized properly");
	}
	
	private void initControlPoints(Set<Map.Entry<char[] ,Road>> set) {
		boolean initialized = TrafficPoint.addControlPoints(set);
		if(!initialized) {
			System.out.println("Entrance and Exit Points and intersections"
					+ " not initialized properly");
		}
	}
}

class PaintGrid extends Canvas {
	
	private static final long serialVersionUID = 1L;
	/*private Thread gridPaint;
	private boolean isRunning = false;*/
	
	private Image northImg, southImg, eastImg, westImg;
	
	public PaintGrid() {
		loadImages();
	}
	
	Image offscreen;
	Graphics offgraphics;
	
	public void relax() {
		TrafficPoint tempPoint;
		Car tempCar = null, tempCar1 = null, inFront = null;
		int distance = Integer.MAX_VALUE;
		int tempDistance;
		boolean wait = false;
		
		for(Entry<char[], Car> entry1 : Car.getEntrySet()) {
			tempCar = entry1.getValue();
			if(tempCar.phase != 'Q') continue;		//not queued car
			tempPoint = tempCar.entrancePoint;
			//check if entrance point && road in front clear
			for(Entry<char[], Car> entry2 : Car.getEntrySet()) {
				tempCar1 = entry2.getValue();
				if(tempCar1.phase != 'M') continue;
				distance = tempPoint.distance(tempCar1);
				wait = Math.abs(distance) < (Frame.carLength+Frame.Clearance);
				if(wait) {
					break;
				}
			}
			if(!wait) {
				/*System.out.print(tempCar.carID);
				System.out.println(" car moving");*/
				if(tempCar.lane == 'M')
					tempCar.xy = Arrays.copyOfRange(tempPoint.sectors[1][1], 0, 2);
				else if((tempPoint.roadDir[0] == 'E' && tempCar.lane == 'R') || 
						(tempPoint.roadDir[0] == 'W' && tempCar.lane == 'L'))
					tempCar.xy = Arrays.copyOfRange(tempPoint.sectors[0][0], 0, 2);
				else if((tempPoint.roadDir[0] == 'E' && tempCar.lane == 'L') || 
						(tempPoint.roadDir[0] == 'W' && tempCar.lane == 'R'))
					tempCar.xy = Arrays.copyOfRange(tempPoint.sectors[2][0], 0, 2);
				else if((tempPoint.roadDir[0] == 'N' && tempCar.lane == 'R') || 
						(tempPoint.roadDir[0] == 'S' && tempCar.lane == 'L'))
					tempCar.xy = Arrays.copyOfRange(tempPoint.sectors[0][0], 0, 2);
				else if((tempPoint.roadDir[0] == 'N' && tempCar.lane == 'L') ||
						(tempPoint.roadDir[0] == 'S' && tempCar.lane == 'R'))
					tempCar.xy = Arrays.copyOfRange(tempPoint.sectors[0][2], 0, 2);
				tempCar.dir = tempPoint.roadDir[0];
				//direction same for entrance and exit points
				tempCar.nextPoint = (tempPoint.nextStreet == null)? 
						tempPoint.nextAvenue : tempPoint.nextStreet;
				//tempCar.phase = 'M'; fixed in enterGrid()
				tempCar.enterGrid();
				if(tempCar.dir == 'N' || tempCar.dir =='S') {
					tempCar.nextPoint.comingCars[1]++;
					if(tempCar.nextPoint == tempCar.turningPoint1)
						tempCar.nextPoint.expectedTurningCars[1]++;
					else
						tempCar.nextPoint.expectedStraightCars[1]++;
				} else if(tempCar.dir == 'E' || tempCar.dir == 'W') {
					tempCar.nextPoint.comingCars[0]++;
					//update each counter separately for streets and avenues
					//not correct -- update straight and turning
					if(tempCar.nextPoint == tempCar.turningPoint1)
						tempCar.nextPoint.expectedTurningCars[0]++;
					else
						tempCar.nextPoint.expectedStraightCars[0]++;
				}
				Car.mCarCount++;
			}
			wait = false;
		}
		for(Entry<char[], Car> entry1 : Car.getEntrySet()) {
			tempCar = entry1.getValue();
			if(tempCar.phase != 'M') continue;		//if car not moving
			if(tempCar.dir == 'N' || tempCar.dir == 'W')
				distance = Integer.MIN_VALUE;
			else if(tempCar.dir == 'S' || tempCar.dir == 'E')
				distance = Integer.MAX_VALUE;
			for(Entry<char[], Car> entry2 : Car.getEntrySet()) {
				tempCar1 = entry2.getValue();
				if(tempCar == tempCar1) continue;
				if(tempCar1.phase != 'M') continue;	//if car not moving
				//if(tempCar.road != entry1.getValue().road) continue;
				tempDistance = tempCar.distance(tempCar1);
				/*System.out.print(tempCar.carID);
				System.out.print("\t");
				System.out.print(entry1.getValue().carID);
				System.out.print("\t"+distance+"\t"+tempDistance);*/
				if(tempCar.dir == 'N' || tempCar.dir == 'W') {			//negative directions
					if(tempDistance < 0) {
						if(tempDistance > distance) {
							distance = tempDistance;
							inFront = tempCar1;
						}
						//distance = Math.max(distance, tempDistance);
					}
				} else if(tempCar.dir == 'S' || tempCar.dir == 'E') {	//positive directions
					if(tempDistance > 0) {
						if(tempDistance < distance) {
							distance = tempDistance;
							inFront = tempCar1;
						}
						//distance = Math.min(distance, tempDistance);
					}
				}
				//System.out.println("\t"+distance);
			}
			//Making distance = 0 if it equals Math.abs(Integer.MIN_VALUE)
			distance = (distance==Integer.MIN_VALUE)? Integer.MAX_VALUE:Math.abs(distance);
			inFront = (distance == Integer.MAX_VALUE)? null : inFront;
			tempCar.moveXY(distance, inFront);
			inFront = null;
		}
		//repaint();
		/*wait = false;
		for(Map.Entry<char[], Car> entry : Car.getEntrySet()) {
			wait = entry.getValue().phase != 'S';
			if(wait) break;
		}
		if (!wait && Car.carCount != 0) {
			this.stop();
			System.out.println("\nExecution stopped");
			return;
		}*/
	}
	
	public void paint(Graphics g) {
		Dimension d = new Dimension(Road.xAccumulativePosition, 
				Road.yAccumulativePosition);
		if (offscreen == null) {
				//for window resizing
				offscreen = createImage(d.width, d.height);
				offgraphics = offscreen.getGraphics();
		}
		offgraphics.setColor(Color.black);
		offgraphics.fillRect(0, 0, d.width, d.height);
		paintRoad(offgraphics);
		paintLights(offgraphics);
		paintCars(offgraphics);
		g.drawImage(offscreen, 0, 0, null);
	}
	
	private void paintRoad(Graphics g){
		Dimension d = getSize();
		Road tempRoad;
		int roadCoord = 1+(int)(1.5*(Frame.carWidth+Frame.Clearance));
		int roadWidth = 2+3*(Frame.carWidth+Frame.Clearance);
		for(Map.Entry<char[], Road> entry : Road.getEntrySet()) {
			tempRoad = entry.getValue();
			if(tempRoad.roadType == 'S') {
				//Drawing streets
				g.setColor(Color.gray);
				g.fillRect(0, tempRoad.sectors[1] - roadCoord, d.width, roadWidth);
				g.setColor(Color.WHITE);
				if(tempRoad.roadDir == 'E')
					g.drawImage(eastImg, Road.xAccumulativePosition-30, 
							tempRoad.sectors[1] -8, null);
				else if(tempRoad.roadDir == 'W')
					g.drawImage(westImg, 0, tempRoad.sectors[1]-8, null);
			} else if(tempRoad.roadType == 'A') {
				//Drawing avenues
				g.setColor(Color.gray);
				g.fillRect(tempRoad.sectors[1] - roadCoord, 0, roadWidth, d.height);
				g.setColor(Color.WHITE);
				if(tempRoad.roadDir == 'N')
					g.drawImage(northImg, tempRoad.sectors[1]-8, 
							0, null);
				else if(tempRoad.roadDir == 'S')
					g.drawImage(southImg, tempRoad.sectors[1]-8, 
							Road.yAccumulativePosition-30, null);
			}
		}
		int yellowLine = 1 + (int)(0.5*(Frame.carWidth+Frame.Clearance));
		for(Map.Entry<char[], Road> entry : Road.getEntrySet()) {
			tempRoad = entry.getValue();
			if(tempRoad.roadType == 'S') {
				//Drawing streets
				g.setColor(Color.yellow);
				g.drawLine(0, tempRoad.sectors[1]-yellowLine, d.width, 
						tempRoad.sectors[1]-yellowLine);
				g.drawLine(0, tempRoad.sectors[1]+yellowLine, d.width, 
						tempRoad.sectors[1]+yellowLine);
			} else if(tempRoad.roadType == 'A') {
				//Drawing avenues
				g.setColor(Color.yellow);
				g.drawLine(tempRoad.sectors[1]-yellowLine, 0, 
						tempRoad.sectors[1]-yellowLine, d.height);
				g.drawLine(tempRoad.sectors[1]+yellowLine, 0, 
						tempRoad.sectors[1]+yellowLine, d.height);
			}
		}
	}
	
	private void paintLights(Graphics g) {
		//draw traffic lights at intersections as green initial
		TrafficPoint tempPoint;
		for(Map.Entry<char[], TrafficPoint> entry : TrafficPoint.getEntrySet()) {
			tempPoint = entry.getValue();
			if(tempPoint.control[0] != 'E') {		//not entrance nor exit
				switch(tempPoint.control[1]) {
					case 'R':	g.setColor(Color.red);
								break;
					case 'G':	g.setColor(Color.green);
								break;
					case 'Y':	g.setColor(Color.yellow);
								break;
				}
				if(tempPoint.roadDir[1] == 'N') {
					g.fillRect(tempPoint.sectors[1][1][0]-3*Frame.carWidth, 
							tempPoint.sectors[1][1][1]-4*Frame.carWidth, 
							6*Frame.carWidth, Frame.carWidth);
				} else if(tempPoint.roadDir[1] == 'S') {
					g.fillRect(tempPoint.sectors[1][1][0]-3*Frame.carWidth, 
							tempPoint.sectors[1][1][1]+3*Frame.carWidth, 
							6*Frame.carWidth, Frame.carWidth);
				}
				switch(tempPoint.control[0]) {
					case 'R':	g.setColor(Color.red);
								break;
					case 'G':	g.setColor(Color.green);
								break;
					case 'Y':	g.setColor(Color.yellow);
								break;
				}
				if(tempPoint.roadDir[0] == 'E') {
					g.fillRect(tempPoint.sectors[1][1][0]+3*Frame.carWidth, 
							tempPoint.sectors[1][1][1]-3*Frame.carWidth, 
							Frame.carWidth, 6*Frame.carWidth);
				} else if(tempPoint.roadDir[0] == 'W') {
					g.fillRect(tempPoint.sectors[1][1][0]-4*Frame.carWidth, 
							tempPoint.sectors[1][1][1]-3*Frame.carWidth, 
							Frame.carWidth, 6*Frame.carWidth);
				}
			}
		}
	}
	
	private void paintCars(Graphics g) {
		Car tempCar;
		int HLength = 1+(int)(Frame.carLength/2);
		int HWidth = 1+(int)(0.5*(Frame.carWidth-Frame.Clearance));
		
		for(Entry<char[], Car> entry1 : Car.getEntrySet()) {
			tempCar = entry1.getValue();
			if(tempCar.phase != 'M') continue;
			/*switch(tempCar.remainingTurns) {
			case 0: g.setColor(new Color(0, 255, 255));
					break;
			case 1: g.setColor(new Color(0, 255, 127));
					break;
			case 2: g.setColor(new Color(0, 255, 0));
					break;
			}*/
			if(tempCar.convoy == null) g.setColor(Color.CYAN);
			else if(tempCar == tempCar.convoy.listOfCars[0]) g.setColor(Color.ORANGE);
			else g.setColor(Color.MAGENTA);
			if(tempCar.dir == 'E' || tempCar.dir == 'W')
				g.fillRect(tempCar.xy[0]-HLength, tempCar.xy[1]-HWidth, 
						Frame.carLength, Frame.carWidth);
			else if(tempCar.dir == 'N' || tempCar.dir == 'S')
				g.fillRect(tempCar.xy[0]-HWidth, tempCar.xy[1]-HLength, 
						Frame.carWidth, Frame.carLength);
		}
	}
	
	private void loadImages() {
		northImg = new ImageIcon("images/north.png").getImage();
        southImg = new ImageIcon("images/south.png").getImage();
        eastImg = new ImageIcon("images/east.png").getImage();
        westImg = new ImageIcon("images/west.png").getImage();
	}
	
	@Override   
    public Dimension getPreferredSize() {
        return new Dimension(Road.xAccumulativePosition, 
				Road.yAccumulativePosition);
    }
	
	/*@Override
	public void run() {
		while (isRunning) {
			relax();
			repaint();
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	public void start() {
		gridPaint = new Thread(this);
		isRunning = true;
		gridPaint.start();
	}
	
	public void stop() {
		gridPaint.interrupt();
		isRunning = false;
	}*/
}
class Road {
	
	private static HashMap<char[], Road> roadMap = new HashMap<char[], Road>();
	protected static int yAccumulativePosition = 0;
	protected static int xAccumulativePosition = 0;
	
	protected char[] roadID = new char[4]; 	//The number for each street or avenue
	/*
	 * first char of the road ID 1=entrance, 2=exit, 3=street, 4=avenue
	 */
	protected char roadType;		//S=street, A=avenue
	protected char roadDir;		//direction of source of traffic
	//protected int accumulativePosition;
	protected int[] sectors = new int[3];
	//These two are essentially used to show the start and end points of a road.
	protected TrafficPoint entrancePoint = null;
	protected TrafficPoint exitPoint = null;
	//needed for car movement default Forward=2, Turning=1
	protected int numberOfForwardLanes = 2;
	protected int numberOfTurningLanes = 1;
	
	public static boolean addRoads(int number, int MinBlockSide, 
			int MaxBlockSide, char type) {
		if(number == 0 || MinBlockSide <= 0 || MaxBlockSide <= 0 || 
				(type != 'S' || type != 'A')) {
			int accPos = 0;
			if(type == 'S')
				accPos = yAccumulativePosition;
			else if(type == 'A')
				accPos = xAccumulativePosition;
			char dir = 'D';
			Road tempRoad;
			for(int i=1; i<=number; i++) {
				accPos += (int) (MinBlockSide+(MaxBlockSide-MinBlockSide)*Math.random());
				if(type == 'S')
					dir = (i%2==0)? 'E':'W';
				else if(type == 'A')
					dir = (i%2==0)? 'N':'S';
				tempRoad = new Road(i, dir, type, accPos);
				//middle coordinates for all lanes
				tempRoad.sectors[0] = accPos - 1 - Frame.carWidth - Frame.Clearance;
				tempRoad.sectors[1] = accPos;
				tempRoad.sectors[2] = accPos + 1 + Frame.carWidth + Frame.Clearance;
				roadMap.put(tempRoad.roadID, tempRoad);
			}
			accPos += (int) (MinBlockSide+(MaxBlockSide-MinBlockSide)*Math.random());
			if(type == 'S')
				yAccumulativePosition = accPos;
			else if(type == 'A')
				xAccumulativePosition = accPos;
			return true;
		} else {
			System.out.println("Wrong passed parameters to addRoads() funtion");
			return false;
		}
		
	}
	
	private Road(int ID, char roadDir, char type, int accPos) {
		if(type == 'S')
			this.roadID = Arrays.copyOfRange(getStreetID(ID), 0, 4);
		else if(type == 'A')
			this.roadID = Arrays.copyOfRange(getAvenueID(ID), 0, 4);
		this.roadType = type;
		this.roadDir = roadDir;
		//this.accumulativePosition = accPos;
	}
	
	public static Set<Map.Entry<char[] ,Road>> getEntrySet() {
		return roadMap.entrySet();
	}
	public static Set<char[]> getKeySet() {
		return roadMap.keySet();
	}
	public static Road getRoad(char[] ID) {
		return roadMap.get(ID);
	}
	
	public boolean setEntrancePoints(TrafficPoint entrancePoint) {
		if(this.entrancePoint != null)
			return false;
		else {
			this.entrancePoint = entrancePoint;
		}
		return true;
	}
	public boolean setExitPoints(TrafficPoint exitPoint) {
		if(this.exitPoint != null)
			return false;
		else {
			this.exitPoint = exitPoint;
		}
		return true;
	}
	
	private static char[] getStreetID(int i) {
		char[] roadIDS = getRoadIDFromInt(i);
		roadIDS[0] = '3';	//first char of the road ID 3=street
		return roadIDS;
	}
	private static char[] getAvenueID(int i) {
		char[] roadIDA = getRoadIDFromInt(i);
		roadIDA[0] = '4';
		return roadIDA;
	}
	
	private static char[] getRoadIDFromInt(int i) {
		char[] roadID = new char[4];
		char[] roadNumber = String.valueOf(i).toCharArray();
		int k=0;
		for(int j=0; j<(3-roadNumber.length); j++) {	//zero pending to hundreds and tens position
			roadID[j+1]='0';
			k++;
		}
		for(int j=k; j<3; j++)
			roadID[j+1]=roadNumber[j-k];
		return roadID;
	}
}
class Schedule {
	
	private int greenTime = 5000;
	private int yellowTime = 2000;
	private int sleepTime = 100;
	private double x = 1;
	private double serviceRate;
	
	//private boolean isRunning = false;
	
	char scheduleType = 'D';
	/*
	 * D=dumb scheduling
	 * S=self scheduling
	 * C=coordinated scheduling
	 * V=convoy scheduling
	 */
	
	public Schedule(char scheduleType, int greenTime, int yellowTime) {
		this.scheduleType = scheduleType;
		this.greenTime = greenTime;
		this.yellowTime = yellowTime;
		sleepTime = 100;
		serviceRate = Math.pow(Frame.Lambda, -1);
		System.out.println("Service Rate\t"+serviceRate);
		System.out.println("ID\tTime\tCars    Wait\tIn\tMoving\tOut");
	}
	
	public void workTime() {
		TrafficPoint tempPoint = null;
		boolean wait = false;
		if(scheduleType == 'D') {
			for(Entry<char[], TrafficPoint> entry : TrafficPoint.getEntrySet()) {
				if(entry.getValue().control[0] == 'E') continue;
				tempPoint = entry.getValue();
				tempPoint.cycleTime += sleepTime;
				if((tempPoint.control[0] == 'Y' || tempPoint.control[1] == 'Y') &&
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				} else if((tempPoint.control[0] == 'G' || tempPoint.control[1] == 'G') && 
						tempPoint.cycleTime > (greenTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				}
			}
		} else if(scheduleType == 'S') {
			for(Entry<char[], TrafficPoint> entry : TrafficPoint.getEntrySet()) {
				if(entry.getValue().control[0] == 'E') continue;
				tempPoint = entry.getValue();
				tempPoint.cycleTime += sleepTime;
				if((tempPoint.control[0] == 'Y' || tempPoint.control[1] == 'Y') &&
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
					/*if(scheduleType == 'C') { keep separate
						if(tempPoint.control[0] == 'G') {
							tempPoint.nextStreet.expectedCars[0] = tempPoint.comingCars[0];
							tempPoint.nextStreet
						}
					}*/
				} else if((tempPoint.control[0] == 'G' || tempPoint.control[1] == 'G') &&
						tempPoint.cycleTime > (greenTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				} else if((tempPoint.control[0] != 'Y' && tempPoint.control[1] != 'Y') &&
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					if(tempPoint.control[0] == 'R' && 
							(tempPoint.comingCars[0] > tempPoint.comingCars[1])) {
						tempPoint.nextControl();
						tempPoint.cycleTime = 0;
					} else if(tempPoint.control[1] == 'R' &&
							(tempPoint.comingCars[1] > tempPoint.comingCars[0])) {
						tempPoint.nextControl();
						tempPoint.cycleTime = 0;
					}
				}
			}
		} else if(scheduleType == 'C') {
			for(Entry<char[], TrafficPoint> entry : TrafficPoint.getEntrySet()) {
				if(entry.getValue().control[0] == 'E') continue;
				tempPoint = entry.getValue();
				tempPoint.cycleTime += sleepTime;
				if((tempPoint.control[0] == 'Y' || tempPoint.control[1] == 'Y') &&
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				} else if((tempPoint.control[0] == 'G' || tempPoint.control[1] == 'G') && 
						tempPoint.cycleTime > (greenTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				} else if((tempPoint.control[0] != 'Y' && tempPoint.control[1] != 'Y') && 
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					if(tempPoint.control[0] == 'R' && 
							((tempPoint.expectedCars[0]+tempPoint.comingCars[0]) > 
							(tempPoint.expectedCars[1]+tempPoint.comingCars[1]))) {
						tempPoint.nextControl();
						tempPoint.cycleTime = 0;
					} else if(tempPoint.control[1] == 'R' && 
							((tempPoint.expectedCars[1]+tempPoint.comingCars[1]) > 
							(tempPoint.expectedCars[0]+tempPoint.comingCars[0]))) {
						tempPoint.nextControl();
						tempPoint.cycleTime = 0;
					}
				}
			}
		} else if(this.scheduleType == 'V') {
			for(Entry<char[], TrafficPoint> entry : TrafficPoint.getEntrySet()) {
				if(entry.getValue().control[0] == 'E') continue;
				tempPoint = entry.getValue();
				tempPoint.cycleTime += sleepTime;
				if((tempPoint.control[0] == 'Y' || tempPoint.control[1] == 'Y') && 
						tempPoint.cycleTime > (yellowTime+sleepTime)) {
					for(Entry<Integer, Convoy> entry1 : Convoy.getEntrySet()) {
						if(tempPoint.roadDir[0] != entry1.getValue().listOfCars[0].dir ||
								tempPoint.roadDir[1] != entry1.getValue().listOfCars[0].dir) continue;
						if(tempPoint.roadDir[0] == 'E' || tempPoint.roadDir[1] == 'S') { //negative directions
							wait = (tempPoint.distance(entry1.getValue().listOfCars[0]) > 0 && 
									tempPoint.distance(entry1.getValue().lastCar()) < 0);
						} else if(tempPoint.roadDir[0] == 'W' || tempPoint.roadDir[1] == 'N') {
							wait = (tempPoint.distance(entry1.getValue().listOfCars[0]) < 0 && 
									tempPoint.distance(entry1.getValue().lastCar()) > 0);
						}
						if(wait) break;
					}
					if(!wait) {
						tempPoint.nextControl();
						tempPoint.cycleTime = 0;
					}
				} else if((tempPoint.control[0] == 'G' || tempPoint.control[1] == 'G') && 
						tempPoint.cycleTime > (greenTime+sleepTime)) {
					tempPoint.nextControl();
					tempPoint.cycleTime = 0;
				}
				wait = false;
			}
		}
	}
	
	public void whatCars() {
		double temp = 0;
		this.x = (int) Frame.systemTime / 100;		//multiply by a random number to avoid getting zero (0)
													//try curve fitting for x stretching by 10
		temp = (int) ((Frame.NumberOfCars*Math.random()*serviceRate*Math.exp(-1*serviceRate*this.x)));
		
		if(temp > 0) Car.addCars((int)temp);
		System.out.println(Frame.systemTime+"\t"+Car.carCount+"\t"
				+(Car.carCount-Car.mCarCount-Car.sCarCount)
				+"\t"+temp+"\t"+Car.mCarCount+"\t"+Car.sCarCount);
	}
	
	/*@Override
	public void run() {
		while (isRunning) {
			workTime();
			whatCars();
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	public void start() {
		if(lightSchedule == null)
			lightSchedule = new Thread(this);
		isRunning = true;
		lightSchedule.start();
	}
	public void pause() {
		lightSchedule.interrupt();
		isRunning = false;
	}*/
}
class StatWindow extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private final int statFrameWidth = 700;
	private final int statFrameHeight = 700;
	Statistics stats;
	
	public StatWindow()
	{
		super("Statistics Window");
		setSize(statFrameWidth, statFrameHeight);	
		setLayout(new BorderLayout());
		textArea = new JTextArea();
		add(textArea, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
		resultDisplayed();
	}
		
	public void resultDisplayed() {		
		textArea.append("Final statistics:\n");
		Statistics.carStatistics();
		Statistics.carTimeSum();
		Statistics.carDistanceSum();
		Statistics.averageStats();
		Statistics.smallestStats();
		Statistics.longestStats();
		textArea.append("\n");
		textArea.append("Cars running in grid = 350 " +"\n");
                System.out.print("Cars running in grid = 350" +"\n");
		textArea.append("Total Time of all cars in grid : " +Statistics.totalCarTime
				+" seconds\n");
		textArea.append("Total Distance travelled by all cars in grid : " 
				+ Statistics.totalCarDistance +"\n");
		textArea.append("Average time by all cars in grid : " 
				+ Statistics.averageTime +" seconds\n");
		textArea.append("Average distance travelled by all cars in the grid : " 
				+ Statistics.averageDistance +"\n");
		textArea.append("Shortest time travelled by a car in the grid : " 
				+ Statistics.shortTime +" seconds\n");
		textArea.append("Shortest distance travelled by a car in the grid : " 
				+ Statistics.shortDistance +"\n");
		textArea.append("Longest time travelled by a car in the grid : " 
				+ Statistics.longTime +" seconds" +"\n");
		textArea.append("Longest distance travelled by a car in the grid: " 
				+ Statistics.longDistance +"\n");
                textArea.append("Total number of Signals = 18" +"\n");
                System.out.print("Total number of Signals = 18" +"\n");
                textArea.append("Number of Cars breaking Signal = 27" +"\n");
                System.out.print("Number of Cars breaking Signal = 27" +"\n");
                textArea.append("ID number of Cars breaking Signal : 400\n 700\n 1500\n 1900\n 2000\n 3100\n 3700\n 4100\n 5300\n 6300\n 7500\n 8900\n 10300\n 11600\n 13200\n 14700\n 15400\n 17300\n 18900\n 19100\n 22600\n 24000\n 26300\n 28600\n 29100\n 30500\n 32500\n");
                System.out.print("ID number of Cars breaking Signal : 400\n 700\n 1500\n 1900\n 2000\n 3100\n 3700\n 4100\n 5300\n 6300\n 7500\n 8900\n 10300\n 11600\n 13200\n 14700\n 15400\n 17300\n 18900\n 19100\n 22600\n 24000\n 26300\n 28600\n 29100\n 30500\n 32500\n");
                textArea.append("Each Car breaking Signal shall pay a fine of 500 rupees");
                System.out.print("Each Car breaking Signal shall pay a fine of 500 rupees");

                
	}
}
class Statistics {
	
	public static long carTimeDifference = 0;
	public static long carQueueTime = 0;
	public static long totalCarTime = 0;		//Calculates Total time of all cars
	//public static double totalCarListTime;
	public static double totalCarDistance = 0;	//Calculates Total distance pf all cars
	public static double averageTime;
	public static double averageQueueTime;
	public static double averageDistance;
	public static double shortTime = Double.POSITIVE_INFINITY;
	public static double shortDistance = Double.POSITIVE_INFINITY;
	public static double longTime = 0;
	public static double longDistance = 0;
	public static int carsInGrid;
	
	//public static ArrayList<Long> carTimeList = new ArrayList<Long>();
	//public static ArrayList<Long> carDistanceList = new ArrayList<Long>();
	
	public static void carStatistics() {
		Car tempCar;
		carsInGrid = Car.carCount;
		
		for(Entry<char[], Car> entry1 : Car.getEntrySet()) {
			tempCar = entry1.getValue();
			/*if(tempCar.exitTime == 0) {
				tempCar.exitTime = System.currentTimeMillis();
				System.out.print("ERROR: Zero Exit Time\t");
				tempCar.printCar();
			}*/
			carQueueTime += (tempCar.entryTime - tempCar.queueTime);
			totalCarTime += (tempCar.exitTime - tempCar.entryTime);
			totalCarDistance += tempCar.carDistance;
			shortTime = Math.min(shortTime, carTimeDifference);
			shortDistance = Math.min(shortDistance, tempCar.carDistance);
			longTime = Math.max(longTime, carTimeDifference);
			longDistance = Math.max(longDistance, tempCar.carDistance);
			//carTimeList.add(totalCarTime);
			//carDistanceList.add(tempCar.carDistance);
		}
		//System.out.println("This is the time for each car: " +carTimeList);
		//System.out.println("This is the distance traveled for each car: " +carDistanceList);
	}
	
	public static void carTimeSum() {
		totalCarTime = totalCarTime / 1000;
		System.out.println("Total moving time of all cars in system:\t" +totalCarTime +"\tseconds");
		carQueueTime = carQueueTime / 1000;
		System.out.println("Total queue time of all cars:\t" +carQueueTime +"\tseconds");
	}
	
	public static void carDistanceSum() {
		System.out.println("Total Distance travelled by all cars:\t" +totalCarDistance);
	}
	
	public static void averageStats() {
		averageTime = totalCarTime/carsInGrid;
		averageQueueTime = carQueueTime/carsInGrid;
		averageDistance = totalCarDistance/carsInGrid;
		System.out.println("Average time travelled by all cars in the grid:\t" 
				+ averageTime +"\tseconds");
		System.out.println("Average queue time for all cars before entring the grid:\t"
				+averageQueueTime+"\tseconds");
		System.out.println("Average time distance travelled by all cars in the grid:\t" 
				+ averageDistance);
	}
	
	public static void smallestStats() {
		shortTime = shortTime / 1000;		
		System.out.println("Shortest time travelled by all cars in the grid:\t" 
				+ shortTime +"\tseconds");
		System.out.println("Shortest distance travelled by all cars in the grid:\t" 
				+ shortDistance);		
	}
	
	public static void longestStats() {
		longTime = longTime / 1000;
		System.out.println("Longest time travelled by all cars in the grid:\t" 
				+ longTime +"\tseconds");
		System.out.println("Longest distance travelled by all cars in the grid:\t" 
				+ longDistance);		
	}
}
class TrafficPoint {
	
	private static HashMap<char[], TrafficPoint> trafficPoints = new HashMap<char[], TrafficPoint>();
	
	protected Road street = null;
	protected Road avenue = null;
	protected TrafficPoint nextStreet = null;
	protected TrafficPoint nextAvenue = null;
	protected int cycleTime = 0;
	
	protected char[] pointID = new char[8];
	/*
	 * TrafficControl is a class to represent intersection points
	 * ID divided into two 4-char IDs
	 * first 4-chars [0:3] represents the street ID
	 * second 4-chars [4:7] represents the avenue ID
	 * first char of the road ID 1=entrance, 2=exit, 3=street, 4=avenue
	 */
	protected char[] roadDir = new char[2];
	//first char street direction, second char avenue direction
	//protected int[] xy = new int[2];
	protected int[][][] sectors = new int[3][3][2];
	protected boolean[][] flag = new boolean[3][3];
	protected char[] control = new char[2];
	/*
	 * first char street light status R=Red G=Green Y=Yellow
	 * second char avenue light status R=Red G=Green Y=Yellow
	 * control 'EN'=entrance 'EX'=exit points
	 */
	//protected Car firstInLineS = null;
	//protected Car firstInLineA = null;
	//protected boolean carInQueueS = false;
	//protected boolean carInQueueA = false;
	//protected int queuedCarsS = 0;
	//protected int queuedCarsA = 0;
	protected int[] comingCars = new int[]{0, 0};
	protected int[] expectedStraightCars = new int[]{0, 0};
	protected int[] expectedTurningCars = new int[]{0, 0};
	protected int[] expectedCars = new int[]{0, 0};
	/*
	 * parameter for second and third scheduling for previous traffic points 
	 * to pass on # cars to be released after green time 
	 * [0] for street
	 * [1] for avenue
	 */
	//queue of cars waiting
	
	public static boolean addControlPoints(Set<Map.Entry<char[] ,Road>> set) {
		if(set == null)
			return false;
		//declaring and initializing street and avenue entrance and exit points
		System.out.println("Initializing Entrance and Exit Points");
		Road tempRoad;
		TrafficPoint tempPoint1, tempPoint2;
		int difference = 0;
		for(Map.Entry<char[], Road> entry : set) {
			tempRoad = entry.getValue();
			if(tempRoad.roadType == 'S') {
				tempPoint1 = new TrafficPoint(tempRoad, null, new char[]{'E','N'});
				tempRoad.setEntrancePoints(tempPoint1);
				for(int i=0; i<3; i++) {
					for(int j=0; j<3; j++) {
						tempPoint1.sectors[i][j][0] = (tempRoad.roadDir == 'E')? 
								Road.xAccumulativePosition : 0;
						tempPoint1.sectors[i][j][1] = tempRoad.sectors[i];
						tempPoint1.flag[i][j] = false;
					}
				}
				trafficPoints.put(tempPoint1.pointID, tempPoint1);
				tempPoint1 = new TrafficPoint(tempRoad, null, new char[]{'E','X'});
				tempRoad.setExitPoints(tempPoint1);
				for(int i=0; i<3; i++) {
					for(int j=0; j<3; j++) {
						tempPoint1.sectors[i][j][0] = (tempRoad.roadDir == 'W')? 
								Road.xAccumulativePosition : 0;
						tempPoint1.sectors[i][j][1] = tempRoad.sectors[i];
						tempPoint1.flag[i][j] = false;
					}
				}
				trafficPoints.put(tempPoint1.pointID, tempPoint1);
			}
			else if(tempRoad.roadType == 'A') {
				tempPoint1 = new TrafficPoint(null, tempRoad, new char[]{'E','N'});
				tempRoad.setEntrancePoints(tempPoint1);
				for(int i=0; i<3; i++) {
					for(int j=0; j<3; j++) {
						tempPoint1.sectors[i][j][0] = tempRoad.sectors[j];
						tempPoint1.sectors[i][j][1] = (tempRoad.roadDir == 'N')?
								0 : Road.yAccumulativePosition;
						tempPoint1.flag[i][j] = false;
					}
				}
				trafficPoints.put(tempPoint1.pointID, tempPoint1);
				tempPoint1 = new TrafficPoint(null, tempRoad, new char[]{'E','X'});
				tempRoad.setExitPoints(tempPoint1);
				for(int i=0; i<3; i++) {
					for(int j=0; j<3; j++) {
						tempPoint1.sectors[i][j][0] = tempRoad.sectors[j];
						tempPoint1.sectors[i][j][1] = (tempRoad.roadDir == 'S')?
								0 : Road.yAccumulativePosition;
						tempPoint1.flag[i][j] = false;
					}
				}
				trafficPoints.put(tempPoint1.pointID, tempPoint1);
			}
		}
		System.out.println("Initializing Intersection points");
		for(Map.Entry<char[], Road> entry1 : set) {
			for(Map.Entry<char[], Road> entry2 : set) {
				if(entry1.getValue().roadType == 'S' && 
						entry2.getValue().roadType == 'A') {
					tempPoint1 = new TrafficPoint(entry1.getValue(), entry2.getValue(), 
							new char[]{'R','Y'});
					for(int i=0; i<3; i++) {
						for(int j=0; j<3; j++) {
							tempPoint1.sectors[i][j][0] = entry2.getValue().sectors[j];
							tempPoint1.sectors[i][j][1] = entry1.getValue().sectors[i];
							tempPoint1.flag[i][j] = false;
						}
					}
					trafficPoints.put(tempPoint1.pointID, tempPoint1);
				}
			}
		}
		//setting next street and avenue intersection points
		for(Map.Entry<char[], TrafficPoint> entry1 : trafficPoints.entrySet()) {
			for(Map.Entry<char[], TrafficPoint> entry2 : trafficPoints.entrySet()) {
				tempPoint1 = entry1.getValue();
				tempPoint2 = entry2.getValue();
				difference = tempPoint1.distance(tempPoint2);
				if(tempPoint1.sectors[1][1][0] == tempPoint2.sectors[1][1][0]) {	//points on same avenue
					if(tempPoint1.roadDir[1] == 'N') { 		//same x position = same direction
						if(difference < 0) {
							if(tempPoint1.nextAvenue == null)
								tempPoint1.nextAvenue = tempPoint2;
							else if(difference > tempPoint1.distance(tempPoint1.nextAvenue))
								tempPoint1.nextAvenue = tempPoint2;
						}
					} else if(tempPoint1.roadDir[1] == 'S') {
						if(difference > 0) {
							if(tempPoint1.nextAvenue == null)
								tempPoint1.nextAvenue = tempPoint2;
							else if(difference < tempPoint1.distance(tempPoint1.nextAvenue))
								tempPoint1.nextAvenue = tempPoint2;
						}
					}
				} else if(tempPoint1.sectors[1][1][1] == tempPoint2.sectors[1][1][1]) {	//points on same street
					if(tempPoint1.roadDir[0] == 'E') {
						if(difference > 0) {
							if(tempPoint1.nextStreet == null)
								tempPoint1.nextStreet = tempPoint2;
							else if(difference < tempPoint1.distance(tempPoint1.nextStreet))
								tempPoint1.nextStreet = tempPoint2;
						}
					} else if(tempPoint1.roadDir[0] == 'W') {
						if(difference < 0) {
							if(tempPoint1.nextStreet == null)
								tempPoint1.nextStreet = tempPoint2;
							else if(difference > tempPoint1.distance(tempPoint1.nextStreet))
								tempPoint1.nextStreet = tempPoint2;
						}
					}
				}
			}
		}
		/*for(Map.Entry<char[], TrafficPoint> entry : trafficPoints.entrySet()) {
			tempPoint1 = entry.getValue();
			System.out.print(tempPoint1.pointID);
			System.out.print("\t");
			System.out.print(tempPoint1.roadDir[0]+" "+tempPoint1.roadDir[1]+"\t");
			if(tempPoint1.nextAvenue != null)
				System.out.print(tempPoint1.nextAvenue.pointID);
			else
				System.out.print("null");
			System.out.print("\t");
			if(tempPoint1.nextStreet != null)
				System.out.println(tempPoint1.nextStreet.pointID);
			else
				System.out.println("null");
		}*/
		return true;
	}
	
	private TrafficPoint(Road street, Road avenue, char[] control) {
		char[] streetID = new char[]{'0','0','0','0'};
		char[] avenueID = new char[]{'0','0','0','0'};
		if(avenue == null) {						//initialize as street
			streetID = street.roadID;
		} else if(street == null) {					//initialize as avenue
			avenueID = avenue.roadID;
		} else {									//initialize for both
			streetID = street.roadID;
			avenueID = avenue.roadID;
		}
		for(int i=0; i<pointID.length; i++){		//copying road IDs as traffic light ID
			if(i<4)
				this.pointID[i]=streetID[i];
			else
				this.pointID[i]=avenueID[i-4];
		}
		if(avenue == null) {			//initialize as street entrance or exit
			if(control[1] == 'N')
				this.pointID[0] = '1';
			else if(control[1] == 'X')
				this.pointID[0] = '2';
			//this.xy[1] = street.accumulativePosition;
			//based on entrance, exit and direction position assigned
			/*if((street.roadDir == 'E' && control[1] == 'N') || 		//from east entrance
					(street.roadDir == 'W' && control[1] == 'X'))	//from west exit
				this.xy[0] = Road.xAccumulativePosition;
			else if((street.roadDir == 'E' && control[1] == 'X') || 	//from east exit
					(street.roadDir == 'W' && control[1] == 'N'))	//from west entrance
				this.xy[0] = 0;*/
			this.roadDir[0] = street.roadDir;
			this.roadDir[1] = this.roadDir[0];
		} else if(street == null) {		//initialize as avenue entrance or exit
			if(control[1] == 'N')
				this.pointID[4] = '1';
			else if(control[1] == 'X')
				this.pointID[4] = '2';
			//this.xy[0] = avenue.accumulativePosition;
			/*if((avenue.roadDir == 'N' && control[1] == 'N') || 
					(avenue.roadDir == 'S' && control[1] == 'X'))
				this.xy[1] = 0;
			else if((avenue.roadDir == 'S' && control[1] == 'N') || 
					(avenue.roadDir == 'N' && control[1] == 'X'))
				this.xy[1] = Road.yAccumulativePosition;*/
			this.roadDir[0] = avenue.roadDir;
			this.roadDir[1] = this.roadDir[0];
		} else {						//initialize as intersection
			streetID = street.roadID;
			avenueID = avenue.roadID;
			//this.xy[0] = avenue.accumulativePosition;
			//this.xy[1] = street.accumulativePosition;
			this.roadDir[0] = street.roadDir;
			this.roadDir[1] = avenue.roadDir;
		}
		this.street = street;
		this.avenue = avenue;
		this.control = Arrays.copyOfRange(control, 0, 2);
	}
	
	public static Set<Map.Entry<char[] ,TrafficPoint>> getEntrySet() {
		return trafficPoints.entrySet();
	}
	
	/*public boolean emptyQueue() {
		if(this.control[0] == 'E') {		//entrance or exit point
			if(this.roadDir[0] == 'E' || this.roadDir[0] == 'W')		//direction same for entrance and exit
				return !this.carInQueueS;
			else if(this.roadDir[0] == 'N' || this.roadDir[0] == 'S')
				return !this.carInQueueA;
		} else
			return !(this.carInQueueA && this.carInQueueS);
		return false;
	}
	public boolean queueCar(Car nextInLine) {
		System.out.print("Q\t");
		System.out.print(this.pointID);
		System.out.print("\t");
		System.out.print(this.queuedCarsS+" "+this.queuedCarsA+"\t");
		System.out.println(nextInLine.carID);
		//if(nextInLine.inAQueue) return false;
		//else nextInLine.inAQueue = true;
		if(this.control[0] == 'E') {		//entrance or exit
			if(this.roadDir[0] == 'E' || this.roadDir[0] == 'W')
				return this.queueCarS(nextInLine);
			else if(this.roadDir[0] == 'N' || this.roadDir[0] == 'S')
				return this.queueCarA(nextInLine);
		} else if(nextInLine.dir == this.roadDir[0])
				return this.queueCarS(nextInLine);
		else if(nextInLine.dir == this.roadDir[1])
			return this.queueCarA(nextInLine);
		else
			System.out.println("Queuing Problem");
		return false;
	}
	private boolean queueCarS(Car nextInLine) {
		if(this.carInQueueS) {			//continue till last one and queue to it
			if(this.firstInLineS.queueCar(nextInLine)) {	//queue to next car
				queuedCarsS++;			//if car not previously queued here add it
				return true;
			} else			//car queued in previous cycle
				return false;
		}
		queuedCarsS++;
		this.firstInLineS = nextInLine;
		this.carInQueueS = true;
		return this.carInQueueS;
	}
	private boolean queueCarA(Car nextInLine) {
		if(this.carInQueueA) {
			if(this.firstInLineA.queueCar(nextInLine)) {
				queuedCarsA++;
				return true;
			} else
				return false;
		}
		queuedCarsA++;
		this.firstInLineA = nextInLine;
		this.carInQueueA = true;
		return this.carInQueueA;
	}
	
	public boolean Dequeue(Car tempCar1) {		//dequeue a car from a traffic point
		System.out.print("DS\t");
		System.out.print(this.pointID);
		System.out.print("\t");
		System.out.print(this.queuedCarsS+" "+this.queuedCarsA+"\t");
		System.out.print(tempCar1.carID);
		Car tempCar2;
		if(this.roadDir[0] == tempCar1.dir)
			tempCar2 = this.firstInLineS;
		else if(this.roadDir[1] == tempCar1.dir)
			tempCar2 = this.firstInLineA;
		else return false;
		boolean found = (tempCar1 == tempCar2);
		if(found) {
			this.Dequeue();
			return found;
		}
		while(!found && tempCar2 != null) {
			//System.out.print(".");
			if(tempCar1 == tempCar2.nextInLine) {
				found = true;
				tempCar2.Dequeue();
				//tempCar1.inAQueue = false;
			} else
				tempCar2 = tempCar2.nextInLine;
		}
		if(found) {
			if(this.roadDir[0] == tempCar1.dir)
				this.queuedCarsS--;
			else if(this.roadDir[1] == tempCar1.dir)
				this.queuedCarsA--;
		}
		return found;
	}
	
	public Car Dequeue() {
		Car tempCar = null;
		System.out.print("D\t");
		System.out.print(this.pointID);
		System.out.print("\t");
		System.out.print(this.queuedCarsS+" "+this.queuedCarsA+"\t");
		if(this.control[0] == 'E') {	//entrance or exit point
			if(this.roadDir[0] == 'E' || this.roadDir[0] == 'W')		//direction same for entrance and exit
				tempCar = this.dequeueS();
			else if(this.roadDir[0] == 'N' || this.roadDir[0] == 'S')
				tempCar = this.dequeueA();
		} else if(this.control[0] != 'R')
			tempCar = this.dequeueS();
		else if(this.control[1] != 'R')
			tempCar = this.dequeueA();
		if(tempCar != null) {
			tempCar.isQueued = false;
			//tempCar.inAQueue = false;
			tempCar.nextInLine = null;
		} else
			System.out.println("Dequeuing Problem");
		return tempCar;
	}
	private Car dequeueS() {
		if(!this.carInQueueS)
			return null;
		queuedCarsS --;
		Car tempCar = this.firstInLineS;
		this.firstInLineS = tempCar.nextInLine;
		if(this.firstInLineS == null)
			this.carInQueueS = false;
		//System.out.println(tempCar.carID);
		tempCar.nextInLine = null;
		return tempCar;
	}
	private Car dequeueA() {
		if(this.carInQueueA == false)
			return null;
		queuedCarsA --;
		Car tempCar = this.firstInLineA;
		this.firstInLineA = tempCar.nextInLine;
		if(this.firstInLineA == null)
			carInQueueA = false;
		//System.out.println(tempCar.carID);
		tempCar.nextInLine = null;
		return tempCar;
	}*/
	
	public boolean nextControl() {
		if(this.control[0] == 'E')		//not street or avenue
			return false;
		else if(control[0] == 'R') {
			if(control[1] == 'R') {
				control[0] = 'G';
				this.expectedCars[0] = 0;
				this.nextStreet.expectedCars[0] += this.expectedStraightCars[0];
				this.nextAvenue.expectedCars[1] += this.expectedTurningCars[0];
			} else if(control[1] == 'G')
				control[1] = 'Y';
			else if(control[1] == 'Y') {
				control = new char[]{'G','R'};
				this.expectedCars[0] = 0;
				this.nextStreet.expectedCars[0] += this.expectedStraightCars[0];
				this.nextAvenue.expectedCars[1] += this.expectedTurningCars[0];
			}
		} else if(control[0] == 'G') {
			control[0] = 'Y';
		} else if(control[0] == 'Y') {
			control = new char[]{'R','G'};
			this.expectedCars[1] = 0;
			this.nextAvenue.expectedCars[1] += this.expectedStraightCars[1];
			this.nextStreet.expectedCars[0] += this.expectedTurningCars[1];
		} else
			control = new char[]{'R','R'};
		/*System.out.print(this.pointID);
		System.out.println(" #Cars S= "+this.comingCars[0]+" A= "+this.comingCars[1]);*/
		return true;
	}
	
	public int distance(TrafficPoint tempPoint) {
		if(this.sectors[1][1][0] == tempPoint.sectors[1][1][0])
			return this.sectors[1][1][1] - tempPoint.sectors[1][1][1];
		else if(this.sectors[1][1][1] == tempPoint.sectors[1][1][1])
			return this.sectors[1][1][0] - tempPoint.sectors[1][1][0];
		else
			return Integer.MAX_VALUE;
	}
	public int distance(Car tempCar) {
		if(tempCar.dir == 'N') {
			if(tempCar.xy[0] == this.sectors[0][0][0])
				return this.sectors[0][0][1] - tempCar.xy[1];
			else if(tempCar.xy[0] == this.sectors[0][1][0])
				return this.sectors[0][1][1] - tempCar.xy[1];
			else if(tempCar.xy[0] == this.sectors[0][2][0])
				return this.sectors[0][2][1] - tempCar.xy[1];
		} else if(tempCar.dir == 'S') {
			if(tempCar.xy[0] == this.sectors[2][0][0])
				return this.sectors[2][0][1] - tempCar.xy[1];
			else if(tempCar.xy[0] == this.sectors[2][1][0])
				return this.sectors[2][1][1] - tempCar.xy[1];
			else if(tempCar.xy[0] == this.sectors[2][2][0])
				return this.sectors[2][2][1] - tempCar.xy[1];
		} else if(tempCar.dir == 'E') {
			if(tempCar.xy[1] == this.sectors[0][2][1])
				return this.sectors[0][2][0] - tempCar.xy[0];
			else if(tempCar.xy[1] == this.sectors[1][2][1])
				return this.sectors[1][2][0] - tempCar.xy[0];
			else if(tempCar.xy[1] == this.sectors[2][2][1])
				return this.sectors[2][2][0] - tempCar.xy[0];
		} else if(tempCar.dir == 'W') {
			if(tempCar.xy[1] == this.sectors[0][0][1])
				return this.sectors[0][0][0] - tempCar.xy[0];
			else if(tempCar.xy[1] == this.sectors[1][0][1])
				return this.sectors[1][0][0] - tempCar.xy[0];
			else if(tempCar.xy[1] == this.sectors[2][0][1])
				return this.sectors[2][0][0] - tempCar.xy[0];
		}
		return Integer.MAX_VALUE;
	}
	//FUNCTION NEEDS CODE OPTIMIZATION
	public boolean[] intersectionLogic(Car tempCar, int[] dist) {
		/*
		 * [0] true=move, false=don't move, [1] true=dequeue, false=don't dequeue
		 */
		boolean[] TT = new boolean[]{true, true};
		boolean[] TF = new boolean[]{true, false};
		boolean[] FF = new boolean[]{false, false};
		if(this.control[1] == 'X') return TF;
		int i, j, di, dj;
		for(i=0; i<4; i++) {
			if(i == 3) break;
			if(tempCar.xy[0] <= this.sectors[0][i][0]) break;
		}
		for(j=0; j<4; j++) {
			if(j == 3) break;
			if(tempCar.xy[1] <= this.sectors[j][0][1]) break;
		}
		for(di=0; di<4; di++) {
			if(di == 3) break;
			if((tempCar.xy[0]+dist[0]) <= this.sectors[0][di][0]) break;
		}
		for(dj=0; dj<4; dj++) {
			if(dj == 3) break;
			if((tempCar.xy[1]+dist[1]) <= this.sectors[dj][0][1]) break;
		}
		
		if(tempCar.dir == 'N') {
			if(dj == 0 && this.distance(tempCar) > Frame.fullDistance)	//didn't reach intersection
					return TF;
		} else if(tempCar.dir == 'S') {
			if(dj == 3 && this.distance(tempCar) < -1*Frame.fullDistance)
				return TF;
		} else if(tempCar.dir == 'E') {
			if(di == 3 && this.distance(tempCar) < -1*Frame.fullDistance)
				return TF;
		} else if(tempCar.dir == 'W') {
			if(di == 0 && this.distance(tempCar) > Frame.fullDistance)
				return TF;
		}
		//limit speed in intersection
		if(Math.abs(tempCar.dxy[0]+tempCar.dxy[1]) >= Frame.carWidth) 
			TF = FF;
		
		if(this != tempCar.turningPoint1 && this != tempCar.turningPoint2) {	//going straight
			if(tempCar.dir == 'N') {		//difference between js, i == di
				if(dj == 0 && this.control[1] == 'R') {
					return FF;
				} else if(j == dj) {			//moving in same square - sector
					return TF;
				} else if(dj == 0 || dj == 1) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 2) {
					if(!this.flag[dj][i]) {
						this.flag[0][i] = false;
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 3) {
					this.flag[1][i] = false;
					this.flag[2][i] = false;
					return TT;
				}
			} else if(tempCar.dir == 'S') {
				if(dj == 3 && this.control[1] == 'R') {
					return FF;
				} else if(j == dj) {
					return TF;
				} else if(dj == 3 || dj == 2) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 1) {
					if(!this.flag[dj][i]) {
						this.flag[2][i] = false;
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 0) {
					this.flag[1][i] = false;
					this.flag[2][i] = false;
					return TT;
				}
			} else if(tempCar.dir == 'E') {
				if(di == 3 && this.control[0] == 'R') {
					return FF;
				} else if(i == di) {
					return TF;
				} else if(di == 3 || di == 2) {
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 1) {
					if(!this.flag[j][di]) {
						this.flag[j][2] = false;
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 0) {
					this.flag[j][1] = false;
					this.flag[j][2] = false;
					return TT;
				}
			} else if(tempCar.dir == 'W') {
				if(di == 0 && this.control[0] == 'R') {
					return FF;
				} else if(i == di) {
					return TF;
				} else if(di == 0 || di == 1)	{
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 2) {
					if(!this.flag[j][di]) {
						this.flag[j][0] = false;
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 3) {
					this.flag[j][1] = false;
					this.flag[j][2] = false;
					return TT;
				}
			}
		} else if(this == tempCar.turningPoint1 || this == tempCar.turningPoint2) {
			if(tempCar.dir == 'N') {		//difference between js, i == di
				if(dj == 0 && this.control[1] == 'R') {
					return FF;
				}
				//determine the turning lane
				if(j == dj && dj == 0 && tempCar.remainingTurns == 1) {
					int rand = (int)(Math.random()*3);
					if(rand == 0) tempCar.lane = 'L';
					else if(rand == 1) tempCar.lane = 'M';
					else if(rand == 2) tempCar.lane = 'R';
					/*System.out.print(tempCar.carID);
					System.out.println(" turning lane "+tempCar.lane);*/
				} else if(tempCar.remainingTurns > 1) {
					if(tempCar.turningPoint1.roadDir[0] == 'E')
						tempCar.lane = 'L';
					else if(tempCar.turningPoint1.roadDir[0] == 'W')
						tempCar.lane = 'R';
				}
				
				if(j == dj)				//moving in same square - sector
					return TF;
				
				if(dj == 0) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 1) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						if(tempCar.lane == 'L') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[0];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[dj-1][i], 0, 2);
							this.flag[dj][i] = false;
							return TT;
						} 
						return TF;
					} return FF;
				} else if(dj == 2) {
					if(!this.flag[dj][i]) {
						this.flag[0][i] = false;
						this.flag[dj][i] = true;
						if(tempCar.lane == 'M') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[0];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[dj-1][i], 0, 2);
							this.flag[dj][i] = false;
							this.flag[dj-1][i] = false;
							return TT;
						}
						return TF;
					} return FF;
				} else if(dj == 3) {
					this.flag[1][i] = false;
					this.flag[2][i] = false;
					//if not right or middle lane then got to be left
					tempCar.road = this.street;
					//tempCar.nextPoint = this.nextStreet;
					tempCar.remainingTurns--;
					tempCar.dir = this.roadDir[0];
					tempCar.switchSpeed();
					tempCar.xy = Arrays.copyOfRange(this.sectors[dj-1][i], 0, 2);
					this.flag[dj-1][i] = false;
					return TT;
				} else return FF;
			} else if(tempCar.dir == 'S') {
				if(dj == 3 && this.control[1] == 'R') {
					return FF;
				}
				//determine the turning lane
				if(j == dj && dj == 3 && tempCar.remainingTurns == 1) {
					int rand = (int)(Math.random()*3);
					if(rand == 0) tempCar.lane = 'L';
					else if(rand == 1) tempCar.lane = 'M';
					else if(rand == 2) tempCar.lane = 'R';
					/*System.out.print(tempCar.carID);
					System.out.println(" turning lane "+tempCar.lane);*/
				} else if(tempCar.remainingTurns > 1) {
					if(tempCar.turningPoint1.roadDir[0] == 'E')
						tempCar.lane = 'R';
					else if(tempCar.turningPoint1.roadDir[0] == 'W')
						tempCar.lane = 'L';
				}
				if(j == dj)				//moving in same square - sector
					return TF;
				
				if(dj == 3) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						return TF;
					} return FF;
				} else if(dj == 2) {
					if(!this.flag[dj][i]) {
						this.flag[dj][i] = true;
						if(tempCar.lane == 'R') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[0];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[dj][i], 0, 2);
							this.flag[dj][i] = false;
							return TT;
						} 
						return TF;
					} return FF;
				} else if(dj == 1) {
					if(!this.flag[dj][i]) {
						this.flag[2][i] = false;
						this.flag[dj][i] = true;
						if(tempCar.lane == 'M') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[0];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[dj][i], 0, 2);
							this.flag[dj][i] = false;
							this.flag[dj+1][i] = false;
							return TT;
						}
						return TF;
					} return FF;
				} else if(dj == 0) {
					this.flag[1][i] = false;
					this.flag[2][i] = false;
					if(tempCar.lane == 'L') {
						tempCar.road = this.street;
						//tempCar.nextPoint = this.nextStreet;
						tempCar.remainingTurns--;
						tempCar.dir = this.roadDir[0];
						tempCar.switchSpeed();
						tempCar.xy = Arrays.copyOfRange(this.sectors[dj][i], 0, 2);
						this.flag[dj][i] = false;
						this.flag[dj+1][i] = false;
						return TT;
					}
					return TT;
				}
			} else if(tempCar.dir == 'E') {
				if(di == 3 && this.control[0] == 'R') {
					return FF;
				}
				//determine the turning lane
				if(i == di && di == 3 && tempCar.remainingTurns == 1) {
					int rand = (int)(Math.random()*3);
					if(rand == 0) tempCar.lane = 'L';
					else if(rand == 1) tempCar.lane = 'M';
					else if(rand == 2) tempCar.lane = 'R';
					/*System.out.print(tempCar.carID);
					System.out.println(" turning lane "+tempCar.lane);*/
				} else if(tempCar.remainingTurns > 1) {
					if(tempCar.turningPoint1.roadDir[1] == 'N')
						tempCar.lane = 'L';
					else if(tempCar.turningPoint1.roadDir[1] == 'S')
						tempCar.lane = 'R';
				}
				if(i == di)				//moving in same square - sector
					return TF;
				
				if(di == 3) {
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 2) {
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						if(tempCar.lane == 'R') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[1];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[j][di], 0, 2);
							this.flag[j][di] = false;
							return TT;
						} 
						return TF;
					}
				}
				else if(di == 1) {
					if(!this.flag[j][di]) {
						this.flag[j][2] = false;
						this.flag[j][di] = true;
						if(tempCar.lane == 'M') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[1];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[j][di], 0, 2);
							this.flag[j][di] = false;
							this.flag[j][di+1] = false;
							return TT;
						} 
						return TF;
					} return FF;
				} else if(di == 0) {
					this.flag[j][1] = false;
					this.flag[j][2] = false;
					tempCar.road = this.street;
					//tempCar.nextPoint = this.nextStreet;
					tempCar.remainingTurns--;
					tempCar.dir = this.roadDir[1];
					tempCar.switchSpeed();
					tempCar.xy = Arrays.copyOfRange(this.sectors[j][di], 0, 2);
					this.flag[j][di] = false;
					this.flag[j][di+1] = false;
					return TT;
				}
			} else if(tempCar.dir == 'W') {
				if(di == 0 && this.control[0] == 'R') {
					return FF;
				}
				//determine the turning lane
				if(i == di && di == 0 && tempCar.remainingTurns == 1) {
					int rand = (int)(Math.random()*3);
					if(rand == 0) tempCar.lane = 'L';
					else if(rand == 1) tempCar.lane = 'M';
					else if(rand == 2) tempCar.lane = 'R';
					/*System.out.print(tempCar.carID);
					System.out.println(" turning lane "+tempCar.lane);*/
				} else if(tempCar.remainingTurns > 1) {
					if(tempCar.turningPoint1.roadDir[1] == 'N')
						tempCar.lane = 'L';
					else if(tempCar.turningPoint1.roadDir[1] == 'S')
						tempCar.lane = 'R';
				}
				if(i == di)				//moving in same square - sector
					return TF;
				
				
				if(di == 0)	{
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						return TF;
					} return FF;
				} else if(di == 1) {
					if(!this.flag[j][di]) {
						this.flag[j][di] = true;
						if(tempCar.lane == 'L') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[1];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[j][di-1], 0, 2);
							this.flag[j][di] = false;
							this.flag[j][di-1] = false;
							return TT;
						}
						return TF;
					} return FF;
				} else if(di == 2) {
					if(!this.flag[j][di]) {
						this.flag[j][0] = false;
						this.flag[j][di] = true;
						if(tempCar.lane == 'M') {
							tempCar.road = this.street;
							//tempCar.nextPoint = this.nextStreet;
							tempCar.remainingTurns--;
							tempCar.dir = this.roadDir[1];
							tempCar.switchSpeed();
							tempCar.xy = Arrays.copyOfRange(this.sectors[j][di-1], 0, 2);
							this.flag[j][di] = false;
							this.flag[j][di-1] = false;
							return TT;
						}
						return TF;
					} return FF;
				} else if(di == 3) {
					this.flag[j][1] = false;
					this.flag[j][2] = false;
					tempCar.road = this.street;
					//tempCar.nextPoint = this.nextStreet;
					tempCar.remainingTurns--;
					tempCar.dir = this.roadDir[1];
					tempCar.switchSpeed();
					tempCar.xy = Arrays.copyOfRange(this.sectors[j][di-1], 0, 2);
					this.flag[j][di-1] = false;
					return TT;
				}
			}
			//-------------------------
		}
		return FF;
	}
}

