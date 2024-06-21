package mdpa.gdpr.dfdconverter;

import java.io.File;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {

	public static void main(String[] args) {
		boolean running = true;
		Scanner scanner = new Scanner(System.in);
		while (running) {
			System.out.println("Enter file directory:");
			String dir = scanner.nextLine();
			System.out.println("Enter output directory:");
			String dirOut = scanner.nextLine();
			
			File file = new File(dir);
			File[] models = file.listFiles();
			Stream.of(models).parallel().forEach((m) -> {
				if (m.getAbsolutePath().endsWith("gdpr")) {
					String fileOut = (dirOut.endsWith("\\") ? dirOut : dirOut + "\\") + removeFileEnding(m);
					
					for (File model : models) {
						if (model.getName().endsWith(".dfd2gdpr.tracemodel")) {
							GDPR2DFD gdpr2dfd = new GDPR2DFD(m.getAbsolutePath(), fileOut + ".datadictionary", model.getAbsolutePath());
							gdpr2dfd.transform();		
							gdpr2dfd.save(fileOut + ".dataflowdiagram", fileOut + ".datadictionary", fileOut + ".gdpr2dfd.tracemodel");
							return;
						}
					}
					
					GDPR2DFD gdpr2dfd = new GDPR2DFD(m.getAbsolutePath());
					gdpr2dfd.transform();
					gdpr2dfd.save(fileOut + ".dataflowdiagram", fileOut + ".datadictionary", fileOut + ".gdpr2dfd.tracemodel");
				}
				
				else if (m.getAbsolutePath().endsWith("dataflowdiagram")) {
					String fileOut = (dirOut.endsWith("\\") ? dirOut : dirOut + "\\") + removeFileEnding(m);
					
					for (File model : models) {
						if (model.getName().equals(removeFileEnding(m) + ".datadictionary")) {
							DFD2GDPR dfd2gdpr = new DFD2GDPR(m.getAbsolutePath(), model.getAbsolutePath());
							dfd2gdpr.transform();
							dfd2gdpr.save(fileOut + ".gdpr", fileOut + ".dfd2gdpr.tracemodel");
							return;
						}
					}
					
				}
			});
			
			System.out.println("type exit to close or anything else to continue:");
			if (scanner.nextLine().equals("exit")) running = false;
		}
		scanner.close();
	}
	
	/**
	 * Removes file ending from file
	 * @param file File whichs ending need to be removed
	 * @return file path without file ending
	 */
	private static String removeFileEnding(File file) {
		String fileName = file.getName();
		if (fileName.indexOf(".") > 0) {
			   return fileName.substring(0, fileName.lastIndexOf("."));
			} else {
			   return fileName;
			}
	}	

}
