import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {
        JFrame f = new JFrame("notepad+++");
        f.setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("tab 1", new TextWindow());

        InputMap inputMap = tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = tabs.getActionMap();

        // Key bindings
        inputMap.put(KeyStroke.getKeyStroke("control N"), "newTab");
        inputMap.put(KeyStroke.getKeyStroke("control W"), "closeTab");
        inputMap.put(KeyStroke.getKeyStroke("control O"), "openFile");
        inputMap.put(KeyStroke.getKeyStroke("control S"), "saveFile");
        inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "increaseFontSize");
        inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "decreaseFontSize");

        actionMap.put("newTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + N released");
                int newTabIndex = tabs.getTabCount() + 1;
                tabs.addTab("tab " + newTabIndex, new TextWindow());
                tabs.setSelectedIndex(newTabIndex - 1);
            }
        });

        actionMap.put("closeTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + W released");
                int selectedIndex = tabs.getSelectedIndex();
                if (selectedIndex != -1) {
                    tabs.removeTabAt(selectedIndex);
                }
                if (tabs.getTabCount() == 0) {
                    f.dispose();
                }
            }
        });

        actionMap.put("openFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + O released");
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(f);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        Component selectedTab = tabs.getSelectedComponent();
                        if (selectedTab instanceof TextWindow) {
                            if (((TextWindow) selectedTab).getText().isEmpty()) {
                                System.out.println("current file empty");
                                ((TextWindow) selectedTab).setText(content);
                            } else {
                                TextWindow newTab = new TextWindow();
                                newTab.setText(content);
                                newTab.setFile(file);
                                tabs.addTab("tab " + (tabs.getTabCount() + 1), newTab);
                            }
                        }
                        ((TextWindow) selectedTab).setFile(file);
                    } catch (Exception ex) {
                        System.out.println("ERR OPENING FILE");
                    }
                }
            }
        });

        actionMap.put("saveFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + S released");
                Component selectedTab = tabs.getSelectedComponent();
                if (selectedTab instanceof TextWindow) {
                    TextWindow textWindow = (TextWindow) selectedTab;
                    File file = textWindow.getFile();
                    if (file != null) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                            writer.write(textWindow.getText());
                        } catch (Exception ex) {
                            System.out.println("ERR SAVING FILE");
                        }
                    } else {
                        JFileChooser fileChooser = new JFileChooser();
                        int option = fileChooser.showSaveDialog(f);
                        if (option == JFileChooser.APPROVE_OPTION) {
                            file = fileChooser.getSelectedFile();
                            textWindow.setFile(file);
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                                writer.write(textWindow.getText());
                            } catch (Exception ex) {
                                System.out.println("ERR SAVING FILE");
                            }
                        }
                    }
                }
            }
        });

        actionMap.put("increaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + + released");
                Component selectedTab = tabs.getSelectedComponent();
                if (selectedTab instanceof TextWindow) {
                    ((TextWindow) selectedTab).changeFontSize(2);
                }
            }
        });

        actionMap.put("decreaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Ctrl + - released");
                Component selectedTab = tabs.getSelectedComponent();
                if (selectedTab instanceof TextWindow) {
                    ((TextWindow) selectedTab).changeFontSize(-2);
                }
            }
        });

        f.add(tabs);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class TextWindow extends JPanel {
    private JTextArea ta;
    private File file;
    private DefaultTableModel tableModel;
    private JLabel counter;

    public TextWindow() {
        this.setLayout(new BorderLayout(10, 10));
        ta = new JTextArea();
        ta.setFont(new Font("Monospaced", Font.PLAIN, 14));
        String[] columns = { "word", "count" };
        tableModel = new DefaultTableModel(columns, 0);
        JTable jt = new JTable(tableModel);
        JPanel table_panel = new JPanel();
        table_panel.setLayout(new BorderLayout());
        table_panel.add(new JLabel("Word Counts", JLabel.CENTER), BorderLayout.NORTH);
        table_panel.add(new JScrollPane(jt), BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, table_panel, new JScrollPane(ta));
        splitPane.setDividerLocation(150);
        splitPane.setResizeWeight(0.1);
        this.add(splitPane, BorderLayout.CENTER);
        counter = new JLabel("Words : 0 | Characters : 0");
        this.add(counter, BorderLayout.SOUTH);
        ta.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateTable();
            }
        });
    }

    public void changeFontSize(int delta) {
        Font currentFont = ta.getFont();
        int newSize = Math.max(currentFont.getSize() + delta, 8);
        ta.setFont(new Font(currentFont.getName(), currentFont.getStyle(), newSize));
    }

    public void updateTable() {
        String[] columns = { "word", "count" };
        String currentText = ta.getText();
        String tmp[] = currentText.trim().split("\\s+");
        counter.setText("Words : " + tmp.length + " | Character : " + currentText.length());
        String[][] data = new String[currentText.length()][2];
        int index = 0;
        for (String w : currentText.trim().split("\\s+")) {
            boolean skip = false;
            if (w.isEmpty()) {
                continue;
            }
            for (String[] t : data) {
                if (w.equals(t[0])) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                int count = 0;
                for (String w1 : currentText.trim().split("\\s+")) {
                    if (w.equals(w1)) {
                        count++;
                    }
                }
                data[index][0] = w;
                data[index][1] = String.valueOf(count);
                index++;
            }
        }
        tableModel.setRowCount(0);
        tableModel.setDataVector(data, columns);
        tableModel.setRowCount(index);
    }

    public String getText() {
        return ta.getText();
    }

    public void setText(String text) {
        ta.setText(text);
        updateTable();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}