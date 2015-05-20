import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HackAssembler {

	/**
	 * types of lines in the hack language
	 * @author Sami
	 *
	 */
	enum LineType { 
		COMMENT, 
		WHITESPACE, 
		CINSTRUCTION, 
		AINSTRUCTION, 
		LABEL, 
		UNKNOWN
	}

	/**
	 * address at which we start to put user variables (now 16 = 0x10)
	 */
	private final static int VARIABLE_START_ADDR	= 0x10; 

	/* C instruction */
	private final static String cRegex 				= "(?:(\\w{1,3})=)?(!?(?:\\w(?:[-+|&]\\w)?)|(?:[+-]?\\d+))(?:;(J[A-Z][A-Z]))?(?:\\/\\/.*)?";
	private final static Pattern cPattern			= Pattern.compile(cRegex);

	/* A instruction */ 
	private final static String aRegex				= "@(?:(\\d+)|([\\S]+))";
	private final static Pattern aPattern			= Pattern.compile(aRegex);

	/* Label */ 
	private final static String labelRegex			= "\\(([\\S]+)\\)";
	private final static Pattern labelPattern 		= Pattern.compile(labelRegex); 

	/* Comment */ 
	private final static String commentRegex		= "//(.)*"; 
	private final static Pattern commentPattern	= Pattern.compile(commentRegex); 

	private final static Map<String, Integer> 	compMatcher = new HashMap<>(); 
	private final static Map<String, Integer> 	jumpMatcher = new HashMap<>(); 
	private final static Map<String, Integer> 	destMatcher = new HashMap<>(); 
	private final static Map<String, Integer> 	predefined	= new HashMap<>();

	static { 
		initCompMatcher();

		initJumpMatcher(); 

		initDestMatcher();

		initPredefinedSymbols();
	}

	/**
	 * initialises the map dictating valid Comp fields in C intructions  
	 */
	private static final void initCompMatcher() {

		compMatcher.put("0", 0x2A); 
		compMatcher.put("1", 0x3F);
		compMatcher.put("-1", 0x3A);
		compMatcher.put("D", 0x0C);

		compMatcher.put("A", 0x30);
		compMatcher.put("M", 0x30);

		compMatcher.put("!D", 0x0D);
		compMatcher.put("!A", 0x31);
		compMatcher.put("!M", 0x31);

		compMatcher.put("-D", 0x0F);
		compMatcher.put("-A", 0x33);
		compMatcher.put("-M", 0x33);

		compMatcher.put("D+1", 0x1F);
		compMatcher.put("A+1", 0x37);
		compMatcher.put("M+1", 0x37);

		compMatcher.put("D-1", 0x0E);
		compMatcher.put("A-1", 0x32);
		compMatcher.put("M-1", 0x32);

		compMatcher.put("D+A", 0x02);
		compMatcher.put("D+M", 0x02);
		compMatcher.put("D-A", 0x13);
		compMatcher.put("D-M", 0x13);

		compMatcher.put("A-D", 0x07);
		compMatcher.put("M-D", 0x07);

		compMatcher.put("D&A", 0x00);
		compMatcher.put("D&M", 0x00);

		compMatcher.put("D|A", 0x15);
		compMatcher.put("D|M", 0x15);

	}
	/**
	 * initialises the map dictating valid jump instructions in C instructions
	 */
	private final static void initJumpMatcher() {

		jumpMatcher.put("",    0x0); 
		jumpMatcher.put("JGT", 0x1); 
		jumpMatcher.put("JEQ", 0x2); 
		jumpMatcher.put("JGE", 0x3);
		jumpMatcher.put("JLT", 0x4); 
		jumpMatcher.put("JNE", 0x5); 
		jumpMatcher.put("JLE", 0x6);
		jumpMatcher.put("JMP", 0x7);

	}
	/**
	 * initialises the map containing valid destinations for C instructions 
	 */
	private final static void initDestMatcher() {

		destMatcher.put("", 	0x0);	
		destMatcher.put("M", 	0x1);	
		destMatcher.put("D", 	0x2);	
		destMatcher.put("MD", 	0x3);	
		destMatcher.put("A", 	0x4);	
		destMatcher.put("AM", 	0x5);	
		destMatcher.put("AD", 	0x6);	
		destMatcher.put("AMD", 	0x7);

	}
	/**
	 * intialises predefined symbols of the hack language
	 */
	private final static void initPredefinedSymbols() {
		predefined.put("SP", 0); 
		predefined.put("LCL", 1); 
		predefined.put("ARG", 2); 
		predefined.put("THIS", 3); 
		predefined.put("THAT", 4); 
		predefined.put("SCREEN", 16384); 
		predefined.put("KBD", 24576); 

		predefined.put("R0",0 ); 
		predefined.put("R1",1 ); 
		predefined.put("R2",2 ); 
		predefined.put("R3",3 ); 
		predefined.put("R4",4 ); 
		predefined.put("R5",5 ); 
		predefined.put("R6",6 ); 
		predefined.put("R7",7 ); 
		predefined.put("R8",8 ); 
		predefined.put("R9",9 ); 
		predefined.put("R10",10 ); 
		predefined.put("R11",11 ); 
		predefined.put("R12",12 ); 
		predefined.put("R13",13 ); 
		predefined.put("R14",14 ); 
		predefined.put("R15",15 ); 

	}

	/**
	 * stores the number of variables the user has declared.
	 */
	private int userVariableCount; 
	/**
	 * name of the .asm file that needs to be assembled
	 */
	private String filename = null;

	/**
	 * user variables. Is cleared each time open(filename) is called
	 */
	private Map<String, Integer> 	variables	= new HashMap<>();
	/**
	 * user labels. Is cleared each time open(filename) is called. 
	 */
	private Map<String, Integer> 	labels		= new HashMap<>();

	private File file = null; 
	/**
	 * @param filename
	 * @throws FileNotFoundException 
	 */
	public void open(String filename) throws FileNotFoundException { 
		this.filename = filename;

		if (!(file = new File(filename)).exists())
			throw new FileNotFoundException("Could no locate " + filename + " file.");

		variables.clear();
		labels.clear(); 

		/* reinitialise to 0 */ 
		userVariableCount = 0; 
	}

	public void assemble() { 

		if (filename == null) { 
			System.out.println("You have to open .asm file before assembling");
			return; 
		}

		/* input stream reader fed into the bufferedReaders */
		InputStreamReader streamReader1 = null; 
		InputStreamReader streamReader2 = null; 
		try { 
			streamReader1 	= new InputStreamReader(new FileInputStream(file)); 
			streamReader2 	= new InputStreamReader(new FileInputStream(filename));
		} catch (FileNotFoundException exc) { System.out.println("Could not open .asm file"); return; }
		/* 
		 * Two buffered readers are used, each for one pass through the file. 
		 * We could have used the reset() functin of the BufferedReader, but it could have unexpected
		 * behavior for big files. Using two BufferedReaders has been deemed safer.
		 */
		BufferedReader reader1 			= new BufferedReader(streamReader1); 
		BufferedReader reader2 			= new BufferedReader(streamReader2); 

		/* replace .asm with .hack for a new output file */ 
		String outputFilename =  filename.replaceAll(".asm", ".hack");

		/* used to write to the output file */
		PrintWriter writer = new PrintWriter(new File(outputFilename)); 

		firstPass(reader1);

		String output = secondPass(reader2);

		writer.write(output);
		writer.close();
	}

	/* during the first pass we only deal with labels */ 
	private void firstPass(BufferedReader reader) throws IOException {

		/* line read */ 
		String line = null;

		int lineCount = 0; 

		/* read all lines until there arent any more */ 
		while ((line = reader.readLine()) != null) {
			LineType type = identifyLine(line);

			switch(type) {
			case WHITESPACE: 
			case COMMENT: 
				continue;
			case UNKNOWN: 
				throw new IllegalArgumentException("the provided string file could not be parsed: " + line +  " " + lineCount);
			case LABEL: 
				label(line, lineCount);
				break;
			case AINSTRUCTION: 
			case CINSTRUCTION: 
				lineCount++; 
			}
		}
	}

	/**
	 * Reads lines from the Hack language and writes their binary equivalent in a .hack file
	 * @param reader reads from the in file
	 * @param writer writes to the output file
	 * @return
	 * @throws IOException
	 */
	private String secondPass(BufferedReader reader) throws IOException {

		StringBuilder sb = new StringBuilder(); 

		/* line read */ 
		String line = null;

		/* read all lines until there arent any more */ 
		while ((line = reader.readLine()) != null) {
			line = removeWhitespace(line);

			LineType type = identifyLine(line);
			switch(type) {

			case UNKNOWN: 
				throw new IllegalArgumentException("the provided string file could not be parsed");

			case COMMENT: 
			case LABEL: 
			case WHITESPACE:
				continue;

			case AINSTRUCTION:
				System.out.println(line + " " + aInstruction(line));
				sb.append(aInstruction(line) + "\n");
				break;
			case CINSTRUCTION:
				System.out.println(line + " " + cInstruction(line));
				sb.append(cInstruction(line) + "\n"); 
				break;
			}
		}
		return sb.toString(); 
	}

	/**
	 * 
	 * @param line
	 * @return
	 */
	private LineType identifyLine(String line) {
		line = removeWhitespace(line); 
		if (line.length() == 0) 
			return LineType.WHITESPACE; 
		else if (commentPattern.matcher(line).matches())
			return LineType.COMMENT; 
		else if (aPattern.matcher(line).matches())
			return LineType.AINSTRUCTION; 
		else if (cPattern.matcher(line).matches())
			return LineType.CINSTRUCTION;
		else if (labelPattern.matcher(line).matches())
			return LineType.LABEL;
		else 
			return LineType.UNKNOWN; 
	}

	private void label(String labelLine, int lineNb) { 
		Matcher m = labelPattern.matcher(labelLine); 
		m.find(); 

		/* get the name of the label */ 
		String labelName = m.group(1);

		/* label can't exist already */ 
		if (labels.containsKey(labelName)) { 
			throw new IllegalArgumentException("Label was already used, pick another label name"); 
		}

		/* put the label name with the instruction line number in the table */ 
		labels.put(labelName, lineNb); 
	}

	private String aInstruction(String aLine) { 
		Matcher m = aPattern.matcher(aLine); 

		m.find();

		/* ------------------------------------------------------------------------ */ 
		/* group1 : int values */ 
		String number 	= m.group(1);
		if (number != null)
			return "0" + toBinary(Integer.parseInt(number), 15);
		/* ------------------------------------------------------------------------ */
		/* group2: word */ 
		/* get the word from the second group of the regex */ 
		String variable = m.group(2);


		/* check if the word is a label */ 
		if (labels.containsKey(variable)) { 
			int val = labels.get(variable);
			return "0" + toBinary(val, 15); 
		}

		/* predefined variable ? */ 
		if (predefined.containsKey(variable)) {
			int val = predefined.get(variable); 
			return "0" + toBinary(val, 15); 
		}

		/* new variable ? */ 
		if (!variables.containsKey(variable)) { 

			/* put the variable in the map */ 
			variables.put(variable, VARIABLE_START_ADDR + userVariableCount);

			/* increment the number of variables created by the user */ 
			userVariableCount++; 
		}			
		int val = variables.get(variable);
		return "0" + toBinary(val, 15); 
	}

	/**
	 * c-instr format: 
	 * 			1 1 1 a  c1 c2 c3 c4 c5 c6  d1 d2 d3  j1 j2 j3
	 * 
	 * @param cLine
	 */
	private String cInstruction(String cLine) {

		final int destLength = 3; 
		final int compLength = 6; 
		final int jumpLength = 3; 

		Matcher m = cPattern.matcher(cLine);
		m.find(); 

		/* 
		 * Step 1: get regex groups
		 * Step 2: for each group get the int from dict
		 * Step 3: convert the int to binary, and cut it to correct length
		 * Step 4: glue the bits together and return
		 */
		/* dest bit - before the equal sign */ 
		String dest = m.group(1); 

		/* comp - between equal and semicolon */ 
		String comp = m.group(2); 
		/* jump - after semicolon */ 
		String jump = m.group(3); 

		/* get a-bit */ 
		String aBit 	= (getABit(comp)) ? "1" : "0"; 

		/* if field is ommitted */ 
		if (jump == null) jump = ""; 
		if (dest == null) dest = ""; 

		/* convert to binary */ 	
		String destBits = toBinary(destMatcher.get(dest), destLength); 
		assert(destBits.length() == 3); 

		String compBits = toBinary(compMatcher.get(comp), compLength); 
		assert(compBits.length() == 6);

		String jumpBits = toBinary(jumpMatcher.get(jump), jumpLength);
		assert(jumpBits.length() == 3); 


		StringBuilder strBuilder = new StringBuilder("111");
		strBuilder.append(aBit); 
		strBuilder.append(compBits); 
		strBuilder.append(destBits); 
		strBuilder.append(jumpBits); 

		assert(strBuilder.toString().length() == 16); 

		return strBuilder.toString(); 
	}

	private String removeWhitespace(String str) { 
		return str.replaceAll("\\s", "");
	}

	/**
	 * format number to 16 bit with leading zeros if necessary 
	 * @param number
	 * @param n
	 * @return
	 */
	private String toBinary(int number, int n) {
		StringBuilder sb= new StringBuilder(Integer.toBinaryString(number));
		while (sb.length() < n)
			sb.insert(0, "0"); 

		return sb.toString(); 
	}

	/**
	 * gets the a-bit from the comp string.
	 * @param comp string containing the comp field (A-D, M-D D&A, D&M)
	 * @return true if the string passed contains M, false otherwise
	 */
	private boolean getABit(String comp) { 
		return comp.contains("M"); 
	}


}
