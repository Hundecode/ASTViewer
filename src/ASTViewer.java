import de.uni_due.s3.jack.greqltools.languages.java.parser.Parser;
import de.uni_koblenz.jgralab.utilities.tg2dot.Tg2Dot;

import java.io.*;
import java.util.Scanner;

public class ASTViewer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Benutzer nach dem Quellordner fragen
        System.out.print("Gib den Pfad zum Quellordner ein: ");
        String sourcePath = scanner.nextLine();
        File sourceFolder = new File(sourcePath);

        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            System.err.println("Ungültiger Ordner: " + sourceFolder.getAbsolutePath());
            System.exit(1);
        }

        // Benutzer nach dem Zielordner fragen
        System.out.print("Gib den Pfad zum Zielordner ein: ");
        String targetPath = scanner.nextLine();
        File targetFolder = new File(targetPath);

        if (!targetFolder.exists()) {
            boolean created = targetFolder.mkdirs();
            if (!created) {
                System.err.println("Konnte den Zielordner nicht erstellen: " + targetFolder.getAbsolutePath());
                System.exit(1);
            }
        }

        try {
            // Parser initialisieren und den Quellcode-Ordner parsen
            Parser p = new Parser();
            p.parseFolder(sourceFolder);

            // DOT-Datei im Zielordner speichern
            File xdotFile = new File(targetFolder, "ast.xdot");
            Tg2Dot.convertGraph(p.getGraph(), xdotFile.getAbsolutePath(), false);

            // SVG-Datei im Zielordner speichern
            File svgFile = new File(targetFolder, "ast.svg");
            String command = "dot -Tsvg " + xdotFile.getAbsolutePath() + " -o " + svgFile.getAbsolutePath();

            // Process starten und Fehlerausgabe einlesen
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            // Fehleroutput von Graphviz auslesen, falls etwas schiefgeht
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            boolean hasErrors = false;
            while ((line = errorReader.readLine()) != null) {
                System.err.println("Graphviz Fehler: " + line);
                hasErrors = true;
            }

            if (hasErrors) {
                System.err.println("Es gab Fehler beim Erstellen der SVG-Datei. Überprüfe, ob Graphviz korrekt installiert ist.");
            } else {
                System.out.println("SVG-Datei erfolgreich erstellt: " + svgFile.getAbsolutePath());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}