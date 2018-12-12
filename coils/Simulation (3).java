
public class Simulation {

	public static void main(String[] args) {

//		basicTest();

		// Test Pushing

		int numberOfCoils = 50;
		int testSize = 1;

		testRandom(numberOfCoils, testSize);

	}

	public static void testRandom(int numberOfCoils, int testSize) {
		double[] pushingAverages = new double[testSize];
		double[] pullingAverages = new double[testSize];
		int nCoilsNotPushed = 0;
		int nCoilsNotPulled = 0;

		// Test Pushing
		for (int k = 0; k < testSize; k++) {
			System.out.println("Initial Stage");
			Brain test = new Brain();

			test.showStage();

			double pushingStepsAverage = 0;

//			int[] pushingQueue = new int[] {39, 28, 49, 14, 19, 27, 23, 46, 27, 47, 14, 26, 80, 93, 91, 94, 92, 98, 81, 99, 88, 82, 99, 97, 99, 97, 91, 95, 86, 80, 93, 85, 91, 81, 88, 97, 86, 89, 91, 93, 92, 97, 90, 98, 90, 83, 90, 86, 93, 87};
//
			int[] pushingQueue = new int[numberOfCoils];
			for (int i = 0; i < numberOfCoils / 3; i++) {
				pushingQueue[i] = (int) (Math.random() * 40 + 10);
			}
			for (int i = numberOfCoils / 3; i < numberOfCoils / 4; i++) {
				pushingQueue[i] = (int) (Math.random() * 30 + 50);
			}
			for (int i = numberOfCoils / 4; i < numberOfCoils; i++) {
				pushingQueue[i] = (int) (Math.random() * 20 + 80);
			}

			System.out.println("Pushing Queue: ");
			System.out.print("{");
			for (int i = 0; i < numberOfCoils - 1; i++) {
				System.out.print(pushingQueue[i] + ", ");
			}
			System.out.println(pushingQueue[numberOfCoils - 1] + "}");

			for (int i = 0; i < numberOfCoils; i++) {
				int cost = test.push(pushingQueue[i], i);
				pushingStepsAverage += cost;
				if (cost >= 100)
					nCoilsNotPushed++;
			}

			pushingStepsAverage /= numberOfCoils;
			System.out.println("Average number of steps: " + pushingStepsAverage);
			pushingAverages[k] = pushingStepsAverage;


			 // Test Pulling

			double pullingStepsAverage = 0;
			int[] pullingQueue = new int[6];
			for (int i = 0; i < 18; i+=3) {
				pullingQueue[i/3] = i+1;
			}

			System.out.println("Pulling Queue: ");
			System.out.print("{");
			for (int i = 0; i < pullingQueue.length - 1; i++) {
				System.out.print(pullingQueue[i] + ", ");
			}
			System.out.println(pullingQueue[pullingQueue.length-1] + "}");

			for (int i = 0; i < pullingQueue.length; i++) {
				int cost = test.pull(pullingQueue[i], 0);
				pullingStepsAverage += cost;
				if (cost >= 100)
					nCoilsNotPulled++;
			}

			pullingStepsAverage /= 6;
			System.out.println("Average number of steps: " + pullingStepsAverage);
			pullingAverages[k] = pullingStepsAverage;
		}

		System.out.println("\n\n############# Report #############");
		System.out.println("Of " + testSize + " test cases, each with " + numberOfCoils + " coils to be pushed and " + 6 + " to be pulled, " + nCoilsNotPushed + " coils couldn't be pushed, and " + nCoilsNotPulled + " coils couldn't be pulled" );
		System.out.println("The average numbers of steps of test cases: " );
		double r = 0;
		double s = 0;
		for (int k = 0; k < testSize; k++) {
			System.out.print(pushingAverages[k] + " ");
			r += pushingAverages[k];
			s += pullingAverages[k];
		}
		System.out.println();
		System.out.println(" total average of steps while pushing: " + r/testSize);
		System.out.println(" total average of steps while pulling: " + s/testSize);


	}

	public static void basicTest() {
		Brain test = new Brain();
		System.out.println("-- Pushing 3's --");
		for (int i = 0; i < 18; i++) {
			test.push(3, 3*18+i);
		}

		System.out.println("-- Pushing 5's --");
		for (int i = 0; i < 17; i++) {
			test.push(5, 5*18+i);
		}

		System.out.println("-- Pushing 4's --");
		for (int i = 0; i < 3; i++) {
			test.push(4, 4*18+i);
		}

		System.out.println("-- Pushing 7's --");
		for (int i = 0; i < 2; i++) {
			test.push(7, 7*18+i);
		}

		System.out.println("-- Pushing 9's --");
		for (int i = 0; i < 5; i++) {
			test.push(9, 9*18+i);
		}

		System.out.println("-- Pushing 6's --");
		for (int i = 0; i < 5; i++) {
			test.push(6, 6*18+i);
		}

		System.out.println("-- Pushing 8's --");
		for (int i = 0; i < 0; i++) {
			test.push(8, 8*18+i);
		}
	}
}
