package org.sandag.abm.reporting.emfac2011;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.pb.sawdust.util.exceptions.RuntimeInterruptedException;

/**
 * The {@code HelpDialog} ...
 * 
 * @author crf Started 2/9/12 3:57 PM
 */
class RunEmfacDialog
        extends JPanel
        implements ActionListener
{
    private static final long     serialVersionUID = -3645537208340049132L;
    private final JButton         button;
    private final JLayeredPane    pane;
    private final Emfac2011Runner emfac2011Runner;

    public RunEmfacDialog(Path outputPath, Emfac2011Runner emfac2011Runner)
    {
        super(new GridBagLayout());

        StringBuilder sb = new StringBuilder();
        sb.append("\n   ")
                .append("Almost there! Press the button to start EMFAC2011, then follow these directions.")
                .append("\n\n");
        sb.append("   ")
                .append("EMFAC2011 should have booted up. If not, check that the installation directory is defined correctly in the properties file.")
                .append("\n\n");
        sb.append("   ").append("The following steps will take you through running the program.")
                .append("\n\n");
        sb.append("   ")
                .append("1) In the \"Regional Scenarios\" box, hit the \"Load Regional Scenarios (External Files)\" button.")
                .append("\n\n");
        sb.append("   ").append("2) Browse to and select: ").append(outputPath).append("\n\n");
        sb.append("   ")
                .append("3) The EMFAC2011-SG-Scenario Builder window should appear. Press the \"Save and Continue\" button.")
                .append("\n\n");
        sb.append("   ").append("4) A message box will appear. Click \"Yes\"").append("\n\n");
        sb.append("   ")
                .append("5) In the EMFAC2011-SG model window, hit the \"Verify Speed Data Quality\" button.")
                .append("\n\n");
        sb.append("   ")
                .append("6) If there are no errors, hit the \"Continue\" button in the \"Verify Speed Inputs\" window.")
                .append("\n\n");
        sb.append("   ")
                .append("7) In the EMFAC2011-SG model window, hit the \"Save Scenarios\" button.")
                .append("\n\n");
        sb.append("   ")
                .append("8) Select the same file that we loaded in step (2). Say \"Yes\" to the question about replacing the file.")
                .append("\n\n");
        sb.append("   ").append("9) EMFAC2011 will tell you it saved the input file. Click \"OK\"")
                .append("\n\n");
        sb.append("   ")
                .append("10) In the EMFAC2011-SG model window, hit the \"Execute Model\" button.")
                .append("\n\n");
        sb.append("   ")
                .append("11) In the EMFAC2011-SG-Model Execution Options window do the following")
                .append("\n");
        sb.append("   ")
                .append("\ta) In the \"Input Parameters\" box, the \"Export Default Input Parameters\" check box should NOT be checked.")
                .append("\n");
        sb.append("   ")
                .append("\tb) In the \"Model Outputs\" box, choose \"XLS\" as the output format, and check the \"Create Additional Summary Outputs\" \n\t     checkbox. Leave the \"Create Separate Output Files for Each Regional Scenario\" checkbox unchecked.")
                .append("\n");
        sb.append("   ").append("\tc) Hit the \"Start\" button.").append("\n\n");
        sb.append("   ")
                .append("12) The EMFAC2011 model should run, and then pop up a dialog box saying it finished. Click \"OK\"")
                .append("\n\n");
        sb.append("   ").append("13) Click the \"Exit EMFAC2011-SG\" button.").append("\n\n");
        sb.append("   ").append("14) All done! Close this window when you are finished.");

        JTextArea textArea = new JTextArea(40, 80);
        textArea.setEditable(false);
        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        pane = new JLayeredPane();
        scrollPane.setSize(850, 675);
        button = new JButton("Start  EMFAC2011");
        button.addActionListener(this);
        button.setLocation(500, 15);
        button.setSize(150, 20);
        pane.add(button, 0, -1);
        pane.add(scrollPane);
        add(pane);
        add(pane, c);

        this.emfac2011Runner = emfac2011Runner;
    }

    public static void createAndShowGUI(Path inputFile, Emfac2011Runner emfac2011Runner)
    {
        final Object lock = new Object();
        final JFrame frame = new JFrame("Run EMFAC2011");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RunEmfacDialog(inputFile, emfac2011Runner));
        frame.setSize(800, 700);
        frame.setVisible(true);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                synchronized (lock)
                {
                    while (frame.isVisible())
                    {
                        try
                        {
                            lock.wait();
                        } catch (InterruptedException e)
                        {
                            throw new RuntimeInterruptedException(e);
                        }
                    }
                }
            }
        };
        thread.start();

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent arg0)
            {
                synchronized (lock)
                {
                    frame.setVisible(false);
                    lock.notify();
                }
            }
        });
        try
        {
            thread.join();
        } catch (InterruptedException e)
        {
            throw new RuntimeInterruptedException(e);
        }

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        pane.remove(button);
        pane.repaint();
        emfac2011Runner.runEmfac2011Program();
    }
}
