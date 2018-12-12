import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class Brain {
	// Constants
	final int MAX_WEIGHT = 3500; //kg
	final int MIN_WEIGHT = 2500;  //kg
	final int stageSize = 18;
	final int tsaSize	= 3;
	
	// Variables
	/*
	 * the Stage is represented by a nx3x2 array. bottom row uses 0..n-1, middle row uses 0..n-2, top row - 0..n-3
	 * 0 represents an empty place in the array
	 * stage_global[..][..][0] are coil weights, and stage_global[..][..][1] are coil IDs
 	 */
	int[][][] stage_global;
	int[][][] tsa_global;
	static Crane crane;


	// Constructors
	Brain() {
		crane = new Crane();
		stage_global = new int[stageSize][3][2];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < stageSize; j++) {
				stage_global[j][i][0] = 0; // empty
				stage_global[j][i][1] = 0;
			}
		}
		tsa_global = new int[6][2][2];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 6; j++) {
				tsa_global[j][i][0] = 0;
				tsa_global[j][i][1] = 0;
			}
		}
 	}


 	// a constructor for testing purposes. can customize the initial stage and test push/pull methods afterwards
	// arr contains coil weights, startng from top-left (stage[0][2]) if the stage and always going left-to-right.
	Brain (int[] arr) {
		crane = new Crane();
		stage_global = new int[stageSize][3][2];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < stageSize - 2 + i; j++) {
				// To turn the one dimensional input array into 2-dimensional stage, we need a function that does the following mappings:
				// for j = 0, 1, 2, ..., 15, 	arr[j] --> stage[j][2][0]
				// for j = 16, 17, ..., 32,  	arr[j] --> stage[j][1][0]
				// for j = 33, 34, ..., 50,  	arr[j] --> stage[j][0][0]
				// this mapping is satisfied by the polynomial arr[i * 16 + i*(i-1)/2 + j] --> stage[j][2-i]
				stage_global[j][2-i][0] = arr[i*(stageSize - 2) + i*(i-1)/2 + j];
				stage_global[j][2-i][1] = i*(stageSize - 2) + i*(i-1)/2 + j;
			}
		}
		tsa_global = new int[6][2][2];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 6; j++) {
				tsa_global[j][i][0] = 0;
				tsa_global[j][i][1] = 0;
			}
		}
	}

	// Methods
	/**
	 * Pushes a coil with the given weight and id to the current stage_global
	 * @param weight
	 * @param id
	 */
	public int push(int weight, int id) {
		System.out.println("\n----- Pushing " + weight + " -----");
		// the incoming coil is on (4, 0) on tsa_global, which is reserved for the truck
		tsa_global[4][0][0] = weight;
		tsa_global[4][0][1] = id;

		// the instructions to push a coil
		Stack<Integer> pushingPath = new Stack<>();

		// get the most optimal pushing path
		int pushCost = pushCoil(pushingPath, weight, stage_global, tsa_global, null, 4, 0, true, false);

		// carry out the steps returned in the pushingPath
		followInstructions(pushingPath, stage_global, tsa_global, 0, pushingPath.size() - 1, true);

		if (pushCost < 100)
			System.out.println("----- Coil pushed in " + pushCost + " step(s) -----");
		else
			System.out.println("----- Couldn't push coil -----");

		showStage();
		return pushCost;
	}

	/**
	 * returns the cost and initializes the pushingPath argument (passed as a parameter) with the best instructions to push coil to current stage_global, to places other than forbiddenPlaces.
	 * @param pushingPath the steps to push; to be modified by the function
	 * @param weight weight of the coil
	 * @param stage
	 * @param tsa
	 * @param forbiddenPlaces
	 * @param srcX    the x coordinate of the coil (at its place before push)
	 * @param srcY    the y coordinate of the coil (at its place before push)
	 * @param fromTsa
	 * @return
	 */
	public int pushCoil(Stack<Integer> pushingPath, int weight, int[][][] stage, int[][][] tsa, ArrayList forbiddenPlaces, int srcX, int srcY, boolean fromTsa, boolean inOneStep) {
		// values pertaining to the place to insert, used to decide the best one
		Stack<Integer> minShortestPath = new Stack<>();
		int minCost = 100; // some large value
		int minReplacedWeight = 0; // weight of the coil to be replaced
		int minReplacedLighterParent = 0; // weight of the lighter coil that is under the coil/place to be replaced
		int minReplacedParentSum = 0; // sum of weights of the coils under the coil/place to be replaced

		// loop through all places on Stage, considering only the ones that can support weight and hold a lighter coil than weight
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < stageSize-i; j++) {
				if (stageCanSupport(j, i, weight, stage)) {
					if (inOneStep ? stage[j][i][0] == 0 : stage[j][i][0] < weight) {

						// initialize local indicators of this particular case (j,i) that will be compared against minimum values (e.g. minCost) to choose the best
						int[] cost = {0}; // the number of steps to push weight to stage_global[j][i]
						// we use cost as an array to be able to update its value from a called function that has it as a parameter
						int replacedWeight = stage[j][i][0]; // weight of the coil to be replaced
						int replacedLighterParent; // the weight of the lighter coil which is under this coil at (j, i)
						int replacedParentSum; // sum of weights of the coils under (j, i)
						if (i == 0) {
							replacedLighterParent = 5000; // some large value heavier that all coils
							replacedParentSum = 10000; // 2 * (that large value)
						} else {
							replacedLighterParent = stage[j][i - 1][0] < stage[j + 1][i - 1][0] ? stage[j][i - 1][0] : stage[j + 1][i - 1][0];
							replacedParentSum = stage[j][i - 1][0] + stage[j + 1][i - 1][0];
						}

						// the shortest path to push weight into this place (j,i)
						Stack<Integer> shortestPath = new Stack<>();
						if (inOneStep) {
							move(srcX, srcY, j, i, fromTsa ? 1 : 2, stage, tsa, shortestPath, null);
							cost[0]++;
						} else
							findShortestPath(shortestPath, j, i, stage, tsa, cost, true, forbiddenPlaces, fromTsa, minCost, srcX, srcY);

						// decide if this case is better than the case currently thought the be best
						if (cost[0] < minCost || (cost[0] == minCost && replacedWeight < minReplacedWeight) || (cost[0] == minCost && stage[j][i][0] == minReplacedWeight && replacedLighterParent < minReplacedLighterParent)
								|| (cost[0] == minCost && stage[j][i][0] == minReplacedWeight && replacedLighterParent == minReplacedLighterParent && replacedParentSum < minReplacedParentSum)) {
							minCost = cost[0];
							minReplacedWeight = replacedWeight;
							minReplacedLighterParent = replacedLighterParent;
							minReplacedParentSum = replacedParentSum;
							minShortestPath = (Stack<Integer>) shortestPath.clone();
						}

					}
				}
			}
		}


		// append best shortest path to pushing path
		for (int i = 0; i < minShortestPath.size(); i++) {
			pushingPath.push(minShortestPath.elementAt(i));
		}

		return minCost;
	}

	/**
	 * Gives a representation of the current stage_global
	 * x for empty, weight for occupied
	 */
	public void showStage() {
		for (int i = 2; i >= 0; i--) {
			for (int q = 0; q < i; q++)
				System.out.print("  ");
			for (int j = 0; j < stageSize - i; j++) {
				if (stage_global[j][i][0] <= 0) {
					System.out.print("xx  ");
				} else {
					System.out.print(stage_global[j][i][0] + "  ");
				}
			}
			System.out.println();
		}
	}

	/**
	 * Find out if location (x, y) on the Stage can support a weight of w
	 * @param x
	 * @param y
	 * @param w
	 * @return
	 */
	public boolean stageCanSupport(int x, int y, int w, int[][][] stage) { // assuming (x,y) are valid coordinates
		if (y == 0)
			return true;
		else if (x + y >= stageSize)
			return false;
		else if (stage[x][y-1][0] > 0 && stage[x+1][y-1][0] > 0)
			return stage[x][y-1][0] >= w && stage[x+1][y-1][0] >= w;
		else 
			return false;
	}

	/**
	 * Find out if location (x, y) on the tsa_global can support a weight of w
	 * @param x
	 * @param y
	 * @param w
	 * @return
	 */
	public boolean tsaCanSupport(int x, int y, int w, int[][][] tsa) { // assuming (x,y) are valid coordinates
		if (y == 0)
			return true;
		else if (x + y >= tsaSize)
			return false;
		else if (tsa[x][y-1][0] > 0 && tsa[x+1][y-1][0] > 0)
			return tsa[x][y-1][0] >= w && tsa[x+1][y-1][0] >= w;
		else
			return false;
	}

	/**
	 * Move a coil from (x1, y1) to (x2, y2)
	 * Modes:
	 * 0 for Stage --> TSA
	 * 1 for Tsa --> Stage
	 * 2 for Stage --> Stage
	 * 3 for TSA --> TSA
	 * update the coilsToMoveBack accordingly and log the move in the shortestPath
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param mode
	 * @param stage
	 * @param tsa
	 * @param shortestPath
	 * @param coilsToMoveBack
	 */
	public void move(int x1, int y1, int x2, int y2, int mode, int[][][] stage, int[][][] tsa, Stack<Integer> shortestPath, ArrayList<Integer> coilsToMoveBack) {
		// push the steps into the stack to track them
		shortestPath.push(mode); // mode
		shortestPath.push(y2); // v_i
		shortestPath.push(x2); // u_i
		shortestPath.push(y1); // y_i
		shortestPath.push(x1); // x_i

		// add Coils that were moved to TSA to the coilsToMoveBack
		if (mode == 0) {
			if (x2 >= 0 && x2 < 3) {
				coilsToMoveBack.add(x2);
				coilsToMoveBack.add(y2);
			}
		} else if (mode == 3) { // it didn't work for mode == 1 because the coilsToMoveBack here does not contain the actually moved coil; it is supposed to hold the coils that will be replaced when pushing this coil
			if (x2 >= 0 && x2 < 3 && x1 >= 0 && y1 < 3) {
				// add x2, y2 and remove x1, y1
				for (int i = 0; i < coilsToMoveBack.size(); i += 2) {
					if (coilsToMoveBack.get(i) == x1 && coilsToMoveBack.get(i+1) == y1) {
						coilsToMoveBack.remove(i+1);
						coilsToMoveBack.remove(i);
					}
				}

				coilsToMoveBack.add(x2);
				coilsToMoveBack.add(y2);
			}
		}

		// physically move coil
		crane.move(x1, y1, x2, y2, mode, stage, tsa);
	}

	/**
	 * Pull the coil at with the specified id from Stage
	 * @param id
	 */
	public void pull(int id) {
		System.out.println("\n--- Pulling coil with id " + id + " ---");

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < stageSize - i; j++) {
				if (id == stage_global[j][i][1]) {
					if (stage_global[j][i][0] != 0) {
						pull(j, i);
						return;
					}
				}
			}
		}
		System.out.println("--- Couldn't find a coil with the specified ID: " + id + " ---");
	}

	/**
	 * Pull the coil at (x, y) from Stage
	 * @param x
	 * @param y
	 */
	public int pull(int x, int y) {
		if (stage_global[x][y][0] == 0) {
			System.out.println("ERROR: There is no coil at the specified place on Stage.");
			return 0;
		} else {
			System.out.println("\n--- Pulling coil with mass " + stage_global[x][y][0] + " from (" + x + ", " + y + ") ---");

			int[] cost = {0}; // cost is the number of steps required to pull the coil from (x, y)

			Stack<Integer> shortestPath = new Stack<>();
			findShortestPath( shortestPath, x, y, stage_global, tsa_global, cost, false, null, true, 100, -1, -1); // -1s not used anyway

			if (cost[0] < 100)
				System.out.println("--- Coil pulled in " + cost[0] + " steps ---");
			else {
				System.out.println("--- Couldn't  pull coil ---");
				return 100 + cost[0];
			}

			// carry out the steps in shortestPath
			followInstructions( shortestPath, stage_global, tsa_global, 0, shortestPath.size() - 1, true);

			showStage();
			return cost[0];
		}
	}

	/**
	 * Find and return (by parameter) the shortest way to push/pull a coil to/from (destX, destY) on Stage
	 * @param shortestPath
	 * @param destX
	 * @param destY
	 * @param stage
	 * @param tsa
	 * @param costSoFar
	 * @param push
	 * @param forbiddenPlaces
	 * @param fromTsa
	 * @param minCostSoFar
	 * @param srcX
	 * @param srcY
	 */
	public void findShortestPath(Stack<Integer> shortestPath, int destX, int destY, int[][][] stage, int[][][] tsa, int[] costSoFar, boolean push, ArrayList<Integer> forbiddenPlaces, boolean fromTsa, int minCostSoFar, int srcX, int srcY) {
		// clone stage_global
		int[][][] stage_copy = new int[stageSize][3][2];
		cloneArray(stage_copy, stage, stageSize, 3, 2);
		// clone tsa_global
		int[][][] tsa_copy = new int[6][2][2];
		cloneArray(tsa_copy, tsa, 6, 2, 2);

		// empty [j][i]
		ArrayList<Integer> coilsToMoveBack = new ArrayList<>(); // to hold the coordinates of coils in TSA to be carried back
		costSoFar[0] += emptySmart(destX, destY, stage_copy, tsa_copy, shortestPath, coilsToMoveBack, push);

		// push/pull the desired coil
		if (push) {
			move(srcX, srcY, destX, destY, fromTsa ? 1 : 2, stage_copy, tsa_copy, shortestPath, coilsToMoveBack); // tsa_global(4,0) is reserved for the truck
		}
		else {
			move(destX, destY, 5, 0, 0, stage_copy, tsa_copy, shortestPath, coilsToMoveBack); // tsa_global(5,0) is reserved for the cutting machine
		}
		costSoFar[0]++; // putting inW

		// move coils on TSA back on Stage, from the heaviest to the lightest
		while (! coilsToMoveBack.isEmpty()) {
			// Check triangle case: if there are 3 coils on tsa and one is on top of the other 2
			if (!push) {
				boolean scaleneTriangle = false; // if there is a triangle and its top is lighter than both of the bottom 2 coils
				int triangleTopX = -1; // the x coordinate of the top of the triangle (if scaleneTriangle)
				if (tsa_copy[0][0][0] == 0 && tsa_copy[1][1][0] != 0 && tsa_copy[1][1][0] != tsa_copy[1][0][0] && tsa_copy[1][1][0] != tsa_copy[2][0][0]) {
					scaleneTriangle = true;
					triangleTopX = 1;
				} else if (tsa_copy[2][0][0] == 0 && tsa_copy[0][1][0] != 0 && tsa_copy[0][1][0] != tsa_copy[0][0][0] && tsa_copy[0][1][0] != tsa_copy[1][0][0]) {
					scaleneTriangle = true;
					triangleTopX = 0;
				}

				if (scaleneTriangle) {
					System.out.println("scalene");
					// check if the top of the triangle can fit to stage_global in 1 step.
					Stack<Integer> temp = new Stack<>();
					int tempSizeBeforePush = temp.size();

					if (pushCoil(temp, tsa_copy[triangleTopX][1][0], stage_copy, tsa_copy, new ArrayList<>(Arrays.asList(destX - 1, destY + 1, destX, destY + 1)), triangleTopX, 1, true, false) == 1) {
						followInstructions(temp, stage_copy, tsa_copy, tempSizeBeforePush, temp.size() - 1, false);
						costSoFar[0]++;

						// append temp to shortestPath
						for (int r = 0; r < temp.size(); r++) {
							shortestPath.push(temp.elementAt(r));
						}

//						 remove from coilsToMoveBack
						for (int p = 0; p < coilsToMoveBack.size(); p += 2) {
							if (coilsToMoveBack.get(p) == triangleTopX && coilsToMoveBack.get(p+1) == 1) {
								coilsToMoveBack.remove(p + 1);
								coilsToMoveBack.remove(p);
							}
						}
					} else {
						tempSizeBeforePush = 0;
						temp.clear();
						int heaviest = tsa_copy[triangleTopX][0][0] >= tsa_copy[triangleTopX+1][0][0] ? tsa_copy[triangleTopX][0][0] : tsa_copy[triangleTopX+1][0][0];
						int secondHeaviest = tsa_copy[triangleTopX][0][0] < tsa_copy[triangleTopX+1][0][0] ? tsa_copy[triangleTopX][0][0] : tsa_copy[triangleTopX+1][0][0];
						boolean canMakePlaceWithAHelpFromNeighbor = destX == 1 ? (stage_copy[destX + 1][destY + 1][0] >= secondHeaviest && stage_copy[destX + 1][destY][0] >= heaviest) : (stage_copy[destX - 2][destY + 1][0] >= secondHeaviest && stage_copy[destX - 1][destY][0] >= heaviest) || (stage_copy[destX + 1][destY + 1][0] >= secondHeaviest && stage_copy[destX + 1][destY][0] >= heaviest);

						if (canMakePlaceWithAHelpFromNeighbor) {
							ArrayList<Integer> forbidden = new ArrayList<>();
							if (destX == 1? false : stage_copy[destX - 2][destY + 1][0] >= secondHeaviest && stage_copy[destX + 1][destY + 1][0] < secondHeaviest) {
								forbidden.add(destX - 1);
								forbidden.add(destY + 1);
							} else if (destX == 1? stage_copy[destX + 1][destY + 1][0] >= secondHeaviest : stage_copy[destX - 2][destY + 1][0] < secondHeaviest && stage_copy[destX + 1][destY + 1][0] >= secondHeaviest) {
								forbidden.add(destX);
								forbidden.add(destY + 1);
							}
							costSoFar[0] += pushCoil(temp, tsa_copy[triangleTopX][1][0], stage_copy, tsa_copy, forbidden, triangleTopX, 1, true, false);


							// remove from coilsToMoveBack
							for (int p = 0; p < coilsToMoveBack.size(); p += 2) {
								if (coilsToMoveBack.get(p) == triangleTopX && coilsToMoveBack.get(p+1) == 1) {
									coilsToMoveBack.remove(p + 1);
									coilsToMoveBack.remove(p);
									break;
								}
							}

							followInstructions(temp, stage_copy, tsa_copy, tempSizeBeforePush, temp.size() - 1, false);

						} else {
							// if trianleTopX == 0, then move(0,1,2,0,3) else if tirangleTopX == 1, then move (1,1,0,0,3)
							int emptyPlaceX = 2-2*triangleTopX; // coordinate of the empty place on TSA
							move(triangleTopX, 1, emptyPlaceX, 0, 3, stage_copy, tsa_copy, temp, coilsToMoveBack);
						}

						// append temp to shortestPath
						for (int r = 0; r < temp.size(); r++) {
							shortestPath.push(temp.elementAt(r));
						}

					}
				}
			}

			// find the heaviest free coil
			int heaviestIndex = findHeaviestFreeCoil(tsa_copy, coilsToMoveBack, false);

			// check if the lighter of the level 0 can be pushed in 1 step to stage_global not the newly emptied spot. if it can,
			int a = coilsToMoveBack.get(heaviestIndex);
			int b = coilsToMoveBack.get(heaviestIndex + 1);
			coilsToMoveBack.remove(heaviestIndex + 1);
			coilsToMoveBack.remove(heaviestIndex);

			int shortestPathSizeBeforePush = shortestPath.size();

			costSoFar[0] += pushCoil(shortestPath, tsa_copy[a][b][0], stage_copy, tsa_copy, forbiddenPlaces, a, b, true, false);

			// update the stage_copy and tsa_copy to reflect the changes made in the pushCoil call
			followInstructions(shortestPath, stage_copy, tsa_copy, shortestPathSizeBeforePush, shortestPath.size() - 1, false);

			if (costSoFar[0] > minCostSoFar) {
				break;
			}
		}

		// check forbidden places
		if (forbiddenPlaces != null)
			for (int f = 0; f < forbiddenPlaces.size(); f += 2) {
				if (forbiddenPlaces.get(f) >= 0 && forbiddenPlaces.get(f+1) < 3)
					if (stage_copy[forbiddenPlaces.get(f)][forbiddenPlaces.get(f+1)][0] != 0) {
						costSoFar[0] += 100;
					}
			}
	}

	/**
	 * Compute the number of steps required to move all coils in and above (x, y) to TSA, and move.
	 * If a coil can be moved to a different spot on the Stage, do so; else, move to TSA
	 * return -1 if impossible (to be implemented when TSA moves are counted more accurately)
	 * @param x
	 * @param y
	 * @return
	 */
	public int emptySmart(int x, int y, int[][][] stage, int[][][] tsa, Stack<Integer> shortestPath, ArrayList<Integer> coilsToMoveBack, boolean push) {
		if (stage[x][y][0] == 0)
			return 0;

		// determine which items to move
		ArrayList<Integer> itemsToBeMoved = new ArrayList<>();
		if (push) {
			itemsToBeMoved.add(x);
			itemsToBeMoved.add(y);
		}
		findAllCoilsAbove (itemsToBeMoved, x, y, stage, false);

		int costOfEmptying = 0;
		boolean startFromHeavy = true;
		// decides if coils should be pushed from heaviest to lightest (true) or vice versa (false)

		// if there are 3 coils or less to be removed, just put them side by side
		if (!push && itemsToBeMoved.size() <= 6) {
			startFromHeavy = false;
		}

		// if there are 5 coils to be removed, push the lightest one to someplace on Stage
		if (!push && itemsToBeMoved.size() == 10) {
			// determine the best one to push to other places on stage_global and push, then push the other 4 as normal
			int lightestIndex = findHeaviestFreeCoil(stage, itemsToBeMoved, true);
			int lightestX = itemsToBeMoved.get(lightestIndex);
			int lightestY = itemsToBeMoved.get(lightestIndex + 1);

			ArrayList<Integer> forbiddenPlaces = new ArrayList<>();
			findAllCoilsAbove(forbiddenPlaces, x, y, stage, true);
			forbiddenPlaces.add(lightestX);
			forbiddenPlaces.add(lightestY);


			int shortestPathSizeBeforePush = shortestPath.size();

			// push the lightest coil, so we are left with only 4 coils above (x,y)
			costOfEmptying += pushCoil(shortestPath, stage[lightestX][lightestY][0], stage, tsa, forbiddenPlaces, lightestX, lightestY, false, false);
			itemsToBeMoved.remove(lightestIndex + 1);
			itemsToBeMoved.remove(lightestIndex);

			// update the stage_copy and tsa_copy to reflect the changes made in the pushCoil call
			followInstructions(shortestPath, stage, tsa, shortestPathSizeBeforePush, shortestPath.size() - 1, false);
		}

		// if there are 4 coils to be removed, start from thr lightest one
		if (itemsToBeMoved.size() == 8) {
			startFromHeavy = false;
			// with the exception if all coils other than lightest are equal (in pull case)
			if (! push) {
				int lightestIndex = findHeaviestFreeCoil(stage, itemsToBeMoved, true);
				int lightestX = itemsToBeMoved.get(lightestIndex);
				int lightestY = itemsToBeMoved.get(lightestIndex + 1);
				if ((lightestX == x && stage[lightestX][lightestY - 1][0] >= stage[lightestX - 1][lightestY - 1][0]) || (lightestX == x - 2 && stage[lightestX + 1][lightestY - 1][0] >= stage[lightestX + 2][lightestY - 1][0])) {
					boolean theOtherThreeAreEqual = stage[itemsToBeMoved.get((lightestIndex + 2) % 8)][itemsToBeMoved.get((lightestIndex + 3) % 8)][0] == stage[itemsToBeMoved.get((lightestIndex + 4) % 8)][itemsToBeMoved.get((lightestIndex + 5) % 8)][0]
							&& stage[itemsToBeMoved.get((lightestIndex + 4) % 8)][itemsToBeMoved.get((lightestIndex + 5) % 8)][0] == stage[itemsToBeMoved.get((lightestIndex + 6) % 8)][itemsToBeMoved.get((lightestIndex + 7) % 8)][0];

					if (!theOtherThreeAreEqual) {
						startFromHeavy = true;
					}
				}
			}
		}

		if (startFromHeavy) { // remove heavy --> light
			while (!itemsToBeMoved.isEmpty()) {
				int heaviestIndex = findHeaviestFreeCoil(stage, itemsToBeMoved, false);
				int heaviestX = itemsToBeMoved.get(heaviestIndex);
				int heaviestY = itemsToBeMoved.get(heaviestIndex+1);

				costOfEmptying += removeSmart(heaviestX, heaviestY, stage, tsa, shortestPath, coilsToMoveBack, new int[]{x, y, x - 1, y + 1, x, y + 1, x - 2, y + 2, x - 1, y + 2, x, y + 2});

				itemsToBeMoved.remove(heaviestIndex + 1);
				itemsToBeMoved.remove(heaviestIndex);
			}
		} else { // remove light --> heavy

			// remove the first 3 coils light --> heavy
			while (itemsToBeMoved.size() > 0) {
				int lightestIndex = findHeaviestFreeCoil(stage, itemsToBeMoved, true);
				int lightestX = itemsToBeMoved.get(lightestIndex);
				int lightestY = itemsToBeMoved.get(lightestIndex + 1);

				costOfEmptying += removeSmart(lightestX, lightestY, stage, tsa, shortestPath, coilsToMoveBack, new int[]{x, y, x - 1, y + 1, x, y + 1, x - 2, y + 2, x - 1, y + 2, x, y + 2});
				itemsToBeMoved.remove(lightestIndex + 1);
				itemsToBeMoved.remove(lightestIndex);
			}
		}

		return costOfEmptying;
	}

	/**
	 * If it is possible to move a coil on the Stage to another place on the Stage in 1 move, do it
	 * else move it to tsa_global
	 * @param x1
	 * @param y1
	 * @param stage
	 * @param tsa
	 * @param shortestPath
	 * @param coilsToMoveBack
	 * @param forbiddenPlaces a list of places where NOT to put the coil
	 */
	public int removeSmart(int x1, int y1, int[][][] stage, int[][][] tsa, Stack<Integer> shortestPath, ArrayList<Integer> coilsToMoveBack, int[] forbiddenPlaces) {

		// look at the parent
		int minReplacedLighterParent = 0; // the weight of the lighter coil which is under this coil at (j, i)
		int x = 0, y = 0; // random initialization
		// possible to put this coil to another place on Stage that is not forbidden in 1 step
		boolean possible = stageCanSupport(0,0, stage[x1][y1][0], stage) && stage[0][0][0] == 0;
		if (possible && forbiddenPlaces != null)
			for (int k = 0; k < forbiddenPlaces.length; k+=2) {
				if (0 == forbiddenPlaces[k] && 0 == forbiddenPlaces[k+1]) {
					possible = false;
					break;
				}
			}

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < stageSize - i; j++) {
				if (stageCanSupport(j, i, stage[x1][y1][0], stage)) {
					if (stage[j][i][0] == 0) {
						boolean thisIsPossible;
						int replaceMinParentCandidate;
						if (i == 0)
							replaceMinParentCandidate = 5000;
						else
							replaceMinParentCandidate = stage[j][i-1][0] < stage[j+1][i-1][0] ? stage[j][i-1][0] : stage[j+1][i-1][0];

						thisIsPossible = true;
						// if (j,i) is some forbidden place, possible = false
						for (int k = 0; k < forbiddenPlaces.length; k+=2) {
							if (j == forbiddenPlaces[k] && i == forbiddenPlaces[k+1]) {
								thisIsPossible = false;
								break;
							}
						}
						if (thisIsPossible) {
							if (! possible || (possible && replaceMinParentCandidate < minReplacedLighterParent) || (possible && replaceMinParentCandidate == minReplacedLighterParent && (minReplacedLighterParent == 5000 ? false : (stage[j][i-1][0] + stage[j+1][i-1][0]) < (stage[x][y-1][0] + stage[x+1][y-1][0])))) {
								x = j;
								y = i;
								minReplacedLighterParent = replaceMinParentCandidate;
								possible = true;
							}
						}
					}
				}
			}
		}

		// add Coils that were moved to TSA to the coilsToMoveBack
		if (possible) {
			move(x1, y1, x, y, 2, stage, tsa, shortestPath, null);
			return 1;
		} else {
			if (tsaCanSupport(0, 1, stage[x1][y1][0], tsa) && tsa[0][1][0] == 0)  {
				if (tsaCanSupport(1, 1, stage[x1][y1][0], tsa) && tsa[1][1][0] == 0 && tsa[2][0][0] < tsa[0][0][0]) {
					move(x1, y1, 1, 1, 0, stage, tsa, shortestPath, coilsToMoveBack);
				} else {
					move(x1, y1, 0, 1, 0, stage, tsa, shortestPath, coilsToMoveBack);
				}
				return 1;
			} else if (tsaCanSupport(1, 1, stage[x1][y1][0], tsa) && tsa[1][1][0] == 0) {
				move(x1, y1, 1, 1, 0, stage, tsa, shortestPath, coilsToMoveBack);
				return 1;
			} else {
				boolean canPush = false;
				for (int i = 0; i < 3; i++) {
					if (tsa[i][0][0] == 0) {
						move(x1, y1, i, 0, 0, stage, tsa, shortestPath, coilsToMoveBack);
						return 1;
					}
				}

				// if couldn't push to level 0 of TSA
				if (tsaCanSupport(1,1, tsa[0][0][0], tsa) && tsa[1][1][0] == 0 && isFree(0, 0, tsa)) {
					move(0, 0, 1, 1, 3, stage, tsa, shortestPath, coilsToMoveBack);
					move(x1, y1, 0, 0, 0, stage, tsa, shortestPath, coilsToMoveBack);
					return 2;
				}
				else if (tsaCanSupport(0,1, tsa[2][0][0], tsa) && tsa[0][1][0] == 0 && isFree(2, 0, tsa)){
					move(2, 0, 0, 1, 3, stage, tsa, shortestPath, coilsToMoveBack);
					move(x1, y1, 2, 0, 0, stage, tsa, shortestPath, coilsToMoveBack);
					return 2;
				}
				else {
					return 100;
				}

			}
		}
	}

	/**
	 * Carry out the steps in steps stack on the stage_global and tsa_global instances given
	 * @param steps
	 * @param stageInstance
	 * @param tsaInstance
	 * @param start
	 * @param end
	 */
	public void followInstructions(Stack<Integer> steps, int[][][] stageInstance, int[][][] tsaInstance, int start, int end, boolean showSteps) {
		for (int base = start; base < end; base += 5) {
			int mode = steps.elementAt(base);
			int v = steps.elementAt(base + 1);
			int u = steps.elementAt(base + 2);
			int y = steps.elementAt(base + 3);
			int x = steps.elementAt(base + 4);

			if (mode == 0) {
				tsaInstance[u][v][0] = stageInstance[x][y][0];
				tsaInstance[u][v][1] = stageInstance[x][y][1];
				if (showSteps)
					if (u == 5)
						System.out.println("Stage --> Shearing Machine. mass: " + stageInstance[x][y][0] + " from ( "+ x + ", " + y + ")");
					else
						System.out.println("Stage --> TSA. mass: " + stageInstance[x][y][0] + " from ( "+ x + ", " + y + ") to (" + u + ", " + v + ")");
				stageInstance[x][y][0] = 0;
				stageInstance[x][y][1] = 0;
			}
			else if (mode == 1) {
				stageInstance[u][v][0] = tsaInstance[x][y][0];
				stageInstance[u][v][1] = tsaInstance[x][y][1];
				if (showSteps)
					if (x == 4)
						System.out.println("Truck --> Stage. mass: " + tsaInstance[x][y][0] + " to (" + u + ", " + v + ")");
					else
						System.out.println("TSA --> Stage. mass: " + tsaInstance[x][y][0] + " from ( "+ x + ", " + y + ") to (" + u + ", " + v + ")");
				tsaInstance[x][y][0] = 0;
				tsaInstance[x][y][1] = 0;
			} else if (mode == 2) {
				stageInstance[u][v][0] = stageInstance[x][y][0];
				stageInstance[u][v][1] = stageInstance[x][y][1];
				if (showSteps)
					System.out.println("Stage --> Stage. mass: " + stageInstance[x][y][0] + " from ( "+ x + ", " + y + ") to (" + u + ", " + v + ")");
				stageInstance[x][y][0] = 0;
				stageInstance[x][y][1] = 0;
			} else if (mode == 3) {
				tsaInstance[u][v][0] = tsaInstance[x][y][0];
				tsaInstance[u][v][1] = tsaInstance[x][y][1];
				if (showSteps)
					System.out.println("TSA --> TSA. mass: " + tsaInstance[x][y][0] + " from ( "+ x + ", " + y + ") to (" + u + ", " + v + ")");
				tsaInstance[x][y][0] = 0;
				tsaInstance[x][y][1] = 0;
			} else {
				System.out.println("ERROR: unrecognizable mode");
			}
		}
	}

	/**
	 * Make a deep copy of an array
	 * @param clone
	 * @param original
	 * @param x
	 * @param y
	 * @param z
	 */
	private void cloneArray( int[][][] clone, int[][][] original, int x, int y, int z) {
		for (int i = 0; i < y; i++) {
			for (int j = 0; j < x; j++) {
				for (int k = 0; k < z; k++) {
					clone[j][i][k] = original[j][i][k];
				}
			}
		}
	}

	/**
	 * Determine if the coils at specified place has no coils on it, i.e. is free to be moved
	 * @param x
	 * @param y
	 * @param area
	 * @return
	 */
	public boolean isFree(int x, int y, int[][][] area) {
		if (y == area[0].length - 1)
			return true;
		else {
			if (x == 0)
				return area[x][y+1][0] == 0;
			else
				return area[x-1][y+1][0] == 0 && area[x][y+1][0] == 0;
		}
	}

	/**
	 * Finds the index of the heaviest coil among those in coilSet in area
	 * when reverse == true, it finds the lightest coil
	 * @param area
	 * @param coilSet
	 * @param reverse
	 * @return
	 */
	public int findHeaviestFreeCoil(int[][][] area, ArrayList<Integer> coilSet, boolean reverse) {
		int index = 0;
		int x = -1;
		int y = -1;
		boolean isFree = false;

		// if there is a heavier one, pick that as the heaviest
		for (int i = 0; i < coilSet.size(); i+= 2) {
			int thisX = coilSet.get(i);
			int thisY = coilSet.get(i+1);

			// update the heaviest coil if the previous selection was not free (i.e. unable to move), or if it was free but this new one is a heavier free coil
			if (isFree(thisX, thisY, area)) { // check if the coil we are currently looking at is free
				if (reverse) {
					if (!isFree || isFree && area[x][y][0] > area[thisX][thisY][0]) {
						x = thisX;
						y = thisY;
						index = i;
						isFree = true;
					}
				} else {
					if (!isFree || isFree && area[x][y][0] < area[thisX][thisY][0]) {
						x = thisX;
						y = thisY;
						index = i;
						isFree = true;
					}
				}

			}
		}
		return index;
	}


	private void findAllCoilsAbove (ArrayList<Integer> itemsToBeMoved, int x, int y, int[][][] stage, boolean empty) {
		if (y < 2) {
			if (x > 0) {
				itemsToBeMoved.add(x - 1);
				itemsToBeMoved.add(y + 1);
			}
			itemsToBeMoved.add(x);
			itemsToBeMoved.add(y + 1);
		}
		if (y < 1) {
			if (x > 1) {
				itemsToBeMoved.add(x-2);
				itemsToBeMoved.add(y+2);
			}
			if (x > 0) {
				itemsToBeMoved.add(x - 1);
				itemsToBeMoved.add(y + 2);
			}
			// if (x < 18) ?
			itemsToBeMoved.add(x);
			itemsToBeMoved.add(y + 2);
		}
		if ( !empty)
			for (int i = itemsToBeMoved.size() - 1; i > 0 ; i -= 2) {
				if (stage[itemsToBeMoved.get(i-1)][itemsToBeMoved.get(i)][0] == 0) {
					itemsToBeMoved.remove(i);
					itemsToBeMoved.remove(i-1);
				}
			}
		else {
			for (int i = itemsToBeMoved.size() - 1; i > 0 ; i -= 2) {
				if (stage[itemsToBeMoved.get(i-1)][itemsToBeMoved.get(i)][0] != 0) {
					itemsToBeMoved.remove(i);
					itemsToBeMoved.remove(i-1);
				}
			}
		}

	}
}


























