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
			Stream.of(models).forEach((m) -> {
				if (m.getAbsolutePath().endsWith("gdpr")) {
					String fileOut = (dirOut.endsWith("\\") ? dirOut : dirOut + "\\") + removeFileEnding(m.getName());
					
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
					String fileOut = (dirOut.endsWith("\\") ? dirOut : dirOut + "\\") + removeFileEnding(m.getName());
					
					File ddFile = null;
					File tmFile = null;
					
					for (File model : models) {
						if (model.getName().equals(removeFileEnding(m.getName()) + ".datadictionary")) {							
							ddFile = model;
						} else if (model.getName().equals((removeFileEnding(m.getName())) + ".gdpr2dfd.tracemodel")) {
							tmFile = model;
						}
						if (ddFile != null && tmFile != null) break;
					}
					DFD2GDPR dfd2gdpr;
					if (tmFile != null) dfd2gdpr = new DFD2GDPR(m.getAbsolutePath(), ddFile.getAbsolutePath(), tmFile.getAbsolutePath());
					else dfd2gdpr = new DFD2GDPR(m.getAbsolutePath(), ddFile.getAbsolutePath());
					
					dfd2gdpr.transform();
					dfd2gdpr.save(fileOut + ".gdpr", fileOut + ".dfd2gdpr.tracemodel");
					return;
					
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
	private static String removeFileEnding(String fileName) {
		if (fileName.indexOf(".") > 0) {
			   return fileName.substring(0, fileName.lastIndexOf("."));
			} else {
			   return fileName;
			}
	}	

}
