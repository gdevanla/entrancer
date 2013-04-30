package entrancer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class JDTParser {
	
	public final String baseFolder;
	public final String fileContainingListOfClassToParse ;
	public final String outputFile;

	public void appendOutput(String s){
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(outputFile, true)));
			out.print(s);
			out.close();
		} catch (IOException e) {

		}
		// oh noes!
	}
	
	
	public JDTParser(String baseFolder, String inputFile, String outputFile){
		this.baseFolder = baseFolder;
		this.fileContainingListOfClassToParse = inputFile;
		this.outputFile = outputFile;
	}


	public void parse(String str) throws JavaModelException {

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(str.toCharArray());


		final CompilationUnit root = (CompilationUnit) parser.createAST(null);
		final Set<String> names = new HashSet<String>(); 
		
		root.accept(new ASTVisitor() {

			public boolean visit(ConstructorInvocation node) {
				IMethodBinding methodBinding = node.resolveConstructorBinding();
				String methodName = "";
				if (methodBinding != null) {
					StringBuilder str = new StringBuilder();
					String[] methodNameSplit = node.resolveConstructorBinding()
							.toString().split(" ");
					if (methodNameSplit[1].equalsIgnoreCase("abstract")
							|| methodNameSplit[1].equalsIgnoreCase("final")
							|| methodNameSplit[1].equalsIgnoreCase("native")
							|| methodNameSplit[1].equalsIgnoreCase("static")
							|| methodNameSplit[1]
									.equalsIgnoreCase("synchronized"))
						methodName = methodNameSplit[3];
					else
						methodName = methodNameSplit[2];
					Pattern pattern = Pattern
							.compile(".*(\\s\\w+)\\s*(\\()(.*)(\\))");
					Matcher matcher = pattern.matcher(node
							.resolveConstructorBinding().toString());

					StringBuilder combineMatches = new StringBuilder("");
					while (matcher.find()) {
						for (int i = 1; i <= matcher.groupCount(); i++) {
							combineMatches.append(matcher.group(i));
						}
					}
					String queryString = combineMatches.toString()
							.replaceFirst(" ", "");
					System.out.print(queryString + "\t");

					StringBuilder sbr = new StringBuilder("");
					String signature = (methodBinding.getDeclaringClass()
							.getQualifiedName() + "." + queryString.toString()
							.replace("java.lang.", ""));

					str.append(sbr + signature);
					appendOutput(str.toString());
				}
				return super.visit(node);
			}

			public boolean visit(VariableDeclarationFragment node) {
				SimpleName name = node.getName();
				names.add(name.getIdentifier());
				// System.out.println(name.propertyDescriptors(3).toString());
				return false; // do not continue to avoid usage info
			}

			public boolean visit(SimpleName node) {
				if (names.contains(node.getIdentifier())) {
					System.out.println(node + "\t");
				}

				appendOutput(" " + node + " ");

				return true;
			}

			public boolean visit(MethodInvocation node) {
				IMethodBinding methodBinding = node.resolveMethodBinding();

				if (methodBinding != null) {
					StringBuilder str = new StringBuilder();

					String methodName = "";
					String[] methodNameSplit = node.resolveMethodBinding()
							.toString().split(" ");
					if (methodNameSplit[1].equalsIgnoreCase("abstract")
							|| methodNameSplit[1].equalsIgnoreCase("final")
							|| methodNameSplit[1].equalsIgnoreCase("native")
							|| methodNameSplit[1].equalsIgnoreCase("abstract")
							|| methodNameSplit[1].equalsIgnoreCase("static")
							|| methodNameSplit[1].equalsIgnoreCase("abstract")
							|| methodNameSplit[1]
									.equalsIgnoreCase("synchronized"))
						methodName = methodNameSplit[3];
					else
						methodName = methodNameSplit[2];

					Pattern pattern = Pattern
							.compile(".*(\\s\\w+)\\s*(\\()(.*)(\\))");
					Matcher matcher = pattern.matcher(node
							.resolveMethodBinding().toString());

					StringBuilder combineMatches = new StringBuilder("");
					while (matcher.find()) {
						for (int i = 1; i <= matcher.groupCount(); i++) {
							combineMatches.append(matcher.group(i));
						}
					}
					String queryString = combineMatches.toString()
							.replaceFirst(" ", "");
					// System.out.print(queryString+"\t");

					StringBuilder sbr = new StringBuilder("");
					String signature = (methodBinding.getDeclaringClass()
							.getQualifiedName() + "." + queryString.toString()
							.replace("java.lang.", ""));

					str.append(sbr + signature);
					appendOutput(" " + str.toString() + " " );

				}

				return super.visit(node);
			}
		});
	}

	// read file content into a string
	public static String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			// System.out.println(numRead);
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}

		reader.close();

		return fileData.toString();
	}

	public String[] getFilesToParse(String fromFile) throws IOException{

		BufferedReader in
			= new BufferedReader(new FileReader(fromFile));

		List<String> files = new ArrayList<String>();
		String currentLine = null;
		while((currentLine = in.readLine()) != null){
			files.add(currentLine);
		}

		return  files.toArray(new String[files.size()]);
	}

	public static void main(String args[]) throws IOException,
	JavaModelException {
		System.out.println("Start generating the corpus.");

		if (args.length < 3){
			System.out.println("Usage: java JDTParser <base-path-to-find-all-source-files> <file-containing-names-of-source-file <output-file>");
			return;
		}
		
		System.out.println("Called with " + args[0] + ", " + args[1] + ", " + args[2]);
		JDTParser parser = new JDTParser(args[0], args[1], args[2]);
		parser.parseAll();
		System.out.println("Completed.");
	}


	private void parseAll() throws JavaModelException, IOException {
		String[] sourceFiles = getFilesToParse(fileContainingListOfClassToParse);
		
		for (String s:sourceFiles){
		    appendOutput("\n" + s + "\t");
		    parse(readFileToString(getFilePath(s)));
		}

	}

	private String getFilePath(String s) {
		return new File(baseFolder, s).getAbsolutePath();
	}
}