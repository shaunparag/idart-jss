package org.celllife.idart.start;

import java.io.File;
import java.net.URL;

import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Standalone launcher for the iDART User Guide (the same JavaHelp helpset
 * shown by the in-app Help button), so it can also be reached directly from
 * the desktop/Start Menu "Documentation" shortcut without starting the full
 * application.
 */
public class HelpViewer {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ClassLoader cl = HelpViewer.class.getClassLoader();
					URL url = new URL((new File(".")).toURI().toURL(), "doc" + File.separator + "jhelpset.hs");
					JHelp helpViewer = new JHelp(new HelpSet(cl, url));

					JFrame frame = new JFrame("iDART User Guide");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.getContentPane().add(helpViewer);
					frame.setSize(900, 700);
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
	}

}
