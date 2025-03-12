import de.uni_due.s3.jack.greqltools.languages.java.parser.Parser;
import de.uni_koblenz.jgralab.utilities.tg2dot.Tg2Dot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class ASTViewerGUI extends JFrame {
    private JTextField sourceField;
    private JTextField targetField;
    private JTextArea logArea;

    public ASTViewerGUI() {
        setTitle("AST Viewer");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));

        // Quellordner Auswahl
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourceField = new JTextField();
        JButton sourceButton = new JButton("Quellordner wählen");
        sourcePanel.add(sourceField, BorderLayout.CENTER);
        sourcePanel.add(sourceButton, BorderLayout.EAST);
        panel.add(sourcePanel);

        // Zielordner Auswahl
        JPanel targetPanel = new JPanel(new BorderLayout());
        targetField = new JTextField();
        JButton targetButton = new JButton("Zielordner wählen");
        targetPanel.add(targetField, BorderLayout.CENTER);
        targetPanel.add(targetButton, BorderLayout.EAST);
        panel.add(targetPanel);

        // Start-Button
        JButton startButton = new JButton("Parsen und SVG erzeugen");
        panel.add(startButton);

        // Log-Bereich
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane);

        add(panel);

        // Action Listener für die Buttons
        sourceButton.addActionListener(e -> chooseDirectory(sourceField));
        targetButton.addActionListener(e -> chooseDirectory(targetField));
        startButton.addActionListener(e -> startParsing());
    }

    private void chooseDirectory(JTextField textField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            textField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startParsing() {
        String sourcePath = sourceField.getText().trim();
        String targetPath = targetField.getText().trim();

        if (sourcePath.isEmpty() || targetPath.isEmpty()) {
            log("Bitte wähle sowohl einen Quell- als auch einen Zielordner aus.");
            return;
        }

        File sourceFolder = new File(sourcePath);
        File targetFolder = new File(targetPath);

        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            log("Ungültiger Quellordner!");
            return;
        }
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            log("Konnte den Zielordner nicht erstellen.");
            return;
        }

        log("Starte Parsing...");

        new Thread(() -> {
            try {
                Parser p = new Parser();
                p.parseFolder(sourceFolder);
                File xdotFile = new File(targetFolder, "ast.xdot");
                Tg2Dot.convertGraph(p.getGraph(), xdotFile.getAbsolutePath(), false);

                File svgFile = new File(targetFolder, "ast.svg");
                String command = "dot -Tsvg " + xdotFile.getAbsolutePath() + " -o " + svgFile.getAbsolutePath();

                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                boolean hasErrors = false;
                while ((line = errorReader.readLine()) != null) {
                    log("Graphviz Fehler: " + line);
                    hasErrors = true;
                }

                if (!hasErrors) {
                    log("SVG erfolgreich erstellt: " + svgFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log("Fehler: " + e.getMessage());
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ASTViewerGUI().setVisible(true));
    }
}
