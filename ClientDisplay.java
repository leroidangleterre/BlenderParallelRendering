package blenderparallelrendering;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 *
 * @author arthu
 */
public class ClientDisplay extends JFrame {

    JButton startStopButton;
    int margin = 20;

    boolean isActive;

    ClientConnectionThread connectionHandler;
    int port = 65432;

    String START_CLIENT = "Start client";
    String STOP_CLIENT = "Stop client";

    public ClientDisplay(String serverIP) {
        setTitle("Client GUI (server at IP " + serverIP + ")");
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        isActive = false;
        startStopButton = new JButton(START_CLIENT);
        startStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (isActive) {
                    // Stop client
                    System.out.println("Button STOP CLIENT");
                    isActive = false;
                    connectionHandler.flagStop();
                    startStopButton.setText(START_CLIENT);
                } else {
                    // Start client
                    System.out.println("Button START CLIENT");
                    connectionHandler = new ClientConnectionThread(serverIP, port);
                    isActive = true;
                    connectionHandler.start();
                    startStopButton.setText(STOP_CLIENT);
                }
            }
        });

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(margin, margin, margin, margin);
        add(startStopButton, c);
        setMinimumSize(new Dimension(600, 400));
        pack();
    }
}
