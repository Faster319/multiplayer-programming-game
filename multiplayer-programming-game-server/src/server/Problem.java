package server; //TODO modularise

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Random;

public class Problem {
	
	// Create variables used to store information about the problem:
	private String problemID;
	private String title;
	private String description;
	private int timeLimit;
	private String hint;
	private String correctSolution;
	private String setupCode;
	private String generationInstructions;
	
	private boolean problemCreationSucceeded; // Stores whether the problem was successfully created or not.
	
	// Create variables used to store information for testing and scoring:
	private int averageTimeTaken;
	private ArrayList<String> testDataVariableNames = new ArrayList<String>();
	private ArrayList<String[]> testDataValues = new ArrayList<String[]>();
	private String[] expectedOutcomes = new String[20];
	
	private PythonExecutor pythonExecutor; // Needed to execute python code.
	
	private ArrayList<String[]> failedResults = new ArrayList<String[]>(); // Stores the failed results of the last test.
	
	// Constructor which is called when this object is created:
	public Problem(String problemID, PythonExecutor pythonExecutor) {
		
		// Store the parameters as class variables:
		this.problemID = problemID;
		this.pythonExecutor = pythonExecutor;
		
		// Call loadProblemFromFile() and store its returned value which indicates whether it was successful or not as problemCreationSucceeded:
		problemCreationSucceeded = loadProblemFromFile();
	}
	
	
	// Method to load the details of a problem from the file (and to call the next stages of the problem creation):
	private boolean loadProblemFromFile() {
		try { // The following block of code could throw an exception which must be caught.
			
			
			String problemDirectory = "Problems\\problem#"+problemID+".txt"; // Create a variable to store the path of the problem within the project folder.
			
			// If the program is being executed in a .jar file:
			if (getClass().getResource("Main.class").toString().contains("jar!")) {
			
				// Attempt to store the directory of the .jar file and use it to find the path of the problem outside of the .jar:
				try {
					String externalDirectory = URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").toURI().toString().replaceFirst("file:", ""), "UTF-8");
					problemDirectory = externalDirectory+"/PythonGame/problem#"+problemID+".txt";
				}
				
				// If an exception is caught, print the stack trace (for debugging purposes), display an error message on the GUI and return:
				catch (UnsupportedEncodingException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
			
			// Pass the problem's file path to a new FileReader, and pass this to a new BufferedReader, in order to read the file:
			BufferedReader bufferedReader = new BufferedReader(new FileReader(problemDirectory));
			
			// Read the title, description, time limit, hint and generation instructions from the first five lines of the file, and store them:
			title = bufferedReader.readLine();
			description = bufferedReader.readLine();
			timeLimit = Integer.parseInt(bufferedReader.readLine());
			hint = bufferedReader.readLine();
			generationInstructions = bufferedReader.readLine();
			
			generateTestData(generationInstructions); // Pass the generation instructions to generateTestData().
			
			correctSolution = ""; // Initialise correctSolution as an empty string.
			String line = bufferedReader.readLine(); // Read and store the next line of the file.
			while (line != null) { // While the last read line isn't null.
				correctSolution += line + "\n"; // Add the last read line to correctSolution.
				line = bufferedReader.readLine(); // Read and store the next line of the file.
			}
			
			bufferedReader.close(); // Close the BufferedReader to free up memory.
			
			setupCode = ""; // Initialise setupCode as an empty string.
			String[] splitSolution = correctSolution.split("\n"); // Split correctSolution into an array with each line being a separate element.
			
			// Loop through the lines of the correct solution, ending after the sys module is deleted:
			for (int i = 0; i < 3 + testDataVariableNames.size(); i++) {
				setupCode += splitSolution[i] + "\n"; // Add each line to setupCode.
			}
			
			String code = ""; // Initialse an empty string to store the correct solution without the setup code.
			
			// Loop through the lines of the correct solution, starting from the line after the end of the setup code:
			for (int i = 3 + testDataVariableNames.size(); i < splitSolution.length; i++) {
				code += splitSolution[i] + "\n"; // Add each line to the new code string.
			}
			correctSolution = code; // Store the new code string as the correct solution.
			
			// Call loadProblemFromFile() and return its returned value which indicates whether it was successful or not:
			return createExpectedOutcomes();
		}
		
		// If the problem file could not be read:
		catch (IOException e) {
			e.printStackTrace(); // Print the stack trace (for debugging purposes).
			return false; // Return false to indicate that the problem creation failed.
		}
	}
	
	
	// Method to generate the test data for the problem when given the generation instructions:
	private boolean generateTestData(String generationInstructions) {
		Random random = new Random(); // Create an instance of a Random object, which is used to generate random numbers.
		
		// Remove all spaces from the generation instruction, and split it into parts separated by a "|" character, then store the parts in an array:
		String[] parts = generationInstructions.replaceAll(" ", "").split("[|]");
		
		int n = 0; // Create a counter variable to count how many parts have been handled. Initialise it to 0.
		
		// Create a variable which stores the number of input variables for the problem, indicated by the value of the first part:
		int numberOfVariables = Integer.parseInt(parts[0]);
		
		// Loop through the next numberOfVariables parts:
		for (n = 1; n <= numberOfVariables; n++) {
			testDataVariableNames.add(parts[n]); // Store the name of the variable.
			testDataValues.add(new String[20]); // Create and store a String[] array which will store 20 input for tests.
		}
		
		int[] nextIndex = new int[numberOfVariables]; // Create an array which stores the next index of each testDataValues array.
		
		// Set the initial value of each next index to 0 by loop through the array:
		for (int i = 0; i < nextIndex.length; i++) {
			nextIndex[i] = 0;
		}
		
		while (n < parts.length) { // Loop through all of the parts.
			int numberOfTests = Integer.parseInt(parts[n]); // Get and store the next part as the number of tests that should be in the range.
			
			for (int i = 0; i < numberOfVariables; i++) { // Loop as many times as the number of variables.
				n++; // Increment n to get the index of the next part.
				int lowerLimit = Integer.parseInt(parts[n]); // Get and store the next part as the lower limit of the range.
				n++; // Increment n to get the index of the next part.
				int upperLimit = Integer.parseInt(parts[n]); // Get and store the next part as the lower limit of the range.
				
				String[] singularTestData = testDataValues.get(i); // Get and store the String[] array of the ith variable from testDataValues.
				for (int j = 0; j < numberOfTests; j++) { // Loop as many times as the number of tests which will be generated.
					int randomValue = random.nextInt(upperLimit - lowerLimit + 1) + lowerLimit; // Generate and store a random number within the limit.
					
					// Store the random number in the singular test data, in the index stored in the nextIndex array.
					singularTestData[nextIndex[i]] = Integer.toString(randomValue);
					
					nextIndex[i]++; // Increment the next index for this input variable's test data.
				}
				
				testDataValues.set(i, singularTestData); // Store the generated test data in the class variable testDataValues.
			}
			
			n++; // Increment n to get the index of next the parts.
		}
		
		return true; // Return true to indicate that the generation was successful.
	}
	
	
	// Method to create the expected outcomes for each test:
	public boolean createExpectedOutcomes() {
		pythonExecutor.createFile("solution.py", setupCode, correctSolution); // Using PythonExecutor's createFile() create a python file for the correct solution.
		
		String[] inputs = new String[testDataVariableNames.size()]; // Create a String[] array for the set of inputs for a single test.
		int totalTimeTaken = 0; // Create an integer variable to store the total execution time for all of the tests.
		
		for (int i = 0; i <= 19; i++) { // Loop 20 times (as there are 20 tests).
			for (int j = 0; j < testDataVariableNames.size(); j++) { // Loop as many times as the number of input variables.
				inputs[j] = testDataValues.get(j)[i]; // Get the ith set of inputs for the jth variable from testDataValues.
			}
			
			String result = pythonExecutor.executeFile("solution.py", inputs); // Execute the correct solution with the inputs and store the result.
			
			// If the execution wasn't successful, return false to indicate that the expected outputs creation has failed:
			if (!pythonExecutor.succeeded()) {
				System.out.println(result);
				return false;
			}
			
			totalTimeTaken += pythonExecutor.getTimeTaken(); // Add the execution time to the total execution time.
			expectedOutcomes[i] = result; // Store the result in the class variable expectedOutcomes.
		}
		
		averageTimeTaken = totalTimeTaken/20; // Calculate the average execution time by dividing by 20, and store this in a class variable.
		return true; // Return true to indicate that the expected outputs were successfully created.
	}
	
	
	// Method to score a given solution:
	public int[] scoreSolution(String solution, int gameMode, long roundStartTime, long roundEndTime, long submissionTime) {
		pythonExecutor.createFile("solution.py", setupCode, solution); // Using PythonExecutor's createFile() create a python file for the solution.
		
		String[] inputs = new String[testDataVariableNames.size()]; // Create a String[] array for the set of inputs for a single test.
		
		int totalTimeTaken = 0; // Create an integer variable to store the total execution time for all of the tests.
		int fails = 0; // Create an integer variable to store the amount of failed tests.
		
		for (int i = 0; i <= 19; i++) { // Loop 20 times (as there are 20 tests).
			for (int j = 0; j < testDataVariableNames.size(); j++) { // Loop as many times as the number of input variables.
				inputs[j] = testDataValues.get(j)[i]; // Get the ith set of inputs for the jth variable from testDataValues.
			}
			
			String result = pythonExecutor.executeFile("solution.py", inputs); // Execute the solution with the inputs and store the result.
			
			// If the execution failed, or if the output doesn't match the expected output:
			if (!pythonExecutor.succeeded() || !result.equals(expectedOutcomes[i])) {
				fails++; // Increment the fails counter.
				
				String[] failedResult = new String[inputs.length + 2]; // Create an array, failedResult, to store the details of the failed test.
				failedResult[0] = result; // Store the output of the test in the first index.
				failedResult[1] = expectedOutcomes[i]; // Store the expected output in the second index.
				
				// Loop through the set of inputs, adding each input to the failedResult array:
				for (int k = 0; k < inputs.length; k++) {
					failedResult[k + 2] = inputs[k];
				}
				
				failedResults.add(failedResult); // Add the failedResult array to the ArrayList class variable.
				
				// Increase the total time taken variable by the correct solution's average time (as the actual time taken of a failed test is unreliable):
				totalTimeTaken += averageTimeTaken;
			}
			
			// If the output matched the expected output, add the execution time to the total execution time variable:
			else {
				totalTimeTaken += pythonExecutor.getTimeTaken();
			}
		}
		
		int score = 0; // Create a variable to store the score of the solution.
		
		// If the game mode is efficiency:
		if (gameMode == 0) {
			int efficiencyPoints = 1600 * averageTimeTaken / totalTimeTaken; // Calculate the efficiency points of the solution.
			int passes = 20 - fails; // Calculate the number of passes.
			
			// Calculate the score of the solution, so that failing or inefficiency reduces the score, but efficiency increases the score:
			score = efficiencyPoints * passes / 20;
			
			return new int[] {score, efficiencyPoints, passes}; // Return the score, efficiency points and the number of passes.
		}
		
		// If the game mode is length:
		else if (gameMode == 1) {
			
			// Calculate the length points of the solution.
			int lengthPoints;
			try {
				lengthPoints = 80 * correctSolution.length() / solution.length(); 
			} catch (ArithmeticException e) {
				lengthPoints = 0;
			}
			
			int passes = 20 - fails; // Calculate the number of passes.
			
			// Calculate the score of the solution, so that failing or longer code reduces the score, but shorter code increases the score:
			score = lengthPoints * passes / 20;
			
			return new int[] {score, lengthPoints, passes}; // Return the score, length points and the number of passes.
		}
		
		// If the game mode is time:
		else if (gameMode == 2) {
			int timePoints = (int) (80 * (roundEndTime - submissionTime) / (roundEndTime - roundStartTime)); // Calculate the time points of the solution.
			int passes = 20 - fails; // Calculate the number of passes.
			
			// Calculate the score of the solution, so that failing or a longer submission time reduces the score, but a shorter submission time increases the score:
			score = timePoints * passes / 20;
			
			return new int[] {score, timePoints, passes}; // Return the score, time points and the number of passes.
		}
		
		// If an unknown game mode was entered:
		else {
			System.out.println("ERROR: Unknown mode.");
			return null;
		}
	}
	
	
	// Method to get the failed test results from the last time scoreSolution() was called (and reset failedResults):
	public ArrayList<String[]> getFailedResults() {
		ArrayList<String[]> temp = new ArrayList<String[]>(failedResults); // Copy the failedResults ArrayList to a temporary variable.
		failedResults.removeAll(failedResults); // Remove all elements from the failedResults ArrayList.
		return temp; // Return the temporary variable.
	}
	
	
	// Method to get the loaded problem's title:
	public String getTitle() {
		return title;
	}
	
	
	// Method to get the loaded problem's description:
	public String getDescription() {
		return description;
	}
	
	
	// Method to get the loaded problem's default time limit:
	public int getTimeLimit() {
		return timeLimit;
	}
	
	
	// Method to get the loaded problem's hint:
	public String getHint() {
		return hint;
	}
	
	
	// Method to check whether the problem was successfully created:
	public boolean problemCreationSucceeded() {
		return problemCreationSucceeded;
	}
	
	
	// Method to convert the test data variable names to a single string and return them:
	public String getVariableNamesAsString() {
		String variableNames = ""; // Create a variable to store the variable names as a string.
		
		// Iterate through the ArrayList of variable names and add them, separated by whitespaces, to the string.
		for (String variableName: testDataVariableNames) {
			variableNames += variableName + ",";
		}
		
		return variableNames; // Return the variable names.
	}
	
	
	// Method to return the setup code for the solution to run:
	public String getSetupCode() {
		return setupCode;
	}
	
	
	// Method to return the solution:
	public String getSolution() {
		return correctSolution;
	}
	
	
	// Method to return the generation instructions:
	public String getGenerationInstructions() {
		return generationInstructions;
	}
}
