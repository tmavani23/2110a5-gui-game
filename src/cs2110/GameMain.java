package cs2110;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Main class for Click-a-Dot game. Creates window with game board, score label, start button, and
 * sliders for target size and speed.
 */
public class GameMain {

    /**
     * Start the application.
     */
    public static void main(String[] args) {
        // Creation of window must occur on Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    /**
     * Create application window.
     * <ul>
     * <li>Window title is "Click-a-Dot"
     * <li>Game board is in center of window, expands to fill window size
     * <li>Score label is at top; text is centered
     * <li>Start button is at bottom
     * <li>Size slider is at right
     * <li>Speed slider is at left
     * </ul>
     * Window should be disposed when closed, and all game tasks stopped. This should be sufficient
     * for application to shut down gracefully.
     */
    private static void createAndShowGUI() {
        // Create frame.
        JFrame frame = new JFrame("Click-a-Dot");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create and add game board.
        GameComponent game = new GameComponent();
        frame.add(game);

        // Create and add score label.
        JLabel scoreLabel = new JLabel("Score: " + game.getScore(),
                SwingConstants.CENTER);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(24.0f));

        // Adds score label to the top center of frame
        frame.add(scoreLabel, BorderLayout.PAGE_START);

        // Create and add start button.
        JButton startButton = new JButton("Start");
        startButton.setFont(startButton.getFont().deriveFont(20.0f));

        // Adds start button to the bottom of frame
        frame.add(startButton, BorderLayout.PAGE_END);

        // Create and add vertical size slider.
        // Allowed target radii are 1..50 (query game board for initial radius).
        JSlider sizeSlider = new JSlider(JSlider.VERTICAL, 1, 50,
                game.getTargetRadius());
        addSliderLabels(sizeSlider, "Small", "Large");
        // Place slider in panel with label and padding.
        frame.add(makeSliderPanel(sizeSlider, "Size"), BorderLayout.WEST);

        // Create and add vertical speed slider.
        // Allowed target durations are 250..2000 ms (query game board for
        // initial duration).
        JSlider speedSlider = new JSlider(JSlider.VERTICAL, 250, 2000,
                game.getTargetTimeMillis());
        addSliderLabels(speedSlider, "Fast", "Slow");
        speedSlider.setInverted(true);
        // Place slider in panel with label and padding.
        frame.add(makeSliderPanel(speedSlider, "Speed"), BorderLayout.EAST);

        // Add menu bar
        JMenuItem saveItem = new JMenuItem("Save score");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Creates new MenuBar and fileMenu objects
        JMenuBar fileMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu ("File");

        // Adds saveItem and exitItem to fileMenu
        fileMenu.add(saveItem);
        fileMenu.add(exitItem);

        // Adds fileMenu to MenuBar and sets it in the frame
        fileMenuBar.add(fileMenu);
        frame.setJMenuBar(fileMenuBar);

        ////////////////
        // Controller
        ////////////////

        // Action listener for when button is pressed. Will start game once pressed
        startButton.addActionListener(e -> {
            game.startGame();
        });

        // Property change listener for if score changes
        // Accordingly adjusts scoreLabel to new score
        game.addPropertyChangeListener("GameScore", e ->
                scoreLabel.setText("Score: " + game.getScore()));

        // Adds a slider for the size and accordingly adjusts dot size if changed by user
        sizeSlider.addChangeListener(evt -> {
            JSlider source = (JSlider) evt.getSource();
            int newRadius = source.getValue();
            game.setTargetRadius(newRadius);
        });

        // Adds a slider for the speed and accordingly adjusts dot speed if changed by user
        speedSlider.addChangeListener(evt -> {
            JSlider source = (JSlider) evt.getSource();
            int newDuration = source.getValue();
            game.setTargetTimeMillis(newDuration);
        });

        // When "Save" menu item is activated, open file dialog and append score
        // to chosen file.
        saveItem.addActionListener((ActionEvent ae) -> saveScore(frame, game.getScore()));

        // When "Exit" menu item is activated, dispose of the JFrame.
        exitItem.addActionListener((ActionEvent ae) -> frame.dispose());

        // Stop game when window is closed to ensure that game background tasks
        // do not hold up application shutdown.
        // Use an anonymous subclass of WindowAdapter to avoid having to handle
        // other window events.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                game.stopGame();
            }
        });

        // Compute ideal window size and show window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Label `slider`'s minimum value with `minLabel` and its maximum value with `maxLabel`.
     */
    private static void addSliderLabels(JSlider slider, String minLabel,
            String maxLabel) {
        Hashtable<Integer, JLabel> labels = new Hashtable<>();

        // Adds labels for the speed and size sliders and adds them to corresponding positions
        labels.put(slider.getMinimum(), new JLabel(minLabel));
        labels.put(slider.getMaximum(), new JLabel(maxLabel));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
    }

    /**
     * Place `slider` in a new padded panel with top label `title` and return the panel.
     */
    private static JComponent makeSliderPanel(JSlider slider, String title) {
        // Creates new panel with empty border of size 4 on all sides
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Creates new label for the slider and adds to panel
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(16.0f));
        panel.add(label, BorderLayout.PAGE_START);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Append a line containing `score` to a user-selected file, using `frame` as the parent of any
     * dialogs. Show an error dialog if a problem occurs when writing the file.
     */
    private static void saveScore(JFrame frame, int score) {
        // Checks if player wants to save score to files
        // Opens specified file
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (PrintWriter out =
                    new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
                // Appends score to score file
                out.println(score);
            } catch (IOException e) {
                // Handles exception `e` by displaying error message if exception is caught
                JOptionPane.showMessageDialog(frame, e.getMessage(),
                        e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
